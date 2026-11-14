/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.getMutableMethod
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.torrent.ProStatusFingerprint
import app.morphe.patches.shared.misc.torrent.removeAdsPatch
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro features and removes ads.",
) {
    compatibleWith(AppCompatibilities.UTORRENT)
    availability(requireArm64)
    dependsOn(removeAdsPatch)

    execute {
        val (_, paidStatusAssignment) = ProStatusFingerprint.matchSingle().instructionMatches
        val paidStatus = paidStatusAssignment.instruction.getReference<FieldReference>()!!
        proStatusGetterFingerprint(paidStatus.type).matchSingle().method.addInstructions(
            0,
            """
                sget-object v0, $paidStatus
                return-object v0
            """,
        )
        val (navigationCheck, _) = ProNavigationFingerprint.matchSingle().instructionMatches
        navigationCheck.instruction.getReference<MethodReference>()!!.getMutableMethod().returnEarly(true)
    }
}
