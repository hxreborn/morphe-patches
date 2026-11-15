/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bittorrent.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.returnTrueFrom
import app.morphe.patches.shared.misc.torrent.ProStatusFingerprint
import app.morphe.patches.shared.misc.torrent.removeAdsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro settings and removes ads.",
) {
    compatibleWith(AppCompatibilities.BITTORRENT)
    availability(requireArm64)
    dependsOn(removeAdsPatch)

    execute {
        returnTrueFrom("Lapp/hxreborn/extension/bittorrent/PatchesSettingsActivity;", "hasPremiumPatch")
        val (_, paidStatusAssignment) = ProStatusFingerprint.matchSingle().instructionMatches
        val paidStatus = paidStatusAssignment.instruction.getReference<FieldReference>()!!
        val (_, restoredStatusAssignment) = RestoreProStatusFingerprint.matchSingle().instructionMatches
        val restoredStatus = restoredStatusAssignment.instruction.getReference<FieldReference>()!!
        check(restoredStatus.type == paidStatus.type) { "Pro status fields have different types" }
        proStatusReadFingerprint(restoredStatus).matchAllMethodIndicesForEach { index ->
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "sget-object v$register, $paidStatus")
        }
    }
}
