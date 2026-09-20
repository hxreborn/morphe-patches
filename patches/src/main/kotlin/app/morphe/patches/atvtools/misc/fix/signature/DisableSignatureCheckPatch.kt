/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atvtools.misc.fix.signature

import app.morphe.patches.all.misc.hex.hexPatch

private const val ARM32 = "lib/armeabi-v7a/liba.so"

internal val disableSignatureCheckPatch = hexPatch(ignoreMissingTargetFiles = true, block = {
    "80 b5 f7 f2 c4 ed 80 bd" asPatternTo "00 20 70 47 00 bf 00 bf" inFile ARM32
    "0b f0 94 fe" asPatternTo "af f3 00 80" inFile ARM32
    "f0 b5 03 af 2d e9 00 0f e3 b0" asPatternTo "70 47 03 af 2d e9 00 0f e3 b0" inFile ARM32
})
