/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/ee7381bebcff21e9d24c484f8104c544ed97296e
 * Commit ee7381bebcff21e9d24c484f8104c544ed97296e (2026-09-28),
 * patches/src/main/kotlin/hoodles/morphe/patches/duolingo/misc/rampup/UnlimitedRampupTimePatch.kt
 */
package app.morphe.patches.duolingo.misc.rampup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.duolingo.misc.settings.PATCH_SETTINGS_CLASS
import app.morphe.patches.duolingo.misc.settings.markPatchApplied
import app.morphe.patches.duolingo.misc.settings.patchesSettingsPatch
import app.morphe.patches.duolingo.shared.integrity.disableLoginIntegrityPatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlimitedRampUpTimePatch = bytecodePatch(
    name = "Unlimited RampUp time",
    description = "Holds the RampUp timer at one hour.",
    default = false,
) {
    compatibleWith(AppCompatibilities.DUOLINGO)

    dependsOn(disableLoginIntegrityPatch, patchesSettingsPatch)

    execute {
        markPatchApplied("unlimitedRampUpTime")

        rampupTimerStateConstructorFingerprints.forEach {
            it.method.addInstructions(
                0,
                """
                    invoke-static { p1 }, $PATCH_SETTINGS_CLASS->rampUpSecondsOrUnlimited(I)I
                    move-result p1
                """,
            )
        }
    }
}
