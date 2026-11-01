/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.misc.signin

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val RECAPTCHA_CLIENT_INIT_FAILED_CODE = "RECAPTCHA_CLIENT_INIT_FAILED"

private const val CLEAR_CREDENTIAL_CALLBACK_COUNT = 4

@Suppress("unused")
val fixSignInPatch = bytecodePatch(
    name = "Fix sign-in",
    description = "Fixes signing in with an email, phone number or social account.",
) {
    compatibleWith(AppCompatibilities.SHAHID)

    execute {
        val tokenRequest = RecaptchaTokenRequestFingerprint.matchSingle().method
        val failureCallbackType = tokenRequest.parameterTypes[2]

        tokenRequest.addInstructions(
            0,
            """
                const-string v0, "$RECAPTCHA_CLIENT_INIT_FAILED_CODE"
                const-string v1, ""
                invoke-interface { p3, v0, p1, v1, v1 }, $failureCallbackType->invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                return-void
            """,
        )

        IgnoredClearCredentialErrorFingerprint.matchAll(CLEAR_CREDENTIAL_CALLBACK_COUNT..CLEAR_CREDENTIAL_CALLBACK_COUNT).forEach { match ->
            match.method.addInstructions(
                match.instructionMatches.last().index,
                """
                    const/4 p1, 0x0
                    invoke-virtual { p0, p1 }, ${match.method.definingClass}->onResult(Ljava/lang/Object;)V
                """,
            )
        }
    }
}
