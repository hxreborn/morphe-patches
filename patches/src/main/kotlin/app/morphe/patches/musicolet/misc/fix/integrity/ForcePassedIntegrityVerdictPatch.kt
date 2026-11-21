/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.fix.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PASSED_VERDICT = 1

internal val forcePassedIntegrityVerdictPatch = patch {
    execute {
        val verdictRead = StoredIntegrityVerdictReadFingerprint.matchSingle()
        val (verdictKey, getIntCall, getIntResult) = verdictRead.instructionMatches
        val keyRegister = (verdictKey.instruction as OneRegisterInstruction).registerA
        val getIntKeyRegister = (getIntCall.instruction as FiveRegisterInstruction).registerD
        if (getIntKeyRegister != keyRegister) {
            throw PatchException(
                "SharedPreferences.getInt in ${verdictRead.originalMethod} reads v$getIntKeyRegister, " +
                    "expected the $STORED_VERDICT_KEY key in v$keyRegister",
            )
        }
        val verdictRegister = (getIntResult.instruction as OneRegisterInstruction).registerA
        verdictRead.method.addInstruction(getIntResult.index + 1, "const/4 v$verdictRegister, $PASSED_VERDICT")
    }
}
