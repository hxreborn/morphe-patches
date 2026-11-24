/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bittorrent.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.decode
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.resourceFile
import app.morphe.util.getNode
import org.w3c.dom.Document
import org.w3c.dom.Element

internal const val SETTINGS_STYLE = "hx_bittorrent_settings"
internal const val SETTINGS_THEME = "@android:style/Theme.DeviceDefault.NoActionBar"
private const val SWITCH_LAYOUT_NAME = "hx_bittorrent_switch"
private const val ENTRY_TAG = "hx_bittorrent_patches_entry"
internal const val ANCHOR_ROW_ID = "tv_incoming_port_title"
private const val SECTION_HEADER_ID = "tv_others"
private const val FOLDER_TITLE_ID = "tv_default_folder"
private const val FOLDER_VALUE_ID = "tv_download_folder"
private const val ENTRY_TITLE_ID = "hx_bittorrent_patches_title"
private const val ENTRY_CHEVRON_ID = "hx_bittorrent_patches_chevron"
private const val ENTRY_CHEVRON_TAG = "hx_bittorrent_patches_entry_chevron"
private const val ENTRY_TITLE = "hxreborn patches"
private const val SWITCH_LAYOUT = """<?xml version="1.0" encoding="utf-8"?>
<com.bittorrent.base.view.CustomSwitch xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="52.0dip"
    android:layout_height="32.0dip"
    app:closeColor="@color/color_7d7d80"
    app:closeText=""
    app:openColor="@color/color_resume_all"
    app:openText="" />
"""

private fun Element.referencesId(attribute: String, id: String) = getAttribute(attribute) == "@id/$id"

private fun Element.elementsByTag(name: String): List<Element> {
    val nodes = getElementsByTagName(name)
    return (0 until nodes.length).map { nodes.item(it) as Element }
}

private fun nextResourceId(declarations: Element, type: String) =
    declarations.elementsByTag("public").filter { it.getAttribute("type") == type }
        .maxOf { it.getAttribute("id").removePrefix("0x").toLong(16) } + 1

private fun Document.declare(type: String, name: String, identifier: Long) {
    val declaration = createElement("public")
    declaration.setAttribute("type", type)
    declaration.setAttribute("name", name)
    declaration.setAttribute("id", "0x${identifier.toString(16)}")
    documentElement.appendChild(declaration)
}

private const val SPLASH_STYLE = "Theme.App.Splash"
private const val NIGHT_STYLES_FILE = "res/values-night/styles.xml"
private const val WHITE = "@android:color/white"
private const val BLACK = "@android:color/black"

private fun addNightSplash(drawables: List<String>, layouts: List<String>) {
    val (parent, items) = document("res/values-v31/styles.xml").use { styles ->
        val style = styles.documentElement.elementsByTag("style").single { it.getAttribute("name") == SPLASH_STYLE }
        style.getAttribute("parent") to style.elementsByTag("item")
            .map { it.getAttribute("name") to it.textContent.replace(WHITE, BLACK) }
    }
    document(NIGHT_STYLES_FILE).use { night ->
        val style = night.createElement("style")
        style.setAttribute("name", SPLASH_STYLE)
        style.setAttribute("parent", parent)
        items.forEach { (name, value) ->
            val item = night.createElement("item")
            item.setAttribute("name", name)
            item.textContent = value
            style.appendChild(item)
        }
        night.documentElement.appendChild(style)
    }
    drawables.forEach { copyWithBlackBackground("res/drawable/$it.xml", "res/drawable-night/$it.xml") }
    layouts.forEach { copyWithBlackBackground("res/layout/$it.xml", "res/layout-night/$it.xml") }
}

private fun copyWithBlackBackground(source: String, target: String) {
    val copy = resourceFile(target)
    copy.parentFile.mkdirs()
    resourceFile(source).copyTo(copy, overwrite = true)
    document(target).use { resource ->
        val root = resource.documentElement
        (listOf(root) + root.elementsByTag("*")).forEach { element ->
            val attributes = element.attributes
            for (index in 0 until attributes.length) {
                val attribute = attributes.item(index)
                if (attribute.nodeValue == WHITE) attribute.nodeValue = BLACK
            }
        }
    }
}

internal data class AmoledResourceIds(val drawableIds: Map<String, Long>, val switchLayoutId: Long)

internal fun addAmoledResources(darkDrawables: Set<String>, settingsActivity: String): AmoledResourceIds {
    addNightSplash(listOf("splash_icon", "splash_icon_fix"), listOf("activity_splash"))
    val resources = document("res/values/public.xml").use { declarations ->
        var nextDrawable = nextResourceId(declarations.documentElement, "drawable")
        val drawableIds = darkDrawables.associateWith { original ->
            val name = "hx_bittorrent_amoled_$original"
            val identifier = nextDrawable++
            declarations.declare("drawable", name, identifier)
            resourceFile("res/drawable/$original.xml").copyTo(resourceFile("res/drawable/$name.xml"), overwrite = true)
            document("res/drawable/$name.xml").use { drawable ->
                val fills = drawable.getElementsByTagName("solid")
                val gradients = drawable.getElementsByTagName("gradient")
                if (fills.length + gradients.length != 1) {
                    throw PatchException("Expected one background fill in $original")
                }
                if (fills.length == 1) {
                    (fills.item(0) as Element).setAttribute("android:color", "#ff000000")
                } else {
                    val solid = drawable.createElement("solid")
                    solid.setAttribute("android:color", "#ff000000")
                    gradients.item(0).let { it.parentNode.replaceChild(solid, it) }
                }
            }
            identifier
        }
        val switchLayoutId = nextResourceId(declarations.documentElement, "layout")
        declarations.declare("layout", SWITCH_LAYOUT_NAME, switchLayoutId)
        AmoledResourceIds(drawableIds, switchLayoutId)
    }

    decode("res/layout/activity_setting.xml")
    resourceFile("res/layout/$SWITCH_LAYOUT_NAME.xml").writeText(SWITCH_LAYOUT)
    document("res/layout/activity_setting.xml").use { layout ->
        val views = layout.documentElement.elementsByTag("*")
        val title = views.single { it.getAttribute("android:id").endsWith("/$ANCHOR_ROW_ID") }
        val chevron = views.single { it.referencesId("app:layout_constraintTop_toTopOf", ANCHOR_ROW_ID) && it.tagName == "ImageView" }
        val nextRow = views.single { it.referencesId("app:layout_constraintTop_toBottomOf", SECTION_HEADER_ID) }
        val folderValue = views.single { it.getAttribute("android:id").endsWith("/$FOLDER_VALUE_ID") }
        folderValue.removeAttribute("app:layout_constraintTop_toBottomOf")
        folderValue.setAttribute("app:layout_constraintTop_toTopOf", "@id/$FOLDER_TITLE_ID")
        folderValue.setAttribute("app:layout_constraintBottom_toBottomOf", "@id/$FOLDER_TITLE_ID")
        val entryTitle = title.cloneNode(true) as Element
        entryTitle.setAttribute("android:id", "@+id/$ENTRY_TITLE_ID")
        entryTitle.setAttribute("android:text", ENTRY_TITLE)
        entryTitle.setAttribute("android:tag", ENTRY_TAG)
        entryTitle.setAttribute("android:clickable", "true")
        entryTitle.setAttribute("app:layout_constraintTop_toBottomOf", "@id/$SECTION_HEADER_ID")
        val entryChevron = chevron.cloneNode(true) as Element
        entryChevron.setAttribute("android:id", "@+id/$ENTRY_CHEVRON_ID")
        entryChevron.setAttribute("android:tag", ENTRY_CHEVRON_TAG)
        entryChevron.setAttribute("android:clickable", "true")
        entryChevron.setAttribute("app:layout_constraintTop_toTopOf", "@id/$ENTRY_TITLE_ID")
        entryChevron.setAttribute("app:layout_constraintBottom_toBottomOf", "@id/$ENTRY_TITLE_ID")
        nextRow.setAttribute("app:layout_constraintTop_toBottomOf", "@id/$ENTRY_TITLE_ID")
        nextRow.parentNode.insertBefore(entryTitle, nextRow)
        nextRow.parentNode.insertBefore(entryChevron, nextRow)
    }
    document("AndroidManifest.xml").use { document ->
        val activity = document.createElement("activity")
        activity.setAttribute("android:name", settingsActivity)
        activity.setAttribute("android:exported", "false")
        activity.setAttribute("android:theme", "@style/$SETTINGS_STYLE")
        document.getNode("application").appendChild(activity)
    }
    return resources
}
