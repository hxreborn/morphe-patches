/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.notifications

import app.morphe.patcher.Fingerprint

internal object EnableNotificationFingerprint : Fingerprint(
    definingClass = "Lcom/king/usdk/notificationtokenprovider/NotificationTokenProviderModule;",
    name = "enableNotification",
    returnType = "V",
    parameters = emptyList(),
)

internal object ScheduleLocalNotificationFingerprint : Fingerprint(
    definingClass = "Lcom/king/usdk/localnotification/LocalNotificationModule;",
    name = "scheduleLocalNotification",
    returnType = "V",
    parameters = listOf(
        "I", "J", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;",
        "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "I",
    ),
)
