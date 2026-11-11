/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.util.asSequence
import app.morphe.util.childElementsSequence
import org.w3c.dom.Element

private const val PANEL_COLOR = "#ff303030"

private const val PANEL_COLOR_COUNT = 3

private const val RGB_HEX_LENGTH = 7

private const val CARD_OVERLAY_COLOR = "#17ffffff"

private const val SELECTED_OVERLAY_COLOR = "#42ffffff"

internal data class ReadEraResourceRoles(
    val theme: String,
    val surface: String,
    val accent: String,
    val checkboxAccent: String,
    val switchStyle: String,
    val checkboxTint: String,
    val radioTint: String,
    val buttonTintAttribute: String,
)

internal data class ReadEraPalette(
    val page: String,
    val bar: String,
    val raised: String,
    val card: String,
    val panels: List<String>,
    val cardOverlay: String,
    val selectedOverlay: String,
    val accent: String,
    val barAttribute: String,
)

private fun ResourcePatchContext.applicationTheme() = document("AndroidManifest.xml").use { document ->
    (document.getElementsByTagName("application").item(0) as Element)
        .getAttribute("android:theme").removePrefix("@style/")
}

private fun ResourcePatchContext.frameworkColorAttributes(vararg names: String): Map<String, String> {
    val attributes = mutableMapOf<String, MutableSet<String>>()
    decode("res/values*/styles.xml").forEach { path ->
        document(path).use { document ->
            document.getElementsByTagName("item").asSequence().map { it as Element }.filter {
                it.getAttribute("name") in names && it.textContent.startsWith("?") && !it.textContent.startsWith("?android:")
            }.forEach { item ->
                attributes.getOrPut(item.getAttribute("name")) { mutableSetOf() }
                    .add(item.textContent.removePrefix("?attr/").removePrefix("?"))
            }
        }
    }
    return names.associateWith { attributes.getValue(it).single() }
}

private fun List<Element>.themeItem(styleName: String, attribute: String): String {
    val style = single { it.getAttribute("name") == styleName }
    return style.childElementsSequence().singleOrNull { it.getAttribute("name") == attribute }?.textContent
        ?: themeItem(style.getAttribute("parent").removePrefix("@style/"), attribute)
}

private fun argb(value: String) = value.lowercase().let { if (it.length == RGB_HEX_LENGTH) "#ff${it.drop(1)}" else it }

private fun ResourcePatchContext.styles() = document("res/values/styles.xml").use { document ->
    document.getElementsByTagName("style").asSequence().map { it as Element }.toList()
}

internal fun ResourcePatchContext.readEraResourceRoles(): ReadEraResourceRoles {
    val theme = applicationTheme()
    val buttonTintAttributes = mutableSetOf<String>()
    val checkboxThemes = mutableSetOf<String>()
    val tintLayouts = decode("res/layout/*.xml").mapNotNull { path ->
        document(path).use { document ->
            document.getElementsByTagName("CheckBox").asSequence().map { it as Element }
                .map { it.getAttribute("android:theme") }.filter { it.startsWith("@style/") }
                .forEach { checkboxThemes += it.removePrefix("@style/") }
            val children = document.documentElement.childElementsSequence().toList()
            val (framework, compat) = children.takeIf { it.size == 2 } ?: return@use null
            if (framework.tagName !in listOf("CheckBox", "RadioButton") || compat.tagName != framework.tagName ||
                framework.getAttribute("android:background").replace("?android:attr/", "?android:") != "?android:colorBackground"
            ) return@use null
            val tintAttribute = (0 until compat.attributes.length).map { compat.attributes.item(it) }
                .filter { it.nodeName.startsWith("app:") && it.nodeValue.startsWith("@color/") }
                .singleOrNull() ?: return@use null
            val tint = tintAttribute.nodeValue
            buttonTintAttributes += tintAttribute.nodeName
            val frameworkTint = framework.getAttribute("android:buttonTint")
            check(frameworkTint.isEmpty() || frameworkTint == tint) { "Control tint mismatch in $path" }
            Triple(framework.tagName, tint, path)
        }
    }
    val (_, checkboxTint, _) = tintLayouts.single { it.first == "CheckBox" }
    val (_, radioTint, _) = tintLayouts.single { it.first == "RadioButton" }
    val frameworkColors = frameworkColorAttributes("android:colorControlActivated", "android:colorAccent")
    val styles = styles()
    val app = styles.single { it.getAttribute("name") == theme }
    fun Element.item(name: String) = childElementsSequence().single { it.getAttribute("name") == name }.textContent
    val switchStyle = styles.single { style ->
        style.getAttribute("parent") == "@android:style/Widget.CompoundButton" &&
            style.childElementsSequence().any { it.getAttribute("name") == "android:thumb" }
    }.getAttribute("name")
    val activated = frameworkColors.getValue("android:colorControlActivated")
    val checkboxAccent = checkboxThemes.map { styles.themeItem(it, activated) }.distinct().single()
    return ReadEraResourceRoles(theme, app.item("android:windowBackground"), app.item(frameworkColors.getValue("android:colorAccent")),
        checkboxAccent, switchStyle, checkboxTint, radioTint, buttonTintAttributes.single())
}

internal fun ResourcePatchContext.readEraAccent(): String =
    styles().themeItem(applicationTheme(), frameworkColorAttributes("android:colorAccent").getValue("android:colorAccent"))

internal fun ResourcePatchContext.readEraPalette(): ReadEraPalette {
    val theme = applicationTheme()
    val attributes = frameworkColorAttributes("android:colorPrimary", "android:colorPrimaryDark")
    val styles = styles()
    fun colorName(reference: String): String {
        check(reference.startsWith("@color/")) { "Expected a color reference, found $reference" }
        return reference.removePrefix("@color/")
    }
    val page = colorName(styles.themeItem(theme, "android:windowBackground"))
    val card = decode("res/drawable*/*.xml").mapNotNull { path ->
        document(path).use { document ->
            val shape = document.documentElement.takeIf { it.tagName == "shape" } ?: return@use null
            val solid = shape.childElementsSequence().singleOrNull { it.tagName == "solid" } ?: return@use null
            val stroke = shape.childElementsSequence().singleOrNull { it.tagName == "stroke" } ?: return@use null
            stroke.getAttribute("android:color").takeIf {
                solid.getAttribute("android:color") == "@color/$page" && it.startsWith("@color/")
            }
        }
    }.single()
    val colors = document("res/values/colors.xml").use { document ->
        document.getElementsByTagName("color").asSequence().map { it as Element }
            .map { it.getAttribute("name") to argb(it.textContent) }.toList()
    }
    fun colorsWithValue(value: String) = colors.filter { (_, color) -> color == value }.map { (name, _) -> name }
    val panels = colorsWithValue(PANEL_COLOR)
    check(panels.size == PANEL_COLOR_COUNT) { "Expected $PANEL_COLOR_COUNT panel colors, found $panels" }
    val barAttribute = attributes.getValue("android:colorPrimary")
    return ReadEraPalette(
        page = page,
        bar = colorName(styles.themeItem(theme, barAttribute)),
        raised = colorName(styles.themeItem(theme, attributes.getValue("android:colorPrimaryDark"))),
        card = colorName(card),
        panels = panels,
        cardOverlay = colorsWithValue(CARD_OVERLAY_COLOR).single(),
        selectedOverlay = colorsWithValue(SELECTED_OVERLAY_COLOR).single(),
        accent = readEraAccent(),
        barAttribute = barAttribute,
    )
}
