/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.oneweather.misc.skyla

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object SkylaFeatureEnabledFingerprint : Fingerprint(
    definingClass = "Lcom/oneweather/remotelibrary/sources/firebase/models/SkylaRemoteConfig;",
    name = "isSkylaFeatureEnabled",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
)
