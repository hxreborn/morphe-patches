/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/browser/BaseBrowserFragment.kt
 */
package app.morphe.patches.firefox.layout.fullscreentoast

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val TOAST = "Landroid/widget/Toast;"

internal object FullscreenToastFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(
            opcode = Opcode.SGET,
            definingClass = "Lorg/mozilla/fenix/R\$string;",
            name = "exit_fullscreen_with_gesture_short",
        ),
        methodCall(definingClass = TOAST, name = "makeText"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(definingClass = TOAST, name = "show", location = MatchAfterImmediately()),
    ),
)
