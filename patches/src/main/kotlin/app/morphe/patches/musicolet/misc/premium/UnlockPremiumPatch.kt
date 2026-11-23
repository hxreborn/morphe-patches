/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.premium

import app.morphe.patcher.Match.InstructionMatch
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.musicolet.misc.fix.cast.castPluginAccessPatch
import app.morphe.patches.musicolet.misc.fix.integrity.forcePassedIntegrityVerdictPatch
import app.morphe.patches.musicolet.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.musicolet.misc.pairip.restoreVirtualizedMethodsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = patch(
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
        unlockNativeLicenseLevelPatch,
        forcePassedIntegrityVerdictPatch,
        markLicensedTierPatch,
    )

    execute {
        val accessLabels = ProAccessLabelFingerprint.matchAll()
        val flagPairs = accessLabels.map { label ->
            val (lifetimeFlag, _, subscriptionFlag) = label.instructionMatches
            lifetimeFlag.fieldReference to subscriptionFlag.fieldReference
        }.distinct()
        val (lifetime, subscription) = flagPairs.singleOrNull()
            ?: throw PatchException("Pro access labels read different flags: $flagPairs")

        proAccessFingerprint(lifetime, subscription).method.returnEarly(true)

        accessLabels.forEach { label ->
            val (lifetimeFlag) = label.instructionMatches
            label.method.readFlagAsTrue(lifetimeFlag)
        }
        val purchaseState = proPurchaseStateFingerprint(lifetime).match()
        val (lifetimeFlag) = purchaseState.instructionMatches
        purchaseState.method.readFlagAsTrue(lifetimeFlag)
    }
}

private fun MutableMethod.readFlagAsTrue(read: InstructionMatch) {
    val register = read.getInstruction<OneRegisterInstruction>().registerA
    replaceInstruction(read.index, "const/16 v$register, 0x1")
}
