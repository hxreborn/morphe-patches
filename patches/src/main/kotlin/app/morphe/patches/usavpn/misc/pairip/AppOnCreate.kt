/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.pairip

internal fun appOnCreate() = """
    invoke-super {p0}, Landroid/app/Application;->onCreate()V
    sget-object v0, Landroidx/lifecycle/ProcessLifecycleOwner;->i:Landroidx/lifecycle/ProcessLifecycleOwner;
    iget-object v0, v0, Landroidx/lifecycle/ProcessLifecycleOwner;->f:Landroidx/lifecycle/LifecycleRegistry;
    invoke-virtual {v0, p0}, Landroidx/lifecycle/LifecycleRegistry;->a(Landroidx/lifecycle/LifecycleObserver;)V
    new-instance v0, Lcom/vpn/lib/util/MotionEventGuard${'$'}LifecycleCallbacks;
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V
    invoke-virtual {p0, v0}, Landroid/app/Application;->registerActivityLifecycleCallbacks(Landroid/app/Application${'$'}ActivityLifecycleCallbacks;)V
    sget-object v0, Lcom/vpn/lib/analytics/FirebaseAnalyticsHelper;->a:Ljava/util/concurrent/atomic/AtomicReference;
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v1
    const/4 v2, 0x0
    invoke-virtual {v0, v2, v1}, Ljava/util/concurrent/atomic/AtomicReference;->compareAndSet(Ljava/lang/Object;Ljava/lang/Object;)Z
    sput-object p0, $APP->F:$APP
    new-instance v0, Lcom/vpn/lib/AppOpenManager;
    invoke-direct {v0, p0}, Lcom/vpn/lib/AppOpenManager;-><init>($APP)V
    iput-object v0, p0, $APP->d:Lcom/vpn/lib/AppOpenManager;
    sget v0, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
    const/16 v1, 0x1c
    if-lt v0, v1, :inject
    invoke-virtual {p0, p0}, $APP->k($APP)Ljava/lang/String;
    move-result-object v0
    if-eqz v0, :inject
    invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-nez v1, :inject
    invoke-static {v0}, Landroid/webkit/WebView;->setDataDirectorySuffix(Ljava/lang/String;)V
    :inject
    invoke-static {}, Lcom/vpn/lib/injection/DaggerAppComponent;->a()Lcom/vpn/lib/injection/AppComponent${'$'}Builder;
    move-result-object v0
    invoke-interface {v0, p0}, Lcom/vpn/lib/injection/AppComponent${'$'}Builder;->a($APP)Lcom/vpn/lib/injection/AppComponent${'$'}Builder;
    invoke-interface {v0}, Lcom/vpn/lib/injection/AppComponent${'$'}Builder;->build()Lcom/vpn/lib/injection/DaggerAppComponent;
    move-result-object v0
    invoke-virtual {v0, p0}, Lcom/vpn/lib/injection/DaggerAppComponent;->c($APP)V
    invoke-virtual {p0}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v0
    sput-object v0, Lcom/vpn/lib/consent/GoogleMobileAdsConsentManager;->a:Landroid/content/Context;
    sput-object v0, Lcom/vpn/lib/consent/ConsentModeHelper;->a:Landroid/content/Context;
    iget-object v0, p0, $APP->c:$SETTINGS
    sput-object v0, Lcom/vpn/lib/consent/ConsentModeHelper;->b:$SETTINGS
    invoke-static {}, Lcom/vpn/lib/consent/ConsentModeHelper;->b()V
    sput-object p0, Lcom/github/shadowsocks/Core;->a:$APP
    new-instance v0, Lcom/google/firebase/firestore/pipeline/evaluation/d;
    const/4 v1, 0x4
    invoke-direct {v0, v1}, Lcom/google/firebase/firestore/pipeline/evaluation/d;-><init>(I)V
    sput-object v0, Lcom/github/shadowsocks/Core;->b:Lcom/google/firebase/firestore/pipeline/evaluation/d;
    invoke-static {}, Lcom/github/shadowsocks/Core;->c()Landroid/app/Application;
    move-result-object v0
    const-string v1, "config.db"
    invoke-virtual {v0, p0, v1}, Landroid/content/Context;->moveDatabaseFrom(Landroid/content/Context;Ljava/lang/String;)Z
    invoke-static {}, Lcom/github/shadowsocks/Core;->i()V
    new-instance v0, Lcom/google/gson/internal/c;
    const/4 v1, 0x4
    invoke-direct {v0, p0, v1}, Lcom/google/gson/internal/c;-><init>(${APP}I)V
    sput-object v0, Lio/reactivex/plugins/RxJavaPlugins;->a:Lcom/google/gson/internal/c;
    new-instance v0, Lcom/google/gson/internal/c;
    const/4 v1, 0x5
    invoke-direct {v0, p0, v1}, Lcom/google/gson/internal/c;-><init>(${APP}I)V
    sput-object v0, Lio/reactivex/rxjava3/plugins/RxJavaPlugins;->a:Lcom/google/gson/internal/c;
    sget v0, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
    const/16 v1, 0x1a
    if-lt v0, v1, :firebase
    const-string v0, "notification"
    invoke-virtual {p0, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Landroid/app/NotificationManager;
    new-instance v1, Landroid/app/NotificationChannel;
    const v2, ${stringId("channel_name_background")}
    invoke-virtual {p0, v2}, Landroid/content/Context;->getString(I)Ljava/lang/String;
    move-result-object v2
    const-string v3, "openvpn_bg"
    const/4 v4, 0x1
    invoke-direct {v1, v3, v2, v4}, Landroid/app/NotificationChannel;-><init>(Ljava/lang/String;Ljava/lang/CharSequence;I)V
    const v2, ${stringId("channel_description_background")}
    invoke-virtual {p0, v2}, Landroid/content/Context;->getString(I)Ljava/lang/String;
    move-result-object v2
    invoke-virtual {v1, v2}, Landroid/app/NotificationChannel;->setDescription(Ljava/lang/String;)V
    invoke-virtual {v0, v1}, Landroid/app/NotificationManager;->createNotificationChannel(Landroid/app/NotificationChannel;)V
    new-instance v1, Landroid/app/NotificationChannel;
    const v2, ${stringId("channel_name_status")}
    invoke-virtual {p0, v2}, Landroid/content/Context;->getString(I)Ljava/lang/String;
    move-result-object v2
    const-string v3, "openvpn_newstat"
    const/4 v4, 0x2
    invoke-direct {v1, v3, v2, v4}, Landroid/app/NotificationChannel;-><init>(Ljava/lang/String;Ljava/lang/CharSequence;I)V
    const v2, ${stringId("channel_description_status")}
    invoke-virtual {p0, v2}, Landroid/content/Context;->getString(I)Ljava/lang/String;
    move-result-object v2
    invoke-virtual {v1, v2}, Landroid/app/NotificationChannel;->setDescription(Ljava/lang/String;)V
    invoke-virtual {v0, v1}, Landroid/app/NotificationManager;->createNotificationChannel(Landroid/app/NotificationChannel;)V
    :firebase
    invoke-static {p0}, Lcom/google/firebase/FirebaseApp;->i(Landroid/content/Context;)V
    return-void
"""
