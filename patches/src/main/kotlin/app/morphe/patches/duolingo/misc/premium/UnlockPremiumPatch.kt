/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/ee7381bebcff21e9d24c484f8104c544ed97296e
 * Commit ee7381bebcff21e9d24c484f8104c544ed97296e (2026-09-28),
 * patches/src/main/kotlin/hoodles/morphe/patches/duolingo/premium/EnablePremiumPatch.kt
 */
package app.morphe.patches.duolingo.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.removeFlags
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.duolingo.misc.settings.PATCH_SETTINGS_CLASS
import app.morphe.patches.duolingo.misc.settings.markPatchApplied
import app.morphe.patches.duolingo.misc.settings.patchesSettingsPatch
import app.morphe.patches.duolingo.shared.integrity.disableLoginIntegrityPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.fieldByName
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch: BytecodePatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Super, or Max from the patches menu, and keeps energy full. " +
        "Max Video Call does not work.",
) {
    compatibleWith(AppCompatibilities.DUOLINGO)

    dependsOn(disableLoginIntegrityPatch, patchesSettingsPatch)

    execute {
        markPatchApplied("unlockPremium", unlockPremiumPatch, withDescription = false)

        val (subscriberLevelRead, _, hasPlusRead, isPaidWrite) = UserIsPaidFingerprint.instructionMatches
        val (subscriberLevelField, hasPlusField, isPaidField) = listOf(subscriberLevelRead, hasPlusRead, isPaidWrite)
            .map { it.instruction.getReference<FieldReference>()!! }
        val (_, _, hasMaxWrite) = UserHasMaxFingerprint.instructionMatches
        val hasMaxField = hasMaxWrite.instruction.getReference<FieldReference>()!!
        val unlockedFlags = listOf(isPaidField, hasPlusField)
        (unlockedFlags + hasMaxField + subscriberLevelField).forEach {
            UserIsPaidFingerprint.classDef.fieldByName(it.name).removeFlags(AccessFlags.FINAL)
        }

        LoggedInConstructorFingerprint.method.apply {
            addInstructionsWithLabels(
                instructions.lastIndex,
                """
                    invoke-static { }, $PATCH_SETTINGS_CLASS->isPremiumUnlocked()Z
                    move-result p0
                    if-eqz p0, :keep_user
                    const/4 p0, 0x1
                    ${unlockedFlags.joinToString("\n") { "iput-boolean p0, p1, $it" }}
                    sget-object p0, $SUBSCRIBER_LEVEL_CLASS->PREMIUM:$SUBSCRIBER_LEVEL_CLASS
                    iput-object p0, p1, $subscriberLevelField
                    invoke-static { }, $PATCH_SETTINGS_CLASS->isMaxUnlocked()Z
                    move-result p0
                    iput-boolean p0, p1, $hasMaxField
                    if-eqz p0, :keep_user
                    sget-object p0, $SUBSCRIBER_LEVEL_CLASS->GOLD:$SUBSCRIBER_LEVEL_CLASS
                    iput-object p0, p1, $subscriberLevelField
                """,
                ExternalLabel("keep_user", instructions.last()),
            )
        }

        EnergyConfigConstructorsFingerprint.matchAll().forEach { match ->
            val (energyWrite, maxEnergyWrite) = match.instructionMatches.map { it.index }
            val (energy, maxEnergy) = listOf(energyWrite, maxEnergyWrite).map {
                match.method.getInstruction<TwoRegisterInstruction>(it).registerA
            }
            match.method.addInstructions(
                energyWrite,
                """
                    invoke-static { v$energy, v$maxEnergy }, $PATCH_SETTINGS_CLASS->energyOrMax(II)I
                    move-result v$energy
                """,
            )
        }
    }
}
