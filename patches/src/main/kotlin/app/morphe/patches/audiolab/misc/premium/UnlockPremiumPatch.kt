/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audiolab.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipKeyImportPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.util.ReferenceUtil
import java.security.MessageDigest

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro tools and removes ads, reward videos and upgrade prompts. " +
        "The AI tools are not included.",
) {
    compatibleWith(AppCompatibilities.AUDIOLAB)

    dependsOn(removePairipKeyImportPatch, removePairipVirtualizationPatch, removePairipProtectionPatch)

    availability(requireArm64)

    execute {
        ProUserCheckFingerprint.matchSingle().method.returnEarly(true)

        val stockCertificateSha256 = MessageDigest.getInstance("SHA-256")
            .digest(packageMetadata.stockSigningCertificate().encoded)
            .joinToString("") { "%02X".format(it) }

        AppSignatureHashFingerprint.matchSingle().method.returnEarly(stockCertificateSha256)

        val priceCallback = FreeProductPriceFingerprint.matchSingle().method
        val freeProductFlag = priceCallback.implementation!!.instructions
            .filter { it.opcode == Opcode.SPUT_BOOLEAN }
            .map { it.getReference<FieldReference>()!! }
            .distinct()
            .single()
        priceCallback.returnEarly()

        val flagClassInitializer = mutableClassDefBy(freeProductFlag.definingClass)
            .methods.single { it.name == "<clinit>" }
        val returnIndex = flagClassInitializer.implementation!!.instructions.count() - 1
        val register = flagClassInitializer.getFreeRegisterProvider(returnIndex, 1).getFreeRegister4Bit()
        flagClassInitializer.addInstructions(
            returnIndex,
            """
                const/4 v$register, 0x1
                sput-boolean v$register, ${ReferenceUtil.getFieldDescriptor(freeProductFlag)}
            """,
        )
    }
}
