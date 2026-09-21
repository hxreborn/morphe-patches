/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.Document
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.hasResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.png.PngImage
import app.morphe.util.adoptChild
import app.morphe.util.childElementsSequence
import app.morphe.util.doRecursively
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getNode
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import java.io.File
import org.w3c.dom.Element

private const val APP_COMPAT_DELEGATE_CLASS = "Landroidx/appcompat/app/AppCompatDelegate;"
private const val WEB_SETTINGS_CLASS = "Landroid/webkit/WebSettings;"
private const val MODE_NIGHT_YES = 2
private val WEB_VIEW_SETTINGS_SITES = 4..4
private val HOME_HEADER_TINT_SITES = 2..2
private const val ALGORITHMIC_DARKENING_MIN_SDK = 33
private const val HOME_BACKGROUND_COLOR = "#000000"
private const val OPAQUE_WHITE_ARGB = "-0x1"

private const val SURFACE_COLOR = "@color/hx_surface"
private const val PAGE_COLOR = "@color/hx_page"
private const val ON_SURFACE_COLOR = "@color/hx_on_surface"
private const val BACK_ICON_NAME = "color_actionbar_back_white"
private const val OPAQUE_BLACK = "#ff000000"

private const val HOME_BANNER_DRAWABLE = "res/drawable/ic_home_bg_default_v2.xml"
private const val MIN_SDK_ATTRIBUTE = "android:minSdkVersion"

private val ADDED_COLORS = mapOf(
    "hx_on_surface" to "#ffffff",
    "hx_page" to "#fafafa",
    "hx_surface" to "#ffffff",
)

private val NIGHT_COLORS = mapOf(
    "hx_page" to "#000000",
    "hx_surface" to "#1a1a1a",
    "white" to "#1a1a1a",
    "ac_color_global_bg" to "#000000",
    "ac_ui_color_FAFAFA" to "#000000",
    "activity_bg_white" to "#000000",
    "appshell_tab_bg" to "#000000",
    "background_device_panel" to "#000000",
    "background_floating_material_dark" to "#1a1a1a",
    "background_main" to "#000000",
    "background_material_dark" to "#000000",
    "browser_actions_bg_grey" to "#000000",
    "c_F7F7F7" to "#000000",
    "cardview_dark_background" to "#1a1a1a",
    "color_F2F2F6" to "#000000",
    "color_F4F4F4" to "#000000",
    "color_F5F5F5" to "#000000",
    "color_FAFAFA" to "#000000",
    "color_FFFAFAFA" to "#000000",
    "color_f8f8f8" to "#000000",
    "color_f9f9f9" to "#000000",
    "color_fafafa" to "#000000",
    "color_setting_userinfo_FAFAFA" to "#000000",
    "color_theme_bg" to "#000000",
    "country_ac_os12_background" to "#000000",
    "design_dark_default_color_background" to "#000000",
    "design_dark_default_color_surface" to "#1a1a1a",
    "feedback_f5f6" to "#000000",
    "home_detail_week_color" to "#000000",
    "material_grey_100" to "#000000",
    "material_grey_50" to "#000000",
    "material_grey_800" to "#1a1a1a",
    "mine_item_bg" to "#000000",
    "nx_toolbar_background" to "#000000",
    "pb_bg" to "#000000",
    "scan_public_bg" to "#000000",
    "sport_indoor_pb_bg_color" to "#000000",
    "ty_web_view_scroll_color" to "#000000",
    "uispecs_view_background" to "#000000",
    "wave_ring_bg" to "#000000",
    "wbcf_custom_dialog_bg" to "#000000",
    "windowbackground" to "#000000",
    "background_device_panel_mask" to "#cc000000",
    "device_spilt_line_color" to "#33ffffff",
    "color_F5F6F9" to "#000000",
    "color_F7F7F7" to "#000000",
    "device_bg" to "#000000",
    "lib_common_bg" to "#000000",
    "realme_common_gray2_F5F5F5" to "#000000",
    "bg_sign_dialog_gray" to "#1a1a1a",
    "col_white" to "#1a1a1a",
    "nx_color_white" to "#1a1a1a",
    "white_alpha80" to "#1a1a1a",
    "lib_common_status_color" to "#1a1a1a",
    "color_EEEEEE" to "#33ffffff",
    "color_line_bg_crude" to "#33ffffff",
    "driver_color" to "#33ffffff",
    "lib_common_line_color" to "#33ffffff",
    "line" to "#33ffffff",
    "line_color" to "#33ffffff",
    "divider_color" to "#25ffffff",
    "fenge_line_color" to "#25ffffff",
    "black" to "#ffffff",
    "black_full" to "#ffffff",
    "colorDark" to "#ffffff",
    "color_22242C" to "#ffffff",
    "color_333333" to "#ffffff",
    "normal_font_color" to "#ffffff",
    "nx_preference_category_focus" to "#ffffff",
    "nx_searchview_cancel_text_color" to "#ffffff",
    "realme_common_black_000000" to "#ffffff",
    "run_gps_text_color" to "#ffffff",
    "searchbox_text_color2" to "#ffffff",
    "sp_device_item_title_color" to "#ffffff",
    "text_black" to "#ffffff",
    "ty_base_ui_dialog_text_default" to "#ffffff",
    "color_666666" to "#b3b3b3",
    "color_text_darkgray" to "#b3b3b3",
    "color_text_gray" to "#b3b3b3",
    "color_737373" to "#b3b3b3",
    "text_color" to "#b3b3b3",
    "ty_base_ui_default_number_color" to "#b3b3b3",
    "account_color_4D000000" to "#4dffffff",
    "color_4C000000" to "#4cffffff",
    "color_4D000000" to "#4dffffff",
    "color_8C000000" to "#8cffffff",
    "color_D9000000" to "#d9ffffff",
    "color_d9000000" to "#d9ffffff",
    "color_E5000000" to "#e5ffffff",
    "month_calendar_title_color" to "#70ffffff",
    "normal_tip_color" to "#77ffffff",
    "sp_device_item_value_color" to "#80ffffff",
    "sp_device_tip_color" to "#80ffffff",
    "nx_color_shape_btn_check_fill_color_off_normal" to "#4dffffff",
    "nx_shape_btn_check_fill_color_off_normal" to "#4dffffff",
    "nx_toolbar_title_text_color" to "#e6ffffff",
    "usercenter_ui_color_85_000000" to "#d9ffffff",
    "nx_hint_text_color" to "#66ffffff",
    "text_grey" to "#66ffffff",
)

private const val HEADSET_PACKAGE = "com.realme.link.realmeHeadset"

private val HEADSET_COLORS = mapOf(
    "color_F2F2F2" to "#000000",
    "color_F3F4F6" to "#000000",
    "color_bg_grey" to "#000000",
    "headset_ffecd" to "#000000",
    "color_bg_white" to "#1a1a1a",
    "headset_white" to "#1a1a1a",
    "color_black" to "#ffffff",
    "color_111111" to "#ffffff",
    "headset_color_333" to "#ffffff",
    "normal_font_color" to "#ffffff",
    "text_black" to "#ffffff",
    "text_black_2" to "#ffffff",
    "color_4D4D4D" to "#b3b3b3",
    "color_conn_drawable" to "#b3b3b3",
    "color_0A000000" to "#0affffff",
    "color_0D000000" to "#0dffffff",
    "color_1F000000" to "#1fffffff",
    "color_4C000000" to "#4cffffff",
    "color_66333333" to "#66ffffff",
    "color_89000000" to "#89ffffff",
    "color_cc000000" to "#ccffffff",
    "color_D8000000" to "#d8ffffff",
    "color_D9000000" to "#d9ffffff",
    "color_check_box_grey" to "#33ffffff",
    "headset_bg_grey" to "#1effffff",
    "text_grey" to "#66ffffff",
)

private val SURFACE_VALUES = setOf(
    "@android:color/white",
    "@color/white",
    "#fff",
    "#ffffff",
    "#ffffffff",
)

private val PAGE_VALUES = setOf(
    "#f4f4f4",
    "#fafafa",
    "#fff4f4f4",
    "#fffafafa",
)

private val LAYOUT_ATTRIBUTES = listOf(
    "android:background",
    "android:backgroundTint",
    "app:cardBackgroundColor",
)

private val COLOR_ATTRIBUTES = listOf("android:color")

private val GRADIENT_ATTRIBUTES = listOf(
    "android:centerColor",
    "android:endColor",
    "android:startColor",
)

private val ON_SURFACE_ATTRIBUTES = listOf(
    "android:drawableTint",
    "android:fillColor",
    "android:strokeColor",
    "android:textColor",
    "android:textColorHint",
    "android:textColorLink",
    "android:tint",
    "app:itemIconTint",
    "app:itemTextColor",
    "app:tabTextColor",
    "app:tint",
    "app:titleTextColor",
)

private val CHROME_ATTRIBUTES = listOf(
    "android:theme",
    "app:navigationIcon",
    "app:popupTheme",
)

private val CHROME_REPLACEMENTS = mapOf(
    "@mipmap/cancel" to "@drawable/color_actionbar_back_white",
    "@style/ThemeOverlay.AppCompat.ActionBar" to "@style/ThemeOverlay.AppCompat.DayNight.ActionBar",
    "@style/ThemeOverlay.AppCompat.Light" to "@style/ThemeOverlay.AppCompat.DayNight",
)

private val ICON_ATTRIBUTES = listOf(
    "android:centerColor",
    "android:color",
    "android:endColor",
    "android:fillColor",
    "android:startColor",
    "android:strokeColor",
)

private val BLACK_VALUES = setOf("#000", "#000000", "#ff000000")

private val TRANSPARENT_VALUES = setOf("#0000", "#00000000")

private const val STYLE_REFERENCE = "style/"
private const val LIGHT_THEME_INFIX = ".Light"
private const val DAY_NIGHT_THEME_INFIX = ".DayNight"

private val LIGHT_THEME_NAMES = listOf("Theme.AppCompat.Light", "Theme.MaterialComponents.Light")

private val STYLE_BACKGROUND_ATTRIBUTES = setOf(
    "android:background",
    "android:windowBackground",
)

private val STYLE_ON_SURFACE_ATTRIBUTES = setOf(
    "android:textColor",
    "android:textColorHint",
)

private val STYLE_CARD_ATTRIBUTES = setOf("cardBackgroundColor")

private val CARD_REPLACEMENTS = mapOf(
    "?android:attr/colorBackgroundFloating" to SURFACE_COLOR,
)

private val SURFACE_REPLACEMENTS = SURFACE_VALUES.associateWith { SURFACE_COLOR } +
    PAGE_VALUES.associateWith { PAGE_COLOR }

private val LAYOUT_REPLACEMENTS = SURFACE_REPLACEMENTS + mapOf("@color/black" to OPAQUE_BLACK)

private val ON_SURFACE_REPLACEMENTS = SURFACE_VALUES.associateWith { ON_SURFACE_COLOR }

private const val BLACK_COLOR = "@color/black"

private val LAYOUT_ON_SURFACE_REPLACEMENTS = ON_SURFACE_REPLACEMENTS +
    BLACK_VALUES.associateWith { BLACK_COLOR }

private const val COLOR_REFERENCE = "@color/"

private fun inkAlpha(value: String): String? {
    val color = value.lowercase()
    if (color.length != 9 || !color.startsWith("#") || !color.endsWith("000000")) return null

    val alpha = color.substring(1, 3)
    if (alpha == "ff" || alpha == "00") return null
    if (!alpha.all { it in "0123456789abcdef" }) return null

    return alpha
}

private fun Document.readColors(): Map<String, String> =
    getNode("resources").childElementsSequence()
        .filter { it.tagName == "color" }
        .associate { it.getAttribute("name") to it.textContent.trim() }

private val TEXT_ATTRIBUTES = setOf(
    "android:textColor",
    "android:textColorHint",
    "android:textColorLink",
    "app:itemTextColor",
    "app:tabTextColor",
    "app:titleTextColor",
)

private const val MIN_TEXT_INK_ALPHA = 0x80

private const val COLOR_ALIAS_DEPTH = 4

private fun Map<String, String>.resolveColor(value: String): String? {
    var color = value

    repeat(COLOR_ALIAS_DEPTH) {
        if (!color.startsWith(COLOR_REFERENCE)) return color
        color = this[color.removePrefix(COLOR_REFERENCE)] ?: return null
    }

    return null
}

private fun invertedInk(palette: Map<String, String>, attribute: String, value: String): String? {
    val color = palette.resolveColor(value) ?: return null

    val alpha = inkAlpha(color)?.toInt(16) ?: return null
    val readable = if (attribute in TEXT_ATTRIBUTES) maxOf(alpha, MIN_TEXT_INK_ALPHA) else alpha

    return "#%02xffffff".format(readable)
}

private val HAIRLINE_SIZES = setOf("1px", "2px", "0.5dp", "1dp")

private fun Element.isHairline() =
    getAttribute("android:layout_width") in HAIRLINE_SIZES ||
        getAttribute("android:layout_height") in HAIRLINE_SIZES

private const val NEUTRAL_CHANNEL_FLOOR = 0xe0
private const val NEUTRAL_CHANNEL_SPREAD = 0x10

private fun lightNeutralSurface(value: String): String? {
    val digits = value.removePrefix("#").lowercase()
    if (!digits.all { it in "0123456789abcdef" }) return null

    val channels = when (digits.length) {
        3 -> digits.map { "$it$it" }
        6 -> digits.chunked(2)
        8 -> if (digits.take(2) != "ff") return null else digits.chunked(2).drop(1)
        else -> return null
    }.map { it.toInt(16) }

    val darkest = channels.min()
    if (darkest < NEUTRAL_CHANNEL_FLOOR) return null
    if (channels.max() - darkest > NEUTRAL_CHANNEL_SPREAD) return null

    return SURFACE_COLOR
}

private fun Document.setColors(colors: Map<String, String>) {
    val resources = getNode("resources")
    val declared = resources.childElementsSequence().associateBy { it.getAttribute("name") }

    colors.forEach { (name, value) ->
        val color = declared[name]

        if (color != null) {
            color.textContent = value
        } else {
            resources.adoptChild("color") {
                setAttribute("name", name)
                textContent = value
            }
        }
    }
}

private fun ResourcePatchContext.resourceFiles(
    extension: String,
    directoryFilter: (String) -> Boolean,
): List<File> =
    get("res").listFiles()
        ?.filter { it.isDirectory && directoryFilter(it.name) }
        ?.flatMap { directory -> directory.listFiles()?.asList().orEmpty() }
        ?.filter { it.extension == extension }
        .orEmpty()

private fun ResourcePatchContext.dayResourceFiles(directoryPrefix: String): List<File> =
    resourceFiles("xml") { it.startsWith(directoryPrefix) && !it.contains("night") }

private fun ResourcePatchContext.dayStyleFiles(): List<File> =
    dayResourceFiles("values").filter { it.name == "styles.xml" }

private fun ResourcePatchContext.iconImageFiles(): List<File> =
    resourceFiles("png") { it.startsWith("mipmap") || it.startsWith("drawable") }
        .filterNot { it.name.endsWith(NINE_PATCH_SUFFIX) }

private val File.resourcePath: String
    get() = "res/${parentFile.name}/$name"

private fun ResourcePatchContext.editDocuments(files: List<File>, edit: Document.() -> Int): Int =
    files.sumOf { file -> document(file.resourcePath).use { it.edit() } }

private fun File.declaresAnyOf(attributes: List<String>): Boolean =
    readText().let { text -> attributes.any { text.contains("$it=\"") } }

private fun ResourcePatchContext.replaceAttributes(
    directoryPrefix: String,
    attributes: List<String>,
    replacements: Map<String, String>,
    unlisted: (Element, String, String) -> String? = { _, _, _ -> null },
): Int = editDocuments(dayResourceFiles(directoryPrefix).filter { it.declaresAnyOf(attributes) }) {
    var replaced = 0

    documentElement.doRecursively { node ->
        val element = node as? Element ?: return@doRecursively

        attributes.forEach { attribute ->
            val value = element.getAttribute(attribute)
            if (value.isEmpty()) return@forEach

            val replacement = replacements[value]
                ?: unlisted(element, attribute, value)
                ?: return@forEach

            element.setAttribute(attribute, replacement)
            replaced++
        }
    }

    replaced
}

private fun dayNightTheme(parent: String): String? {
    val nameStart = parent.indexOf(STYLE_REFERENCE)
    if (nameStart < 0) return null

    val prefixLength = nameStart + STYLE_REFERENCE.length
    val name = parent.substring(prefixLength)
    if (LIGHT_THEME_NAMES.none { name.startsWith(it) }) return null

    val dayNight = name.replaceFirst(LIGHT_THEME_INFIX, DAY_NIGHT_THEME_INFIX)
    if (!hasResourceId(ResourceType.STYLE, dayNight)) return null

    return parent.take(prefixLength) + dayNight
}

private fun Document.useDayNightThemes(): Int {
    var replaced = 0

    getNode("resources").childElementsSequence().forEach { style ->
        val dayNight = dayNightTheme(style.getAttribute("parent")) ?: return@forEach

        style.setAttribute("parent", dayNight)
        replaced++
    }

    return replaced
}

private fun Document.invertIfMonochrome(): Int {
    val blackAttributes = mutableListOf<Pair<Element, String>>()
    var monochrome = true

    documentElement.doRecursively { node ->
        val element = node as? Element ?: return@doRecursively

        ICON_ATTRIBUTES.forEach { attribute ->
            val value = element.getAttribute(attribute).lowercase()

            when {
                value.isEmpty() || value in TRANSPARENT_VALUES -> Unit
                value in BLACK_VALUES -> blackAttributes += element to attribute
                else -> monochrome = false
            }
        }
    }

    if (!monochrome) return 0

    blackAttributes.forEach { (element, attribute) -> element.setAttribute(attribute, ON_SURFACE_COLOR) }

    return blackAttributes.size
}

private fun ResourcePatchContext.invertMonochromeVectors(): Int {
    val blackVectors = dayResourceFiles("drawable").filter { file ->
        val text = file.readText()

        text.contains("<vector") && BLACK_VALUES.any { text.contains("\"$it\"") }
    }

    return editDocuments(blackVectors) { invertIfMonochrome() }
}

private fun ResourcePatchContext.replaceStyleItems(
    attributes: Set<String>,
    replacements: Map<String, String>,
): Int = editDocuments(dayStyleFiles()) {
    var replaced = 0

    documentElement.doRecursively { node ->
        val item = node as? Element ?: return@doRecursively
        if (item.tagName != "item" || item.getAttribute("name") !in attributes) return@doRecursively

        val replacement = replacements[item.textContent] ?: return@doRecursively

        item.textContent = replacement
        replaced++
    }

    replaced
}

private const val NINE_PATCH_SUFFIX = ".9.png"
private const val GLYPH_ALPHA_FLOOR = 40
private const val GLYPH_LUMINANCE_CEILING = 90
private const val GLYPH_SATURATION_CEILING = 18
private const val GLYPH_TONE_CEILING = 8
private const val GLYPH_SIZE_CEILING = 256
private const val OPAQUE_WHITE_RGB = 0xffffff

private fun PngImage.isDarkGlyph(): Boolean {
    if (maxOf(width, height) > GLYPH_SIZE_CEILING) return false

    var opaque = 0
    var luminance = 0L
    var saturation = 0L
    val tones = mutableSetOf<Int>()

    for (pixel in argb) {
        if ((pixel ushr 24) <= GLYPH_ALPHA_FLOOR) continue

        val red = pixel shr 16 and 0xff
        val green = pixel shr 8 and 0xff
        val blue = pixel and 0xff
        val tone = (299 * red + 587 * green + 114 * blue) / 1000

        opaque++
        luminance += tone
        saturation += maxOf(red, green, blue) - minOf(red, green, blue)
        tones += tone
    }

    if (opaque == 0) return false

    return luminance / opaque < GLYPH_LUMINANCE_CEILING &&
        saturation / opaque < GLYPH_SATURATION_CEILING &&
        tones.size <= GLYPH_TONE_CEILING
}

private fun PngImage.whitenOpaquePixels() {
    for (index in argb.indices) {
        val alpha = argb[index] and 0xff000000.toInt()
        if (alpha != 0) argb[index] = alpha or OPAQUE_WHITE_RGB
    }
}

private fun ResourcePatchContext.whitenDarkGlyphIcons(): Int {
    var whitened = 0

    iconImageFiles().forEach icon@{ file ->
        val image = PngImage.read(file)
        if (!image.isDarkGlyph()) return@icon

        image.whitenOpaquePixels()
        image.write(file)
        whitened++
    }

    return whitened
}

private const val TAB_ANIMATION_PREFIX = "tab_"
private const val LOTTIE_BLACK = "\"k\":[0,0,0,1]"
private const val LOTTIE_WHITE = "\"k\":[1,1,1,1]"

private fun ResourcePatchContext.invertTabAnimations(): Int {
    var replaced = 0

    get("assets").listFiles()
        ?.filter { it.name.startsWith(TAB_ANIMATION_PREFIX) && it.extension == "json" }
        ?.forEach { file ->
            val text = file.readText()
            if (!text.contains(LOTTIE_BLACK)) return@forEach

            replaced += text.split(LOTTIE_BLACK).size - 1
            file.writeText(text.replace(LOTTIE_BLACK, LOTTIE_WHITE))
        }

    return replaced
}

private fun requireDeclared(description: String, names: Set<String>, declared: Set<String>) {
    val missing = names - declared
    if (missing.isNotEmpty()) {
        throw PatchException("Could not find the $description colours: ${missing.sorted().joinToString()}")
    }
}

private fun requireReplaced(description: String, replaced: Int) {
    if (replaced == 0) {
        throw PatchException("Could not replace any $description")
    }
}

private val amoledThemeResourcesPatch = resourcePatch {
    dependsOn(resourceMappingPatch)

    execute {
        val palette = document("res/values/colors.xml").use { document ->
            val declared = document.readColors()
            document.setColors(ADDED_COLORS)
            declared
        }
        requireDeclared("night override", NIGHT_COLORS.keys - ADDED_COLORS.keys, palette.keys)
        document("res/values-night/colors.xml").use { it.setColors(NIGHT_COLORS) }

        document("res/values/colors.xml", HEADSET_PACKAGE).use { document ->
            requireDeclared("headset override", HEADSET_COLORS.keys, document.readColors().keys)
            document.setColors(HEADSET_COLORS)
        }

        val themes = editDocuments(dayStyleFiles()) { useDayNightThemes() } +
            document("res/values/styles.xml", HEADSET_PACKAGE).use { it.useDayNightThemes() }

        val lightSurface = { element: Element, _: String, value: String ->
            if (element.isHairline()) null else lightNeutralSurface(value)
        }

        requireReplaced("light theme parent", themes)
        requireReplaced(
            "light layout background",
            replaceAttributes("layout", LAYOUT_ATTRIBUTES, LAYOUT_REPLACEMENTS, lightSurface),
        )
        requireReplaced("light drawable fill", replaceAttributes("drawable", COLOR_ATTRIBUTES, SURFACE_REPLACEMENTS))
        requireReplaced(
            "light drawable gradient",
            replaceAttributes("drawable", GRADIENT_ATTRIBUTES, SURFACE_REPLACEMENTS, lightSurface),
        )
        requireReplaced("black monochrome icon", invertMonochromeVectors())
        requireReplaced("black tab animation", invertTabAnimations())
        requireReplaced("dark glyph icon", whitenDarkGlyphIcons())

        val ink = { _: Element, attribute: String, value: String -> invertedInk(palette, attribute, value) }

        requireReplaced(
            "light layout foreground",
            replaceAttributes("layout", ON_SURFACE_ATTRIBUTES, LAYOUT_ON_SURFACE_REPLACEMENTS, ink),
        )
        requireReplaced(
            "light drawable foreground",
            replaceAttributes("drawable", ON_SURFACE_ATTRIBUTES, ON_SURFACE_REPLACEMENTS, ink),
        )
        requireReplaced(
            "light selector foreground",
            replaceAttributes("color", COLOR_ATTRIBUTES, ON_SURFACE_REPLACEMENTS, ink),
        )
        requireReplaced("light toolbar chrome", replaceAttributes("layout", CHROME_ATTRIBUTES, CHROME_REPLACEMENTS))
        requireReplaced("light style background", replaceStyleItems(STYLE_BACKGROUND_ATTRIBUTES, LAYOUT_REPLACEMENTS))
        requireReplaced("white style foreground", replaceStyleItems(STYLE_ON_SURFACE_ATTRIBUTES, ON_SURFACE_REPLACEMENTS))
        requireReplaced("framework card background", replaceStyleItems(STYLE_CARD_ATTRIBUTES, CARD_REPLACEMENTS))

        document(HOME_BANNER_DRAWABLE).use { document ->
            val banner = document.getNode("path") as? Element
                ?: throw PatchException("Could not find a path in $HOME_BANNER_DRAWABLE")

            banner.setAttribute("android:fillColor", PAGE_COLOR)
        }

        document("AndroidManifest.xml").use { document ->
            val usesSdk = document.getNode("uses-sdk") as? Element
                ?: throw PatchException("Could not find uses-sdk in the manifest")

            usesSdk.setAttribute(MIN_SDK_ATTRIBUTE, ALGORITHMIC_DARKENING_MIN_SDK.toString())
        }
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the light theme with a pure black dark theme. " +
        "Requires Android 13 or later.",
) {
    compatibleWith(AppCompatibilities.REALME_LINK)

    dependsOn(amoledThemeResourcesPatch, resourceMappingPatch)

    execute {
        CommonTitleBarBackIconFingerprint.matchSingle().apply {
            val index = instructionMatches.first().index
            val register = method.getInstruction<OneRegisterInstruction>(index).registerA
            val backIcon = getResourceId(ResourceType.DRAWABLE, BACK_ICON_NAME)

            method.replaceInstruction(index, "const v$register, 0x${backIcon.toString(16)}")
        }

        SetDefaultNightModeFingerprint.matchSingle().method
            .addInstructions(0, "const/4 p0, 0x${MODE_NIGHT_YES.toString(16)}")

        listOf(
            SetAppearanceLightStatusBarsFingerprint,
            SetAppearanceLightNavigationBarsFingerprint,
        ).forEach { fingerprint ->
            fingerprint.matchSingle().method.addInstructions(0, "const/4 p1, 0x0")
        }

        DefaultHomeBackgroundColorFingerprint.matchSingle().apply {
            val index = instructionMatches.first().index
            val register = method.getInstruction<OneRegisterInstruction>(index).registerA

            method.replaceInstruction(index, "const-string v$register, \"$HOME_BACKGROUND_COLOR\"")
        }

        WebViewSettingsFingerprint.matchAll(WEB_VIEW_SETTINGS_SITES).forEach { match ->
            match.method.apply {
                val settingsIndex = match.instructionMatches.last().index
                val settingsRegister = getInstruction<OneRegisterInstruction>(settingsIndex).registerA
                val allowedRegister = getFreeRegisterProvider(settingsIndex + 1, 1, settingsRegister)
                    .getFreeRegister4Bit()

                addInstructions(
                    settingsIndex + 1,
                    """
                        const/4 v$allowedRegister, 0x1
                        invoke-virtual { v$settingsRegister, v$allowedRegister }, $WEB_SETTINGS_CLASS->setAlgorithmicDarkeningAllowed(Z)V
                    """,
                )
            }
        }

        HomeHeaderTintFingerprint.matchAll(HOME_HEADER_TINT_SITES).forEach { match ->
            val index = match.instructionMatches.last().index
            val register = match.method.getInstruction<OneRegisterInstruction>(index).registerA

            match.method.replaceInstruction(index, "const v$register, $OPAQUE_WHITE_ARGB")
        }

        ApplicationOnCreateFingerprint.matchSingle().method.apply {
            val modeRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructions(
                0,
                """
                    const/4 v$modeRegister, 0x${MODE_NIGHT_YES.toString(16)}
                    invoke-static { v$modeRegister }, $APP_COMPAT_DELEGATE_CLASS->setDefaultNightMode(I)V
                """,
            )
        }
    }
}
