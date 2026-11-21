/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.devcheck.misc.pairip.restoreVirtualizedMethodsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.security.MessageDigest
import java.util.Base64

@Suppress("unused")
val unlockPremiumPatch = patch(
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

        val premiumStateMatch = PremiumStateFingerprint.matchSingle()
        val premiumState = premiumStateMatch.method
        val (_, _, mediatorInstantiation) = premiumStateMatch.instructionMatches
        val mediatorType = requireNotNull(mediatorInstantiation.instruction.getReference<TypeReference>()).type
        val (mode, _) = liveDataModeFingerprint(mediatorType, premiumState.returnType).matchSingle().instructionMatches
        val liveDataMode = mode.getInstruction<WideLiteralInstruction>().wideLiteral
        val liveDataConstructor = liveDataConstructorFingerprint(premiumState.returnType).matchSingle().method
        val setValue = LiveDataSetValueFingerprint.matchSingle().method
        val registers = premiumState.getFreeRegisterProvider(0, 2)
        val liveDataRegister = registers.getFreeRegister4Bit()
        val valueRegister = registers.getFreeRegister4Bit()
        premiumState.addInstructions(
            0,
            """
                new-instance v$liveDataRegister, ${premiumState.returnType}
                const/16 v$valueRegister, $liveDataMode
                invoke-direct {v$liveDataRegister, v$valueRegister}, $liveDataConstructor
                sget-object v$valueRegister, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                invoke-virtual {v$liveDataRegister, v$valueRegister}, $setValue
                return-object v$liveDataRegister
            """,
        )
    }
}
