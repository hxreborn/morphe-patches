/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.liapp

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle

val disableLiappTamperAlertPatch = bytecodePatch {
    execute {
        val match = LiappTamperAlertFingerprint.matchSingle()
        val (_, showAlert, _) = match.instructionMatches
        match.method.replaceInstruction(showAlert.index, "nop")
    }
}
