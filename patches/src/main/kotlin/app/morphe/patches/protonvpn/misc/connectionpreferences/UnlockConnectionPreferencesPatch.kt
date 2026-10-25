/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.connectionpreferences

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonvpn.misc.anchors.VpnUserIsFreeUserFingerprint
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheck
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheckInLambdaOf
import app.morphe.patches.protonvpn.misc.restrictions.clearConstructorProperty
import app.morphe.patches.protonvpn.misc.restrictions.keepUserSetting
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction20t
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockConnectionPreferencesPatch = bytecodePatch(
    name = "Unlock connection preferences",
    description = "Unlocks the default connection, recent connections and excluded locations on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("unlockConnectionPreferences")
        clearConstructorProperty(ConnectionPreferencesToStringFingerprint, "isFreeUser")
        keepUserSetting(defaultConnectionRestrictionFingerprint())
        clearFreeUserCheck(recentsRecordingFingerprint())
        clearFreeUserCheckInLambdaOf(RecentsListViewStateFlowFingerprint)
        clearLastFreeUserCheckBefore(defaultConnectionRecentsFingerprint().matchSingle())
        clearFreeUserCheck(quickConnectIntentFingerprint())
        val connectionCardLabel = connectionCardLabelFingerprint()
        if (connectionCardLabel.matchOrNull() != null) {
            clearFreeUserCheck(connectionCardLabel)
        } else {
            clearLastFreeUserCheckBefore(inlinedConnectionCardFingerprint().matchSingle())
        }

        val recentIdLookup = defaultConnectionSettingFingerprint().matchOrNull()
        if (recentIdLookup != null) {
            val recentIdLookupIndex = recentIdLookup.instructionMatches[1].index
            recentIdLookup.alwaysTakeLastIfEqz { it.target.location.index == recentIdLookupIndex }
        } else {
            inlinedDefaultConnectionSettingFingerprint().matchSingle().run {
                val recentCheck = instructionMatches.first().index
                alwaysTakeLastIfEqz { it.location.index < recentCheck && it.target.location.index <= recentCheck }
            }
        }
    }
}

private fun Match.alwaysTakeLastIfEqz(isTarget: (BuilderOffsetInstruction) -> Boolean) {
    val branch = method.instructions.filterIsInstance<BuilderOffsetInstruction>().last {
        it.opcode == Opcode.IF_EQZ && isTarget(it)
    }
    method.replaceInstruction(branch.location.index, BuilderInstruction20t(Opcode.GOTO_16, branch.target))
}

private fun BytecodePatchContext.clearLastFreeUserCheckBefore(anchor: Match) {
    val isFreeUser = VpnUserIsFreeUserFingerprint.originalMethod
    val instructions = anchor.method.instructions
    val callIndex = instructions.take(anchor.instructionMatches.last().index).indexOfLast {
        it.getReference<MethodReference>()?.let { reference ->
            reference.definingClass == isFreeUser.definingClass && reference.name == isFreeUser.name &&
                reference.parameterTypes == isFreeUser.parameterTypes
        } == true
    }
    if (callIndex < 0) throw PatchException("No free-user check before the anchor in ${anchor.method}")
    val register = (instructions[callIndex + 1] as OneRegisterInstruction).registerA
    anchor.method.addInstruction(callIndex + 2, "const/16 v$register, 0x0")
}
