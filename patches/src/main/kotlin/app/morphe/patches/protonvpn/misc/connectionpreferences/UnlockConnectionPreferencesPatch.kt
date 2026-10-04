/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.connectionpreferences

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheck
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheckInLambdaOf
import app.morphe.patches.protonvpn.misc.restrictions.unlockUserSetting
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction20t

@Suppress("unused")
val unlockConnectionPreferencesPatch = bytecodePatch(
    name = "Unlock connection preferences",
    description = "Unlocks the default connection, recent connections and excluded locations on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("unlockConnectionPreferences")
        unlockUserSetting(
            ConnectionPreferencesViewStateFingerprint,
            freeUserParameter = 2,
            DefaultConnectionRestrictionFingerprint,
        )
        clearFreeUserCheckInLambdaOf(ConnectingUpdatesRecentsFingerprint)
        clearFreeUserCheckInLambdaOf(RecentsListViewStateFlowFingerprint)
        clearFreeUserCheckInLambdaOf(DefaultConnectionViewStateFlowFingerprint)
        clearFreeUserCheck(quickConnectIntentFingerprint())
        clearFreeUserCheck(connectionCardLabelFingerprint())

        defaultConnectionSettingFingerprint().matchSingle().run {
            val recentIdIndex = instructionMatches[1].index
            val freeUserBranch = method.instructions.filterIsInstance<BuilderOffsetInstruction>().last {
                it.opcode == Opcode.IF_EQZ && it.target.location.index == recentIdIndex
            }
            method.replaceInstruction(
                freeUserBranch.location.index,
                BuilderInstruction20t(Opcode.GOTO_16, freeUserBranch.target),
            )
        }
    }
}
