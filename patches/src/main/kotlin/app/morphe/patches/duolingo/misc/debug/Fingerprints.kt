/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/ee7381bebcff21e9d24c484f8104c544ed97296e
 * Commit ee7381bebcff21e9d24c484f8104c544ed97296e (2026-09-28),
 * patches/src/main/kotlin/hoodles/morphe/patches/duolingo/misc/debug/Fingerprints.kt
 */
package app.morphe.patches.duolingo.misc.debug

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object BuildTargetFingerprint : Fingerprint(
    strings = listOf("BUILD_TARGET"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN),
        opcode(Opcode.IF_EQZ, MatchAfterImmediately()),
        string("debug", location = MatchAfterImmediately()),
    ),
)
