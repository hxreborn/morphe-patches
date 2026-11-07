/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castletv.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.string

private val VIDEO_REQUEST_PARAMETERS = listOf(
    "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/Long;", "Ljava/lang/Integer;",
)

internal object VideoBodyClassFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(string("VideoBody(episodeId=", StringComparisonType.STARTS_WITH)),
)

internal object VideoBodyConstructorFingerprint : Fingerprint(
    classFingerprint = VideoBodyClassFingerprint,
    name = "<init>",
    custom = { method, _ ->
        method.parameterTypes.take(VIDEO_REQUEST_PARAMETERS.size) == VIDEO_REQUEST_PARAMETERS &&
            method.parameterTypes.lastOrNull()?.startsWith("Ljava/") == true
    },
)

internal object UserVipInfosFingerprint : Fingerprint(
    definingClass = "Lcom/gxgx/base/bean/User;",
    name = "getVipInfos",
    returnType = "Lcom/gxgx/base/bean/VipInfo;",
    parameters = listOf(),
)

internal object VideoIsPreviewFingerprint : Fingerprint(
    definingClass = "Lcom/xunbai/daqiantvpro/bean/respon/VideoBean;",
    name = "isPreview",
    returnType = "Ljava/lang/Boolean;",
    parameters = listOf(),
)

internal object DefaultResolutionFingerprint : Fingerprint(
    strings = listOf("clarityPremium==="),
)

internal object PlaylistParserFingerprint : Fingerprint(
    parameters = listOf("Landroid/net/Uri;", "L"),
    strings = listOf("Input does not start with the #EXTM3U header."),
)
