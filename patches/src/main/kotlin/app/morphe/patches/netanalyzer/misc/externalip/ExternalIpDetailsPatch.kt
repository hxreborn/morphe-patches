/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.externalip

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.FEATURES_CLASS
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.composableParameters
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.netanalyzer.shared.requireNibbleRegisters
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTERNAL_IP_ROW_IPV6 = "ipv6"
private const val BUTTON_LABEL = "buttonLabel"
private const val BUTTON_ON_CLICK = "onButtonClick"

@Suppress("unused")
val externalIpDetailsPatch = bytecodePatch(
    name = "External IP details",
    description = "Adds a Details button to the External IP rows that shows the address's hostname, " +
        "network operator and location.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(patchesMenuPatch)

    execute {
        addFeatureActivity("ExternalIpActivity")

        val buttonRowMatch = ButtonRowFingerprint.matchSingle()
        val buttonRow = buttonRowMatch.originalMethod
        val parameters = buttonRowMatch.composableParameters()
        val externalIpRow = externalIpRowFingerprint(buttonRow).matchSingle()
        val (_, stateRead, buttonCall) = externalIpRow.instructionMatches
        val method = externalIpRow.method
        val callIndex = buttonCall.index
        val call = method.getInstruction<RegisterRangeInstruction>(callIndex)
        val labelRegister = call.startRegister + parameters.indexOf(BUTTON_LABEL)
        val onClickRegister = call.startRegister + parameters.indexOf(BUTTON_ON_CLICK)
        val onClickType = buttonRow.parameterTypes[parameters.indexOf(BUTTON_ON_CLICK)]

        val stateRegister = method.getInstruction<FiveRegisterInstruction>(stateRead.index).registerC

        if (externalIpRow.composableParameters().firstOrNull() != EXTERNAL_IP_ROW_IPV6) {
            throw PatchException("$EXTERNAL_IP_ROW does not take $EXTERNAL_IP_ROW_IPV6 as its first parameter")
        }
        val ipv6Register = method.implementation!!.registerCount - MethodUtil.getParameterRegisterCount(method)
        requireNibbleRegisters(labelRegister, onClickRegister, stateRegister, ipv6Register)

        method.addInstructions(
            callIndex,
            """
                invoke-static { v$labelRegister, v$stateRegister }, $FEATURES_CLASS->externalIpLabel(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;
                move-result-object v$labelRegister
                invoke-static { v$onClickRegister, v$stateRegister, v$ipv6Register }, $FEATURES_CLASS->onExternalIpClick(Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/Object;
                move-result-object v$onClickRegister
                check-cast v$onClickRegister, $onClickType
            """,
        )
    }
}
