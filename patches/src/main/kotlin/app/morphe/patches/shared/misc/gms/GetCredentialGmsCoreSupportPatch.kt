/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal val getCredentialGmsCoreSignInPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    dependsOn(gmsCoreSpoofedSignaturePatch, gmsCoreManifestPatch)

    execute {
        bindGmsCoreSignInTypes()
        renameCloudMessagingToGmsCore()

        GetCredentialHandlerFingerprint.matchSingle().let { match ->
            val (isEmpty, activityCheck, _, _, completeFailure) = match.instructionMatches.map { it.index }
            val method = match.method

            val serverClientIdRegister = method.getInstruction<FiveRegisterInstruction>(isEmpty).registerC
            val activityRegister = method.getInstruction<OneRegisterInstruction>(activityCheck).registerA
            val callbackRegister = method.getInstruction<FiveRegisterInstruction>(completeFailure).registerC

            method.addInstructions(
                activityCheck,
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

@Suppress("unused")
val getCredentialGmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Signs in through GmsCore instead of Google Play Services. " +
        "Requires GmsCore to be installed.",
) {
    compatibleWith(AppCompatibilities.JUJO, AppCompatibilities.MEMONEET, AppCompatibilities.STORYWISE)

    dependsOn(getCredentialGmsCoreSignInPatch)
}
