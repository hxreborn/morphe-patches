/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/ee7381bebcff21e9d24c484f8104c544ed97296e
 * Commit ee7381bebcff21e9d24c484f8104c544ed97296e (2026-09-28),
 * patches/src/main/kotlin/hoodles/morphe/patches/duolingo/shared/integrity/DisableLoginIntegrityPatch.kt
 */
package app.morphe.patches.duolingo.shared.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.duolingo.shared.constructorWritingFingerprint
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val disableLoginIntegrityPatch = bytecodePatch {
    execute {
        val (signalRead, emptySignalRead, _) = EmailLoginRequestSerializeFingerprint.instructionMatches
        val signalField = signalRead.instruction.getReference<FieldReference>()!!
        val emptySignal = emptySignalRead.instruction.getReference<FieldReference>()!!

        constructorWritingFingerprint(signalField).let {
            val signalWriteIndex = it.instructionMatches.single().index
            val signalRegister = it.method.getInstruction<TwoRegisterInstruction>(signalWriteIndex).registerA
            it.method.addInstruction(signalWriteIndex, "sget-object v$signalRegister, $emptySignal")
        }
    }
}
