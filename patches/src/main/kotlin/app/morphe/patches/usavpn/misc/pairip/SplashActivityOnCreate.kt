/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.pairip

import app.morphe.patcher.resource.ResourceType

private const val PREFERENCES = "Lcom/vpn/lib/Preferences;"
private const val REPOSITORY = "Lcom/vpn/lib/data/repo/Repository;"

internal fun splashOnCreate() = """
    invoke-static {p0}, Ldagger/android/AndroidInjection;->a(Landroid/app/Activity;)V
    invoke-super {p0, p1}, Landroidx/fragment/app/FragmentActivity;->onCreate(Landroid/os/Bundle;)V
    const v1, ${resourceIdHex(ResourceType.LAYOUT, "activity_splash")}
    invoke-virtual {p0, v1}, Landroidx/appcompat/app/AppCompatActivity;->setContentView(I)V
    const v0, ${viewId("progressBar")}
    invoke-virtual {p0, v0}, $FIND_VIEW
    move-result-object v0
    check-cast v0, Landroid/widget/ProgressBar;
    iput-object v0, p0, $SPLASH->s:Landroid/widget/ProgressBar;
    const/16 v1, 0x64
    invoke-virtual {v0, v1}, Landroid/widget/ProgressBar;->setMax(I)V
    const-string v1, ""
    sput-object v1, $APP->I:Ljava/lang/String;
    iget-object v2, p0, $SPLASH->c:$SETTINGS
    iget-object v3, v2, $SETTINGS->a:$SHARED_PREFERENCES
    const-string v4, "key_theme"
    const/4 v5, 0x0
    invoke-interface {v3, v4, v5}, $SHARED_PREFERENCES->getInt(Ljava/lang/String;I)I
    move-result v4
    if-nez v4, :theme_set
    const/4 v4, -0x1
    :theme_set
    const/4 v5, 0x2
    if-gt v4, v5, :theme_done
    invoke-static {v4}, Landroidx/appcompat/app/AppCompatDelegate;->A(I)V
    :theme_done
    new-instance v4, Lio/reactivex/disposables/CompositeDisposable;
    invoke-direct {v4}, Ljava/lang/Object;-><init>()V
    iput-object v4, p0, $SPLASH->g:Lio/reactivex/disposables/CompositeDisposable;
    const-string v4, "key_ex_time"
    const-wide/16 v5, 0x0
    invoke-interface {v3, v4, v5, v6}, $SHARED_PREFERENCES->getLong(Ljava/lang/String;J)J
    move-result-wide v4
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
    move-result-wide v6
    cmp-long v4, v4, v6
    if-gez v4, :urls
    invoke-virtual {v2, v1}, $SETTINGS->s(Ljava/lang/String;)V
    :urls
    iget-object v2, p0, $SPLASH->b:$REPOSITORY
    invoke-interface {v2}, $REPOSITORY->m()Lcom/vpn/lib/data/pojo/ReserveUrls;
    move-result-object v3
    if-eqz v3, :manual
    invoke-virtual {v3}, Lcom/vpn/lib/data/pojo/ReserveUrls;->getUrls()Ljava/util/List;
    move-result-object v3
    if-eqz v3, :manual
    invoke-interface {v3}, Ljava/util/List;->isEmpty()Z
    move-result v3
    if-eqz v3, :manual
    sget-object v3, Lde/blinkt/openvpn/core/BuildConfig;->h:Ljava/lang/String;
    invoke-interface {v2, v3}, $REPOSITORY->k(Ljava/lang/String;)V
    :manual
    new-instance v3, $PREFERENCES
    invoke-direct {v3, p0}, $PREFERENCES-><init>(Landroid/content/Context;)V
    invoke-virtual {v3}, $PREFERENCES->d()Ljava/lang/String;
    move-result-object v3
    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z
    move-result v4
    if-nez v4, :activation
    invoke-interface {v2, v3}, $REPOSITORY->f(Ljava/lang/String;)V
    :activation
    sget-boolean v2, $APP->p:Z
    if-eqz v2, :billing
    invoke-virtual {p0}, $SPLASH->i1()V
    goto :progress
    :billing
    sget-object v2, $PREPARING_STEPS->c:$PREPARING_STEPS
    const/4 v3, 0x0
    invoke-virtual {p0, v2, v3}, $SPLASH->j1(${PREPARING_STEPS}Ljava/lang/Throwable;)V
    new-instance v0, Lcom/services/RemoteConfigHelper;
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V
    iput-object v0, p0, $SPLASH->d:Lcom/services/RemoteConfigHelper;
    invoke-virtual {v0}, Lcom/services/RemoteConfigHelper;->b()V
    new-instance v2, Lcom/vpn/lib/feature/splash/d;
    const/4 v3, 0x1
    invoke-direct {v2, p0, v3}, Lcom/vpn/lib/feature/splash/d;-><init>(Landroid/view/KeyEvent${'$'}Callback;I)V
    iput-object v2, v0, Lcom/services/RemoteConfigHelper;->b:Lcom/services/RemoteConfigHelper${'$'}RemoteConfigHelperCompleteListener;
    new-instance v2, Lcom/vpn/lib/feature/splash/d;
    const/4 v3, 0x2
    invoke-direct {v2, p0, v3}, Lcom/vpn/lib/feature/splash/d;-><init>(Landroid/view/KeyEvent${'$'}Callback;I)V
    iput-object v2, v0, Lcom/services/RemoteConfigHelper;->c:Lcom/services/RemoteConfigHelper${'$'}RemoteConfigHelperFailureListener;
    invoke-virtual {v0, p0}, Lcom/services/RemoteConfigHelper;->a(Landroid/app/Activity;)V
    new-instance v0, $PREFERENCES
    invoke-direct {v0, p0}, $PREFERENCES-><init>(Landroid/content/Context;)V
    iget-object v2, v0, $PREFERENCES->a:$SHARED_PREFERENCES
    const-string v3, "key_firestore_sticky_until"
    const-wide/16 v4, 0x0
    invoke-interface {v2, v3, v4, v5}, $SHARED_PREFERENCES->getLong(Ljava/lang/String;J)J
    move-result-wide v6
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
    move-result-wide v8
    cmp-long v1, v6, v8
    if-gtz v1, :sticky
    cmp-long v1, v6, v4
    if-lez v1, :sticky_off
    invoke-interface {v2}, $SHARED_PREFERENCES->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v1
    invoke-interface {v1, v3}, Landroid/content/SharedPreferences${'$'}Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    invoke-virtual {v0, v1}, $PREFERENCES->f(Landroid/content/SharedPreferences${'$'}Editor;)Z
    move-result v3
    if-nez v3, :sticky_apply
    const-string v3, "key_firestore_sticky_ttl_days"
    const/4 v4, 0x2
    invoke-interface {v2, v3, v4}, $SHARED_PREFERENCES->getInt(Ljava/lang/String;I)I
    move-result v4
    add-int/lit8 v4, v4, 0x1
    invoke-interface {v1, v3, v4}, Landroid/content/SharedPreferences${'$'}Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences${'$'}Editor;
    :sticky_apply
    invoke-interface {v1}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
    invoke-virtual {v0}, $PREFERENCES->b()I
    :sticky_off
    const/4 v0, 0x0
    sput-boolean v0, $APP->d0:Z
    goto :load
    :sticky
    const/4 v0, 0x1
    sput-boolean v0, $APP->d0:Z
    iput-boolean v0, p0, $SPLASH->l:Z
    const v0, ${stringId("app_settings_loading")}
    invoke-virtual {p0, v0}, Landroid/content/Context;->getString(I)Ljava/lang/String;
    move-result-object v0
    invoke-virtual {p0, v0}, $SPLASH->s1(Ljava/lang/String;)V
    iget-object v0, p0, $SPLASH->b:$REPOSITORY
    invoke-interface {v0}, $REPOSITORY->N()V
    :load
    invoke-virtual {p0}, $SPLASH->d1()V
    :progress
    sget v0, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
    const/16 v1, 0x23
    if-lt v0, v1, :diagnostics
    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
    move-result-object v0
    const/4 v1, 0x0
    invoke-static {v0, v1}, Landroidx/core/view/WindowCompat;->a(Landroid/view/Window;Z)V
    const v0, 0x1020002
    invoke-virtual {p0, v0}, $FIND_VIEW
    move-result-object v0
    const v1, ${viewId("body")}
    invoke-virtual {p0, v1}, $FIND_VIEW
    move-result-object v1
    new-instance v2, Lcom/vpn/lib/feature/splash/d;
    const/16 v3, 0xd
    invoke-direct {v2, v1, v3}, Lcom/vpn/lib/feature/splash/d;-><init>(Landroid/view/KeyEvent${'$'}Callback;I)V
    invoke-static {v0, v2}, Landroidx/core/view/ViewCompat;->G(Landroid/view/View;Landroidx/core/view/OnApplyWindowInsetsListener;)V
    :diagnostics
    new-instance v0, Lcom/vpn/lib/troubleshooting/ConnectionDiagnosticManager;
    sget-object v1, Lde/blinkt/openvpn/core/BuildConfig;->h:Ljava/lang/String;
    iget-object v2, p0, $SPLASH->b:$REPOSITORY
    invoke-direct {v0, p0, v1, v2}, Lcom/vpn/lib/troubleshooting/ConnectionDiagnosticManager;-><init>(${SPLASH}Ljava/lang/String;$REPOSITORY)V
    iput-object v0, p0, $SPLASH->w:Lcom/vpn/lib/troubleshooting/ConnectionDiagnosticManager;
    invoke-virtual {p0}, Landroidx/fragment/app/FragmentActivity;->getSupportFragmentManager()Landroidx/fragment/app/FragmentManager;
    move-result-object v1
    iput-object v1, v0, Lcom/vpn/lib/troubleshooting/ConnectionDiagnosticManager;->u:Landroidx/fragment/app/FragmentManager;
    new-instance v1, Lcom/vpn/lib/feature/splash/b;
    const/4 v2, 0x0
    invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/splash/b;-><init>(${SPLASH}I)V
    iput-object v1, v0, Lcom/vpn/lib/troubleshooting/ConnectionDiagnosticManager;->x:Lcom/vpn/lib/feature/splash/b;
    new-instance v0, Lcom/vpn/lib/troubleshooting/EmergencyModeConnectionManager;
    invoke-direct {v0, p0}, Lcom/vpn/lib/troubleshooting/EmergencyModeConnectionManager;-><init>($SPLASH)V
    iput-object v0, p0, $SPLASH->x:Lcom/vpn/lib/troubleshooting/EmergencyModeConnectionManager;
    new-instance v1, Lcom/vpn/lib/troubleshooting/EmergencyMode;
    new-instance v2, Lcom/vpn/lib/feature/splash/d;
    const/4 v3, 0x4
    invoke-direct {v2, p0, v3}, Lcom/vpn/lib/feature/splash/d;-><init>(Landroid/view/KeyEvent${'$'}Callback;I)V
    new-instance v3, Lcom/vpn/lib/feature/splash/b;
    const/4 v4, 0x1
    invoke-direct {v3, p0, v4}, Lcom/vpn/lib/feature/splash/b;-><init>(${SPLASH}I)V
    invoke-direct {v1, v2, v0, v3}, Lcom/vpn/lib/troubleshooting/EmergencyMode;-><init>(Lcom/vpn/lib/feature/splash/d;Lcom/vpn/lib/troubleshooting/EmergencyModeConnectionManager;Lcom/vpn/lib/feature/splash/b;)V
    iput-object v1, p0, $SPLASH->y:Lcom/vpn/lib/troubleshooting/EmergencyMode;
    sput-object v1, Lcom/vpn/lib/troubleshooting/EmergencyModeProvider;->a:Lcom/vpn/lib/troubleshooting/EmergencyMode;
    new-instance v0, Lcom/github/shadowsocks/utils/StartService;
    invoke-direct {v0}, Landroidx/activity/result/contract/ActivityResultContract;-><init>()V
    new-instance v1, Lcom/vpn/lib/feature/splash/d;
    const/4 v2, 0x5
    invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/splash/d;-><init>(Landroid/view/KeyEvent${'$'}Callback;I)V
    invoke-virtual {p0, v0, v1}, Landroidx/activity/ComponentActivity;->registerForActivityResult(Landroidx/activity/result/contract/ActivityResultContract;Landroidx/activity/result/ActivityResultCallback;)Landroidx/activity/result/ActivityResultLauncher;
    move-result-object v0
    iput-object v0, p0, $SPLASH->z:Landroidx/activity/result/ActivityResultLauncher;
    new-instance v0, Landroid/os/Handler;
    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
    move-result-object v1
    invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
    new-instance v1, Lcom/vpn/lib/feature/splash/k;
    const/4 v2, 0x2
    invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/splash/k;-><init>(${SPLASH}I)V
    invoke-virtual {v0, v1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z
    const/4 v0, 0x1
    sput-boolean v0, $APP->c0:Z
    return-void
"""
