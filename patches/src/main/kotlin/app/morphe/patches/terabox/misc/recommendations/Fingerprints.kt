/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.recommendations

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object VideoRecommendationsFingerprint : Fingerprint(
    definingClass = "Lcom/dubox/drive/ui/preview/video/pageC/VideoPlayerCViewModel;",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "I"),
    filters = listOf(methodCall(definingClass = "Lkotlin/collections/CollectionsKt;", name = "take")),
)
