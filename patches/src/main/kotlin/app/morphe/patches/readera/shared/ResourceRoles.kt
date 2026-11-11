/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.util.asSequence
import app.morphe.util.childElementsSequence
import org.w3c.dom.Element

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
