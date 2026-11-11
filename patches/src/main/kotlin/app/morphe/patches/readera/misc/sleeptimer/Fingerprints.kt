/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.sleeptimer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import com.android.tools.smali.dexlib2.Opcode

internal object SleepTimerChoicesFingerprint : Fingerprint(
    name = "<clinit>",
    superclass = "Landroid/app/Fragment;",
    filters = listOf(
        literal(60),
        literal(15),
        literal(30),
        literal(45),
        fieldAccess(type = "[I", opcode = Opcode.SPUT_OBJECT),
    ),
)
