/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yuka.misc.signin

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64StandardInstall
import app.morphe.patches.shared.misc.signature.replaceString
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.patches.yuka.misc.pairip.guardVirtualizedTasksPatch
import app.morphe.util.matchSingle
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

internal const val TOKEN_SEED_CLASS = "Lapp/hxreborn/extension/shared/AppCheckTokenSeed;"

private val APP_CHECK_TOKEN_FORMAT = Regex("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$")

private val EXPIRY_CLAIM = Regex("\"exp\"\\s*:\\s*(\\d+)")

@Suppress("unused")
val signInTokenPatch = bytecodePatch(
    name = "Sign-in token",
    description = "Signs in on a standard install with an App Check token. " +
        "Requires a token copied from Yuka installed from Google Play on a rooted phone. " +
        "Steps are in the Yuka section of the README.",
    default = false,
) {
    compatibleWith(AppCompatibilities.YUKA)

    availability(requireArm64StandardInstall)

    dependsOn(guardVirtualizedTasksPatch, removePairipProtectionPatch)

    extendWith("extensions/extension.mpe")

    val appCheckToken by stringOption(
        key = "appCheckToken",
        title = "App Check token",
        description = "Copy it from Yuka installed from Google Play on a rooted phone. Steps: github.com/hxreborn/morphe-patches#yuka",
        required = true,
    ) {
        it != null && APP_CHECK_TOKEN_FORMAT.matches(it.trim())
    }

    execute {
        val token = appCheckToken!!.trim()
        val claims = String(Base64.getUrlDecoder().decode(token.split('.')[1].trimEnd('=')))
        val expiry = EXPIRY_CLAIM.find(claims)?.groupValues?.get(1)?.toLong()
            ?: throw PatchException("App Check token has no expiry")
        if (Instant.ofEpochSecond(expiry).isBefore(Instant.now())) {
            throw PatchException("App Check token expired at ${Instant.ofEpochSecond(expiry)}")
        }

        AppCheckTokenFingerprint.matchSingle().method.replaceString("<app-check-token>", token)

        FirebaseInitProviderOnCreateFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-virtual { p0 }, Landroid/content/ContentProvider;->getContext()Landroid/content/Context;
                move-result-object v0
                invoke-static { v0 }, $TOKEN_SEED_CLASS->seed(Landroid/content/Context;)V
            """,
        )

        val certificateSha1 = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest(packageMetadata.stockSigningCertificate().encoded),
        )
        CertificateSha1Fingerprint.matchSingle().method.addInstructions(
            0,
            """
                const-string v0, "$certificateSha1"
                const/4 v1, 0x0
                invoke-static {v0, v1}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B
                move-result-object v0
                return-object v0
            """,
        )
    }
}
