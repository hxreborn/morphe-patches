/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.layout.menu

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private val HIDDEN_MENU_ITEMS = listOf("action_manage_subscriptions", "action_share", "action_about")

@Suppress("unused")
val hideMenuItemsPatch = bytecodePatch(
    name = "Hide menu items",
    description = "Hides Manage subscription, Share and About from the main menu.",
) {
    compatibleWith(AppCompatibilities.DEVCHECK)

    execute {
        val prepareOptionsMenu = PrepareMainOptionsMenuFingerprint.matchSingle().method
        val idRegister = prepareOptionsMenu.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

        prepareOptionsMenu.addInstructions(
            0,
            HIDDEN_MENU_ITEMS.joinToString("\n") { name ->
                """
                    const v$idRegister, ${resourceId(ResourceType.ID, name)}
                    invoke-interface {p1, v$idRegister}, Landroid/view/Menu;->removeItem(I)V
                """
            },
        )
    }
}
