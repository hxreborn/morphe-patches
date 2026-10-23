/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel

internal fun MutableMethod.returnEarlyWhen(check: String, returnsNull: Boolean = false) {
    val flag = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
    val returnInstructions = if (returnsNull) {
        "const/4 v$flag, 0x0\n            return-object v$flag"
    } else {
        "return-void"
    }
    addInstructionsWithLabels(
        0,
        """
            invoke-static { }, $check
            move-result v$flag
            if-eqz v$flag, :continue
            $returnInstructions
        """,
        ExternalLabel("continue", getInstruction(0)),
    )
}
