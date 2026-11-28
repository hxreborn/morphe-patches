/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.inputspacing

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal val insetsImplementationRead = fieldAccess(opcode = Opcode.IGET_OBJECT)
internal val imeInsetsRead = methodCall(parameters = listOf("I"), returnType = "L", opcode = Opcode.INVOKE_VIRTUAL)
internal val imeInsetsResult = opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
internal val imeBottomRead = fieldAccess(type = "I", opcode = Opcode.IGET, location = MatchAfterImmediately())
internal val safeAreaBottomRead = fieldAccess(type = "I", opcode = Opcode.IGET)
internal val bottomInsetMaximum = methodCall(
    definingClass = "Ljava/lang/Math;",
    name = "max",
    parameters = listOf("I", "I"),
    returnType = "I",
    location = MatchAfterImmediately(),
)

internal object RootWindowInsetsFingerprint : Fingerprint(
    isStatic = true,
    returnType = "L",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "getRootWindowInsets",
            parameters = emptyList(),
            returnType = "Landroid/view/WindowInsets;",
        ),
    ),
)

internal object SafeAreaInsetsFingerprint : Fingerprint(
    strings = listOf("Safe area insets injected: top="),
    filters = listOf(
        insetsImplementationRead,
        imeInsetsRead,
        imeInsetsResult,
        imeBottomRead,
        safeAreaBottomRead,
        bottomInsetMaximum,
    ),
)
