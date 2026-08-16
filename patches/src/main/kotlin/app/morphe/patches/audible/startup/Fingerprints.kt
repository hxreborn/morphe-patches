/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audible.startup

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object NextActivityRouterFingerprint : Fingerprint(
    strings = listOf(
        "Launching next activity given intent action {}, {} data",
        "Bottom nav enabled, launching Main Nav Activity",
    ),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/String;", name = "toLowerCase"),
        opcode(Opcode.SPARSE_SWITCH, InstructionLocation.MatchAfterWithin(8)),
        opcode(Opcode.MOVE, InstructionLocation.MatchAfterImmediately()),
        string("my_library"),
        opcode(Opcode.PACKED_SWITCH),
    ),
)
