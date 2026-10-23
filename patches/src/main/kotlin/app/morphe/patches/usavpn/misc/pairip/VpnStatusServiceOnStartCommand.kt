/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.pairip

import app.morphe.patcher.resource.ResourceType

private const val FOREGROUND_SERVICE = "Lapp/hxreborn/extension/shared/ForegroundService;"
private const val NETWORK_INTERFACES = "Lapp/hxreborn/extension/shared/NetworkInterfaces;"

internal fun statusServiceOnStartCommand() = """
    invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
    move-result-object v0
    const-string v1, "StatusChannel"
    invoke-virtual {v1, v0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    iput-object v0, p0, $STATUS_SERVICE->a:Ljava/lang/String;
    const-string v1, ".ACTION_CHECK_VPN_STATUS"
    invoke-virtual {v0, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1
    iput-object v1, p0, $STATUS_SERVICE->b:Ljava/lang/String;
    sget v1, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
    const/16 v2, 0x1a
    if-lt v1, v2, :notification
    new-instance v1, Landroid/app/NotificationChannel;
    const-string v2, "VPN Status Service Channel"
    const/4 v3, 0x3
    invoke-direct {v1, v0, v2, v3}, Landroid/app/NotificationChannel;-><init>(Ljava/lang/String;Ljava/lang/CharSequence;I)V
    const-class v2, Landroid/app/NotificationManager;
    invoke-virtual {p0, v2}, Landroid/content/Context;->getSystemService(Ljava/lang/Class;)Ljava/lang/Object;
    move-result-object v2
    check-cast v2, Landroid/app/NotificationManager;
    invoke-virtual {v2, v1}, Landroid/app/NotificationManager;->createNotificationChannel(Landroid/app/NotificationChannel;)V
    :notification
    new-instance v1, Landroidx/core/app/NotificationCompat${'$'}Builder;
    invoke-direct {v1, p0, v0}, Landroidx/core/app/NotificationCompat${'$'}Builder;-><init>(Landroid/content/Context;Ljava/lang/String;)V
    const v0, ${stringId("app_name")}
    invoke-virtual {p0, v0}, Landroid/content/Context;->getString(I)Ljava/lang/String;
    move-result-object v0
    filled-new-array {v0}, [Ljava/lang/Object;
    move-result-object v0
    const v2, ${stringId("is_not_connected")}
    invoke-virtual {p0, v2, v0}, Landroid/content/Context;->getString(I[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v0
    invoke-static {v0}, Landroidx/core/app/NotificationCompat${'$'}Builder;->c(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
    move-result-object v0
    iput-object v0, v1, Landroidx/core/app/NotificationCompat${'$'}Builder;->e:Ljava/lang/CharSequence;
    const v0, ${stringId("click_to_select_server_and_connect")}
    invoke-virtual {p0, v0}, Landroid/content/Context;->getString(I)Ljava/lang/String;
    move-result-object v0
    invoke-static {v0}, Landroidx/core/app/NotificationCompat${'$'}Builder;->c(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
    move-result-object v0
    iput-object v0, v1, Landroidx/core/app/NotificationCompat${'$'}Builder;->f:Ljava/lang/CharSequence;
    const/4 v0, 0x2
    invoke-virtual {v1, v0}, Landroidx/core/app/NotificationCompat${'$'}Builder;->d(I)V
    const/4 v0, 0x1
    iput-boolean v0, v1, Landroidx/core/app/NotificationCompat${'$'}Builder;->p:Z
    iput-boolean v0, v1, Landroidx/core/app/NotificationCompat${'$'}Builder;->q:Z
    iput-boolean v0, v1, Landroidx/core/app/NotificationCompat${'$'}Builder;->y:Z
    const v2, ${resourceIdHex(ResourceType.COLOR, "colorPrimary")}
    invoke-virtual {p0, v2}, Landroid/content/Context;->getColor(I)I
    move-result v2
    iput v2, v1, Landroidx/core/app/NotificationCompat${'$'}Builder;->t:I
    new-instance v2, Landroid/content/Intent;
    const-class v3, $SPLASH
    invoke-direct {v2, p0, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
    const/high16 v4, 0x14000000
    invoke-virtual {v2, v4}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
    invoke-static {p0, v0, v2, v4}, Landroid/app/PendingIntent;->getActivity(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;
    move-result-object v2
    iput-object v2, v1, Landroidx/core/app/NotificationCompat${'$'}Builder;->g:Landroid/app/PendingIntent;
    new-instance v2, Landroid/content/Intent;
    const-class v5, $NAVIGATION
    invoke-direct {v2, p0, v5}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
    const-string v5, "action_connect_last_notification"
    invoke-virtual {v2, v5}, Landroid/content/Intent;->setAction(Ljava/lang/String;)Landroid/content/Intent;
    invoke-virtual {v2, v4}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
    invoke-static {p0, v0, v2, v4}, Landroid/app/PendingIntent;->getActivity(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;
    move-result-object v2
    const v5, ${stringId("push_stats_action_2")}
    invoke-virtual {p0, v5}, Landroid/content/Context;->getString(I)Ljava/lang/String;
    move-result-object v5
    const/4 v6, 0x0
    invoke-virtual {v1, v6, v5, v2}, Landroidx/core/app/NotificationCompat${'$'}Builder;->a(ILjava/lang/String;Landroid/app/PendingIntent;)V
    new-instance v2, Landroid/content/Intent;
    invoke-direct {v2, p0, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
    invoke-virtual {v2, v4}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
    invoke-static {p0, v0, v2, v4}, Landroid/app/PendingIntent;->getActivity(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;
    move-result-object v2
    const v5, ${stringId("push_stats_action_1")}
    invoke-virtual {p0, v5}, Landroid/content/Context;->getString(I)Ljava/lang/String;
    move-result-object v5
    invoke-virtual {v1, v6, v5, v2}, Landroidx/core/app/NotificationCompat${'$'}Builder;->a(ILjava/lang/String;Landroid/app/PendingIntent;)V
    iget-object v2, v1, Landroidx/core/app/NotificationCompat${'$'}Builder;->x:Landroid/app/Notification;
    const v3, ${resourceIdHex(ResourceType.DRAWABLE, "ic_logo")}
    iput v3, v2, Landroid/app/Notification;->icon:I
    invoke-virtual {v1}, Landroidx/core/app/NotificationCompat${'$'}Builder;->b()Landroid/app/Notification;
    move-result-object v1
    const/16 v2, 0x4d3
    const/high16 v3, 0x40000000
    invoke-static {p0, v2, v1, v3}, $FOREGROUND_SERVICE->start(Landroid/app/Service;ILandroid/app/Notification;I)Z
    move-result v1
    if-nez v1, :started
    const/4 v0, 0x2
    return v0
    :started
    if-eqz p1, :schedule
    invoke-virtual {p1}, Landroid/content/Intent;->getAction()Ljava/lang/String;
    move-result-object v1
    iget-object v2, p0, $STATUS_SERVICE->b:Ljava/lang/String;
    invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-eqz v1, :schedule
    invoke-static {}, $NETWORK_INTERFACES->isVpnUp()Z
    move-result v1
    if-eqz v1, :schedule
    invoke-virtual {p0}, Landroid/app/Service;->stopSelf()V
    :schedule
    invoke-virtual {p0}, $STATUS_SERVICE->a()V
    return v0
"""
