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

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonvpn.misc.anchors.ServerGroupsMainScreenStateToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.setExtensionMember
import app.morphe.patches.protonvpn.misc.restrictions.filterConstructorProperty
import app.morphe.patches.protonvpn.misc.restrictions.freeAccountStatePatch
import app.morphe.patches.protonvpn.misc.restrictions.invertFreeServerCheckForFreeAccount
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied
import app.morphe.patches.shared.misc.smali.parameterRegister
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val FREE_SERVER_LOCATIONS = "Lapp/hxreborn/extension/protonvpn/FreeServerLocations;"

@Suppress("unused")
val showFreeServerLocationsPatch = bytecodePatch(
    name = "Show free server locations",
    description = "Lists free server locations in Countries and Search and connects to the one you pick. " +
        "Applies only to free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch, freeAccountStatePatch)

    execute {
        markPatchApplied("showFreeServerLocations")

        val freeLocationsHeader = resourceId(ResourceType.STRING, "free_connections_info_server_locations")
        invertFreeServerCheckForFreeAccount(serverListFilterFingerprint())

        serverGroupItemStateFingerprint().matchSingle().method.apply {
            val tierIndex = tierParameterIndex()
            val serverGroupItem = parameterTypes[tierIndex - 1].toString()
            setExtensionMember(
                "serverGroupTier",
                classDefBy(serverGroupItem).methods.single {
                    AccessFlags.ABSTRACT.isSet(it.accessFlags) && it.returnType == "I" && it.parameterTypes.isEmpty()
                }.name,
            )
            val itemRegister = parameterRegister(tierIndex - 1)
            val tierRegister = parameterRegister(tierIndex)
            addInstructions(
                0,
                """
                    invoke-static { $itemRegister, $tierRegister }, $FREE_SERVER_LOCATIONS->tierForAvailabilityCheck(Ljava/lang/Object;Ljava/lang/Integer;)Ljava/lang/Integer;
                    move-result-object $tierRegister
                """,
            )
        }

        CountriesHeaderLabelFingerprint.matchSingle().run {
            val label = instructionMatches.first()
            val labelRegister = label.getInstruction<OneRegisterInstruction>().registerA
            method.replaceInstruction(label.index, "const v$labelRegister, $freeLocationsHeader")
        }

        filterConstructorProperty(
            ServerGroupsMainScreenStateToStringFingerprint,
            "filterButtons",
            "$FREE_SERVER_LOCATIONS->resolveFilterButtons(Ljava/util/List;)Ljava/util/List;",
        )

        saveStateToStringFingerprints.forEach {
            filterConstructorProperty(
                it,
                "selectedFilter",
                "$FREE_SERVER_LOCATIONS->resolveSelectedFilter(Ljava/lang/Object;)Ljava/lang/Object;",
            )
        }

        searchResultSectionFingerprint().matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p2, p4 }, $FREE_SERVER_LOCATIONS->itemsVisibleToTier(Ljava/util/List;Ljava/lang/Integer;)Ljava/util/List;
                move-result-object p2
            """,
        )
    }
}
