/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.account

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.PASS_SETTINGS_CLASS
import app.morphe.patches.shared.misc.proton.markPatchApplied

private const val USER_CHECK_SUCCESS_CLASS =
    "Lme/proton/core/auth/domain/usecase/PostLoginAccountSetup\$UserCheckResult\$Success;"

@Suppress("unused")
val removeFreeAccountsLimitPatch: Patch = patch(
    name = "Remove free accounts limit",
    description = "Removes the limit on signed-in free accounts.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS, AppCompatibilities.PROTON_PASS_FDROID)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied(PASS_SETTINGS_CLASS, "removeFreeAccountsLimit", removeFreeAccountsLimitPatch)

        FreeAccountsLimitCheckFingerprint.method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister()

            addInstructions(
                0,
                """
                    sget-object v$register, $USER_CHECK_SUCCESS_CLASS->INSTANCE:$USER_CHECK_SUCCESS_CLASS
                    return-object v$register
                """,
            )
        }
    }
}
