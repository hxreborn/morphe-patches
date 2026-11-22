/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.networkflow.misc.fix.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

internal val disableArchiveSignatureCheckPatch = patch {
    compatibleWith(AppCompatibilities.NETWORKFLOW)

    execute {
        listOf(
            mainActivityArchiveSignatureFingerprint,
            networkServiceArchiveSignatureFingerprint,
            appFunctionArchiveSignatureFingerprint,
        ).forEach { fingerprint ->
            val match = fingerprint.match()
            val (signatureArray, _, _, _, _) = match.instructionMatches
            val registers = signatureArray.getInstruction<FiveRegisterInstruction>()
            val archiveHash = registers.registerC
            match.method.replaceInstruction(signatureArray.index, "const/4 v$archiveHash, 0x0")
            match.method.addInstruction(
                signatureArray.index + 1,
                "filled-new-array {v$archiveHash, v${registers.registerD}}, [Ljava/lang/String;",
            )
        }
    }
}
