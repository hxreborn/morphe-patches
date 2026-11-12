/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.notifications

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object FirebaseMessagingHandleIntentFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/messaging/FirebaseMessagingService;",
    name = "handleIntent",
    returnType = "V",
    parameters = listOf("Landroid/content/Intent;"),
)

internal object NotificationPermissionPromptFingerprint : Fingerprint(
    definingClass = "Lcom/camerasideas/collagemaker/mvp/vm/MainActivityViewModel;",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Landroid/content/Context;", "L"),
    filters = listOf(string("android.permission.POST_NOTIFICATIONS")),
)
