/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.layout

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

private const val ACTIVITY_PACKAGE = "Lcom/camerasideas/collagemaker/activity"

internal fun shareRowFingerprint(activityName: String) = Fingerprint(
    definingClass = "$ACTIVITY_PACKAGE/$activityName;",
    name = "onCreate",
    filters = listOf(
        methodCall(
            definingClass = "Landroidx/recyclerview/widget/RecyclerView;",
            name = "setAdapter",
        ),
    ),
)

internal val SHARE_ROW_FINGERPRINTS = listOf(
    shareRowFingerprint("ImageResultActivity"),
    shareRowFingerprint("ImageAiResultActivity"),
)
