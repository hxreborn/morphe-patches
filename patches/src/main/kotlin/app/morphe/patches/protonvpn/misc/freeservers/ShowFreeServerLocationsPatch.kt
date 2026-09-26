/*
 * Copyright (C) 2026 Rushi Ranpise
 * Copyright (C) 2026 Paresh Maheshwari
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from rushiranpise/morphe-patches:
 * https://github.com/rushiranpise/morphe-patches/commit/81207e12513860720ac4f56c90d582a0c7e008b6
 * Commit 81207e12513860720ac4f56c90d582a0c7e008b6 (2026-09-15),
 * patches/src/main/kotlin/app/template/patches/protonvpn/premium/ProtonVpnPremiumPatch.kt
 */
package app.morphe.patches.protonvpn.misc.freeservers

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.protonvpn.misc.restrictions.filterReturnValue
import app.morphe.patches.protonvpn.misc.restrictions.freeAccountStatePatch
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val FREE_SERVER_LOCATIONS = "Lapp/hxreborn/extension/protonvpn/FreeServerLocations;"

@Suppress("unused")
val showFreeServerLocationsPatch = bytecodePatch(
    name = "Show free server locations",
    description = "Lists free server locations in Countries and Search and connects to the one you pick. " +
        "Applies only to free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch, resourceMappingPatch, freeAccountStatePatch)

    execute {
        markPatchApplied("showFreeServerLocations")

        val freeLocationsHeader = getResourceId(ResourceType.STRING, "free_connections_info_server_locations")
            ?: throw PatchException("Missing string: free_connections_info_server_locations")
        ServerListFilterFingerprint.matchSingle().run {
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

        ServerGroupItemStateFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p1, p2 }, $FREE_SERVER_LOCATIONS->tierForAvailabilityCheck(Ljava/lang/Object;Ljava/lang/Integer;)Ljava/lang/Integer;
                move-result-object p2
            """,
        )

        CountriesHeaderLabelFingerprint.matchSingle().run {
            val isFreeUser = instructionMatches.first().getInstruction<FiveRegisterInstruction>().registerD
            val label = instructionMatches.last()
            val labelRegister = label.getInstruction<OneRegisterInstruction>().registerA
            method.addInstructionsWithLabels(
                label.index + 1,
                """
                    if-eqz v$isFreeUser, :label_chosen
                    const v$labelRegister, $freeLocationsHeader
                """,
                ExternalLabel("label_chosen", method.getInstruction(label.index + 1)),
            )
        }

        ServerGroupsMainScreenStateFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p3 }, $FREE_SERVER_LOCATIONS->resolveFilterButtons(Ljava/util/List;)Ljava/util/List;
                move-result-object p3
            """,
        )

        selectedFilterFingerprints.forEach { fingerprint ->
            fingerprint.matchSingle().method.filterReturnValue(
                "$FREE_SERVER_LOCATIONS->resolveSelectedFilter(Ljava/lang/Object;)Ljava/lang/Object;",
            )
        }

        SearchResultSectionFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p2, p4 }, $FREE_SERVER_LOCATIONS->itemsVisibleToTier(Ljava/util/List;Ljava/lang/Integer;)Ljava/util/List;
                move-result-object p2
            """,
        )
    }
}
