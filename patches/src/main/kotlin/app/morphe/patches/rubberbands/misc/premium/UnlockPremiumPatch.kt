/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rubberbands.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.rubberbands.misc.license.disableLicenseCheckPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/rubberbands/PremiumUnlock;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks running and logging workouts, progress tracking and personal records " +
        "(AI workout generation is not included).",
) {
    compatibleWith(AppCompatibilities.RUBBER_BANDS)
    extendWith("extensions/extension.mpe")

    dependsOn(disableLicenseCheckPatch)

    execute {
        BuildCustomerInfoFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->grantEntitlement(Lorg/json/JSONObject;)V",
        )
    }
}
