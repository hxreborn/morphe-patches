/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bittorrent.layout.insets

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.torrent.removeProtectionPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patches.shared.misc.extension.bittorrentFixScreenInsetsExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/bittorrent/ScreenInsets;"

@Suppress("unused")
val fixScreenInsetsPatch = patch(
    name = "Fix screen insets",
    description = "Keeps main screens and settings clear of system bars and display cutouts on Android 17.",
) {
    dependsOn(bittorrentFixScreenInsetsExtensionPatch)
    compatibleWith(AppCompatibilities.BITTORRENT)
    dependsOn(removeProtectionPatch)

    execute {
        mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).replaceResourceIdStubs(
            ResourceType.ID,
            mapOf(
                "mainContentId" to "cl_main",
                "pageContentId" to "cl_body",
                "drawerContentId" to "fl_drawer",
                "drawerButtonId" to "iv_home_drawer",
                "searchButtonId" to "iv_search_torrents",
                "addButtonId" to "iv_add_torrent",
            ),
        )
        val match = ActivityContentFingerprint.match()
        val (_, contentView) = match.instructionMatches
        match.method.addInstruction(
            contentView.index + 1,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->attach(Landroid/app/Activity;)V",
        )
        val sheet = PlaylistSheetFingerprint.match()
        val (_, _, inflatedView) = sheet.instructionMatches
        val view = (inflatedView.instruction as OneRegisterInstruction).registerA
        sheet.method.addInstruction(
            inflatedView.index + 1,
            "invoke-static/range { v$view .. v$view }, $EXTENSION_CLASS_DESCRIPTOR->attachSheet(Landroid/view/View;)V",
        )
    }
}
