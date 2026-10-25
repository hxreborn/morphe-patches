/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cloneapp.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal fun serverUploadFingerprint(endpoint: String) = Fingerprint(
    filters = listOf(
        string("?fn=$endpoint", comparison = StringComparisonType.ENDS_WITH),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
    ),
)
