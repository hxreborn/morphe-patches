/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.childElementsSequence
import org.w3c.dom.Element

private const val BLACK = "#ff000000"
private const val SKIN_FLAG_ITEM = "biui_skin_flag"
private const val DARK_SKIN_FLAG = "1"
private const val ROW_SELECTOR = "hx_amoled_row_selector"
private const val ROW_RIPPLE = "hx_amoled_row_ripple"
private const val PRESSED_ROW_COLOR = "#26ffffff"
private const val CHAT_BACKGROUND_SOURCE = "biui_color_shape_background_tertiary"
private const val CHAT_BACKGROUND_ATTR = "hx_amoled_chat_background"
private const val CHAT_BACKGROUND_VIEW = "@id/background_layout\""

private val BLACK_ITEMS = mapOf(
    "biui_color_shape_background_primary" to BLACK,
    "biui_color_shape_background_secondary" to BLACK,
    "imo_skin_im_chat_bubble_list_bg" to "@android:color/black",
    "imo_skin_im_chat_tool_bar_bg" to "@android:color/black",
    "imo_skin_normal_rect_selector_bg" to "@drawable/$ROW_SELECTOR",
    "imo_skin_normal_rect_selector_bg_ripple" to "@drawable/$ROW_RIPPLE",
)

private val DRAWABLES = mapOf(
    ROW_SELECTOR to """
        <selector xmlns:android="http://schemas.android.com/apk/res/android">
            <item android:state_pressed="true">
                <shape><solid android:color="$PRESSED_ROW_COLOR" /></shape>
            </item>
            <item android:drawable="@android:color/black" />
        </selector>
    """,
    ROW_RIPPLE to """
        <ripple xmlns:android="http://schemas.android.com/apk/res/android" android:color="$PRESSED_ROW_COLOR">
            <item android:drawable="@android:color/black" />
        </ripple>
    """,
)

private fun Element.items() = childElementsSequence().filter { it.tagName == "item" }

private fun Element.addItem(name: String, value: String) {
    appendChild(ownerDocument.createElement("item").also {
        it.setAttribute("name", name)
        it.textContent = value
    })
}

@Suppress("unused")
val amoledThemePatch = resourcePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        DRAWABLES.forEach { (name, xml) -> get("res/drawable/$name.xml").writeText(xml.trimIndent()) }

        document("res/values/attrs.xml").use { document ->
            document.documentElement.appendChild(document.createElement("attr").also {
                it.setAttribute("name", CHAT_BACKGROUND_ATTR)
                it.setAttribute("format", "color")
            })
        }

        document("res/values/styles.xml").use { document ->
            val styles = document.documentElement.childElementsSequence()
                .filter { it.tagName == "style" }
                .associateBy { it.getAttribute("name") }

            fun Element.skinFlag() = generateSequence(this) { styles[it.getAttribute("parent").substringAfter("@style/")] }
                .firstNotNullOfOrNull { style -> style.items().firstOrNull { it.getAttribute("name") == SKIN_FLAG_ITEM } }
                ?.textContent

            val darkStyles = styles.values.filter { it.skinFlag() == DARK_SKIN_FLAG }
            val blackened = darkStyles
                .flatMap { it.items() }
                .filter { it.getAttribute("name") in BLACK_ITEMS }
                .onEach { it.textContent = BLACK_ITEMS.getValue(it.getAttribute("name")) }
            val missing = BLACK_ITEMS.keys - blackened.map { it.getAttribute("name") }.toSet()
            if (missing.isNotEmpty()) throw PatchException("Dark skin does not define $missing")

            val darkSkins = darkStyles.filter { style -> style.items().any { it.getAttribute("name") == SKIN_FLAG_ITEM } }
            styles.values.forEach { style ->
                val chatBackground = style.items().firstOrNull { it.getAttribute("name") == CHAT_BACKGROUND_SOURCE }
                when {
                    style in darkSkins -> style.addItem(CHAT_BACKGROUND_ATTR, BLACK)
                    chatBackground != null -> style.addItem(CHAT_BACKGROUND_ATTR, chatBackground.textContent)
                }
            }
        }

        val chatLayouts = get("res").listFiles { dir -> dir.name.startsWith("layout") }.orEmpty()
            .flatMap { it.listFiles().orEmpty().toList() }
            .filter { CHAT_BACKGROUND_VIEW in it.readText() }
        val chatBackgroundTag = Regex("<[^<>]*$CHAT_BACKGROUND_VIEW[^<>]*>")
        val rewritten = chatLayouts.count { layout ->
            val xml = layout.readText()
            val patched = xml.replace(chatBackgroundTag) { it.value.replace(CHAT_BACKGROUND_SOURCE, CHAT_BACKGROUND_ATTR) }
            (patched != xml).also { if (it) layout.writeText(patched) }
        }
        if (rewritten == 0) throw PatchException("No chat layout uses $CHAT_BACKGROUND_SOURCE")
    }
}
