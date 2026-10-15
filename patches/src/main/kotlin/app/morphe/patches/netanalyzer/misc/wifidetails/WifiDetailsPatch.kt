/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.wifidetails

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.FEATURES_CLASS
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.composableParameters
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val NULL_REFERENCE = 0L
private const val ROW_ON_CLICK = "onClick"
private const val ROW_DATA = "data"

@Suppress("unused")
val wifiDetailsPatch = bytecodePatch(
    name = "Wi-Fi network details",
    description = "Adds a details page for each network in the Signal list with its channel range, " +
        "utilization, beamforming support, maximum speed and modulation.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(patchesMenuPatch)

    execute {
        addFeatureActivity("WifiDetailsActivity")

        val rowMatch = WifiSignalRowFingerprint.matchSingle()
        val row = rowMatch.originalMethod
        val rowCall = Fingerprint(
            filters = listOf(
                literal(NULL_REFERENCE),
                methodCall(
                    reference = row,
                    location = MatchAfterImmediately(),
                ),
            ),
        ).matchSingle()

        val nullIndex = rowCall.instructionMatches.first().index
        val nullRegister = rowCall.method.getInstruction<OneRegisterInstruction>(nullIndex).registerA
        val call = rowCall.method.getInstruction<FiveRegisterInstruction>(rowCall.instructionMatches.last().index)
        val parameters = rowMatch.composableParameters()
        val callRegisters = listOf(call.registerC, call.registerD, call.registerE, call.registerF, call.registerG)
        if (callRegisters[parameters.indexOf(ROW_ON_CLICK)] != nullRegister) {
            throw PatchException("The null loaded into v$nullRegister is not the onClick argument of $WIFI_SIGNAL_ROW")
        }
        val onClickType = row.parameterTypes[parameters.indexOf(ROW_ON_CLICK)]
        val dataRegister = callRegisters[parameters.indexOf(ROW_DATA)]

        rowCall.method.addInstructions(
            nullIndex + 1,
            """
                const-class v$nullRegister, $onClickType
                invoke-static { v$nullRegister, v$dataRegister }, $FEATURES_CLASS->onHotspotClick(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$nullRegister
                check-cast v$nullRegister, $onClickType
            """,
        )
    }
}
