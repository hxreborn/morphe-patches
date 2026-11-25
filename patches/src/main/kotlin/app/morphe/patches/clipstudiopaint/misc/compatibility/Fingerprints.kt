/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clipstudiopaint.misc.compatibility

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object FirebaseProcessNameFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/sessions/ProcessDetailsProvider;",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = "Landroid/os/Process;", name = "myProcessName", parameters = emptyList()),
    ),
)

internal object ProfilePackageInfoFingerprint : Fingerprint(
    definingClass = "Landroidx/profileinstaller/ProfileVerifier\$Api33Impl;",
    name = "getPackageInfo",
    isStatic = true,
    parameters = listOf("Landroid/content/pm/PackageManager;", "Landroid/content/Context;"),
    returnType = "Landroid/content/pm/PackageInfo;",
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/pm/PackageManager;",
            name = "getPackageInfo",
            parameters = listOf("Ljava/lang/String;", "Landroid/content/pm/PackageManager\$PackageInfoFlags;"),
        ),
    ),
)
