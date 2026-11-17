/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.fix.signature

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object PackageCertificateHashFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/util/AndroidUtilsLight;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "[B",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
    strings = listOf("SHA1"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/pm/Signature;",
            name = "toByteArray",
            returnType = "[B",
        ),
    ),
)
