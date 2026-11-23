/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bittorrent.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.returnTrueFrom
import app.morphe.patches.shared.misc.torrent.ProStatusFingerprint
import app.morphe.patches.shared.misc.torrent.removeAdsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patches.shared.misc.extension.bittorrentSettingsExtensionPatch

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Pro settings and removes ads.",
) {
    dependsOn(bittorrentSettingsExtensionPatch)
    compatibleWith(AppCompatibilities.BITTORRENT)
    availability(requireArm64)
    dependsOn(removeAdsPatch)

    execute {
        returnTrueFrom("Lapp/hxreborn/extension/bittorrent/PatchesSettingsActivity;", "hasPremiumPatch")
        val (_, paidStatusAssignment) = ProStatusFingerprint.match().instructionMatches
        val paidStatus = paidStatusAssignment.fieldReference
        val (_, restoredStatusAssignment) = RestoreProStatusFingerprint.match().instructionMatches
        val restoredStatus = restoredStatusAssignment.fieldReference
        check(restoredStatus.type == paidStatus.type) { "Pro status fields have different types" }
        proStatusReadFingerprint(restoredStatus).matchAllMethodIndicesForEach { index ->
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "sget-object v$register, $paidStatus")
        }
    }
}
