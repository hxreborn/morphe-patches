.class public Lcom/myscript/nebo/launch/LaunchActivity;
.super Landroidx/appcompat/app/AppCompatActivity;

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 13
    sget-object v0, Landroidx/core/splashscreen/SplashScreen;->Companion:Landroidx/core/splashscreen/SplashScreen$Companion;
    move-object v1, p0
    check-cast v1, Landroid/app/Activity;
    invoke-virtual {v0, v1}, Landroidx/core/splashscreen/SplashScreen$Companion;->installSplashScreen(Landroid/app/Activity;)Landroidx/core/splashscreen/SplashScreen;
    move-result-object v0
    invoke-super {p0, p1}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V
    sget-object p1, Lcom/myscript/nebo/AppLaunchRouter;->INSTANCE:Lcom/myscript/nebo/AppLaunchRouter;
    invoke-virtual {p1}, Lcom/myscript/nebo/AppLaunchRouter;->getRedirect()Lkotlin/jvm/functions/Function1;
    move-result-object p1
    const/4 v1, 0x1
    if-eqz p1, :cond_25
    invoke-interface {p1, p0}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object p1
    check-cast p1, Ljava/lang/Boolean;
    invoke-virtual {p1}, Ljava/lang/Boolean;->booleanValue()Z
    move-result p1
    if-ne p1, v1, :cond_25
    invoke-virtual {p0}, Lcom/myscript/nebo/launch/LaunchActivity;->finish()V
    return-void
    :cond_25
    iget-object p1, p0, Lcom/myscript/nebo/launch/LaunchActivity;->appUpdateHandler:Lcom/myscript/nebo/AppUpdateHandler;
    move-object v2, p0
    check-cast v2, Landroidx/appcompat/app/AppCompatActivity;
    invoke-virtual {p1, v2}, Lcom/myscript/nebo/AppUpdateHandler;->registerActivity(Landroidx/appcompat/app/AppCompatActivity;)V
    new-instance p1, Lcom/myscript/nebo/launch/LaunchActivity$$ExternalSyntheticLambda3;
    invoke-direct {p1, p0}, Lcom/myscript/nebo/launch/LaunchActivity$$ExternalSyntheticLambda3;-><init>(Lcom/myscript/nebo/launch/LaunchActivity;)V
    invoke-virtual {v0, p1}, Landroidx/core/splashscreen/SplashScreen;->setKeepOnScreenCondition(Landroidx/core/splashscreen/SplashScreen$KeepOnScreenCondition;)V
    move-object p1, p0
    check-cast p1, Landroidx/activity/ComponentActivity;
    const/4 v0, 0x0
    const/4 v2, 0x3
    invoke-static {p1, v0, v0, v2, v0}, Landroidx/activity/EdgeToEdge;->enable$default(Landroidx/activity/ComponentActivity;Landroidx/activity/SystemBarStyle;Landroidx/activity/SystemBarStyle;ILjava/lang/Object;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/launch/LaunchActivity;->getApplication()Landroid/app/Application;
    move-result-object v3
    const-string v4, "null cannot be cast to non-null type com.myscript.nebo.log.TechnicalLoggerProvider"
    invoke-static {v3, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v3, Lcom/myscript/nebo/log/TechnicalLoggerProvider;
    invoke-interface {v3}, Lcom/myscript/nebo/log/TechnicalLoggerProvider;->provideTechnicalLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v3
    iput-object v3, p0, Lcom/myscript/nebo/launch/LaunchActivity;->logger:Lcom/myscript/nebo/log/TechnicalLogger;
    invoke-virtual {p0}, Lcom/myscript/nebo/launch/LaunchActivity;->getApplication()Landroid/app/Application;
    move-result-object v3
    const-string v4, "null cannot be cast to non-null type com.myscript.nebo.whatsnew.IApplicationStateProvider"
    invoke-static {v3, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v3, Lcom/myscript/nebo/whatsnew/IApplicationStateProvider;
    invoke-interface {v3}, Lcom/myscript/nebo/whatsnew/IApplicationStateProvider;->provideApplicationState()Lcom/myscript/nebo/whatsnew/ApplicationState;
    move-result-object v3
    invoke-virtual {v3}, Lcom/myscript/nebo/whatsnew/ApplicationState;->getStoredVersion()Ljava/lang/String;
    move-result-object v3
    if-nez v3, :cond_69
    move-object v3, p0
    check-cast v3, Landroid/content/Context;
    invoke-static {v3}, Lcom/myscript/nebo/utils/NewFeatureVisibilityHelper;->markAllFeaturesDiscovered(Landroid/content/Context;)V
    :cond_69
    new-instance v3, Lkotlin/jvm/internal/Ref$ObjectRef;
    invoke-direct {v3}, Lkotlin/jvm/internal/Ref$ObjectRef;-><init>()V
    move-object v4, p0
    check-cast v4, Landroid/content/Context;
    invoke-virtual {p0}, Lcom/myscript/nebo/launch/LaunchActivity;->getIntent()Landroid/content/Intent;
    move-result-object v5
    invoke-static {v4, v5}, Lcom/myscript/nebo/main/ImportRequestData;->fromIntent(Landroid/content/Context;Landroid/content/Intent;)Lcom/myscript/nebo/main/ImportRequestData;
    move-result-object v5
    if-eqz v5, :cond_12c
    invoke-virtual {v5}, Lcom/myscript/nebo/main/ImportRequestData;->getUri()Landroid/net/Uri;
    move-result-object v6
    const-string v7, "getUri(...)"
    invoke-static {v6, v7}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    new-instance v7, Ljava/io/File;
    new-instance v8, Ljava/io/File;
    invoke-virtual {p0}, Lcom/myscript/nebo/launch/LaunchActivity;->getApplication()Landroid/app/Application;
    move-result-object v9
    invoke-virtual {v9}, Landroid/app/Application;->getCacheDir()Ljava/io/File;
    move-result-object v9
    const-string v10, "imported"
    invoke-direct {v8, v9, v10}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
    invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;
    move-result-object v9
    invoke-virtual {v9}, Ljava/util/UUID;->toString()Ljava/lang/String;
    move-result-object v9
    invoke-direct {v7, v8, v9}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
    invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;
    move-result-object v8
    invoke-virtual {v8}, Ljava/util/UUID;->toString()Ljava/lang/String;
    move-result-object v8
    invoke-virtual {v5, v4}, Lcom/myscript/nebo/main/ImportRequestData;->getMimeType(Landroid/content/Context;)Ljava/lang/String;
    move-result-object v5
    invoke-static {v6, v4, v8, v5}, Lcom/myscript/nebo/main/ImportRequestData;->resolveFileName(Landroid/net/Uri;Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v5
    const-string v8, "resolveFileName(...)"
    invoke-static {v5, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-static {v4, v6}, Lcom/myscript/nebo/main/ui/ImportableDocumentKt;->classifyImportableDocument(Landroid/content/Context;Landroid/net/Uri;)Lcom/myscript/nebo/main/ui/ImportableKind;
    move-result-object v8
    sget-object v9, Lcom/myscript/nebo/launch/LaunchActivity$WhenMappings;->$EnumSwitchMapping$1:[I
    invoke-virtual {v8}, Lcom/myscript/nebo/main/ui/ImportableKind;->ordinal()I
    move-result v8
    aget v8, v9, v8
    if-eq v8, v1, :cond_cf
    const/4 v9, 0x2
    if-eq v8, v9, :cond_100
    if-ne v8, v2, :cond_c9
    goto :goto_100
    :cond_c9
    new-instance p0, Lkotlin/NoWhenBranchMatchedException;
    invoke-direct {p0}, Lkotlin/NoWhenBranchMatchedException;-><init>()V
    throw p0
    :cond_cf
    const/16 v2, 0x2e
    const-string v8, ""
    invoke-static {v5, v2, v8}, Lkotlin/text/StringsKt;->substringAfterLast(Ljava/lang/String;CLjava/lang/String;)Ljava/lang/String;
    move-result-object v2
    sget-object v8, Ljava/util/Locale;->ROOT:Ljava/util/Locale;
    invoke-virtual {v2, v8}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;
    move-result-object v2
    const-string v8, "toLowerCase(...)"
    invoke-static {v2, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-static {}, Lcom/myscript/nebo/main/ui/ImportableDocumentKt;->getIMPORTABLE_DOCUMENT_EXTENSIONS()Ljava/util/Set;
    move-result-object v8
    invoke-interface {v8, v2}, Ljava/util/Set;->contains(Ljava/lang/Object;)Z
    move-result v2
    if-eqz v2, :cond_ed
    goto :goto_100
    :cond_ed
    new-instance v2, Ljava/lang/StringBuilder;
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v2
    const-string v5, ".pdf"
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v2
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v5
    :cond_100
    :goto_100
    invoke-virtual {p0}, Lcom/myscript/nebo/launch/LaunchActivity;->getApplication()Landroid/app/Application;
    move-result-object v2
    check-cast v2, Landroid/content/Context;
    iget-object v8, p0, Lcom/myscript/nebo/launch/LaunchActivity;->logger:Lcom/myscript/nebo/log/TechnicalLogger;
    if-nez v8, :cond_110
    const-string v8, "logger"
    invoke-static {v8}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    move-object v8, v0
    :cond_110
    invoke-static {v2, v7, v6, v5, v8}, Lcom/myscript/nebo/dms/util/ImportController;->copyFile(Landroid/content/Context;Ljava/io/File;Landroid/net/Uri;Ljava/lang/String;Lcom/myscript/nebo/log/TechnicalLogger;)Lcom/myscript/nebo/dms/util/ImportController$ImportResult;
    move-result-object v2
    invoke-virtual {v2}, Lcom/myscript/nebo/dms/util/ImportController$ImportResult;->isSuccess()Z
    move-result v5
    if-eqz v5, :cond_11b
    goto :goto_11c
    :cond_11b
    move-object v2, v0
    :goto_11c
    if-eqz v2, :cond_121
    iget-object v2, v2, Lcom/myscript/nebo/dms/util/ImportController$ImportResult;->file:Ljava/io/File;
    goto :goto_122
    :cond_121
    move-object v2, v0
    :goto_122
    if-eqz v2, :cond_12c
    const-string v5, "com.myscript.nebo.file"
    invoke-static {v4, v5, v2}, Landroidx/core/content/FileProvider;->getUriForFile(Landroid/content/Context;Ljava/lang/String;Ljava/io/File;)Landroid/net/Uri;
    move-result-object v2
    iput-object v2, v3, Lkotlin/jvm/internal/Ref$ObjectRef;->element:Ljava/lang/Object;
    :cond_12c
    new-instance v2, Lcom/myscript/nebo/launch/LaunchActivity$$ExternalSyntheticLambda4;
    invoke-direct {v2, p0, v3}, Lcom/myscript/nebo/launch/LaunchActivity$$ExternalSyntheticLambda4;-><init>(Lcom/myscript/nebo/launch/LaunchActivity;Lkotlin/jvm/internal/Ref$ObjectRef;)V
    const v3, 0x5029e44f
    invoke-static {v3, v1, v2}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->composableLambdaInstance(IZLjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambda;
    move-result-object v2
    check-cast v2, Lkotlin/jvm/functions/Function2;
    invoke-static {p1, v0, v2, v1, v0}, Landroidx/activity/compose/ComponentActivityKt;->setContent$default(Landroidx/activity/ComponentActivity;Landroidx/compose/runtime/CompositionContext;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/launch/LaunchActivity;->getSupportFragmentManager()Landroidx/fragment/app/FragmentManager;
    move-result-object p1
    const-string v0, "getSupportFragmentManager(...)"
    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    move-object v0, p0
    check-cast v0, Landroidx/lifecycle/LifecycleOwner;
    new-instance v1, Lcom/myscript/nebo/launch/LaunchActivity$$ExternalSyntheticLambda5;
    invoke-direct {v1, p0}, Lcom/myscript/nebo/launch/LaunchActivity$$ExternalSyntheticLambda5;-><init>(Lcom/myscript/nebo/launch/LaunchActivity;)V
    const-string v2, "OnboardingLanguageSelectionFragment_REQUEST_KEY"
    invoke-virtual {p1, v2, v0, v1}, Landroidx/fragment/app/FragmentManager;->setFragmentResultListener(Ljava/lang/String;Landroidx/lifecycle/LifecycleOwner;Landroidx/fragment/app/FragmentResultListener;)V
    new-instance v1, Lcom/myscript/nebo/launch/LaunchActivity$$ExternalSyntheticLambda6;
    invoke-direct {v1, p0}, Lcom/myscript/nebo/launch/LaunchActivity$$ExternalSyntheticLambda6;-><init>(Lcom/myscript/nebo/launch/LaunchActivity;)V
    const-string p0, "PLS_MIGRATION_KEY"
    invoke-virtual {p1, p0, v0, v1}, Landroidx/fragment/app/FragmentManager;->setFragmentResultListener(Ljava/lang/String;Landroidx/lifecycle/LifecycleOwner;Landroidx/fragment/app/FragmentResultListener;)V
    return-void
.end method
