/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.restrictions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal fun MutableMethod.filterReturnValue(filter: String) {
    findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
        val register = getInstruction<OneRegisterInstruction>(index).registerA
        replaceInstruction(index, "invoke-static/range { v$register .. v$register }, $filter")
        addInstructions(
            index + 1,
            """
                move-result-object v$register
                check-cast v$register, $returnType
                return-object v$register
            """,
        )
    }
}
