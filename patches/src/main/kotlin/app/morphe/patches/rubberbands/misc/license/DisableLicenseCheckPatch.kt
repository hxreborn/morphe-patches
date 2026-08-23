/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rubberbands.misc.license

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

internal val disableLicenseCheckPatch = bytecodePatch {
    execute {
        InitializeLicenseCheckFingerprint.matchSingle().method.returnEarly()
    }
}
