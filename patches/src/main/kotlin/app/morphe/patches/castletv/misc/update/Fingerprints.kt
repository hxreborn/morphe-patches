/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castletv.misc.update

import app.morphe.patcher.Fingerprint

private const val VERSION_BEAN_CLASS = "Lcom/xunbai/daqiantvpro/bean/respon/VersionBean;"

internal object VersionCodeFingerprint : Fingerprint(
    definingClass = VERSION_BEAN_CLASS,
    name = "getVersionCode",
    parameters = emptyList(),
    returnType = "Ljava/lang/Integer;",
)

internal object UpdateTypeFingerprint : Fingerprint(
    definingClass = VERSION_BEAN_CLASS,
    name = "getUpdateType",
    parameters = emptyList(),
    returnType = "I",
)
