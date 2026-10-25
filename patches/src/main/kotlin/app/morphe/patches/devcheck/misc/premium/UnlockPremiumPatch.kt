/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.devcheck.misc.pairip.restoreVirtualizedMethodsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import java.security.MessageDigest
import java.util.Base64

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the Pro tools, tests, themes and colors.",
) {
    compatibleWith(AppCompatibilities.DEVCHECK)

    dependsOn(restoreVirtualizedMethodsPatch)

    availability(requireArm64)

    execute {
        val stockCertificateSha1Base64 = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest(packageMetadata.stockSigningCertificate().encoded),
        )
        SigningCertificateDigestFingerprint.matchSingle().method.returnEarly(stockCertificateSha1Base64)

        IsPremiumFingerprint.matchSingle().method.returnEarly(true)

        val premiumState = PremiumStateFingerprint.matchSingle().method
        val liveDataConstructor = liveDataConstructorFingerprint(premiumState.returnType).matchSingle().method
        val setValue = LiveDataSetValueFingerprint.matchSingle().method
        val registers = premiumState.getFreeRegisterProvider(0, 2)
        val liveDataRegister = registers.getFreeRegister4Bit()
        val valueRegister = registers.getFreeRegister4Bit()
        premiumState.addInstructions(
            0,
            """
                new-instance v$liveDataRegister, ${premiumState.returnType}
                const/4 v$valueRegister, 0x0
                invoke-direct {v$liveDataRegister, v$valueRegister}, $liveDataConstructor
                sget-object v$valueRegister, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                invoke-virtual {v$liveDataRegister, v$valueRegister}, $setValue
                return-object v$liveDataRegister
            """,
        )
    }
}
