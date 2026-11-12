/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.depthpix.misc.installer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object InstallerHeaderFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("X-AmoledPix-Installer"),
        methodCall(
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "V",
            location = InstructionLocation.MatchAfterImmediately(),
        ),
    ),
)
