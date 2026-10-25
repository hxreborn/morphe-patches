/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.alpinequest.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

val bypassSignatureCheckPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.ALPINEQUEST)

    execute {
        HasValidSignatureFingerprint.matchSingle().method.returnEarly(true)
    }
}
