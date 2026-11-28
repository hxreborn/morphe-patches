/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.writeit.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object DisableDeployFingerprint : Fingerprint(
    strings = listOf("DisableDeploy"),
    filters = listOf(
        string("DisableDeploy"),
        opcode(Opcode.CONST_4, MatchAfterImmediately()),
    ),
)
