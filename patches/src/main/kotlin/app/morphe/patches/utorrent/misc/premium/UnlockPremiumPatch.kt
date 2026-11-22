/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getMutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.returnTrueFrom
import app.morphe.patches.shared.misc.torrent.ProStatusFingerprint
import app.morphe.patches.shared.misc.torrent.removeAdsPatch
import app.morphe.patches.shared.misc.extension.utorrentSettingsExtensionPatch

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Pro features and removes ads.",
) {
    dependsOn(utorrentSettingsExtensionPatch)
    compatibleWith(AppCompatibilities.UTORRENT)
    availability(requireArm64)
    dependsOn(removeAdsPatch)

    execute {
        returnTrueFrom("Lapp/hxreborn/extension/utorrent/PatchesSettingsActivity;", "hasPremiumPatch")
        val (_, paidStatusAssignment) = ProStatusFingerprint.match().instructionMatches
        val paidStatus = paidStatusAssignment.fieldReference
        proStatusGetterFingerprint(paidStatus.type).method.addInstructions(
            0,
            """
                sget-object v0, $paidStatus
                return-object v0
            """,
        )
        val (navigationCheck, _) = ProNavigationFingerprint.match().instructionMatches
        navigationCheck.methodReference.getMutableMethod().returnEarly(true)
    }
}
