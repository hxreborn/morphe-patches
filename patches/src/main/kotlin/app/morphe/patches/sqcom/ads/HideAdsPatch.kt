/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.sqcom.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes splash, banner, native, interstitial and rewarded ads, " +
        "and the ad consent prompt.",
) {
    compatibleWith(AppCompatibilities.SQCOM)

    execute {
        val adListResponse = AdListResponseFingerprint.matchSingle()
        val moveResult = adListResponse.instructionMatches.last()
        val adListRegister = moveResult.getInstruction<OneRegisterInstruction>().registerA
        adListResponse.method.addInstruction(moveResult.index + 1, "const/16 v$adListRegister, 0x0")

        val consentRequest = ConsentRequestFingerprint.matchSingle().method
        val listenerType = consentRequest.parameterTypes.last().toString()
        val listenerMethod = consentListenerFingerprint(listenerType).matchSingle().method
        val listenerCall = "$listenerType->${listenerMethod.name}($FORM_ERROR_TYPE)V"
        val listenerRegister = consentRequest.implementation!!.registerCount - 1
        val nullFormError = consentRequest.getFreeRegisterProvider(0, 1, listenerRegister).getFreeRegister4Bit()
        consentRequest.addInstructions(
            0,
            """
                const/4 v$nullFormError, 0x0
                invoke-interface { v$listenerRegister, v$nullFormError }, $listenerCall
                return-void
            """,
        )
    }
}
