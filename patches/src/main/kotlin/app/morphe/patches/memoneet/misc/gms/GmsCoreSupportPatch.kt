/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.memoneet.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.GMS_CORE_SIGN_IN_CLASS
import app.morphe.patches.shared.misc.gms.bindGmsCoreSignInTypes
import app.morphe.patches.shared.misc.gms.gmsCoreSpoofedSignaturePatch
import app.morphe.patches.shared.misc.gms.redirectGmsPackageToGmsCore
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Signs in through GmsCore instead of Google Play Services. " +
        "Requires GmsCore to be installed.",
) {
    compatibleWith(AppCompatibilities.MEMONEET)
    extendWith("extensions/extension.mpe")

    dependsOn(gmsCoreSpoofedSignaturePatch)

    execute {
        bindGmsCoreSignInTypes()

        GetCredentialHandlerFingerprint.matchSingle().let { match ->
            val (isEmpty, activityRead, _, completeFailure) = match.instructionMatches.map { it.index }
            val method = match.method

            val serverClientIdRegister = method.getInstruction<FiveRegisterInstruction>(isEmpty).registerC
            val activityRegister = method.getInstruction<OneRegisterInstruction>(activityRead).registerA
            val callbackRegister = method.getInstruction<FiveRegisterInstruction>(completeFailure).registerC

            method.addInstructions(
                activityRead + 1,
                """
                    invoke-static { v$activityRegister, v$serverClientIdRegister, v$callbackRegister }, $GMS_CORE_SIGN_IN_CLASS->getCredential(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/Object;)V
                    return-void
                """,
            )
        }
    }

    finalize {
        redirectGmsPackageToGmsCore()
    }
}
