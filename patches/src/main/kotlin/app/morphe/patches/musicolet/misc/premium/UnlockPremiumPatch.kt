/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.premium

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.musicolet.misc.fix.cast.castPluginAccessPatch
import app.morphe.patches.musicolet.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.musicolet.misc.pairip.restoreVirtualizedMethodsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro features.",
) {
    compatibleWith(AppCompatibilities.MUSICOLET)

    availability(requireArm64)

    dependsOn(
        removePairipVirtualizationPatch,
        removePairipProtectionPatch,
        restoreVirtualizedMethodsPatch,
        spoofSignaturePatch,
        castPluginAccessPatch,
    )

    execute {
        val accessLabels = ProAccessLabelFingerprint.matchAll()
        val flagPairs = accessLabels.map { it.flagAt(0) to it.flagAt(2) }.distinct()
        val (lifetime, subscription) = flagPairs.singleOrNull()
            ?: throw PatchException("Pro access labels read different flags: $flagPairs")

        proAccessFingerprint(lifetime, subscription).matchSingle().method.returnEarly(true)

        accessLabels.forEach { it.readFirstFlagAsTrue() }
        proPurchaseStateFingerprint(lifetime).matchSingle().readFirstFlagAsTrue()
    }
}

private fun Match.flagAt(filterIndex: Int) =
    instructionMatches[filterIndex].instruction.getReference<FieldReference>()!!

private fun Match.readFirstFlagAsTrue() {
    val read = instructionMatches.first()
    val register = read.getInstruction<OneRegisterInstruction>().registerA
    method.replaceInstruction(read.index, "const/16 v$register, 0x1")
}
