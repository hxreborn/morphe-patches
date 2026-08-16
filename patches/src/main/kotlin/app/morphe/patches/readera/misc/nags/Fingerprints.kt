/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.nags

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object StartupNagDispatchFingerprint : Fingerprint(
    definingClass = "Lorg/readera/MainActivity;",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        methodCall(
            parameters = listOf("Landroid/app/Activity;"),
            returnType = "V",
            opcodes = listOf(Opcode.INVOKE_STATIC),
        ),
        methodCall(
            returnType = "V",
            opcodes = listOf(Opcode.INVOKE_STATIC),
            location = InstructionLocation.MatchAfterImmediately(),
        ),
    ),
)
