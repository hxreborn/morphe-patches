/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.misc.settings

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.shared.misc.fillAppliedPatch
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.util.ResourceGroup
import app.morphe.util.asSequence
import app.morphe.util.copyResources
import java.util.logging.Logger
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val APPLIED_PATCHES_CLASS = "Lapp/hxreborn/extension/vpnsuper/AppliedPatches;"
private const val SETTINGS_STYLE_CLASS = "Lapp/hxreborn/extension/vpnsuper/SettingsStyle;"
private const val PATCHES_MENU_ROW_VIEW = "app.hxreborn.extension.vpnsuper.PatchesMenuRow"
private const val SETTINGS_ROW_TITLE = "hxreborn patches"
private const val SETTINGS_LAYOUT = "res/layout/menu_activity_settings.xml"
private const val SETTINGS_CARD_ID = "@id/settings_items_card"
private const val TEMPLATE_ROW_ID = "@id/settings_always_on_vpn_container"
private const val TEMPLATE_DIVIDER_ID = "@id/settings_divider_1"
private const val ENTRY_ICON_NAME = "hx_vpnsuper_patches_icon"
private const val ENTRY_ICON_TINT = "@color/connect_active_color"
private val ICON_RESOURCES = listOf("notifications", "reset", "info", "patches").map { "hx_vpnsuper_ic_$it" }

private val logger = Logger.getLogger("app.morphe.patches.vpnsuper.misc.settings")

private var menuStylesResolved = false

internal fun BytecodePatchContext.markPatchApplied(methodName: String, patch: BytecodePatch) =
    fillAppliedPatch(APPLIED_PATCHES_CLASS, methodName, patch, withDescription = false)

private fun Document.elementWithId(id: String) = getElementsByTagName("*").asSequence()
    .map { it as Element }
    .singleOrNull { it.getAttribute("android:id") == id }

private fun Document.addPatchesMenuRow() {
    val card = elementWithId(SETTINGS_CARD_ID)
    val templateRow = elementWithId(TEMPLATE_ROW_ID)
    val templateDivider = elementWithId(TEMPLATE_DIVIDER_ID)
    if (card == null || templateRow?.parentNode != card || templateDivider?.parentNode != card) {
        logger.warning("Skipped the patches settings row: $TEMPLATE_ROW_ID and $TEMPLATE_DIVIDER_ID are not both in $SETTINGS_CARD_ID")
        return
    }
    val row = renameNode(templateRow.cloneNode(true), null, PATCHES_MENU_ROW_VIEW) as Element
    val rowChildren = row.childNodes.asSequence().filterIsInstance<Element>().toList()
    val icon = rowChildren.singleOrNull { it.hasAttribute("android:id") }
    val title = rowChildren.singleOrNull { it.tagName == "TextView" }
    if (icon == null || title == null) {
        logger.warning("Skipped the patches settings row: unexpected $TEMPLATE_ROW_ID children")
        return
    }

    val divider = templateDivider.cloneNode(true) as Element
    divider.removeAttribute("android:id")
    row.removeAttribute("android:id")
    icon.setAttribute("android:id", "@id/$ENTRY_ICON_NAME")
    icon.setAttribute("android:src", "@drawable/hx_vpnsuper_ic_patches")
    icon.setAttribute("android:tint", ENTRY_ICON_TINT)
    title.setAttribute("android:text", SETTINGS_ROW_TITLE)
    title.setAttribute("android:layout_toEndOf", "@id/$ENTRY_ICON_NAME")
    card.appendChild(divider)
    card.appendChild(row)
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(
        resourcePatch {
            execute {
                copyResources("vpnsuper", ResourceGroup("drawable", *ICON_RESOURCES.map { "$it.xml" }.toTypedArray()))
            }
            finalize {
                if (!menuStylesResolved) return@finalize
                try {
                    document("res/values/ids.xml").use { document ->
                        val id = document.createElement("item")
                        id.setAttribute("type", "id")
                        id.setAttribute("name", ENTRY_ICON_NAME)
                        document.documentElement.appendChild(id)
                    }
                    document(SETTINGS_LAYOUT).use { it.addPatchesMenuRow() }
                } catch (ex: Exception) {
                    logger.warning("Skipped the patches settings row: ${ex.message}")
                }
            }
        },
    )
    extendWith("extensions/extension.mpe")

    execute {
        menuStylesResolved = false
        try {
            mutableClassDefBy(SETTINGS_STYLE_CLASS).apply {
                replaceResourceIdStubs(
                    ResourceType.COLOR,
                    mapOf(
                        "screenBackgroundColor" to "dark_purple",
                        "cardBackgroundColor" to "menu_color_groups_bg_purple",
                        "primaryTextColor" to "white_100",
                        "secondaryTextColor" to "menu_color_menu_item",
                        "switchOnColor" to "unsecure_wifi_protection_accent",
                        "switchOffColor" to "color_gray_light",
                    ),
                )
                replaceResourceIdStubs(ResourceType.FONT, mapOf("regularFont" to "inter_regular", "mediumFont" to "inter_medium"))
                replaceResourceIdStubs(ResourceType.DRAWABLE, mapOf("backIconDrawable" to "ic_chevron_left"))
            }
            menuStylesResolved = true
        } catch (ex: PatchException) {
            logger.warning("Skipped the patches settings row: ${ex.message}")
        }
    }
}
