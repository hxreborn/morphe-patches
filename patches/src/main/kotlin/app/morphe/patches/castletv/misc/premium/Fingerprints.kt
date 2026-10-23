/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castletv.misc.premium

import app.morphe.patcher.Fingerprint

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
