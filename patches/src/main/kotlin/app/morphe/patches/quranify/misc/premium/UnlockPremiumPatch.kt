/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.quranify.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.quranify.misc.integrity.bypassIntegrityCheckPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction

private const val ENTITLEMENT = "Quranify Premium"
private const val PRODUCT = "quranify_premium"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks downloading every surah, lyrics and tafsir, Android Auto, " +
        "background playback controls, and insights. Removes the Free Premium challenge " +
        "section from Settings.",
) {
    dependsOn(bypassIntegrityCheckPatch, removePairipProtectionPatch)
    compatibleWith(AppCompatibilities.QURANIFY)
    extendWith("extensions/extension.mpe")

    execute {
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)

        val settingsSections = SettingsSectionsFingerprint.matchSingle()
        val (_, _, _, _, arrayMatch) = settingsSections.instructionMatches
        val arrayRange = arrayMatch.getInstruction<RegisterRangeInstruction>()
        val arrayRegisters = arrayRange.startRegister until arrayRange.startRegister + arrayRange.registerCount
        val firstRegister = arrayRegisters.first
        val lastRegister = arrayRegisters.last
        val arrayType = arrayMatch.getInstruction<ReferenceInstruction>().reference
        val shiftDownByOne = (firstRegister + 1 until lastRegister)
            .joinToString("\n") { "move-object v$it, v${it + 1}" }

        settingsSections.method.replaceInstruction(
            arrayMatch.index,
            "filled-new-array/range {v$firstRegister .. v${lastRegister - 1}}, $arrayType",
        )
        settingsSections.method.addInstructions(arrayMatch.index, shiftDownByOne)
    }
}
