/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.classdojo.misc.reports

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

private const val COMPATIBLE_BUNDLE_LOG_WINDOW = 4

internal object DownloadedBundleFingerprint : Fingerprint(
    definingClass = "Lcom/microsoft/codepush/react/CodePush;",
    name = "getJSBundleFileInternal",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        string("assets://"),
        methodCall(name = "isPackageBundleLatest", returnType = "Z"),
        methodCall(
            definingClass = "Lcom/microsoft/codepush/react/CodePushUtils;",
            name = "logBundleUrl",
            location = MatchAfterWithin(COMPATIBLE_BUNDLE_LOG_WINDOW),
        ),
    ),
)
