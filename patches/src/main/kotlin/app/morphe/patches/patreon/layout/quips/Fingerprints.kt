/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.quips

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal const val AUTO_PLAYBACK_SETTING = "Lcom/patreon/android/data/model/AutoPlaybackInFeedsSetting;"

internal object AutoPlaybackSettingParserFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = AUTO_PLAYBACK_SETTING,
    parameters = listOf("Ljava/lang/String;"),
    custom = { _, classDef -> classDef.sourceFile == "FeedPostFeaturesUseCaseImpl.kt" },
)
