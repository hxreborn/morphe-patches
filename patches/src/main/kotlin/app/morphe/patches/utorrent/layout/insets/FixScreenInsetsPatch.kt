/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.layout.insets

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.torrent.removeProtectionPatch
import app.morphe.patches.shared.misc.extension.utorrentFixScreenInsetsExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/utorrent/ScreenInsets;"

@Suppress("unused")
val fixScreenInsetsPatch = bytecodePatch(
    name = "Fix screen insets",
    description = "Keeps main screens and settings clear of system bars and display cutouts on Android 17.",
) {
    dependsOn(utorrentFixScreenInsetsExtensionPatch)
    compatibleWith(AppCompatibilities.UTORRENT)
    dependsOn(removeProtectionPatch)

    execute {
        mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).replaceResourceIdStubs(
            ResourceType.ID,
            mapOf("mainContentId" to "ll_bottom", "pageContentId" to "ll_body", "drawerContentId" to "navigation_list_layout"),
        )
        val match = ActivityContentFingerprint.matchSingle()
        val (contentView) = match.instructionMatches
        match.method.addInstruction(
            contentView.index + 1,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->attach(Landroid/app/Activity;)V",
        )
        val audio = AudioActivityFingerprint.matchSingle()
        val (_, audioContentView) = audio.instructionMatches
        audio.method.addInstruction(
            audioContentView.index + 1,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->attachAudio(Landroid/app/Activity;)V",
        )
        val queueOwner = QueueLayoutFingerprint.matchSingle().classDef.type
        val queue = queueWindowFingerprint(queueOwner).matchSingle()
        val (queueLayout) = queue.instructionMatches
        queue.method.addInstruction(
            queueLayout.index + 1,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->attachQueue(Landroid/app/Dialog;)V",
        )
    }
}
