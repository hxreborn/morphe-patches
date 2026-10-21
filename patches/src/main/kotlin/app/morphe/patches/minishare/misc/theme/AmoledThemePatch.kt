/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.minishare.misc.theme

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.Document
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.png.PngImage
import app.morphe.util.adoptChild
import app.morphe.util.childElementsSequence
import app.morphe.util.doRecursively
import app.morphe.util.getNode
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import java.io.File
import org.w3c.dom.Element

private const val LIGHT_THEME = "@android:style/Theme.Light.NoTitleBar"
private const val DARK_THEME = "@android:style/Theme.Black.NoTitleBar"
private const val SPLASH_ICON_ATTRIBUTE = "r0x0101062d"
private const val SPLASH_ICON_BACKGROUND_ATTRIBUTE = "r0x01010630"
private const val SPLASH_LOGO = "splash_logo"
private const val SPLASH_ICON_DRAWABLE = "hx_amoled_splash_icon"
private const val SPLASH_ICON_INSET = "25%"

private const val BLACK = 0xff000000L.toInt()
private const val LINE_GREY = 0x33
private const val SURFACE_LIFT = 0x1c
private const val STATE_LIFT = 0x26
private const val PAGE_TONE_FLOOR = 0xe8
private const val LIGHT_THRESHOLD = 0x80
private const val NEAR_WHITE = 0xf0
private const val INK_FLOOR = 0x80
private const val NEUTRAL_SPREAD = 0x10
private const val MAX_LINE_DP = 4f
private const val COLOR_ALIAS_DEPTH = 4
private const val ADDED_COLOR_PREFIX = "hx_amoled_"

private val CODE_INK_COLORS = setOf("black_300", "black_700")
private const val DIALOG_LAYOUT_MARKER = "dialog"
private const val DIALOG_DRAWABLE_PREFIX = "hx_amoled_dialog_"
private const val NINE_PATCH_SUFFIX = ".9.png"
private const val OPAQUE_ALPHA_FLOOR = 0xc8
private const val LIGHT_NINE_PATCH_SHARE = 0.9

private const val GLYPH_ALPHA_FLOOR = 40
private const val GLYPH_SIZE_CEILING = 256
private const val GLYPH_LUMINANCE_CEILING = 110
private const val GLYPH_SATURATION_CEILING = 25
private const val GLYPH_TONE_CEILING = 8
private const val GLYPH_COVERAGE_CEILING = 0.9
private const val ALPHA_MASK = 0xff shl 24
private const val RGB_MASK = 0xffffff

private const val MODE_SWITCH_TRACK_DRAWABLE = "res/drawable/groupfirst_switch_track.xml"
private const val TRACK_CORNER_RADIUS = "50dp"
private const val PRESSED_TITLE_DRAWABLE = "res/drawable/titleview_pressed_bg.xml"

private val BRAND_COLORS = setOf(
    "@color/localfile_sharetip_bg_color",
    "@color/primary_color",
    "@color/titleview_color_1",
    "@color/titleview_color_2",
    "@color/statusbar_color_1",
    "@color/statusbar_color_2",
)

private const val WHITE = "#ffffffff"

private val FRAMEWORK_COLORS = mapOf(
    "@android:color/white" to WHITE,
    "@android:color/black" to "#ff000000",
)

private enum class Role { PAGE, SURFACE, STATE, INK, LINE }

private val INK_ATTRIBUTES = setOf(
    "android:textColor",
    "android:textColorHint",
    "android:textColorLink",
    "android:tint",
    "android:drawableTint",
)

private val GRADIENT_ATTRIBUTES = setOf("android:startColor", "android:centerColor", "android:endColor")

private val BACKGROUND_ATTRIBUTES = setOf(
    "android:background",
    "android:windowBackground",
    "android:popupBackground",
    "android:listSelector",
    "android:cacheColorHint",
)

private val RESOURCE_DIRECTORY_PREFIXES = listOf("layout", "drawable", "color")

private val LIBRARY_RESOURCE_PREFIXES = listOf(
    "abc_", "\$avd_", "avd_", "browser_actions_", "design_", "mtrl_", "notification_", "switch_thumb_material",
    "tooltip_", "upsdk_", "alertdialog.", "animation.", "base.", "cardview", "platform.", "rtloverlay",
    "rtlunderlay", "textappearance.", "theme.", "themeoverlay.", "widget.",
)

private fun isLibraryResource(name: String) = LIBRARY_RESOURCE_PREFIXES.any { name.lowercase().startsWith(it) }

private fun channelsOf(argb: Int) = listOf(argb shr 16 and 0xff, argb shr 8 and 0xff, argb and 0xff)

private fun luminance(argb: Int) = channelsOf(argb).sum() / 3

private fun isNeutral(argb: Int): Boolean {
    val channels = channelsOf(argb)
    return channels.max() - channels.min() <= NEUTRAL_SPREAD
}

private fun grey(alpha: Int, value: Int) = (alpha shl 24) or (value shl 16) or (value shl 8) or value

private fun recolor(argb: Int, role: Role): Int? {
    if (!isNeutral(argb)) return null

    val alpha = argb ushr 24
    val value = luminance(argb)

    return when (role) {
        Role.PAGE -> when {
            value >= PAGE_TONE_FLOOR -> grey(alpha, 0)
            value >= LIGHT_THRESHOLD -> grey(alpha, 0xff - value)
            else -> null
        }
        Role.SURFACE -> if (value >= LIGHT_THRESHOLD) grey(alpha, 0xff - value + SURFACE_LIFT) else null
        Role.STATE -> if (value >= LIGHT_THRESHOLD) grey(alpha, 0xff - value + STATE_LIFT) else null
        Role.INK -> if (value < NEAR_WHITE) grey(alpha, maxOf(0xff - value, INK_FLOOR)) else null
        Role.LINE -> if (value in LIGHT_THRESHOLD until NEAR_WHITE) grey(alpha, LINE_GREY) else null
    }
}

private fun parseColor(value: String): Int? {
    val digits = value.removePrefix("#").takeIf { value.startsWith("#") } ?: return null
    if (!digits.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null

    val argb = when (digits.length) {
        3 -> "ff" + digits.map { "$it$it" }.joinToString("")
        4 -> digits.map { "$it$it" }.joinToString("")
        6 -> "ff$digits"
        8 -> digits
        else -> return null
    }

    return argb.toLong(16).toInt()
}

private val DIMENSION_PATTERN = Regex("""^(-?[\d.]+)(dp|dip|px)$""")

private fun dimensionDp(value: String) = DIMENSION_PATTERN.find(value)?.groupValues?.get(1)?.toFloatOrNull()

private fun isLineSized(width: String?, height: String?) =
    listOfNotNull(width, height).mapNotNull(::dimensionDp).any { it > 0f && it <= MAX_LINE_DP }

private class Recolorer(private val palette: Map<String, String>) {
    val addedColors = sortedMapOf<String, String>()

    private fun resolve(value: String): Int? {
        var color = value.trim()

        repeat(COLOR_ALIAS_DEPTH) {
            FRAMEWORK_COLORS[color]?.let { return parseColor(it) }
            if (!color.startsWith("@color/")) return parseColor(color)
            color = palette[color.removePrefix("@color/")] ?: return null
        }

        return null
    }

    private fun reference(argb: Int): String {
        val hex = "%08x".format(argb)
        addedColors["$ADDED_COLOR_PREFIX$hex"] = "#$hex"
        return "@color/$ADDED_COLOR_PREFIX$hex"
    }

    fun whiteAs(role: Role): String = rewrite(WHITE, role, false)!!

    fun rewrite(value: String, role: Role, brandToBlack: Boolean): String? {
        if (brandToBlack && value in BRAND_COLORS) return reference(BLACK)

        return resolve(value)?.let { recolor(it, role) }?.let(::reference)
    }
}

private fun Element.attributeNodes() = (0 until attributes.length).map { attributes.item(it) }

private fun Element.isInStateItem(): Boolean =
    generateSequence(this) { it.parentNode as? Element }.any { element ->
        element.tagName == "item" && element.attributeNodes()
            .any { it.nodeName.startsWith("android:state_") && it.nodeValue == "true" }
    }

private fun Element.hasSiblingCorners() =
    (parentNode as? Element)?.childElementsSequence()?.any { it.tagName == "corners" } == true

private fun Element.surfaceOrPage() = if (hasSiblingCorners()) Role.SURFACE else Role.PAGE

private fun Element.roleOf(attribute: String, fileKind: String): Pair<Role, Boolean>? {
    val lineSized = isLineSized(getAttribute("android:layout_width"), getAttribute("android:layout_height"))

    return when {
        fileKind == "color" && attribute == "android:color" -> Role.INK to false
        attribute in INK_ATTRIBUTES -> Role.INK to false
        attribute in GRADIENT_ATTRIBUTES && isInStateItem() -> Role.STATE to false
        attribute in GRADIENT_ATTRIBUTES -> surfaceOrPage() to true
        tagName == "stroke" && attribute == "android:color" -> Role.INK to false
        tagName == "solid" && attribute == "android:color" && isInStateItem() -> Role.STATE to false
        tagName == "solid" && attribute == "android:color" -> surfaceOrPage() to false
        attribute == "android:drawable" -> (if (isInStateItem()) Role.STATE else Role.PAGE) to false
        attribute in BACKGROUND_ATTRIBUTES && lineSized -> Role.LINE to false
        attribute in BACKGROUND_ATTRIBUTES -> Role.PAGE to (fileKind == "layout")
        else -> null
    }
}

private fun PngImage.isDarkGlyph(): Boolean {
    if (maxOf(width, height) > GLYPH_SIZE_CEILING) return false

    val visible = argb.filter { it ushr 24 > GLYPH_ALPHA_FLOOR }
    if (visible.isEmpty() || visible.size > argb.size * GLYPH_COVERAGE_CEILING) return false

    val channels = visible.map(::channelsOf)
    val meanLuminance = channels.sumOf { it.sum() / 3 } / visible.size
    val meanSaturation = channels.sumOf { it.max() - it.min() } / visible.size
    val tones = channels.map { it.sum() / 3 shr 4 }.toSet().size

    return meanLuminance < GLYPH_LUMINANCE_CEILING &&
        meanSaturation < GLYPH_SATURATION_CEILING &&
        tones <= GLYPH_TONE_CEILING
}

private fun Document.readPalette(): Map<String, String> =
    getNode("resources").childElementsSequence()
        .filter { it.tagName == "color" }
        .associate { it.getAttribute("name") to it.textContent.trim() }

private fun ResourcePatchContext.resourceFiles(directoryPrefixes: List<String>, extension: String): List<File> =
    get("res").listFiles().orEmpty()
        .filter { directory -> directoryPrefixes.any { directory.name.startsWith(it) } }
        .sortedBy { it.name }
        .flatMap { directory -> directory.listFiles().orEmpty().sortedBy { it.name } }
        .filter { it.extension == extension && !isLibraryResource(it.name) }

private fun ResourcePatchContext.cloneDialogDrawables() {
    val drawables = get("res/drawable")
    val drawableReference = Regex("""@drawable/([a-z0-9_]+)"""")

    resourceFiles(listOf("layout"), "xml").filter { DIALOG_LAYOUT_MARKER in it.name }.forEach { layout ->
        val xml = layout.readText()
        val patched = xml.replace(drawableReference) { reference ->
            val name = reference.groupValues[1]
            val source = drawables.resolve("$name.xml").takeIf { it.exists() && !isLibraryResource(name) }
                ?: return@replace reference.value
            source.copyTo(drawables.resolve("$DIALOG_DRAWABLE_PREFIX$name.xml"), overwrite = true)
            "@drawable/$DIALOG_DRAWABLE_PREFIX$name\""
        }
        if (patched != xml) layout.writeText(patched)
    }
}

private fun ResourcePatchContext.recolorXmlAttributes(recolorer: Recolorer) {
    var rewritten = 0

    resourceFiles(RESOURCE_DIRECTORY_PREFIXES, "xml").forEach { file ->
        val kind = file.parentFile.name.substringBefore('-')
        val inDialog = (kind == "layout" && DIALOG_LAYOUT_MARKER in file.name) ||
            file.name.startsWith(DIALOG_DRAWABLE_PREFIX)

        document("res/${file.parentFile.name}/${file.name}").use { document ->
            document.documentElement.doRecursively { node ->
                val element = node as? Element ?: return@doRecursively
                element.attributeNodes().map { it.nodeName }.forEach { attribute ->
                    val (baseRole, brandToBlack) = element.roleOf(attribute, kind) ?: return@forEach
                    val role = if (inDialog && baseRole == Role.PAGE) Role.SURFACE else baseRole
                    recolorer.rewrite(element.getAttribute(attribute), role, brandToBlack)?.let {
                        element.setAttribute(attribute, it)
                        rewritten++
                    }
                }
            }
        }
    }

    if (rewritten == 0) throw PatchException("No layout or drawable color was rewritten")
}

private fun ResourcePatchContext.addSplashIcon() {
    val logoExists = get("res").listFiles().orEmpty()
        .filter { it.name.startsWith("mipmap") }
        .any { directory -> directory.listFiles().orEmpty().any { it.nameWithoutExtension == SPLASH_LOGO } }
    if (!logoExists) throw PatchException("No mipmap/$SPLASH_LOGO to show on the system splash screen")

    get("res/drawable/$SPLASH_ICON_DRAWABLE.xml").writeText(
        """
        <inset xmlns:android="http://schemas.android.com/apk/res/android"
            android:drawable="@mipmap/$SPLASH_LOGO"
            android:inset="$SPLASH_ICON_INSET" />
        """.trimIndent(),
    )
}

private fun ResourcePatchContext.recolorStyles(recolorer: Recolorer) {
    var rewrittenItems = 0
    var retargetedThemes = 0

    document("res/values/styles.xml").use { document ->
        document.getNode("resources").childElementsSequence().filter { it.tagName == "style" }.forEach { style ->
            if (isLibraryResource(style.getAttribute("name"))) return@forEach

            if (style.getAttribute("parent") == LIGHT_THEME) {
                style.setAttribute("parent", DARK_THEME)
                style.adoptChild("item") {
                    setAttribute("name", SPLASH_ICON_ATTRIBUTE)
                    textContent = "@drawable/$SPLASH_ICON_DRAWABLE"
                }
                style.adoptChild("item") {
                    setAttribute("name", SPLASH_ICON_BACKGROUND_ATTRIBUTE)
                    textContent = recolorer.whiteAs(Role.PAGE)
                }
                retargetedThemes++
            }

            val items = style.childElementsSequence().filter { it.tagName == "item" }
                .associateBy { it.getAttribute("name") }
            val lineSized = isLineSized(
                items["android:layout_width"]?.textContent,
                items["android:layout_height"]?.textContent,
            )

            items.forEach { (name, item) ->
                val (role, brandToBlack) = when (name) {
                    in INK_ATTRIBUTES -> Role.INK to false
                    in BACKGROUND_ATTRIBUTES -> (if (lineSized) Role.LINE else Role.PAGE) to !lineSized
                    else -> return@forEach
                }
                recolorer.rewrite(item.textContent, role, brandToBlack)?.let {
                    item.textContent = it
                    rewrittenItems++
                }
            }
        }
    }

    if (retargetedThemes == 0) throw PatchException("No style extends $LIGHT_THEME")
    if (rewrittenItems == 0) throw PatchException("No style color was rewritten")
}

private fun ResourcePatchContext.recolorPressedTitle(recolorer: Recolorer) {
    document(PRESSED_TITLE_DRAWABLE).use { document ->
        val gradient = document.getElementsByTagName("gradient").item(0) as? Element
            ?: throw PatchException("$PRESSED_TITLE_DRAWABLE has no gradient")
        val pressed = recolorer.whiteAs(Role.STATE)
        GRADIENT_ATTRIBUTES.filter(gradient::hasAttribute).forEach { gradient.setAttribute(it, pressed) }
    }
}

private fun ResourcePatchContext.addModeSwitchTrackSurface(recolorer: Recolorer) {
    document(MODE_SWITCH_TRACK_DRAWABLE).use { document ->
        val surface = recolorer.whiteAs(Role.SURFACE)
        val items = document.documentElement.childElementsSequence()
            .filter { it.hasAttribute("android:drawable") }
            .toList()
        if (items.isEmpty()) throw PatchException("$MODE_SWITCH_TRACK_DRAWABLE has no track states")

        items.forEach { item ->
            val track = item.getAttribute("android:drawable")
            item.removeAttribute("android:drawable")
            item.adoptChild("layer-list") {
                adoptChild("item") {
                    adoptChild("shape") {
                        adoptChild("solid") { setAttribute("android:color", surface) }
                        adoptChild("corners") { setAttribute("android:radius", TRACK_CORNER_RADIUS) }
                    }
                }
                adoptChild("item") { setAttribute("android:drawable", track) }
            }
        }
    }
}

private fun ResourcePatchContext.recolorLightNinePatches() {
    val recolored = resourceFiles(listOf("drawable"), "png")
        .filter { it.name.endsWith(NINE_PATCH_SUFFIX) }
        .count { file ->
            val image = PngImage.read(file)
            val opaque = image.argb.filter { it ushr 24 >= OPAQUE_ALPHA_FLOOR }
            val light = opaque.count { recolor(it, Role.SURFACE) != null }
            if (opaque.isEmpty() || light < opaque.size * LIGHT_NINE_PATCH_SHARE) return@count false

            image.argb.indices.forEach { index ->
                recolor(image.argb[index], Role.SURFACE)?.let { image.argb[index] = it }
            }
            image.write(file)
            true
        }

    if (recolored == 0) throw PatchException("No light nine-patch was recolored")
}

private fun ResourcePatchContext.invertDarkGlyphs() {
    val inverted = resourceFiles(listOf("drawable", "mipmap"), "png")
        .filterNot { it.name.endsWith(NINE_PATCH_SUFFIX) }
        .count { file ->
            val image = PngImage.read(file)
            if (!image.isDarkGlyph()) return@count false

            image.argb.indices.forEach { index ->
                val pixel = image.argb[index]
                image.argb[index] = (pixel and ALPHA_MASK) or (pixel.inv() and RGB_MASK)
            }
            image.write(file)
            true
        }

    if (inverted == 0) throw PatchException("No dark glyph icon was inverted")
}

private fun ResourcePatchContext.rewriteColorsXml(recolorer: Recolorer) {
    val xmlReferencingCodeInk = get("res").walk()
        .filter { it.extension == "xml" && it.name != "colors.xml" }
        .filter { file ->
            val xml = file.readText()
            CODE_INK_COLORS.any { "@color/$it\"" in xml || "@color/$it<" in xml }
        }
        .toList()
    if (xmlReferencingCodeInk.isNotEmpty()) {
        throw PatchException("$CODE_INK_COLORS still referenced from XML: $xmlReferencingCodeInk")
    }

    document("res/values/colors.xml").use { document ->
        val resources = document.getNode("resources")
        resources.childElementsSequence().filter { it.getAttribute("name") in CODE_INK_COLORS }.forEach { color ->
            color.textContent = recolorer.rewrite(color.textContent, Role.INK, false)
                ?.let { recolorer.addedColors.getValue(it.removePrefix("@color/")) }
                ?: throw PatchException("${color.getAttribute("name")} is not a neutral ink color")
        }
        recolorer.addedColors.forEach { (name, value) ->
            resources.adoptChild("color") {
                setAttribute("name", name)
                textContent = value
            }
        }
    }
}

private val amoledThemeResourcesPatch = resourcePatch {
    execute {
        val recolorer = Recolorer(document("res/values/colors.xml").use { it.readPalette() })

        cloneDialogDrawables()
        recolorXmlAttributes(recolorer)
        addSplashIcon()
        recolorStyles(recolorer)
        recolorPressedTitle(recolorer)
        addModeSwitchTrackSurface(recolorer)
        recolorLightNinePatches()
        invertDarkGlyphs()
        rewriteColorsXml(recolorer)
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the light theme with a pure black dark theme.",
) {
    compatibleWith(AppCompatibilities.MINISHARE)

    dependsOn(amoledThemeResourcesPatch)

    execute {
        HomeBackgroundPaintFingerprint.matchSingle().let { match ->
            val setColorIndex = match.instructionMatches.last().index
            val colorRegister = match.method.getInstruction<FiveRegisterInstruction>(setColorIndex).registerD
            match.method.addInstruction(setColorIndex, "const v$colorRegister, $BLACK")
        }

        StatusBarTintFingerprint.matchSingle().clearTranslucentStatusFlag()
        SplashTranslucentStatusFingerprint.matchSingle().clearTranslucentStatusFlag()

        BrandHeaderBackgroundFingerprint.matchAll(2..2).forEach { it.replaceColorString(BLACK) }
        val labelColor = recolor(parseColor(STOCK_LABEL_COLOR)!!, Role.INK)!!
        RadarLabelColorFingerprint.matchSingle().replaceColorString(labelColor)
    }
}

private fun Match.clearTranslucentStatusFlag() {
    val flag = instructionMatches.first {
        (it.instruction as? WideLiteralInstruction)?.wideLiteral == FLAG_TRANSLUCENT_STATUS.toLong()
    }
    val register = (flag.instruction as OneRegisterInstruction).registerA
    method.replaceInstruction(flag.index, "const/16 v$register, 0x0")
}

private fun Match.replaceColorString(argb: Int) {
    val index = instructionMatches.first().index
    val register = method.getInstruction<OneRegisterInstruction>(index).registerA
    method.replaceInstruction(index, "const-string v$register, \"#%08x\"".format(argb))
}
