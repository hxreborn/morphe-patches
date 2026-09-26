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
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
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
    extendWith("extensions/extension.mpe")

    execute {
        UserInfoUpdateFingerprint.matchSingle().run {
            method.addInstruction(
                instructionMatches.first().index,
                "invoke-static { p1 }, $FREE_SERVER_LOCATIONS->onUserInfo(Ljava/lang/Object;)V",
            )
        }
        UserInfoInvalidateFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { }, $FREE_SERVER_LOCATIONS->onUserInfoInvalidated()V",
        )

        ServerListFilterFingerprint.matchSingle().run {
            val result = instructionMatches.last()
            val register = result.getInstruction<OneRegisterInstruction>().registerA
            method.addInstructions(
                result.index + 1,
                """
                    invoke-static { v$register }, $FREE_SERVER_LOCATIONS->shouldExcludeServer(Z)Z
                    move-result v$register
                """,
            )
        }

        ItemStateFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p1, p2 }, $FREE_SERVER_LOCATIONS->tierForAvailabilityCheck(Ljava/lang/Object;Ljava/lang/Integer;)Ljava/lang/Integer;
                move-result-object p2
            """,
        )

        ListHeaderFingerprint.matchSingle().run {
            val call = instructionMatches.first()
            val isFreeUser = call.getInstruction<FiveRegisterInstruction>().registerD
            method.addInstruction(call.index, "const/4 v$isFreeUser, 0x0")
        }

        MainScreenStateFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p3 }, $FREE_SERVER_LOCATIONS->resolveFilterButtons(Ljava/util/List;)Ljava/util/List;
                move-result-object p3
            """,
        )

        selectedFilterFingerprints.forEach { fingerprint ->
            fingerprint.matchSingle().method.apply {
                val index = instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                replaceInstruction(
                    index,
                    "invoke-static { v$register }, $FREE_SERVER_LOCATIONS->resolveSelectedFilter(Ljava/lang/Object;)Ljava/lang/Object;",
                )
                addInstructions(
                    index + 1,
                    """
                        move-result-object v$register
                        check-cast v$register, Lcom/protonvpn/android/redesign/countries/ui/ServerFilterType;
                        return-object v$register
                    """,
                )
            }
        }

        SearchResultSectionFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p2, p4 }, $FREE_SERVER_LOCATIONS->itemsForTier(Ljava/util/List;Ljava/lang/Integer;)Ljava/util/List;
                move-result-object p2
            """,
        )
    }
}
