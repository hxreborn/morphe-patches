/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.RestoredMethod
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.restoreVirtualizedMethods
import app.morphe.patches.shared.misc.pairip.virtualizedMethods

private const val APPLICATION_CLASS = "Lme/proton/android/lumo/LumoApplication;"

private val RESTORED_METHODS = listOf(
    RestoredMethod(
        "Lme/proton/android/lumo/Hilt_LumoApplication;", "onCreate", 4,
        """
            iget-boolean v0, p0, Lme/proton/android/lumo/Hilt_LumoApplication;->g:Z
            if-nez v0, :cond_48
            const/4 v0, 0x1
            iput-boolean v0, p0, Lme/proton/android/lumo/Hilt_LumoApplication;->g:Z
            iget-object v0, p0, Lme/proton/android/lumo/Hilt_LumoApplication;->h:Lhl;
            invoke-virtual {v0}, Lhl;->d()Ljava/lang/Object;
            move-result-object v0
            check-cast v0, Lme/proton/android/lumo/LumoApplication_GeneratedInjector;
            move-object v1, p0
            check-cast v1, Lme/proton/android/lumo/LumoApplication;
            check-cast v0, Lme/proton/android/lumo/DaggerLumoApplication_HiltComponents_SingletonC${'$'}SingletonCImpl;
            iget-object v2, v0, Lme/proton/android/lumo/DaggerLumoApplication_HiltComponents_SingletonC${'$'}SingletonCImpl;->a0:Lx33;
            invoke-interface {v2}, Lw33;->get()Ljava/lang/Object;
            move-result-object v2
            check-cast v2, Lme/proton/android/lumo/webview/LumoWebViewPool;
            invoke-static {v1, v2}, Lme/proton/android/lumo/LumoApplication_MembersInjector;->d(Lme/proton/android/lumo/LumoApplication;Lme/proton/android/lumo/webview/LumoWebViewPool;)V
            iget-object v2, v0, Lme/proton/android/lumo/DaggerLumoApplication_HiltComponents_SingletonC${'$'}SingletonCImpl;->Y:Lx33;
            invoke-interface {v2}, Lw33;->get()Ljava/lang/Object;
            move-result-object v2
            check-cast v2, Lme/proton/android/lumo/webview/RendererDiagnostics;
            invoke-static {v1, v2}, Lme/proton/android/lumo/LumoApplication_MembersInjector;->c(Lme/proton/android/lumo/LumoApplication;Lme/proton/android/lumo/webview/RendererDiagnostics;)V
            iget-object v2, v0, Lme/proton/android/lumo/DaggerLumoApplication_HiltComponents_SingletonC${'$'}SingletonCImpl;->U:Lx33;
            invoke-interface {v2}, Lw33;->get()Ljava/lang/Object;
            move-result-object v2
            check-cast v2, Lme/proton/android/lumo/data/repository/ThemeRepository;
            iget-object v2, v0, Lme/proton/android/lumo/DaggerLumoApplication_HiltComponents_SingletonC${'$'}SingletonCImpl;->h0:Lx33;
            invoke-interface {v2}, Lw33;->get()Ljava/lang/Object;
            move-result-object v2
            check-cast v2, Lme/proton/android/lumo/payments/PaymentsInitializer;
            invoke-static {v1, v2}, Lme/proton/android/lumo/LumoApplication_MembersInjector;->b(Lme/proton/android/lumo/LumoApplication;Lme/proton/android/lumo/payments/PaymentsInitializer;)V
            iget-object v0, v0, Lme/proton/android/lumo/DaggerLumoApplication_HiltComponents_SingletonC${'$'}SingletonCImpl;->i0:Lx33;
            invoke-interface {v0}, Lw33;->get()Ljava/lang/Object;
            move-result-object v0
            check-cast v0, Lme/proton/android/lumo/core/LumoCoreRepository;
            invoke-static {v1, v0}, Lme/proton/android/lumo/LumoApplication_MembersInjector;->a(Lme/proton/android/lumo/LumoApplication;Lme/proton/android/lumo/core/LumoCoreRepository;)V
            :cond_48
            invoke-super {p0}, Landroid/app/Application;->onCreate()V
            return-void
        """,
    ),
    RestoredMethod(
        "Lme/proton/android/lumo/LumoApplication;", "onCreate", 5,
        """
            invoke-super {p0}, Lme/proton/android/lumo/Hilt_LumoApplication;->onCreate()V
            iget-object v0, p0, Lme/proton/android/lumo/LumoApplication;->k:Lme/proton/android/lumo/payments/PaymentsInitializer;
            const/4 v1, 0x0
            if-eqz v0, :cond_42
            iget-object v0, p0, Lme/proton/android/lumo/LumoApplication;->i:Lme/proton/android/lumo/webview/LumoWebViewPool;
            if-eqz v0, :cond_3c
            iget-object v2, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->d:Landroid/webkit/WebView;
            if-eqz v2, :cond_11
            goto :goto_2e
            :cond_11
            invoke-static {}, Landroid/webkit/ServiceWorkerController;->getInstance()Landroid/webkit/ServiceWorkerController;
            move-result-object v2
            new-instance v3, Lme/proton/android/lumo/webview/LumoWebViewPool${'$'}prewarm${'$'}1;
            invoke-direct {v3}, Lme/proton/android/lumo/webview/LumoWebViewPool${'$'}prewarm${'$'}1;-><init>()V
            invoke-virtual {v2, v3}, Landroid/webkit/ServiceWorkerController;->setServiceWorkerClient(Landroid/webkit/ServiceWorkerClient;)V
            new-instance v2, Landroid/webkit/WebView;
            invoke-virtual {v0}, Lme/proton/android/lumo/webview/LumoWebViewPool;->d()Landroid/content/MutableContextWrapper;
            move-result-object v3
            invoke-direct {v2, v3}, Landroid/webkit/WebView;-><init>(Landroid/content/Context;)V
            invoke-virtual {v0, v2}, Lme/proton/android/lumo/webview/LumoWebViewPool;->a(Landroid/webkit/WebView;)V
            invoke-virtual {v2}, Landroid/webkit/WebView;->onPause()V
            iput-object v2, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->d:Landroid/webkit/WebView;
            :goto_2e
            iget-object p0, p0, Lme/proton/android/lumo/LumoApplication;->l:Lme/proton/android/lumo/core/LumoCoreRepository;
            if-eqz p0, :cond_36
            invoke-virtual {p0}, Lme/proton/android/lumo/core/LumoCoreRepository;->b()V
            return-void
            :cond_36
            const-string p0, "lumoCoreRepository"
            invoke-static {p0}, Lfq1;->J(Ljava/lang/String;)V
            throw v1
            :cond_3c
            const-string p0, "webViewPool"
            invoke-static {p0}, Lfq1;->J(Ljava/lang/String;)V
            throw v1
            :cond_42
            const-string p0, "paymentsInitializer"
            invoke-static {p0}, Lfq1;->J(Ljava/lang/String;)V
            throw v1
        """,
    ),
    RestoredMethod(
        "Lme/proton/android/lumo/MainActivity;", "onCreate", 7,
        """
            invoke-static {p0}, Log3;->v(Lme/proton/android/lumo/MainActivity;)Lbw2;
            move-result-object v0
            new-instance v1, Lme/proton/android/lumo/MainActivity${'$'}onCreate${'$'}1;
            invoke-direct {v1, p0}, Lme/proton/android/lumo/MainActivity${'$'}onCreate${'$'}1;-><init>(Lme/proton/android/lumo/MainActivity;)V
            invoke-virtual {v0, v1}, Lbw2;->K(Lcv3;)V
            invoke-super {p0, p1}, Lme/proton/android/lumo/Hilt_MainActivity;->onCreate(Landroid/os/Bundle;)V
            invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
            move-result v0
            const/4 v1, 0x1
            if-eqz p1, :cond_18
            move v2, v1
            goto :goto_19
            :cond_18
            const/4 v2, 0x0
            :goto_19
            new-instance v3, Ljava/lang/StringBuilder;
            const-string v4, "account.activity.created instance="
            invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
            invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
            const-string v0, " restored="
            invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;
            invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            const/4 v2, 0x6
            const/4 v3, 0x0
            invoke-static {p0, v0, v3, v3, v2}, Lme/proton/android/core/logging/LoggerExtKt;->f(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Exception;I)V
            invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
            move-result-object v0
            invoke-virtual {v0}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;
            move-result-object v0
            iget v0, v0, Landroid/content/res/Configuration;->smallestScreenWidthDp:I
            const/16 v2, 0x258
            if-ge v0, v2, :cond_44
            move v0, v1
            goto :goto_45
            :cond_44
            const/4 v0, -0x1
            :goto_45
            invoke-virtual {p0, v0}, Landroid/app/Activity;->setRequestedOrientation(I)V
            invoke-static {p0}, Lgv0;->a(Lv80;)V
            iget-object v0, p0, Lme/proton/android/lumo/MainActivity;->H:Lme/proton/android/lumo/webview/WebViewCookieStore;
            if-eqz v0, :cond_91
            iget-object v2, p0, Lu80;->g:Le12;
            invoke-virtual {v2, v0}, Le12;->a(La12;)V
            if-eqz p1, :cond_62
            const-string v0, "pending_camera_uri"
            invoke-virtual {p1, v0}, Landroid/os/BaseBundle;->getString(Ljava/lang/String;)Ljava/lang/String;
            move-result-object p1
            if-eqz p1, :cond_62
            invoke-static {p1}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
            move-result-object v3
            :cond_62
            iput-object v3, p0, Lme/proton/android/lumo/MainActivity;->Q:Landroid/net/Uri;
            new-instance p1, Lme/proton/android/lumo/webview/LumoChromeClient;
            new-instance v0, Lme/proton/android/lumo/MainActivity${'$'}onCreate${'$'}lumoChromeClient${'$'}1;
            invoke-direct {v0, p0}, Lme/proton/android/lumo/MainActivity${'$'}onCreate${'$'}lumoChromeClient${'$'}1;-><init>(Lme/proton/android/lumo/MainActivity;)V
            invoke-direct {p1, p0, v0}, Lme/proton/android/lumo/webview/LumoChromeClient;-><init>(Lme/proton/android/lumo/MainActivity;Lyb1;)V
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->t()Lme/proton/android/lumo/MainContainerViewModel;
            move-result-object v0
            invoke-virtual {v0}, Lme/proton/android/lumo/MainContainerViewModel;->t()V
            new-instance v0, Lme/proton/android/lumo/MainActivity${'$'}onCreate${'$'}callback${'$'}1;
            invoke-direct {v0, p0}, Lme/proton/android/lumo/MainActivity${'$'}onCreate${'$'}callback${'$'}1;-><init>(Lme/proton/android/lumo/MainActivity;)V
            invoke-virtual {p0}, Lv80;->b()Lho2;
            move-result-object v2
            invoke-virtual {v2, p0, v0}, Lho2;->a(Lb12;Lbo2;)V
            new-instance v0, Lme/proton/android/lumo/MainActivity${'$'}onCreate${'$'}2;
            invoke-direct {v0, p0, p1}, Lme/proton/android/lumo/MainActivity${'$'}onCreate${'$'}2;-><init>(Lme/proton/android/lumo/MainActivity;Lme/proton/android/lumo/webview/LumoChromeClient;)V
            const p1, 0x4b01c786    # 8505222.0f
            invoke-static {v0, v1, p1}, Leu1;->o(Ljava/lang/Object;ZI)Ld90;
            move-result-object p1
            invoke-static {p0, p1}, Lw80;->a(Lv80;Ld90;)V
            return-void
            :cond_91
            const-string p0, "cookieStore"
            invoke-static {p0}, Lfq1;->J(Ljava/lang/String;)V
            throw v3
        """,
    ),
    RestoredMethod(
        "Lme/proton/android/lumo/MainActivity;", "onStart", 4,
        """
            invoke-super {p0}, Landroid/app/Activity;->onStart()V
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->s()Lme/proton/android/lumo/webview/RendererDiagnostics;
            move-result-object v0
            const/4 v1, 0x1
            invoke-virtual {v0, v1}, Lme/proton/android/lumo/webview/RendererDiagnostics;->a(Z)V
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->v()Lme/proton/android/lumo/webview/LumoWebViewPool;
            move-result-object v0
            iput-object p0, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->g:Lme/proton/android/lumo/MainActivity;
            invoke-virtual {v0, p0}, Lme/proton/android/lumo/webview/LumoWebViewPool;->e(Lme/proton/android/lumo/MainActivity;)Z
            move-result v2
            if-eqz v2, :cond_1e
            invoke-virtual {v0}, Lme/proton/android/lumo/webview/LumoWebViewPool;->d()Landroid/content/MutableContextWrapper;
            move-result-object v2
            invoke-virtual {v2, p0}, Landroid/content/MutableContextWrapper;->setBaseContext(Landroid/content/Context;)V
            :cond_1e
            invoke-virtual {v0, v1}, Lme/proton/android/lumo/webview/LumoWebViewPool;->h(Z)V
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->t()Lme/proton/android/lumo/MainContainerViewModel;
            move-result-object v0
            iput-boolean v1, v0, Lme/proton/android/lumo/MainContainerViewModel;->t:Z
            invoke-virtual {v0}, Lme/proton/android/lumo/MainContainerViewModel;->q()V
            iget-object v0, p0, Lme/proton/android/lumo/MainActivity;->O:Lme/proton/android/lumo/MainContainerViewModel${'$'}UiEvent${'$'}LoadUrl;
            if-eqz v0, :cond_31
            invoke-virtual {p0, v0}, Lme/proton/android/lumo/MainActivity;->w(Lme/proton/android/lumo/MainContainerViewModel${'$'}UiEvent${'$'}LoadUrl;)V
            :cond_31
            return-void
        """,
    ),
    RestoredMethod(
        "Lme/proton/android/lumo/MainActivity;", "onResume", 2,
        """
            invoke-super {p0}, Landroid/app/Activity;->onResume()V
            iget-object v0, p0, Lme/proton/android/lumo/MainActivity;->G:Lme/proton/android/lumo/ActivityProvider;
            if-eqz v0, :cond_c
            check-cast v0, Lme/proton/android/lumo/DefaultActivityProvider;
            iput-object p0, v0, Lme/proton/android/lumo/DefaultActivityProvider;->a:Lme/proton/android/lumo/MainActivity;
            return-void
            :cond_c
            const-string p0, "activityProvider"
            invoke-static {p0}, Lfq1;->J(Ljava/lang/String;)V
            const/4 p0, 0x0
            throw p0
        """,
    ),
    RestoredMethod(
        "Lme/proton/android/lumo/MainActivity;", "onPause", 4,
        """
            invoke-super {p0}, Landroid/app/Activity;->onPause()V
            iget-object v0, p0, Lme/proton/android/lumo/MainActivity;->G:Lme/proton/android/lumo/ActivityProvider;
            const/4 v1, 0x0
            if-eqz v0, :cond_11
            check-cast v0, Lme/proton/android/lumo/DefaultActivityProvider;
            iget-object v2, v0, Lme/proton/android/lumo/DefaultActivityProvider;->a:Lme/proton/android/lumo/MainActivity;
            if-ne v2, p0, :cond_10
            iput-object v1, v0, Lme/proton/android/lumo/DefaultActivityProvider;->a:Lme/proton/android/lumo/MainActivity;
            :cond_10
            return-void
            :cond_11
            const-string p0, "activityProvider"
            invoke-static {p0}, Lfq1;->J(Ljava/lang/String;)V
            throw v1
        """,
    ),
    RestoredMethod(
        "Lme/proton/android/lumo/MainActivity;", "onStop", 6,
        """
            invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
            move-result v0
            iget-object v1, p0, Lme/proton/android/lumo/MainActivity;->O:Lme/proton/android/lumo/MainContainerViewModel${'$'}UiEvent${'$'}LoadUrl;
            const/4 v2, 0x0
            if-eqz v1, :cond_c
            iget-object v1, v1, Lme/proton/android/lumo/MainContainerViewModel${'$'}UiEvent${'$'}LoadUrl;->b:Ljava/lang/Long;
            goto :goto_d
            :cond_c
            move-object v1, v2
            :goto_d
            new-instance v3, Ljava/lang/StringBuilder;
            const-string v4, "account.activity.stopped instance="
            invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
            invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
            const-string v0, " pendingId="
            invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;
            invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            const/4 v1, 0x6
            invoke-static {p0, v0, v2, v2, v1}, Lme/proton/android/core/logging/LoggerExtKt;->f(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Exception;I)V
            invoke-super {p0}, Landroid/app/Activity;->onStop()V
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->s()Lme/proton/android/lumo/webview/RendererDiagnostics;
            move-result-object v0
            const/4 v1, 0x0
            invoke-virtual {v0, v1}, Lme/proton/android/lumo/webview/RendererDiagnostics;->a(Z)V
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->v()Lme/proton/android/lumo/webview/LumoWebViewPool;
            move-result-object v0
            iget-object v3, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->g:Lme/proton/android/lumo/MainActivity;
            if-ne v3, p0, :cond_3f
            iput-object v2, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->g:Lme/proton/android/lumo/MainActivity;
            invoke-virtual {v0, v1}, Lme/proton/android/lumo/webview/LumoWebViewPool;->h(Z)V
            :cond_3f
            invoke-virtual {v0, p0}, Lme/proton/android/lumo/webview/LumoWebViewPool;->e(Lme/proton/android/lumo/MainActivity;)Z
            move-result v3
            if-eqz v3, :cond_4e
            invoke-virtual {v0}, Lme/proton/android/lumo/webview/LumoWebViewPool;->d()Landroid/content/MutableContextWrapper;
            move-result-object v3
            iget-object v0, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->a:Landroid/content/Context;
            invoke-virtual {v3, v0}, Landroid/content/MutableContextWrapper;->setBaseContext(Landroid/content/Context;)V
            :cond_4e
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->t()Lme/proton/android/lumo/MainContainerViewModel;
            move-result-object v0
            iput-boolean v1, v0, Lme/proton/android/lumo/MainContainerViewModel;->t:Z
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->t()Lme/proton/android/lumo/MainContainerViewModel;
            move-result-object p0
            iget-object p0, p0, Lme/proton/android/lumo/MainContainerViewModel;->d:Lme/proton/android/lumo/analytics/LumoAnalytics;
            check-cast p0, Lme/proton/android/lumo/analytics/MainScreenAnalytics;
            iget-object p0, p0, Lme/proton/android/lumo/analytics/MainScreenAnalytics;->a:Lme/proton/android/lumo/tracer/LumoTracer;
            check-cast p0, Lme/proton/android/lumo/sentry/tracer/Tracer;
            iget-object v0, p0, Lme/proton/android/lumo/sentry/tracer/Tracer;->b:Lio/sentry/p1;
            if-eqz v0, :cond_69
            sget-object v1, Lio/sentry/l7;->CANCELLED:Lio/sentry/l7;
            invoke-interface {v0, v1}, Lio/sentry/n1;->a(Lio/sentry/l7;)V
            :cond_69
            iget-object v0, p0, Lme/proton/android/lumo/sentry/tracer/Tracer;->b:Lio/sentry/p1;
            if-eqz v0, :cond_70
            invoke-interface {v0}, Lio/sentry/n1;->i()V
            :cond_70
            iput-object v2, p0, Lme/proton/android/lumo/sentry/tracer/Tracer;->b:Lio/sentry/p1;
            iget-object p0, p0, Lme/proton/android/lumo/sentry/tracer/Tracer;->c:Ljava/util/LinkedHashMap;
            invoke-virtual {p0}, Ljava/util/LinkedHashMap;->clear()V
            return-void
        """,
    ),
    RestoredMethod(
        "Lme/proton/android/lumo/MainActivity;", "onDestroy", 7,
        """
            invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
            move-result v0
            invoke-virtual {p0}, Landroid/app/Activity;->isFinishing()Z
            move-result v1
            new-instance v2, Ljava/lang/StringBuilder;
            const-string v3, "account.activity.destroyed instance="
            invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
            invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
            const-string v0, " finishing="
            invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;
            invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            const/4 v1, 0x6
            const/4 v2, 0x0
            invoke-static {p0, v0, v2, v2, v1}, Lme/proton/android/core/logging/LoggerExtKt;->f(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Exception;I)V
            invoke-super {p0}, Lme/proton/android/lumo/Hilt_MainActivity;->onDestroy()V
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->v()Lme/proton/android/lumo/webview/LumoWebViewPool;
            move-result-object v0
            invoke-virtual {v0, p0}, Lme/proton/android/lumo/webview/LumoWebViewPool;->e(Lme/proton/android/lumo/MainActivity;)Z
            move-result v0
            if-eqz v0, :cond_c5
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->u()Lme/proton/android/lumo/webview/WebAppInterface;
            move-result-object v0
            invoke-virtual {p0}, Landroid/app/Activity;->isFinishing()Z
            move-result v1
            const/4 v3, 0x1
            xor-int/2addr v1, v3
            invoke-static {v0, v1, v3}, Lme/proton/android/lumo/webview/WebAppInterface;->a(Lme/proton/android/lumo/webview/WebAppInterface;ZI)V
            invoke-virtual {p0}, Landroid/app/Activity;->isFinishing()Z
            move-result v0
            if-eqz v0, :cond_4c
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->v()Lme/proton/android/lumo/webview/LumoWebViewPool;
            move-result-object v0
            invoke-virtual {v0}, Lme/proton/android/lumo/webview/LumoWebViewPool;->b()V
            goto/16 :goto_c5
            :cond_4c
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->v()Lme/proton/android/lumo/webview/LumoWebViewPool;
            move-result-object v0
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->u()Lme/proton/android/lumo/webview/WebAppInterface;
            move-result-object v1
            invoke-virtual {p0}, Lme/proton/android/lumo/MainActivity;->s()Lme/proton/android/lumo/webview/RendererDiagnostics;
            move-result-object v4
            new-instance v5, Lme/proton/android/lumo/MainActivity${'$'}onDestroy${'$'}1;
            invoke-direct {v5, v4, v0, v1}, Lme/proton/android/lumo/MainActivity${'$'}onDestroy${'$'}1;-><init>(Lme/proton/android/lumo/webview/RendererDiagnostics;Lme/proton/android/lumo/webview/LumoWebViewPool;Lme/proton/android/lumo/webview/WebAppInterface;)V
            invoke-virtual {v0, p0}, Lme/proton/android/lumo/webview/LumoWebViewPool;->e(Lme/proton/android/lumo/MainActivity;)Z
            move-result v1
            if-nez v1, :cond_64
            goto :goto_c5
            :cond_64
            iget-object v1, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->e:Lme/proton/android/lumo/webview/LumoWebClient;
            if-eqz v1, :cond_8b
            iput-boolean v3, v1, Lme/proton/android/lumo/webview/LumoWebClient;->l:Z
            iget-object v1, v1, Lme/proton/android/lumo/webview/LumoWebClient;->k:Lme/proton/android/lumo/webview/PendingWebViewCallbacks;
            iput-boolean v3, v1, Lme/proton/android/lumo/webview/PendingWebViewCallbacks;->c:Z
            iget-object v1, v1, Lme/proton/android/lumo/webview/PendingWebViewCallbacks;->b:Ljava/util/LinkedHashSet;
            invoke-static {v1}, Ln60;->B0(Ljava/lang/Iterable;)Ljava/util/List;
            move-result-object v3
            invoke-interface {v3}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;
            move-result-object v3
            :goto_78
            invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z
            move-result v4
            if-eqz v4, :cond_88
            invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v4
            check-cast v4, Lgr1;
            invoke-interface {v4, v2}, Lgr1;->j(Ljava/util/concurrent/CancellationException;)V
            goto :goto_78
            :cond_88
            invoke-interface {v1}, Ljava/util/Set;->clear()V
            :cond_8b
            iput-object v2, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->e:Lme/proton/android/lumo/webview/LumoWebClient;
            iput-object v5, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->i:Lyb1;
            iget-object v1, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->d:Landroid/webkit/WebView;
            if-eqz v1, :cond_b7
            iget-object v3, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->k:Lf04;
            invoke-virtual {v3}, Lf04;->getValue()Ljava/lang/Object;
            move-result-object v3
            check-cast v3, Lme/proton/android/lumo/webview/LumoWebViewPool${'$'}detachedClient${'$'}2${'$'}1;
            invoke-virtual {v1, v3}, Landroid/webkit/WebView;->setWebViewClient(Landroid/webkit/WebViewClient;)V
            invoke-virtual {v1, v2}, Landroid/webkit/WebView;->setWebChromeClient(Landroid/webkit/WebChromeClient;)V
            sget-object v3, Lbl4;->a:Ljava/util/WeakHashMap;
            invoke-static {v1, v2}, Lvk4;->c(Landroid/view/View;Lvn2;)V
            invoke-virtual {v1}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
            move-result-object v3
            instance-of v4, v3, Landroid/view/ViewGroup;
            if-eqz v4, :cond_b1
            check-cast v3, Landroid/view/ViewGroup;
            goto :goto_b2
            :cond_b1
            move-object v3, v2
            :goto_b2
            if-eqz v3, :cond_b7
            invoke-virtual {v3, v1}, Landroid/view/ViewGroup;->removeView(Landroid/view/View;)V
            :cond_b7
            iget-object v1, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->f:Ljava/lang/ref/WeakReference;
            invoke-virtual {v1}, Ljava/lang/ref/Reference;->clear()V
            invoke-virtual {v0}, Lme/proton/android/lumo/webview/LumoWebViewPool;->d()Landroid/content/MutableContextWrapper;
            move-result-object v1
            iget-object v0, v0, Lme/proton/android/lumo/webview/LumoWebViewPool;->a:Landroid/content/Context;
            invoke-virtual {v1, v0}, Landroid/content/MutableContextWrapper;->setBaseContext(Landroid/content/Context;)V
            :cond_c5
            :goto_c5
            iput-object v2, p0, Lme/proton/android/lumo/MainActivity;->N:Landroid/webkit/WebView;
            return-void
        """,
    ),
    RestoredMethod(
        "Lio/sentry/android/core/SentryPerformanceProvider;", "onCreate", 9,
        """
            invoke-static {}, Lio/sentry/android/core/performance/h;->c()Lio/sentry/android/core/performance/h;
            move-result-object v0
            invoke-virtual {p0}, Landroid/content/ContentProvider;->getContext()Landroid/content/Context;
            move-result-object v1
            iget-object v2, v0, Lio/sentry/android/core/performance/h;->k:Lio/sentry/android/core/performance/i;
            sget-wide v3, Lio/sentry/android/core/SentryPerformanceProvider;->k:J
            invoke-virtual {v2, v3, v4}, Lio/sentry/android/core/performance/i;->c(J)V
            iget-object v2, p0, Lio/sentry/android/core/SentryPerformanceProvider;->j:Lio/sentry/w;
            invoke-virtual {v2}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
            iget-object v2, v0, Lio/sentry/android/core/performance/h;->j:Lio/sentry/android/core/performance/i;
            invoke-static {}, Landroid/os/Process;->getStartUptimeMillis()J
            move-result-wide v3
            invoke-virtual {v2, v3, v4}, Lio/sentry/android/core/performance/i;->c(J)V
            instance-of v2, v1, Landroid/app/Application;
            if-eqz v2, :cond_25
            check-cast v1, Landroid/app/Application;
            iput-object v1, p0, Lio/sentry/android/core/SentryPerformanceProvider;->h:Landroid/app/Application;
            :cond_25
            iget-object v1, p0, Lio/sentry/android/core/SentryPerformanceProvider;->h:Landroid/app/Application;
            if-nez v1, :cond_2a
            goto :goto_2d
            :cond_2a
            invoke-virtual {v0, v1}, Lio/sentry/android/core/performance/h;->e(Landroid/app/Application;)V
            :goto_2d
            invoke-virtual {p0}, Landroid/content/ContentProvider;->getContext()Landroid/content/Context;
            move-result-object v1
            const/4 v2, 0x0
            iget-object v3, p0, Lio/sentry/android/core/SentryPerformanceProvider;->i:Lio/sentry/android/core/k;
            if-nez v1, :cond_41
            sget-object p0, Lio/sentry/s5;->FATAL:Lio/sentry/s5;
            const-string v0, "App. Context from ContentProvider is null"
            new-array v1, v2, [Ljava/lang/Object;
            invoke-virtual {v3, p0, v0, v1}, Lio/sentry/android/core/k;->g(Lio/sentry/s5;Ljava/lang/String;[Ljava/lang/Object;)V
            goto/16 :goto_d0
            :cond_41
            new-instance v4, Ljava/io/File;
            invoke-virtual {v1}, Landroid/content/Context;->getCacheDir()Ljava/io/File;
            move-result-object v5
            const-string v6, "sentry"
            invoke-direct {v4, v5, v6}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
            new-instance v5, Ljava/io/File;
            const-string v6, "app_start_profiling_config"
            invoke-direct {v5, v4, v6}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
            invoke-virtual {v5}, Ljava/io/File;->exists()Z
            move-result v4
            if-eqz v4, :cond_d0
            invoke-virtual {v5}, Ljava/io/File;->canRead()Z
            move-result v4
            if-nez v4, :cond_61
            goto/16 :goto_d0
            :cond_61
            :try_start_61
            new-instance v4, Ljava/io/BufferedReader;
            new-instance v6, Ljava/io/InputStreamReader;
            new-instance v7, Ljava/io/FileInputStream;
            invoke-direct {v7, v5}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
            invoke-direct {v6, v7}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V
            invoke-direct {v4, v6}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
            :try_end_70
            .catch Ljava/io/FileNotFoundException; {:try_start_61 .. :try_end_70} :catch_92
            .catchall {:try_start_61 .. :try_end_70} :catchall_90
            :try_start_70
            new-instance v5, Lio/sentry/l2;
            invoke-static {}, Lio/sentry/SentryOptions;->empty()Lio/sentry/SentryOptions;
            move-result-object v6
            invoke-direct {v5, v6}, Lio/sentry/l2;-><init>(Lio/sentry/SentryOptions;)V
            const-class v6, Lio/sentry/s4;
            invoke-virtual {v5, v4, v6}, Lio/sentry/l2;->d(Ljava/io/Reader;Ljava/lang/Class;)Ljava/lang/Object;
            move-result-object v5
            check-cast v5, Lio/sentry/s4;
            if-nez v5, :cond_96
            sget-object p0, Lio/sentry/s5;->WARNING:Lio/sentry/s5;
            const-string v0, "Unable to deserialize the SentryAppStartProfilingOptions. App start profiling will not start."
            new-array v1, v2, [Ljava/lang/Object;
            invoke-virtual {v3, p0, v0, v1}, Lio/sentry/android/core/k;->g(Lio/sentry/s5;Ljava/lang/String;[Ljava/lang/Object;)V
            :try_end_8c
            .catchall {:try_start_70 .. :try_end_8c} :catchall_94
            :cond_8c
            :goto_8c
            :try_start_8c
            invoke-virtual {v4}, Ljava/io/Reader;->close()V
            :try_end_8f
            .catch Ljava/io/FileNotFoundException; {:try_start_8c .. :try_end_8f} :catch_92
            .catchall {:try_start_8c .. :try_end_8f} :catchall_90
            goto :goto_d0
            :catchall_90
            move-exception p0
            goto :goto_c1
            :catch_92
            move-exception p0
            goto :goto_c9
            :catchall_94
            move-exception p0
            goto :goto_b8
            :cond_96
            :try_start_96
            iget-boolean v6, v5, Lio/sentry/s4;->m:Z
            if-eqz v6, :cond_a2
            iget-boolean v6, v5, Lio/sentry/s4;->q:Z
            if-eqz v6, :cond_a2
            invoke-virtual {p0, v1, v5, v0}, Lio/sentry/android/core/SentryPerformanceProvider;->a(Landroid/content/Context;Lio/sentry/s4;Lio/sentry/android/core/performance/h;)V
            goto :goto_8c
            :cond_a2
            iget-boolean v6, v5, Lio/sentry/s4;->l:Z
            if-nez v6, :cond_b0
            sget-object p0, Lio/sentry/s5;->INFO:Lio/sentry/s5;
            const-string v0, "Profiling is not enabled. App start profiling will not start."
            new-array v1, v2, [Ljava/lang/Object;
            invoke-virtual {v3, p0, v0, v1}, Lio/sentry/android/core/k;->g(Lio/sentry/s5;Ljava/lang/String;[Ljava/lang/Object;)V
            goto :goto_8c
            :cond_b0
            iget-boolean v2, v5, Lio/sentry/s4;->p:Z
            if-eqz v2, :cond_8c
            invoke-virtual {p0, v1, v5, v0}, Lio/sentry/android/core/SentryPerformanceProvider;->b(Landroid/content/Context;Lio/sentry/s4;Lio/sentry/android/core/performance/h;)V
            :try_end_b7
            .catchall {:try_start_96 .. :try_end_b7} :catchall_94
            goto :goto_8c
            :goto_b8
            :try_start_b8
            invoke-virtual {v4}, Ljava/io/Reader;->close()V
            :try_end_bb
            .catchall {:try_start_b8 .. :try_end_bb} :catchall_bc
            goto :goto_c0
            :catchall_bc
            move-exception v0
            :try_start_bd
            invoke-virtual {p0, v0}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V
            :goto_c0
            throw p0
            :try_end_c1
            .catch Ljava/io/FileNotFoundException; {:try_start_bd .. :try_end_c1} :catch_92
            .catchall {:try_start_bd .. :try_end_c1} :catchall_90
            :goto_c1
            sget-object v0, Lio/sentry/s5;->ERROR:Lio/sentry/s5;
            const-string v1, "Error reading app start profiling config file. "
            invoke-virtual {v3, v0, v1, p0}, Lio/sentry/android/core/k;->e(Lio/sentry/s5;Ljava/lang/String;Ljava/lang/Throwable;)V
            goto :goto_d0
            :goto_c9
            sget-object v0, Lio/sentry/s5;->ERROR:Lio/sentry/s5;
            const-string v1, "App start profiling config file not found. "
            invoke-virtual {v3, v0, v1, p0}, Lio/sentry/android/core/k;->e(Lio/sentry/s5;Ljava/lang/String;Ljava/lang/Throwable;)V
            :cond_d0
            :goto_d0
            const/4 p0, 0x1
            return p0
        """,
    ),
    RestoredMethod(
        "Landroidx/work/impl/foreground/SystemForegroundService;", "onStartCommand", 8,
        """
            invoke-super {p0, p1, p2, p3}, Landroid/app/Service;->onStartCommand(Landroid/content/Intent;II)I
            iget-boolean p2, p0, Landroidx/work/impl/foreground/SystemForegroundService;->h:Z
            sget-object v0, Landroidx/work/impl/foreground/SystemForegroundService;->k:Ljava/lang/String;
            if-eqz p2, :cond_1d
            invoke-static {}, Li52;->d()Li52;
            move-result-object p2
            const-string v1, "Re-initializing SystemForegroundService after a request to shut-down."
            invoke-virtual {p2, v0, v1}, Li52;->e(Ljava/lang/String;Ljava/lang/String;)V
            iget-object p2, p0, Landroidx/work/impl/foreground/SystemForegroundService;->i:Lj04;
            invoke-virtual {p2}, Lj04;->f()V
            invoke-virtual {p0}, Landroidx/work/impl/foreground/SystemForegroundService;->a()V
            const/4 p2, 0x0
            iput-boolean p2, p0, Landroidx/work/impl/foreground/SystemForegroundService;->h:Z
            :cond_1d
            if-eqz p1, :cond_df
            iget-object p0, p0, Landroidx/work/impl/foreground/SystemForegroundService;->i:Lj04;
            invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
            sget-object p2, Lj04;->p:Ljava/lang/String;
            invoke-virtual {p1}, Landroid/content/Intent;->getAction()Ljava/lang/String;
            move-result-object v1
            const-string v2, "ACTION_START_FOREGROUND"
            invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v2
            const-string v3, "KEY_WORKSPEC_ID"
            if-eqz v2, :cond_5f
            invoke-static {}, Li52;->d()Li52;
            move-result-object p3
            new-instance v0, Ljava/lang/StringBuilder;
            const-string v1, "Started foreground service "
            invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
            invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;
            invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            invoke-virtual {p3, p2, v0}, Li52;->e(Ljava/lang/String;Ljava/lang/String;)V
            invoke-virtual {p1, v3}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
            move-result-object p2
            iget-object p3, p0, Lj04;->h:Lhq4;
            new-instance v0, Lkx4;
            const/16 v1, 0x8
            invoke-direct {v0, v1, p2, p0}, Lkx4;-><init>(ILjava/lang/Object;Ljava/lang/Object;)V
            iget-object p2, p3, Lhq4;->a:Lan3;
            invoke-virtual {p2, v0}, Lan3;->execute(Ljava/lang/Runnable;)V
            invoke-virtual {p0, p1}, Lj04;->e(Landroid/content/Intent;)V
            goto/16 :goto_df
            :cond_5f
            const-string v2, "ACTION_NOTIFY"
            invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v2
            if-eqz v2, :cond_6b
            invoke-virtual {p0, p1}, Lj04;->e(Landroid/content/Intent;)V
            goto :goto_df
            :cond_6b
            const-string v2, "ACTION_CANCEL_WORK"
            invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v2
            if-eqz v2, :cond_b8
            invoke-static {}, Li52;->d()Li52;
            move-result-object p3
            new-instance v0, Ljava/lang/StringBuilder;
            const-string v1, "Stopping foreground work for "
            invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
            invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;
            invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            invoke-virtual {p3, p2, v0}, Li52;->e(Ljava/lang/String;Ljava/lang/String;)V
            invoke-virtual {p1, v3}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
            move-result-object p1
            if-eqz p1, :cond_df
            invoke-static {p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
            move-result p2
            if-nez p2, :cond_df
            iget-object p0, p0, Lj04;->g:Leq4;
            invoke-static {p1}, Ljava/util/UUID;->fromString(Ljava/lang/String;)Ljava/util/UUID;
            move-result-object p1
            invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
            invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
            iget-object p2, p0, Leq4;->b:Lfc0;
            iget-object p2, p2, Lfc0;->m:Lo3;
            iget-object p3, p0, Leq4;->d:Lhq4;
            iget-object p3, p3, Lhq4;->a:Lan3;
            invoke-virtual {p3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
            new-instance v0, Lbd;
            const/16 v1, 0x8
            invoke-direct {v0, v1, p0, p1}, Lbd;-><init>(ILjava/lang/Object;Ljava/lang/Object;)V
            const-string p0, "CancelWorkById"
            invoke-static {p2, p0, p3, v0}, Lcv0;->Q(Lo3;Ljava/lang/String;Ljava/util/concurrent/Executor;Lnb1;)Led3;
            goto :goto_df
            :cond_b8
            const-string p1, "ACTION_STOP_FOREGROUND"
            invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result p1
            if-eqz p1, :cond_df
            invoke-static {}, Li52;->d()Li52;
            move-result-object p1
            const-string v1, "Stopping foreground service"
            invoke-virtual {p1, p2, v1}, Li52;->e(Ljava/lang/String;Ljava/lang/String;)V
            iget-object p0, p0, Lj04;->o:Landroidx/work/impl/foreground/SystemForegroundService;
            if-eqz p0, :cond_df
            const/4 p1, 0x1
            iput-boolean p1, p0, Landroidx/work/impl/foreground/SystemForegroundService;->h:Z
            invoke-static {}, Li52;->d()Li52;
            move-result-object p2
            const-string v1, "Shutting down."
            invoke-virtual {p2, v0, v1}, Li52;->a(Ljava/lang/String;Ljava/lang/String;)V
            invoke-virtual {p0, p1}, Landroid/app/Service;->stopForeground(Z)V
            invoke-virtual {p0, p3}, Landroid/app/Service;->stopSelf(I)V
            :cond_df
            :goto_df
            const/4 p0, 0x3
            return p0
        """,
    ),
)

internal val restoreVirtualizedMethodsPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.LUMO)
    dependsOn(removePairipVirtualizationPatch)

    execute {
        if (classDefBy(APPLICATION_CLASS).virtualizedMethods().isEmpty()) return@execute
        restoreVirtualizedMethods(RESTORED_METHODS)
    }
}
