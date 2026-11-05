/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.theme

import java.io.File

private const val TOKEN_PREFIX = "hx_dark"
private const val COLOR_REFERENCE_PREFIX = "@color/"
private const val ANDROID_WHITE = "@android:color/white"
private const val ANDROID_BLACK = "@android:color/black"
private const val WHITE_ARGB = -1
private const val BLACK_ARGB = 0xFF000000.toInt()

private val NIGHT_DIRECTORY = Regex("-night")
private val TAG = Regex("<([\\w.]+)([^<>]*)>")
private val ATTRIBUTE = Regex("""([\w:]+)="([^"]*)"""")
private val STYLE_ITEM = Regex("""<item name="([^"]+)">((?:@color/|@android:color/|#)[^<]+)</item>""")
private val COLOR_ELEMENT = Regex("""<color name="([^"]+)">([^<]+)</color>""")

private val FOREGROUND_ATTRIBUTES = setOf(
    "textColor", "textColorHint", "textColorLink", "textColorPrimary", "textColorSecondary", "drawableTint",
    "iconTint", "tint", "titleTextColor", "subtitleTextColor", "indeterminateTint", "fillColor", "strokeColor",
)
private val BACKGROUND_ATTRIBUTES = setOf(
    "background", "cardBackgroundColor", "backgroundTint", "windowBackground", "colorBackground", "solidColor",
    "cacheColorHint",
)
private val LINE_ATTRIBUTES = setOf("divider")
private val GRADIENT_ATTRIBUTES = setOf("startColor", "centerColor", "endColor")
private val SHAPE_TAGS = mapOf("solid" to ColorRole.BACKGROUND, "stroke" to ColorRole.LINE)
private val LIBRARY_SURFACE_COLORS = setOf("cardview_light_background")

internal class DarkResources(private val resources: File) {

    private val lightColors = mutableMapOf<String, String>()
    private val nightNames = mutableSetOf<String>()
    private val tokens = sortedMapOf<String, Pair<Int, Int>>()
    private val nightLibraryColors = sortedMapOf<String, Int>()

    fun rewrite() {
        readColors()
        addNightLibraryColors()
        sourceFiles().forEach { file ->
            val original = file.readText()
            val rewritten = if (file.parentFile.name.startsWith("values")) rewriteStyleItems(original) else rewriteTags(original, file)
            if (rewritten != original) file.writeText(rewritten)
        }
        writeTokens()
    }

    private fun directories() = resources.listFiles().orEmpty().filter { it.isDirectory }

    private fun readColors() {
        directories().filter { it.name.startsWith("values") }.forEach { directory ->
            val night = NIGHT_DIRECTORY.containsMatchIn(directory.name)
            directory.listFiles().orEmpty().filter { it.extension == "xml" }.forEach { file ->
                COLOR_ELEMENT.findAll(file.readText()).forEach { match ->
                    val (name, value) = match.destructured
                    if (night) nightNames += name else lightColors.putIfAbsent(name, value.trim())
                }
            }
        }
    }

    private fun addNightLibraryColors() {
        LIBRARY_SURFACE_COLORS.filter { it !in nightNames }.forEach { name ->
            val light = lightColors[name]?.let { resolve(it) } ?: return@forEach
            DarkPalette.night(ColorRole.BACKGROUND, light)?.let { nightLibraryColors[name] = it }
        }
    }

    private fun sourceFiles() = directories()
        .filter { !NIGHT_DIRECTORY.containsMatchIn(it.name) }
        .filter { directory ->
            val kind = directory.name.substringBefore('-')
            kind == "layout" || kind == "drawable" || kind == "color" || directory.name.startsWith("values")
        }
        .flatMap { directory ->
            directory.listFiles().orEmpty().filter { file ->
                file.extension == "xml" && (!directory.name.startsWith("values") || file.name == "styles.xml")
            }
        }

    private fun resolve(value: String, depth: Int = 0): Int? = when {
        depth > MAX_ALIAS_DEPTH -> null
        value == ANDROID_WHITE -> WHITE_ARGB
        value == ANDROID_BLACK -> BLACK_ARGB
        value.startsWith(COLOR_REFERENCE_PREFIX) -> lightColors[value.removePrefix(COLOR_REFERENCE_PREFIX)]
            ?.let { resolve(it, depth + 1) }
        value.startsWith("#") -> DarkPalette.parse(value)
        else -> null
    }

    private fun tokenFor(role: ColorRole, value: String): String? {
        if (value.startsWith(COLOR_REFERENCE_PREFIX) && value.removePrefix(COLOR_REFERENCE_PREFIX) in nightNames) return null
        val light = resolve(value) ?: return null
        val night = DarkPalette.night(role, light) ?: return null
        val name = "${TOKEN_PREFIX}_${role.name.lowercase()}_%08x".format(light)
        tokens[name] = light to night
        return COLOR_REFERENCE_PREFIX + name
    }

    private fun rewriteStyleItems(text: String) = STYLE_ITEM.replace(text) { match ->
        val (name, value) = match.destructured
        val role = roleOfAttribute(name.substringAfter(':'))
        val token = role?.let { tokenFor(it, value) }
        if (token == null) match.value else "<item name=\"$name\">$token</item>"
    }

    private fun rewriteTags(text: String, file: File) = TAG.replace(text) { tag ->
        val (element, attributes) = tag.destructured
        val rewritten = ATTRIBUTE.replace(attributes) { attribute ->
            val (name, value) = attribute.destructured
            val role = roleOf(element, name.substringAfter(':'), file.parentFile.name)
            val token = role?.let { tokenFor(it, value) }
            if (token == null) attribute.value else "$name=\"$token\""
        }
        if (rewritten == attributes) tag.value else "<$element$rewritten>"
    }

    private fun roleOf(element: String, attribute: String, directory: String): ColorRole? = when {
        attribute == "color" && element in SHAPE_TAGS -> SHAPE_TAGS.getValue(element)
        attribute in GRADIENT_ATTRIBUTES && element == "gradient" -> ColorRole.BACKGROUND
        attribute == "color" && directory.startsWith("color") && element == "item" -> ColorRole.FOREGROUND
        else -> roleOfAttribute(attribute)
    }

    private fun roleOfAttribute(attribute: String): ColorRole? = when (attribute) {
        in FOREGROUND_ATTRIBUTES -> ColorRole.FOREGROUND
        in BACKGROUND_ATTRIBUTES -> ColorRole.BACKGROUND
        in LINE_ATTRIBUTES -> ColorRole.LINE
        else -> null
    }

    private fun writeTokens() {
        appendColors(File(resources, "values/colors.xml"), tokens.mapValues { it.value.first })
        appendColors(File(resources, "values-night/colors.xml"), tokens.mapValues { it.value.second } + nightLibraryColors)
    }

    private fun appendColors(file: File, colors: Map<String, Int>) {
        val entries = colors.entries.joinToString("") { (name, color) ->
            "    <color name=\"$name\">${DarkPalette.format(color)}</color>\n"
        }
        file.writeText(file.readText().replace("</resources>", "$entries</resources>"))
    }

}

private const val MAX_ALIAS_DEPTH = 8
