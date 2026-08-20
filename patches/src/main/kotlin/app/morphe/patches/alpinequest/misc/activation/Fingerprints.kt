/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.alpinequest.misc.activation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object RestoreActivationFingerprint : Fingerprint(
    strings = listOf("Reg_Status"),
    filters = listOf(opcode(Opcode.IPUT_CHAR)),
)
