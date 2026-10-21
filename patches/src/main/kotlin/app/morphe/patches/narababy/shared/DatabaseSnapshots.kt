/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.narababy.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal const val DATABASE_SNAPSHOTS_CLASS = "Lapp/hxreborn/extension/narababy/DatabaseSnapshots;"

internal fun BytecodePatchContext.rewriteDatabaseSnapshots(extensionCall: String) {
    DatabaseSnapshotToMapFingerprint.matchSingle().method.apply {
        findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { returnIndex ->
            val mapRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA
            addInstruction(
                returnIndex,
                "invoke-static { v$mapRegister }, $extensionCall",
            )
        }
    }
}
