/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val TAMPER_ANALYTICS_EVENT = "is_tampered"
private const val SIGNATURE_VERIFIED = 1

internal val disableTamperDetectionPatch = bytecodePatch {
    dependsOn(stringKeyPatch, disableNativeTamperCheckPatch)

    execute {
        val tamperClass = TamperDialogFingerprint.matchSingle().classDef
        val signatureMatched = tamperClass.staticFields.single { it.type == "Z" }

        val check = Fingerprint(
            filters = listOf(
                fieldAccess(signatureMatched, Opcode.SPUT_BOOLEAN),
                decryptedString(TAMPER_ANALYTICS_EVENT),
            ),
        ).matchSingle()

        val writeIndex = check.instructionMatches.first().index
        val flagRegister = check.method.getInstruction<OneRegisterInstruction>(writeIndex).registerA
        check.method.replaceInstruction(writeIndex, "const/16 v$flagRegister, $SIGNATURE_VERIFIED")
        check.method.addInstruction(
            writeIndex + 1,
            "sput-boolean v$flagRegister, ${tamperClass.type}->${signatureMatched.name}:Z",
        )
    }
}
