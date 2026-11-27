/*
 * Copyright (C) 2026 Paresh Maheshwari
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from Paresh-Maheshwari/paresh-patches:
 * https://gitlab.com/Paresh-Maheshwari/paresh-patches/-/commit/7c732a6b7791eade5d22fdd64f0d9addac57f56b
 * Commit 7c732a6b7791eade5d22fdd64f0d9addac57f56b (2026-04-29),
 * patches/src/main/kotlin/app/paresh/patches/protonvpn/misc/DisableTelemetryPatch.kt
 */
package app.morphe.patches.protonvpn.misc.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patches.protonvpn.misc.anchors.resourceFieldsPatch
import app.morphe.patches.protonvpn.misc.anchors.unitInstanceField
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.proton.VPN_SETTINGS_CLASS
import app.morphe.patches.shared.misc.proton.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val disableTelemetryPatch: Patch = patch(
    name = "Disable telemetry",
    description = "Stops sending usage statistics and diagnostics to Proton.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN, AppCompatibilities.PROTON_VPN_FDROID)
    dependsOn(patchesSettingsPatch, resourceFieldsPatch, disableAnalyticsCollectionPatch)

    execute {
        markPatchApplied(VPN_SETTINGS_CLASS, "disableTelemetry", disableTelemetryPatch)
        ObservabilityEnabledFingerprint.matchAll(2..2).forEach {
            val (_, _, flagRead) = it.instructionMatches
            it.method.addInstructions(
                flagRead.index + 1,
                "const/16 v${flagRead.getInstruction<OneRegisterInstruction>().registerA}, 0x0",
            )
        }
        val returnUnit = """
            sget-object p0, $unitInstanceField
            return-object p0
        """
        VpnTelemetryEventFingerprint.method.addInstructions(0, returnUnit)
        ObservabilityUploadFingerprint.method.addInstructions(0, returnUnit)
        CoreTelemetryEnqueueFingerprint.method.addInstructions(0, returnUnit)
        PaymentsObservabilityWorkerFingerprint.method.addInstructions(0, returnUnit)
    }
}
