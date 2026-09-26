/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.profiles

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheck
import app.morphe.patches.protonvpn.misc.restrictions.filterReturnValue
import app.morphe.patches.protonvpn.misc.restrictions.freeAccountStatePatch
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val FREE_SERVER_LOCATIONS = "Lapp/hxreborn/extension/protonvpn/FreeServerLocations;"

@Suppress("unused")
val unlockProfilesPatch = bytecodePatch(
    name = "Unlock profiles",
    description = "Unlocks profiles on free plans and limits them to free locations. " +
        "Profiles for other locations are hidden.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch, freeAccountStatePatch)

    execute {
        markPatchApplied("unlockProfiles")
        clearFreeUserCheck(ProfileAvailabilityFingerprint)
        VpnCountriesFingerprint.matchSingle().method.filterReturnValue(
            "$FREE_SERVER_LOCATIONS->countriesForAccount(Ljava/util/List;)Ljava/util/List;",
        )
        ProfileServerFilterFingerprint.matchSingle().run {
            val isFreeServerResult = instructionMatches.last()
            val register = isFreeServerResult.getInstruction<OneRegisterInstruction>().registerA
            method.addInstructions(
                isFreeServerResult.index + 1,
                """
                    invoke-static { v$register }, $FREE_SERVER_LOCATIONS->shouldExcludeServer(Z)Z
                    move-result v$register
                """,
            )
        }
        ProfileCitiesFingerprint.matchSingle().run {
            val serverList = instructionMatches.last()
            val register = serverList.getInstruction<OneRegisterInstruction>().registerA
            method.addInstructions(
                serverList.index + 1,
                """
                    invoke-static { v$register }, $FREE_SERVER_LOCATIONS->serversForAccount(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$register
                """,
            )
        }
        ProfilesListStateFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p1 }, $FREE_SERVER_LOCATIONS->profilesForAccount(Ljava/util/List;)Ljava/util/List;
                move-result-object p1
            """,
        )
        profileTypesFingerprints.forEach { fingerprint ->
            fingerprint.matchSingle().method.filterReturnValue(
                "$FREE_SERVER_LOCATIONS->profileTypesForAccount(Ljava/util/List;)Ljava/util/List;",
            )
        }
    }
}
