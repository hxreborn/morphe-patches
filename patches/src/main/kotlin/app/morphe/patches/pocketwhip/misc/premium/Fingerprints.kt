/*
 * Copyright (C) 2026 Bogat25
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pocketwhip.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * `toString()` of the whip data class. The boolean appended right after the ", isAvailable=" label
 * is the field that tells whether a whip is owned.
 *
 * Matched only by the label and `StringBuilder` calls, so it does not depend on class, field or
 * method names.
 */
internal object WhipToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string(", isAvailable="),
        methodCall(
            definingClass = "Ljava/lang/StringBuilder;",
            name = "append",
            parameters = listOf("Z"),
            location = MatchAfterWithin(5),
        ),
    ),
)
