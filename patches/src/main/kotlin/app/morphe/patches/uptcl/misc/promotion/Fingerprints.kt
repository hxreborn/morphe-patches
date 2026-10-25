/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.uptcl.misc.promotion

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

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

internal object FullScreenOfferDialogFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroidx/compose/runtime/Composer;", "I"),
    filters = listOf(
        string("work.space.superapp.presentation.widgets.FullScreenDialog (", StringComparisonType.STARTS_WITH),
    ),
)

internal object ShowFullScreenOfferFingerprint : Fingerprint(
    classFingerprint = FullScreenOfferDialogFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
)
