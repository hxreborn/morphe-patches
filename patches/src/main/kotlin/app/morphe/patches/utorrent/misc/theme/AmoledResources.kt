/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.listDecodableFiles
import app.morphe.patcher.patch.resourceFile
import app.morphe.util.getNode
import org.w3c.dom.Element

private fun Element.elementsByTag(name: String): List<Element> {
    val nodes = getElementsByTagName(name)
    return (0 until nodes.length).map { nodes.item(it) as Element }
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
    addNightSplash(listOf("splash_icon", "splash_icon_fix"), emptyList())
    val resources = document("res/values/public.xml").use { declarationsDocument ->
        val declarations = declarationsDocument.getElementsByTagName("public")
        var nextId = (0 until declarations.length).map { declarations.item(it) as Element }
            .filter { it.getAttribute("type") == "drawable" }
            .maxOf { it.getAttribute("id").removePrefix("0x").toLong(16) } + 1
        val switchLayoutId = (0 until declarations.length).map { declarations.item(it) as Element }
            .filter { it.getAttribute("type") == "layout" }
            .maxOf { it.getAttribute("id").removePrefix("0x").toLong(16) } + 1
        val switchDeclaration = declarationsDocument.createElement("public")
        switchDeclaration.setAttribute("type", "layout")
        switchDeclaration.setAttribute("name", "hx_utorrent_amoled_switch")
        switchDeclaration.setAttribute("id", "0x${switchLayoutId.toString(16)}")
        declarationsDocument.documentElement.appendChild(switchDeclaration)
        resourceFile("res/layout/hx_utorrent_amoled_switch.xml").writeText("""
            <com.bittorrent.app.view.CustomSwitch
                xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="40dp" android:layout_height="21dp"
                app:openText="" app:closeText="" app:closeColor="#ff777777"
                app:openColor="@color/primaryColor" />
        """.trimIndent())
        val drawableIds = darkDrawables.associateWith { original ->
            val name = "hx_utorrent_amoled_$original"
            val identifier = nextId++
            val declaration = declarationsDocument.createElement("public")
            declaration.setAttribute("type", "drawable")
            declaration.setAttribute("name", name)
            declaration.setAttribute("id", "0x${identifier.toString(16)}")
            declarationsDocument.documentElement.appendChild(declaration)
            if (original == "bg_main_top_img_dark") {
                if (listDecodableFiles("res/drawable*/$original.webp").isEmpty()) {
                    throw PatchException("Missing dark header bitmap")
                }
                resourceFile("res/drawable/$name.xml").writeText("""
                    <bitmap xmlns:android="http://schemas.android.com/apk/res/android"
                        android:src="@drawable/$original" android:tint="#ff000000" />
                """.trimIndent())
            } else {
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
                        val gradient = gradients.item(0)
                        val solid = drawable.createElement("solid")
                        solid.setAttribute("android:color", "#ff000000")
                        gradient.parentNode.replaceChild(solid, gradient)
                    }
                }
            }
            identifier
        }
        AmoledResourceIds(drawableIds, switchLayoutId)
    }
    document("res/values/colors.xml").use { colors ->
        val names = setOf("color_dialog_content_dark", "color_notorrent_txt_dark", "color_unselect_tab_dark", "color_search_hint_dark")
        val entries = colors.getElementsByTagName("color")
        val matched = (0 until entries.length).map { entries.item(it) as Element }
            .filter { it.getAttribute("name") in names }
        if (matched.size != names.size) throw PatchException("Expected four dark label colors")
        matched.forEach { it.textContent = "#ffb3b3b3" }
    }
    document("AndroidManifest.xml").use { document ->
        val activity = document.createElement("activity")
        activity.setAttribute("android:name", settingsActivity)
        activity.setAttribute("android:exported", "false")
        activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault.NoActionBar")
        document.getNode("application").appendChild(activity)
    }
    return resources
}
