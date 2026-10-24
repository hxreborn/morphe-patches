/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.promotion

import app.morphe.patcher.Fingerprint

internal object LaunchContentToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("DynamicSplashResponse(videoLink="),
)

internal object ShowLaunchSplashFingerprint : Fingerprint(
    classFingerprint = LaunchContentToStringFingerprint,
    name = "getShowSplash",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
)

internal object ShowLaunchBannerFingerprint : Fingerprint(
    classFingerprint = LaunchContentToStringFingerprint,
    name = "getShowBanner",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
)

internal object ShowLaunchVideoFingerprint : Fingerprint(
    classFingerprint = LaunchContentToStringFingerprint,
    name = "getShowVideo",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
)
