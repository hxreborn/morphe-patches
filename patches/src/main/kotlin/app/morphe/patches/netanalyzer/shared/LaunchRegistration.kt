/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.matchSingle

context(context: BytecodePatchContext)
internal fun registerAtLaunch(receiver: String, registration: String, classes: List<String> = emptyList()) {
    val onCreate = MainActivityOnCreateFingerprint.matchSingle().method
    val registers = onCreate.getFreeRegisterProvider(0, classes.size + 1)
    val classRegisters = classes.map { registers.getFreeRegister4Bit() }
    val registrationRegister = registers.getFreeRegister4Bit()
    onCreate.addInstructions(
        0,
        buildString {
            classes.zip(classRegisters).forEach { (type, register) -> appendLine("const-class v$register, $type") }
            appendLine("const-string v$registrationRegister, \"$registration\"")
            appendLine("invoke-static { ${(classRegisters + registrationRegister).joinToString { "v$it" }} }, $receiver")
        },
    )
}
