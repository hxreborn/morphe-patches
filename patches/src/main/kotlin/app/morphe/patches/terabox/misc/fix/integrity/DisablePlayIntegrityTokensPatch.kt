/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.fix.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val disablePlayIntegrityTokensPatch = bytecodePatch {
    execute {
        StandardIntegrityTokenRequestFingerprint.matchSingle().method.apply {
            val providerIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IGET_OBJECT &&
                    getReference<FieldReference>()?.type == STANDARD_INTEGRITY_TOKEN_PROVIDER
            }
            val providerRegister = getInstruction<OneRegisterInstruction>(providerIndex).registerA

            addInstruction(providerIndex + 1, "const/4 v$providerRegister, 0x0")
        }

        ClassicIntegrityTokenRequestFingerprint.matchSingle().method.returnEarly()
    }
}
