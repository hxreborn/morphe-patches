/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.automatag.misc.signature

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object IsOriginalFingerprint : Fingerprint(
    definingClass = "Lhelpers/Auth;",
    name = "isOriginal",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.NATIVE),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
)
