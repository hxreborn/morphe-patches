.class public Lcom/myscript/nebo/NeboApplication;
.super Landroid/app/Application;

.method public onCreate()V
    .registers 25
    move-object/from16 v1, p0
    const-string v2, "context"
    const-string v3, "count"
    const-string v4, "NeboApplication"
    const-string v5, "signature"
    move-object v0, v1
    check-cast v0, Landroid/content/Context;
    invoke-direct {v1, v0}, Lcom/myscript/nebo/NeboApplication;->isMainProcess(Landroid/content/Context;)Z
    move-result v6
    if-nez v6, :cond_14
    return-void
    :cond_14
    sput-object v1, Lcom/myscript/nebo/NeboApplication;->Self:Lcom/myscript/nebo/NeboApplication;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setLogger(Lcom/myscript/nebo/log/TechnicalLogger;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v7
    const/4 v11, 0x4
    const/4 v12, 0x0
    const-string v8, "NeboApplication"
    const-string v9, "Start"
    const/4 v10, 0x0
    invoke-static/range {v7 .. v12}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    invoke-static {v0}, Lcom/myscript/nebo/analytics/AnalyticsController;->instantiate(Landroid/content/Context;)V
    sget-object v6, Lcom/myscript/nebo/survicate/SurvicateController;->INSTANCE:Lcom/myscript/nebo/survicate/SurvicateController;
    sget v7, Lcom/myscript/nebo/R$string;->survicate_workspace_key:I
    invoke-virtual {v1, v7}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v7
    const-string v8, "getString(...)"
    invoke-static {v7, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {v6, v0, v7}, Lcom/myscript/nebo/survicate/SurvicateController;->initSDK(Landroid/content/Context;Ljava/lang/String;)V
    new-instance v6, Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfigController;
    invoke-direct {v6}, Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfigController;-><init>()V
    iput-object v6, v1, Lcom/myscript/nebo/NeboApplication;->remoteConfig:Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfigController;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createPreferences()Landroid/content/SharedPreferences;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setPreferences(Landroid/content/SharedPreferences;)V
    new-instance v6, Lcom/myscript/nebo/UserSessionManager;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v7
    invoke-direct {v6, v7}, Lcom/myscript/nebo/UserSessionManager;-><init>(Landroid/content/SharedPreferences;)V
    iput-object v6, v1, Lcom/myscript/nebo/NeboApplication;->sessionManager:Lcom/myscript/nebo/UserSessionManager;
    new-instance v9, Lcom/myscript/nebo/whatsnew/CrossSellFilter;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v10
    move-object v11, v1
    check-cast v11, Lcom/myscript/nebo/common/IClock;
    const/4 v13, 0x4
    const/4 v14, 0x0
    const/4 v12, 0x0
    invoke-direct/range {v9 .. v14}, Lcom/myscript/nebo/whatsnew/CrossSellFilter;-><init>(Landroid/content/SharedPreferences;Lcom/myscript/nebo/common/IClock;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V
    iput-object v9, v1, Lcom/myscript/nebo/NeboApplication;->crossSellFilter:Lcom/myscript/nebo/whatsnew/CrossSellFilter;
    new-instance v6, Lcom/myscript/nebo/whatsnew/ApplicationState;
    move-object v7, v1
    check-cast v7, Landroid/app/Application;
    invoke-direct {v6, v7}, Lcom/myscript/nebo/whatsnew/ApplicationState;-><init>(Landroid/app/Application;)V
    iput-object v6, v1, Lcom/myscript/nebo/NeboApplication;->applicationState:Lcom/myscript/nebo/whatsnew/ApplicationState;
    invoke-virtual {v6}, Lcom/myscript/nebo/whatsnew/ApplicationState;->wasCrashed()Z
    move-result v6
    sput-boolean v6, Lcom/myscript/nebo/NeboApplication;->wasRestartedFromCrash:Z
    iget-object v6, v1, Lcom/myscript/nebo/NeboApplication;->applicationState:Lcom/myscript/nebo/whatsnew/ApplicationState;
    const-string v9, "applicationState"
    const/4 v10, 0x0
    if-nez v6, :cond_83
    invoke-static {v9}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    move-object v6, v10
    :cond_83
    invoke-virtual {v6}, Lcom/myscript/nebo/whatsnew/ApplicationState;->clearCrash()V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v11
    const/4 v15, 0x4
    const/16 v16, 0x0
    const-string v12, "NeboApplication"
    const-string v13, "Create MyScript engine"
    const/4 v14, 0x0
    invoke-static/range {v11 .. v16}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    :try_start_95
    invoke-static {}, Lcom/myscript/certificate/MyCertificate;->getBytes()[B
    move-result-object v6
    const-wide/32 v11, 0x466510d
    invoke-static {v6, v11, v12}, Lcom/myscript/atk/core/Context;->getInstance([BJ)Lcom/myscript/atk/core/Context;
    move-result-object v6
    :try_end_a0
    .catch Lcom/myscript/atk/core/EngineError; {:try_start_95 .. :try_end_a0} :catch_711
    iput-object v6, v1, Lcom/myscript/nebo/NeboApplication;->engineContext:Lcom/myscript/atk/core/Context;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createUserDataProvider()Lcom/myscript/nebo/sso/model/UserDataProvider;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setUserDataProvider(Lcom/myscript/nebo/sso/model/UserDataProvider;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createHttpClient()Lokhttp3/OkHttpClient;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setHttpClient(Lokhttp3/OkHttpClient;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createSsoManager()Lcom/myscript/nebo/sso/api/ISSOManager;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setSsoManager(Lcom/myscript/nebo/sso/api/ISSOManager;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getSsoManager()Lcom/myscript/nebo/sso/api/ISSOManager;
    move-result-object v6
    new-instance v11, Lcom/myscript/nebo/NeboApplication$onCreate$2;
    invoke-direct {v11, v1}, Lcom/myscript/nebo/NeboApplication$onCreate$2;-><init>(Lcom/myscript/nebo/NeboApplication;)V
    check-cast v11, Lcom/myscript/nebo/sso/api/ISSOManager$OnLoginListener;
    invoke-interface {v6, v11}, Lcom/myscript/nebo/sso/api/ISSOManager;->addLoginListener(Lcom/myscript/nebo/sso/api/ISSOManager$OnLoginListener;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createGoogleSignInHelper()Lcom/myscript/nebo/sso/ui/login/GoogleSignInHelper;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setGoogleSignInHelper(Lcom/myscript/nebo/sso/ui/login/GoogleSignInHelper;)V
    new-instance v6, Lcom/myscript/nebo/utils/InstrumentedFeatureFlagProvider;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getUserDataProvider()Lcom/myscript/nebo/sso/model/UserDataProvider;
    move-result-object v11
    invoke-direct {v6, v11}, Lcom/myscript/nebo/utils/InstrumentedFeatureFlagProvider;-><init>(Lcom/myscript/nebo/sso/model/UserDataProvider;)V
    check-cast v6, Lcom/myscript/nebo/featureflags/provider/FeatureFlagProvider;
    invoke-static {v6}, Lcom/myscript/nebo/featureflags/RuntimeFeatureFlagHelper;->addProvider(Lcom/myscript/nebo/featureflags/provider/FeatureFlagProvider;)Z
    new-instance v6, Lcom/myscript/nebo/utils/EarlyAccessFeaturesProvider;
    invoke-direct {v6, v7}, Lcom/myscript/nebo/utils/EarlyAccessFeaturesProvider;-><init>(Landroid/app/Application;)V
    check-cast v6, Lcom/myscript/nebo/featureflags/provider/FeatureFlagProvider;
    invoke-static {v6}, Lcom/myscript/nebo/featureflags/RuntimeFeatureFlagHelper;->addProvider(Lcom/myscript/nebo/featureflags/provider/FeatureFlagProvider;)Z
    new-instance v6, Lcom/myscript/nebo/remoteconfig/RemoteConfigFeatureFlagProvider;
    iget-object v11, v1, Lcom/myscript/nebo/NeboApplication;->remoteConfig:Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfigController;
    const-string v12, "remoteConfig"
    if-nez v11, :cond_f0
    invoke-static {v12}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    move-object v11, v10
    :cond_f0
    check-cast v11, Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfig;
    invoke-direct {v6, v11}, Lcom/myscript/nebo/remoteconfig/RemoteConfigFeatureFlagProvider;-><init>(Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfig;)V
    check-cast v6, Lcom/myscript/nebo/featureflags/provider/FeatureFlagProvider;
    invoke-static {v6}, Lcom/myscript/nebo/featureflags/RuntimeFeatureFlagHelper;->addProvider(Lcom/myscript/nebo/featureflags/provider/FeatureFlagProvider;)Z
    sget v6, Lcom/myscript/nebo/R$string;->pref_internal_force_document_type_disabled_key:I
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v6
    invoke-static {v6, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    new-instance v13, Lcom/myscript/nebo/abtest/DocumentTypeAvailabilityRepository;
    iget-object v11, v1, Lcom/myscript/nebo/NeboApplication;->remoteConfig:Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfigController;
    if-nez v11, :cond_10d
    invoke-static {v12}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    move-object v11, v10
    :cond_10d
    move-object v14, v11
    check-cast v14, Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfig;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v15
    new-instance v11, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda0;
    invoke-direct {v11, v1, v6}, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda0;-><init>(Lcom/myscript/nebo/NeboApplication;Ljava/lang/String;)V
    const/16 v19, 0x10
    const/16 v20, 0x0
    const/16 v18, 0x0
    move-object/from16 v16, v6
    move-object/from16 v17, v11
    invoke-direct/range {v13 .. v20}, Lcom/myscript/nebo/abtest/DocumentTypeAvailabilityRepository;-><init>(Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfig;Landroid/content/SharedPreferences;Ljava/lang/String;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;ILkotlin/jvm/internal/DefaultConstructorMarker;)V
    iput-object v13, v1, Lcom/myscript/nebo/NeboApplication;->documentTypeAvailability:Lcom/myscript/nebo/abtest/DocumentTypeAvailabilityRepository;
    iget-object v6, v1, Lcom/myscript/nebo/NeboApplication;->remoteConfig:Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfigController;
    if-nez v6, :cond_130
    invoke-static {v12}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    move-object v6, v10
    :cond_130
    new-instance v11, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda3;
    invoke-direct {v11, v1}, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda3;-><init>(Lcom/myscript/nebo/NeboApplication;)V
    invoke-virtual {v6, v11}, Lcom/myscript/nebo/analytics/remoteconfig/RemoteConfigController;->instantiate(Lkotlin/jvm/functions/Function0;)V
    iget-object v6, v1, Lcom/myscript/nebo/NeboApplication;->applicationState:Lcom/myscript/nebo/whatsnew/ApplicationState;
    if-nez v6, :cond_140
    invoke-static {v9}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    move-object v6, v10
    :cond_140
    invoke-virtual {v6}, Lcom/myscript/nebo/whatsnew/ApplicationState;->getNativeCrashesToLog()Ljava/util/List;
    move-result-object v6
    check-cast v6, Ljava/lang/Iterable;
    invoke-interface {v6}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;
    move-result-object v6
    :goto_14a
    invoke-interface {v6}, Ljava/util/Iterator;->hasNext()Z
    move-result v11
    if-eqz v11, :cond_17e
    invoke-interface {v6}, Ljava/util/Iterator;->next()Ljava/lang/Object;
    move-result-object v11
    check-cast v11, Ljava/lang/Number;
    invoke-virtual {v11}, Ljava/lang/Number;->intValue()I
    move-result v11
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v12
    new-instance v13, Lcom/myscript/nebo/NativeCrashCaughtException;
    new-instance v14, Ljava/lang/StringBuilder;
    const-string v15, "A native crash happened in Nebo. Signal "
    invoke-direct {v14, v15}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v14, v11}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    move-result-object v11
    const-string v14, " was caught by signal handler."
    invoke-virtual {v11, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v11
    invoke-virtual {v11}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v11
    invoke-direct {v13, v11}, Lcom/myscript/nebo/NativeCrashCaughtException;-><init>(Ljava/lang/String;)V
    check-cast v13, Ljava/lang/Exception;
    invoke-interface {v12, v13}, Lcom/myscript/nebo/log/TechnicalLogger;->logError(Ljava/lang/Exception;)V
    goto :goto_14a
    :cond_17e
    iget-object v6, v1, Lcom/myscript/nebo/NeboApplication;->applicationState:Lcom/myscript/nebo/whatsnew/ApplicationState;
    if-nez v6, :cond_186
    invoke-static {v9}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    move-object v6, v10
    :cond_186
    invoke-virtual {v6}, Lcom/myscript/nebo/whatsnew/ApplicationState;->clearNativeCrashesToLog()V
    sget v6, Lcom/myscript/nebo/R$string;->pref_dark_mode_key:I
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v6
    invoke-static {v6, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    sget v11, Lcom/myscript/nebo/R$string;->pref_dark_mode_follow_system_value:I
    invoke-virtual {v1, v11}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v11
    invoke-static {v11, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    sget v12, Lcom/myscript/nebo/R$string;->pref_dark_mode_dark_value:I
    invoke-virtual {v1, v12}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v12
    invoke-static {v12, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v13
    invoke-interface {v13, v6, v11}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v6
    invoke-static {v6, v12}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z
    move-result v12
    const/4 v13, -0x1
    const/4 v14, 0x2
    const/4 v15, 0x1
    if-eqz v12, :cond_1b7
    move v6, v14
    goto :goto_1c0
    :cond_1b7
    invoke-static {v6, v11}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z
    move-result v6
    if-eqz v6, :cond_1bf
    move v6, v13
    goto :goto_1c0
    :cond_1bf
    move v6, v15
    :goto_1c0
    invoke-static {v6}, Landroidx/appcompat/app/AppCompatDelegate;->setDefaultNightMode(I)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createNetworkStateRepository()Lcom/myscript/nebo/common/network/NeboNetworkStateRepository;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setNetworkStateRepository(Lcom/myscript/nebo/common/network/NeboNetworkStateRepository;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v16
    const/16 v20, 0x4
    const/16 v21, 0x0
    const-string v17, "NeboApplication"
    const-string v18, "Initialize User Profile Manager"
    const/16 v19, 0x0
    invoke-static/range {v16 .. v21}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createPurchaseController()Lcom/myscript/nebo/freemium/PurchaseController;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setPurchaseController(Lcom/myscript/nebo/freemium/PurchaseController;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createAiClient()Lcom/myscript/nebo/ai/data/NeboAIClient;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setAiClient(Lcom/myscript/nebo/ai/data/NeboAIClient;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createLegacyPaidStorage()Lcom/myscript/nebo/freemium/storage/LegacyPaidStorage;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setLegacyPaidStorage(Lcom/myscript/nebo/freemium/storage/LegacyPaidStorage;)V
    new-instance v6, Lcom/myscript/nebo/freemium/storage/PurchaseSecuredStorage;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v11
    invoke-direct {v6, v7, v11}, Lcom/myscript/nebo/freemium/storage/PurchaseSecuredStorage;-><init>(Landroid/app/Application;Lcom/myscript/nebo/log/TechnicalLogger;)V
    check-cast v6, Lcom/myscript/nebo/freemium/storage/PurchaseStorage;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createNeboBackend()Lcom/myscript/nebo/account/backend/Backend;
    move-result-object v11
    invoke-virtual {v1, v11}, Lcom/myscript/nebo/NeboApplication;->setNeboBackend(Lcom/myscript/nebo/account/backend/Backend;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createMailServices()Lcom/myscript/nebo/account/backend/MailServices;
    move-result-object v11
    invoke-virtual {v1, v11}, Lcom/myscript/nebo/NeboApplication;->setMailServices(Lcom/myscript/nebo/account/backend/MailServices;)V
    sget-object v11, Lcom/myscript/nebo/migration/InstallReferralRepository;->Companion:Lcom/myscript/nebo/migration/InstallReferralRepository$Companion;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v12
    invoke-virtual {v11, v0, v12}, Lcom/myscript/nebo/migration/InstallReferralRepository$Companion;->create(Landroid/content/Context;Lcom/myscript/nebo/log/TechnicalLogger;)Lcom/myscript/nebo/migration/InstallReferralRepository;
    move-result-object v11
    check-cast v11, Lcom/myscript/nebo/migration/IInstallReferralRepository;
    iput-object v11, v1, Lcom/myscript/nebo/NeboApplication;->installReferralRepository:Lcom/myscript/nebo/migration/IInstallReferralRepository;
    sget-object v11, Landroidx/lifecycle/ProcessLifecycleOwner;->Companion:Landroidx/lifecycle/ProcessLifecycleOwner$Companion;
    invoke-virtual {v11}, Landroidx/lifecycle/ProcessLifecycleOwner$Companion;->get()Landroidx/lifecycle/LifecycleOwner;
    move-result-object v11
    invoke-static {v11}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;
    move-result-object v11
    move-object/from16 v16, v11
    check-cast v16, Lkotlinx/coroutines/CoroutineScope;
    invoke-static {}, Lkotlinx/coroutines/Dispatchers;->getIO()Lkotlinx/coroutines/CoroutineDispatcher;
    move-result-object v11
    move-object/from16 v17, v11
    check-cast v17, Lkotlin/coroutines/CoroutineContext;
    new-instance v11, Lcom/myscript/nebo/NeboApplication$onCreate$6;
    invoke-direct {v11, v1, v10}, Lcom/myscript/nebo/NeboApplication$onCreate$6;-><init>(Lcom/myscript/nebo/NeboApplication;Lkotlin/coroutines/Continuation;)V
    move-object/from16 v19, v11
    check-cast v19, Lkotlin/jvm/functions/Function2;
    const/16 v20, 0x2
    const/16 v18, 0x0
    invoke-static/range {v16 .. v21}, Lkotlinx/coroutines/BuildersKt;->launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;
    new-instance v11, Lcom/myscript/nebo/purchase/RevenueCatPurchaseProvider;
    invoke-direct {v11, v7}, Lcom/myscript/nebo/purchase/RevenueCatPurchaseProvider;-><init>(Landroid/app/Application;)V
    invoke-virtual {v1, v11}, Lcom/myscript/nebo/NeboApplication;->createSubscriptionRepository(Lcom/myscript/nebo/purchase/RevenueCatPurchaseProvider;)Lcom/myscript/nebo/purchase/ISubscriptionRepository;
    move-result-object v11
    invoke-virtual {v1, v11}, Lcom/myscript/nebo/NeboApplication;->setSubscriptionRepository(Lcom/myscript/nebo/purchase/ISubscriptionRepository;)V
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->createUserProfileManager(Lcom/myscript/nebo/freemium/storage/PurchaseStorage;)Lcom/myscript/nebo/account/UserProfileManager;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setUserProfileManager(Lcom/myscript/nebo/account/UserProfileManager;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPurchaseController()Lcom/myscript/nebo/freemium/PurchaseController;
    move-result-object v6
    invoke-interface {v6}, Lcom/myscript/nebo/freemium/PurchaseController;->connect()V
    sget-object v6, Landroidx/lifecycle/ProcessLifecycleOwner;->Companion:Landroidx/lifecycle/ProcessLifecycleOwner$Companion;
    invoke-virtual {v6}, Landroidx/lifecycle/ProcessLifecycleOwner$Companion;->get()Landroidx/lifecycle/LifecycleOwner;
    move-result-object v6
    invoke-static {v6}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;
    move-result-object v6
    move-object/from16 v16, v6
    check-cast v16, Lkotlinx/coroutines/CoroutineScope;
    new-instance v6, Lcom/myscript/nebo/NeboApplication$onCreate$7;
    invoke-direct {v6, v1, v10}, Lcom/myscript/nebo/NeboApplication$onCreate$7;-><init>(Lcom/myscript/nebo/NeboApplication;Lkotlin/coroutines/Continuation;)V
    move-object/from16 v19, v6
    check-cast v19, Lkotlin/jvm/functions/Function2;
    const/16 v20, 0x3
    const/16 v17, 0x0
    invoke-static/range {v16 .. v21}, Lkotlinx/coroutines/BuildersKt;->launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;
    sget-object v6, Landroidx/lifecycle/ProcessLifecycleOwner;->Companion:Landroidx/lifecycle/ProcessLifecycleOwner$Companion;
    invoke-virtual {v6}, Landroidx/lifecycle/ProcessLifecycleOwner$Companion;->get()Landroidx/lifecycle/LifecycleOwner;
    move-result-object v6
    invoke-static {v6}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;
    move-result-object v6
    move-object/from16 v16, v6
    check-cast v16, Lkotlinx/coroutines/CoroutineScope;
    new-instance v6, Lcom/myscript/nebo/NeboApplication$onCreate$8;
    invoke-direct {v6, v1, v10}, Lcom/myscript/nebo/NeboApplication$onCreate$8;-><init>(Lcom/myscript/nebo/NeboApplication;Lkotlin/coroutines/Continuation;)V
    move-object/from16 v19, v6
    check-cast v19, Lkotlin/jvm/functions/Function2;
    invoke-static/range {v16 .. v21}, Lkotlinx/coroutines/BuildersKt;->launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createResourceManager()Lcom/myscript/atk/resourcemanager/ResourceManager;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setResourceManager(Lcom/myscript/atk/resourcemanager/ResourceManager;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v16
    const/16 v20, 0x4
    const-string v17, "NeboApplication"
    const-string v18, "Initialize LibraryRepository"
    const/16 v19, 0x0
    invoke-static/range {v16 .. v21}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setLibraryRepository(Lcom/myscript/nebo/dms/core/LibraryRepository;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createAutoSyncTimerManager()Lcom/myscript/nebo/dms/core/IAutoSyncTimerManager;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setAutoSyncTimerManager(Lcom/myscript/nebo/dms/core/IAutoSyncTimerManager;)V
    new-instance v16, Lcom/myscript/nebo/abtest/DocumentPresenceObserver;
    iget-object v6, v1, Lcom/myscript/nebo/NeboApplication;->documentTypeAvailability:Lcom/myscript/nebo/abtest/DocumentTypeAvailabilityRepository;
    if-nez v6, :cond_2c1
    const-string v6, "documentTypeAvailability"
    invoke-static {v6}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    move-object/from16 v17, v10
    goto :goto_2c3
    :cond_2c1
    move-object/from16 v17, v6
    :goto_2c3
    new-instance v6, Lcom/myscript/nebo/abtest/LibraryDocumentPresenceSource;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;
    move-result-object v11
    invoke-direct {v6, v11}, Lcom/myscript/nebo/abtest/LibraryDocumentPresenceSource;-><init>(Lcom/myscript/nebo/dms/core/LibraryRepository;)V
    move-object/from16 v18, v6
    check-cast v18, Lcom/myscript/nebo/abtest/DocumentPresenceSource;
    sget-object v6, Landroidx/lifecycle/ProcessLifecycleOwner;->Companion:Landroidx/lifecycle/ProcessLifecycleOwner$Companion;
    invoke-virtual {v6}, Landroidx/lifecycle/ProcessLifecycleOwner$Companion;->get()Landroidx/lifecycle/LifecycleOwner;
    move-result-object v6
    invoke-static {v6}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;
    move-result-object v6
    move-object/from16 v19, v6
    check-cast v19, Lkotlinx/coroutines/CoroutineScope;
    const/16 v21, 0x8
    const/16 v22, 0x0
    const/16 v20, 0x0
    invoke-direct/range {v16 .. v22}, Lcom/myscript/nebo/abtest/DocumentPresenceObserver;-><init>(Lcom/myscript/nebo/abtest/DocumentTypeAvailabilityRepository;Lcom/myscript/nebo/abtest/DocumentPresenceSource;Lkotlinx/coroutines/CoroutineScope;Lkotlinx/coroutines/CoroutineDispatcher;ILkotlin/jvm/internal/DefaultConstructorMarker;)V
    move-object/from16 v6, v16
    invoke-virtual {v6}, Lcom/myscript/nebo/abtest/DocumentPresenceObserver;->start()V
    iput-object v6, v1, Lcom/myscript/nebo/NeboApplication;->documentPresenceObserver:Lcom/myscript/nebo/abtest/DocumentPresenceObserver;
    new-instance v6, Lcom/myscript/nebo/penpanel/laser/SharedPreferencesLaserPointerSettings;
    invoke-direct {v6, v0}, Lcom/myscript/nebo/penpanel/laser/SharedPreferencesLaserPointerSettings;-><init>(Landroid/content/Context;)V
    iput-object v6, v1, Lcom/myscript/nebo/NeboApplication;->laserPointerSettings:Lcom/myscript/nebo/penpanel/laser/ILaserPointerSettings;
    sget-object v6, Lcom/myscript/nebo/migration/LenovoMigrationController;->Companion:Lcom/myscript/nebo/migration/LenovoMigrationController$Companion;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;
    move-result-object v11
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getAutoSyncTimerManager()Lcom/myscript/nebo/dms/core/IAutoSyncTimerManager;
    move-result-object v12
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v10
    invoke-virtual {v6, v7, v11, v12, v10}, Lcom/myscript/nebo/migration/LenovoMigrationController$Companion;->create(Landroid/app/Application;Lcom/myscript/nebo/dms/core/LibraryRepository;Lcom/myscript/nebo/dms/core/IAutoSyncTimerManager;Lcom/myscript/nebo/log/TechnicalLogger;)Lcom/myscript/nebo/migration/LenovoMigrationController;
    move-result-object v6
    check-cast v6, Lcom/myscript/nebo/migration/ILenovoMigrationController;
    iput-object v6, v1, Lcom/myscript/nebo/NeboApplication;->lenovoMigrationController:Lcom/myscript/nebo/migration/ILenovoMigrationController;
    invoke-static {}, Ljava/lang/Thread;->getDefaultUncaughtExceptionHandler()Ljava/lang/Thread$UncaughtExceptionHandler;
    move-result-object v6
    new-instance v10, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda4;
    invoke-direct {v10, v1, v6}, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda4;-><init>(Lcom/myscript/nebo/NeboApplication;Ljava/lang/Thread$UncaughtExceptionHandler;)V
    invoke-static {v10}, Ljava/lang/Thread;->setDefaultUncaughtExceptionHandler(Ljava/lang/Thread$UncaughtExceptionHandler;)V
    const-class v6, Lcom/myscript/nebo/NeboApplication;
    invoke-static {v6}, Lcom/myscript/snt/core/NeboEngine;->setNativeCrashHandlerClass(Ljava/lang/Object;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createPreRatingFilter()Lcom/myscript/nebo/rating/PreRatingFilter;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setPreRatingFilter(Lcom/myscript/nebo/rating/PreRatingFilter;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createCloudManager()Lcom/myscript/nebo/cloudsync/CloudManager;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setCloudManager(Lcom/myscript/nebo/cloudsync/CloudManager;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getCloudManager()Lcom/myscript/nebo/cloudsync/CloudManager;
    move-result-object v6
    new-instance v10, Lcom/myscript/nebo/NeboApplication$onCreate$11;
    invoke-direct {v10, v1}, Lcom/myscript/nebo/NeboApplication$onCreate$11;-><init>(Lcom/myscript/nebo/NeboApplication;)V
    check-cast v10, Lcom/myscript/nebo/cloudsync/CloudManager$CloudSyncInitializedListener;
    invoke-interface {v6, v10}, Lcom/myscript/nebo/cloudsync/CloudManager;->addCloudSyncInitializedListener(Lcom/myscript/nebo/cloudsync/CloudManager$CloudSyncInitializedListener;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createSharePageController()Lcom/myscript/nebo/dms/sharepage/SharePageController;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setSharePageController(Lcom/myscript/nebo/dms/sharepage/SharePageController;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createHelpUsManager()Lcom/myscript/nebo/sso/HelpUsManager;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setHelpUsManager(Lcom/myscript/nebo/sso/HelpUsManager;)V
    sget-object v17, Lcom/myscript/shared/datacollect/DataCollect;->Companion:Lcom/myscript/shared/datacollect/DataCollect$Companion;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getHttpClient()Lokhttp3/OkHttpClient;
    move-result-object v18
    invoke-static {v7}, Lcom/myscript/nebo/MyScriptBackendConfiguration;->getNeboAppUrl(Landroid/app/Application;)Ljava/lang/String;
    move-result-object v19
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getCacheDir()Ljava/io/File;
    move-result-object v6
    const-string v10, "getCacheDir(...)"
    invoke-static {v6, v10}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    new-instance v22, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda5;
    invoke-direct/range {v22 .. v22}, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda5;-><init>()V
    new-instance v23, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda6;
    invoke-direct/range {v23 .. v23}, Lcom/myscript/nebo/NeboApplication$$ExternalSyntheticLambda6;-><init>()V
    const-string v21, "MyScript Notes"
    move-object/from16 v20, v6
    invoke-virtual/range {v17 .. v23}, Lcom/myscript/shared/datacollect/DataCollect$Companion;->create(Lokhttp3/OkHttpClient;Ljava/lang/String;Ljava/io/File;Ljava/lang/String;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function1;)Lcom/myscript/shared/datacollect/DataCollect;
    move-result-object v6
    iput-object v6, v1, Lcom/myscript/nebo/NeboApplication;->neboDataCollect:Lcom/myscript/shared/datacollect/DataCollect;
    invoke-direct {v1}, Lcom/myscript/nebo/NeboApplication;->enqueueDataCollectWork()V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPackageCodePath()Ljava/lang/String;
    move-result-object v6
    new-instance v10, Ljava/lang/StringBuilder;
    const-string/jumbo v11, "zip://"
    invoke-direct {v10, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v10, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v6
    const-string v10, "!/assets/resources/mul-sk-nebo_disabled.res"
    invoke-virtual {v6, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v6
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v6
    iget-object v10, v1, Lcom/myscript/nebo/NeboApplication;->_customResources:Ljava/util/List;
    new-instance v11, Lcom/myscript/atk/core/CustomResource;
    sget-object v12, Lcom/myscript/atk/core/CustomResourceType;->STANDARD:Lcom/myscript/atk/core/CustomResourceType;
    invoke-direct {v11, v6, v12}, Lcom/myscript/atk/core/CustomResource;-><init>(Ljava/lang/String;Lcom/myscript/atk/core/CustomResourceType;)V
    invoke-interface {v10, v11}, Ljava/util/List;->add(Ljava/lang/Object;)Z
    new-instance v6, Ljava/io/File;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getFilesDir()Ljava/io/File;
    move-result-object v10
    const-string v11, "user_dict-lk.res"
    invoke-direct {v6, v10, v11}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
    new-instance v10, Lcom/myscript/atk/text/UserDictionaryConfiguration;
    invoke-virtual {v6}, Ljava/io/File;->getParentFile()Ljava/io/File;
    move-result-object v11
    invoke-static {v11}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V
    invoke-virtual {v11}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;
    move-result-object v11
    invoke-virtual {v6}, Ljava/io/File;->getName()Ljava/lang/String;
    move-result-object v12
    invoke-direct {v10, v11, v12}, Lcom/myscript/atk/text/UserDictionaryConfiguration;-><init>(Ljava/lang/String;Ljava/lang/String;)V
    new-instance v11, Lcom/myscript/atk/text/UserDictionary;
    invoke-direct {v11, v10}, Lcom/myscript/atk/text/UserDictionary;-><init>(Lcom/myscript/atk/text/UserDictionaryConfiguration;)V
    iput-object v11, v1, Lcom/myscript/nebo/NeboApplication;->userDictionary:Lcom/myscript/atk/text/UserDictionary;
    iget-object v10, v1, Lcom/myscript/nebo/NeboApplication;->_customResources:Ljava/util/List;
    new-instance v11, Lcom/myscript/atk/core/CustomResource;
    invoke-virtual {v6}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;
    move-result-object v6
    sget-object v12, Lcom/myscript/atk/core/CustomResourceType;->MUTABLE:Lcom/myscript/atk/core/CustomResourceType;
    invoke-direct {v11, v6, v12}, Lcom/myscript/atk/core/CustomResource;-><init>(Ljava/lang/String;Lcom/myscript/atk/core/CustomResourceType;)V
    invoke-interface {v10, v11}, Ljava/util/List;->add(Ljava/lang/Object;)Z
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->createColorPolicyRepository()Lcom/myscript/nebo/common/ColorPolicyRepository;
    move-result-object v6
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->setColorPolicyRepository(Lcom/myscript/nebo/common/ColorPolicyRepository;)V
    sget-object v6, Lcom/myscript/nebo/penpanel/InkSettings;->Companion:Lcom/myscript/nebo/penpanel/InkSettings$Companion;
    sget v10, Lcom/myscript/nebo/R$array;->default_predefined_ink_colors:I
    sget v11, Lcom/myscript/nebo/penpanel/R$color;->default_pen_color:I
    sget v12, Lcom/myscript/nebo/penpanel/R$color;->default_highlighter_color:I
    invoke-virtual {v6, v7, v10, v11, v12}, Lcom/myscript/nebo/penpanel/InkSettings$Companion;->bootstrapIfNeeded(Landroid/app/Application;III)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v17
    const/16 v21, 0x4
    const/16 v22, 0x0
    const-string v18, "NeboApplication"
    const-string v19, "Initialize custom fonts"
    const/16 v20, 0x0
    invoke-static/range {v17 .. v22}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    invoke-direct {v1}, Lcom/myscript/nebo/NeboApplication;->deleteOldFonts()V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v6
    const-string v10, "FONT_VERSION_KEY"
    invoke-interface {v6, v10, v13}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v6
    const/4 v11, 0x0
    if-eq v6, v15, :cond_3fa
    move v6, v15
    goto :goto_3fb
    :cond_3fa
    move v6, v11
    :goto_3fb
    const-string v12, "fonts"
    invoke-static {v0, v12, v6}, Lcom/myscript/atk/core/ui/utils/TypefaceUtils;->loadFonts(Landroid/content/Context;Ljava/lang/String;Z)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v0
    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;
    move-result-object v0
    invoke-interface {v0, v10, v15}, Landroid/content/SharedPreferences$Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences$Editor;
    invoke-interface {v0}, Landroid/content/SharedPreferences$Editor;->apply()V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v17
    const/16 v21, 0x4
    const/16 v22, 0x0
    const-string v18, "NeboApplication"
    const-string v19, "Initialize PDFNet library"
    const/16 v20, 0x0
    invoke-static/range {v17 .. v22}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    :try_start_41f
    new-instance v0, Ljava/io/File;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getFilesDir()Ljava/io/File;
    move-result-object v6
    const-string v10, "pdftron_layout_resources.plugin"
    invoke-direct {v0, v6, v10}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
    :try_end_42a
    .catch Lcom/pdftron/common/PDFNetException; {:try_start_41f .. :try_end_42a} :catch_481
    :try_start_42a
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getResources()Landroid/content/res/Resources;
    move-result-object v6
    sget v10, Lcom/pdftron/pdfnet/R$raw;->pdftron_layout_resources:I
    invoke-virtual {v6, v10}, Landroid/content/res/Resources;->openRawResource(I)Ljava/io/InputStream;
    move-result-object v6
    check-cast v6, Ljava/io/Closeable;
    :try_end_436
    .catch Ljava/io/IOException; {:try_start_42a .. :try_end_436} :catch_463
    .catch Lcom/pdftron/common/PDFNetException; {:try_start_42a .. :try_end_436} :catch_481
    :try_start_436
    move-object v10, v6
    check-cast v10, Ljava/io/InputStream;
    new-instance v12, Ljava/io/FileOutputStream;
    invoke-direct {v12, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    check-cast v12, Ljava/io/Closeable;
    :try_end_440
    .catchall {:try_start_436 .. :try_end_440} :catchall_45b
    :try_start_440
    move-object v0, v12
    check-cast v0, Ljava/io/FileOutputStream;
    invoke-static {v10}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V
    check-cast v0, Ljava/io/OutputStream;
    const/4 v13, 0x0
    invoke-static {v10, v0, v11, v14, v13}, Lkotlin/io/ByteStreamsKt;->copyTo$default(Ljava/io/InputStream;Ljava/io/OutputStream;IILjava/lang/Object;)J
    :try_end_44c
    .catchall {:try_start_440 .. :try_end_44c} :catchall_453
    :try_start_44c
    invoke-static {v12, v13}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V
    :try_end_44f
    .catchall {:try_start_44c .. :try_end_44f} :catchall_45b
    :try_start_44f
    invoke-static {v6, v13}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V
    :try_end_452
    .catch Ljava/io/IOException; {:try_start_44f .. :try_end_452} :catch_463
    .catch Lcom/pdftron/common/PDFNetException; {:try_start_44f .. :try_end_452} :catch_481
    goto :goto_46b
    :catchall_453
    move-exception v0
    move-object v10, v0
    :try_start_455
    throw v10
    :try_end_456
    .catchall {:try_start_455 .. :try_end_456} :catchall_456
    :catchall_456
    move-exception v0
    :try_start_457
    invoke-static {v12, v10}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V
    throw v0
    :try_end_45b
    .catchall {:try_start_457 .. :try_end_45b} :catchall_45b
    :catchall_45b
    move-exception v0
    move-object v10, v0
    :try_start_45d
    throw v10
    :try_end_45e
    .catchall {:try_start_45d .. :try_end_45e} :catchall_45e
    :catchall_45e
    move-exception v0
    :try_start_45f
    invoke-static {v6, v10}, Lkotlin/io/CloseableKt;->closeFinally(Ljava/io/Closeable;Ljava/lang/Throwable;)V
    throw v0
    :try_end_463
    .catch Ljava/io/IOException; {:try_start_45f .. :try_end_463} :catch_463
    .catch Lcom/pdftron/common/PDFNetException; {:try_start_45f .. :try_end_463} :catch_481
    :catch_463
    move-exception v0
    :try_start_464
    const-string v6, "Error during PDFNet initialization: could not copy font resource file"
    check-cast v0, Ljava/lang/Throwable;
    invoke-static {v4, v6, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    :goto_46b
    move-object v0, v1
    check-cast v0, Landroid/content/Context;
    sget v6, Lcom/pdftron/pdfnet/R$raw;->pdfnet:I
    const-string v10, "MyScript SAS:OEM:Nebo::IARP+:AMS(20280930):D4B6AF13B6DCFA6D3D01C6616F6F2DFAD3F13F24E6E64862308656CCBEF5C7"
    invoke-static {v0, v6, v10}, Lcom/pdftron/pdf/PDFNet;->initialize(Landroid/content/Context;ILjava/lang/String;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getFilesDir()Ljava/io/File;
    move-result-object v0
    invoke-virtual {v0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;
    move-result-object v0
    invoke-static {v0}, Lcom/pdftron/pdf/PDFNet;->addResourceSearchPath(Ljava/lang/String;)V
    :try_end_480
    .catch Lcom/pdftron/common/PDFNetException; {:try_start_464 .. :try_end_480} :catch_481
    goto :goto_489
    :catch_481
    move-exception v0
    const-string v6, "Error during PDFNet initialization"
    check-cast v0, Ljava/lang/Throwable;
    invoke-static {v4, v6, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    :goto_489
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getResources()Landroid/content/res/Resources;
    move-result-object v0
    invoke-virtual {v0}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;
    move-result-object v0
    invoke-virtual {v0}, Landroid/content/res/Configuration;->getLocales()Landroid/os/LocaleList;
    move-result-object v0
    const-string v4, "getLocales(...)"
    invoke-static {v0, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {v0}, Landroid/os/LocaleList;->size()I
    move-result v4
    move v6, v11
    :goto_49f
    if-ge v6, v4, :cond_4ce
    invoke-virtual {v0, v6}, Landroid/os/LocaleList;->get(I)Ljava/util/Locale;
    move-result-object v10
    iget-object v12, v1, Lcom/myscript/nebo/NeboApplication;->suggestedLanguageKeys:Ljava/util/Set;
    check-cast v12, Ljava/util/Collection;
    invoke-virtual {v10}, Ljava/util/Locale;->getLanguage()Ljava/lang/String;
    move-result-object v13
    invoke-virtual {v10}, Ljava/util/Locale;->getCountry()Ljava/lang/String;
    move-result-object v10
    new-instance v14, Ljava/lang/StringBuilder;
    invoke-direct {v14}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {v14, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v13
    const-string v14, "_"
    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v13
    invoke-virtual {v13, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v10
    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v10
    invoke-interface {v12, v10}, Ljava/util/Collection;->add(Ljava/lang/Object;)Z
    add-int/lit8 v6, v6, 0x1
    goto :goto_49f
    :cond_4ce
    iget-object v0, v1, Lcom/myscript/nebo/NeboApplication;->suggestedLanguageKeys:Ljava/util/Set;
    check-cast v0, Ljava/util/Collection;
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getResources()Landroid/content/res/Resources;
    move-result-object v4
    sget v6, Lcom/myscript/nebo/R$array;->suggested_language_keys:I
    invoke-virtual {v4, v6}, Landroid/content/res/Resources;->getStringArray(I)[Ljava/lang/String;
    move-result-object v4
    const-string v6, "getStringArray(...)"
    invoke-static {v4, v6}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v4, [Ljava/lang/Object;
    invoke-static {v4}, Lkotlin/collections/ArraysKt;->toList([Ljava/lang/Object;)Ljava/util/List;
    move-result-object v4
    check-cast v4, Ljava/lang/Iterable;
    invoke-static {v0, v4}, Lkotlin/collections/CollectionsKt;->addAll(Ljava/util/Collection;Ljava/lang/Iterable;)Z
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v17
    const/16 v21, 0x4
    const/16 v22, 0x0
    const-string v18, "NeboApplication"
    const-string v19, "System Application.onCreate()"
    const/16 v20, 0x0
    invoke-static/range {v17 .. v22}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    invoke-super {v1}, Landroid/app/Application;->onCreate()V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getUserDataProvider()Lcom/myscript/nebo/sso/model/UserDataProvider;
    move-result-object v0
    invoke-virtual {v0}, Lcom/myscript/nebo/sso/model/UserDataProvider;->hasUser()Z
    move-result v0
    if-eqz v0, :cond_51e
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v17
    const/16 v21, 0x4
    const/16 v22, 0x0
    const-string v18, "NeboApplication"
    const-string v19, "Check SSO Account for invalidated refresh token"
    const/16 v20, 0x0
    invoke-static/range {v17 .. v22}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    invoke-direct {v1}, Lcom/myscript/nebo/NeboApplication;->checkSSOAccount()V
    :cond_51e
    sget-object v0, Lcom/myscript/nebo/common/MainThreadBus;->eventBus:Lcom/squareup/otto/Bus;
    invoke-virtual {v0, v1}, Lcom/squareup/otto/Bus;->register(Ljava/lang/Object;)V
    sget v0, Lcom/myscript/nebo/R$string;->pref_interactivity_strike_through_key:I
    invoke-virtual {v1, v0}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v0
    invoke-static {v0, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v4
    invoke-interface {v4, v0, v11}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
    move-result v4
    if-eqz v4, :cond_573
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v17
    const/16 v21, 0x4
    const/16 v22, 0x0
    const-string v18, "NeboApplication"
    const-string v19, "Migrate legacy strike-through setting"
    const/16 v20, 0x0
    invoke-static/range {v17 .. v22}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v4
    invoke-interface {v4}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;
    move-result-object v4
    sget v6, Lcom/myscript/nebo/R$string;->pref_interactivity_strike_through_document_key:I
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v6
    sget v10, Lcom/myscript/nebo/R$string;->pref_interactivity_erase_value:I
    invoke-virtual {v1, v10}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v10
    invoke-interface {v4, v6, v10}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;
    sget v6, Lcom/myscript/nebo/R$string;->pref_interactivity_strike_through_note_key:I
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v6
    sget v10, Lcom/myscript/nebo/R$string;->pref_interactivity_erase_value:I
    invoke-virtual {v1, v10}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v10
    invoke-interface {v4, v6, v10}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;
    invoke-interface {v4, v0}, Landroid/content/SharedPreferences$Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;
    invoke-interface {v4}, Landroid/content/SharedPreferences$Editor;->apply()V
    :cond_573
    sget v0, Lcom/myscript/nebo/R$string;->pref_interactivity_underline_words_key:I
    invoke-virtual {v1, v0}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v0
    invoke-static {v0, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v4
    invoke-interface {v4, v0, v11}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
    move-result v4
    if-eqz v4, :cond_5c3
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v17
    const/16 v21, 0x4
    const/16 v22, 0x0
    const-string v18, "NeboApplication"
    const-string v19, "Migrate legacy underline setting"
    const/16 v20, 0x0
    invoke-static/range {v17 .. v22}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v4
    invoke-interface {v4}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;
    move-result-object v4
    sget v6, Lcom/myscript/nebo/R$string;->pref_interactivity_underline_words_document_key:I
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v6
    sget v8, Lcom/myscript/nebo/R$string;->pref_interactivity_bold_value:I
    invoke-virtual {v1, v8}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v8
    invoke-interface {v4, v6, v8}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;
    sget v6, Lcom/myscript/nebo/R$string;->pref_interactivity_underline_words_note_key:I
    invoke-virtual {v1, v6}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v6
    sget v8, Lcom/myscript/nebo/R$string;->pref_interactivity_bold_value:I
    invoke-virtual {v1, v8}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v8
    invoke-interface {v4, v6, v8}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;
    invoke-interface {v4, v0}, Landroid/content/SharedPreferences$Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;
    invoke-interface {v4}, Landroid/content/SharedPreferences$Editor;->apply()V
    :cond_5c3
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v17
    const/16 v21, 0x4
    const/16 v22, 0x0
    const-string v18, "NeboApplication"
    const-string v19, "Initialization done"
    const/16 v20, 0x0
    invoke-static/range {v17 .. v22}, Lcom/myscript/nebo/log/TechnicalLoggerExt;->logAction$default(Lcom/myscript/nebo/log/TechnicalLogger;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)V
    sput-boolean v15, Lcom/myscript/nebo/NeboApplication;->successfulInit:Z
    sget-object v0, Landroidx/lifecycle/ProcessLifecycleOwner;->Companion:Landroidx/lifecycle/ProcessLifecycleOwner$Companion;
    invoke-virtual {v0}, Landroidx/lifecycle/ProcessLifecycleOwner$Companion;->get()Landroidx/lifecycle/LifecycleOwner;
    move-result-object v0
    invoke-interface {v0}, Landroidx/lifecycle/LifecycleOwner;->getLifecycle()Landroidx/lifecycle/Lifecycle;
    move-result-object v0
    move-object v4, v1
    check-cast v4, Landroidx/lifecycle/LifecycleObserver;
    invoke-virtual {v0, v4}, Landroidx/lifecycle/Lifecycle;->addObserver(Landroidx/lifecycle/LifecycleObserver;)V
    :try_start_5e6
    sget-object v0, Lkotlin/Result;->Companion:Lkotlin/Result$Companion;
    move-object v0, v1
    check-cast v0, Lcom/myscript/nebo/NeboApplication;
    move-object v0, v1
    check-cast v0, Landroid/content/Context;
    invoke-static {v0}, Lcom/myscript/android/utils/SignatureUtilsKt;->getApkSigners(Landroid/content/Context;)[Landroid/content/pm/Signature;
    move-result-object v0
    if-eqz v0, :cond_5fb
    invoke-static {v0}, Lkotlin/collections/ArraysKt;->firstOrNull([Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v4
    check-cast v4, Landroid/content/pm/Signature;
    goto :goto_5fc
    :cond_5fb
    const/4 v4, 0x0
    :goto_5fc
    if-nez v4, :cond_601
    const-string v4, "null"
    goto :goto_64f
    :cond_601
    invoke-virtual {v4}, Landroid/content/pm/Signature;->toCharsString()Ljava/lang/String;
    move-result-object v6
    const-string v8, "toCharsString(...)"
    invoke-static {v6, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v6, Ljava/lang/CharSequence;
    invoke-static {v6}, Lkotlin/text/StringsKt;->isBlank(Ljava/lang/CharSequence;)Z
    move-result v6
    if-eqz v6, :cond_615
    const-string v4, "blank"
    goto :goto_64f
    :cond_615
    const-string v6, "SHA1"
    invoke-static {v6}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;
    move-result-object v6
    invoke-virtual {v4}, Landroid/content/pm/Signature;->toByteArray()[B
    move-result-object v4
    invoke-virtual {v6, v4}, Ljava/security/MessageDigest;->digest([B)[B
    move-result-object v4
    invoke-static {}, Lcom/google/common/io/BaseEncoding;->base16()Lcom/google/common/io/BaseEncoding;
    move-result-object v6
    invoke-virtual {v6}, Lcom/google/common/io/BaseEncoding;->lowerCase()Lcom/google/common/io/BaseEncoding;
    move-result-object v6
    invoke-virtual {v6, v4}, Lcom/google/common/io/BaseEncoding;->encode([B)Ljava/lang/String;
    move-result-object v4
    new-instance v6, Lkotlin/ranges/IntRange;
    invoke-static {v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V
    move-object v8, v4
    check-cast v8, Ljava/lang/CharSequence;
    invoke-static {v8}, Lkotlin/text/StringsKt;->getLastIndex(Ljava/lang/CharSequence;)I
    move-result v8
    invoke-direct {v6, v11, v8}, Lkotlin/ranges/IntRange;-><init>(II)V
    check-cast v6, Lkotlin/ranges/ClosedRange;
    const/16 v8, 0xf
    invoke-static {v8, v6}, Lkotlin/ranges/RangesKt;->coerceIn(ILkotlin/ranges/ClosedRange;)I
    move-result v6
    new-instance v8, Lkotlin/ranges/IntRange;
    invoke-direct {v8, v11, v6}, Lkotlin/ranges/IntRange;-><init>(II)V
    invoke-static {v4, v8}, Lkotlin/text/StringsKt;->substring(Ljava/lang/String;Lkotlin/ranges/IntRange;)Ljava/lang/String;
    move-result-object v4
    :goto_64f
    new-instance v6, Landroid/os/Bundle;
    invoke-direct {v6}, Landroid/os/Bundle;-><init>()V
    if-eqz v0, :cond_658
    array-length v0, v0
    goto :goto_659
    :cond_658
    move v0, v11
    :goto_659
    invoke-static {v0}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
    move-result-object v0
    invoke-virtual {v6, v3, v0}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V
    invoke-virtual {v6, v2, v4}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V
    invoke-static {v5, v6}, Lcom/myscript/nebo/analytics/AnalyticsController;->logAnalytics(Ljava/lang/String;Landroid/os/Bundle;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v0
    invoke-interface {v0, v5, v4}, Lcom/myscript/nebo/log/TechnicalLogger;->setKeyValue(Ljava/lang/String;Ljava/lang/String;)V
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    invoke-static {v0}, Lkotlin/Result;->constructor-impl(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v0
    :try_end_673
    .catchall {:try_start_5e6 .. :try_end_673} :catchall_674
    goto :goto_67f
    :catchall_674
    move-exception v0
    sget-object v4, Lkotlin/Result;->Companion:Lkotlin/Result$Companion;
    invoke-static {v0}, Lkotlin/ResultKt;->createFailure(Ljava/lang/Throwable;)Ljava/lang/Object;
    move-result-object v0
    invoke-static {v0}, Lkotlin/Result;->constructor-impl(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v0
    :goto_67f
    invoke-static {v0}, Lkotlin/Result;->exceptionOrNull-impl(Ljava/lang/Object;)Ljava/lang/Throwable;
    move-result-object v0
    if-eqz v0, :cond_6ae
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v4
    new-instance v6, Ljava/lang/RuntimeException;
    const-string v8, "Could not get signature"
    invoke-direct {v6, v8, v0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;Ljava/lang/Throwable;)V
    check-cast v6, Ljava/lang/Exception;
    invoke-interface {v4, v6}, Lcom/myscript/nebo/log/TechnicalLogger;->logError(Ljava/lang/Exception;)V
    new-instance v0, Landroid/os/Bundle;
    invoke-direct {v0}, Landroid/os/Bundle;-><init>()V
    const-string v4, "0"
    invoke-virtual {v0, v3, v4}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V
    const-string v3, "error"
    invoke-virtual {v0, v2, v3}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V
    invoke-static {v5, v0}, Lcom/myscript/nebo/analytics/AnalyticsController;->logAnalytics(Ljava/lang/String;Landroid/os/Bundle;)V
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v0
    invoke-interface {v0, v5, v3}, Lcom/myscript/nebo/log/TechnicalLogger;->setKeyValue(Ljava/lang/String;Ljava/lang/String;)V
    :cond_6ae
    invoke-direct {v1}, Lcom/myscript/nebo/NeboApplication;->getDiscoverabilitySessionManager()Lcom/myscript/nebo/discoverability/session/SessionManager;
    move-result-object v0
    invoke-virtual {v0, v7}, Lcom/myscript/nebo/discoverability/session/SessionManager;->registerSessionListener(Landroid/app/Application;)V
    sget-object v0, Landroidx/lifecycle/ProcessLifecycleOwner;->Companion:Landroidx/lifecycle/ProcessLifecycleOwner$Companion;
    invoke-virtual {v0}, Landroidx/lifecycle/ProcessLifecycleOwner$Companion;->get()Landroidx/lifecycle/LifecycleOwner;
    move-result-object v0
    invoke-static {v0}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;
    move-result-object v0
    move-object v2, v0
    check-cast v2, Lkotlinx/coroutines/CoroutineScope;
    new-instance v0, Lcom/myscript/nebo/NeboApplication$onCreate$21;
    const/4 v13, 0x0
    invoke-direct {v0, v1, v13}, Lcom/myscript/nebo/NeboApplication$onCreate$21;-><init>(Lcom/myscript/nebo/NeboApplication;Lkotlin/coroutines/Continuation;)V
    move-object v5, v0
    check-cast v5, Lkotlin/jvm/functions/Function2;
    const/4 v6, 0x3
    const/4 v7, 0x0
    const/4 v3, 0x0
    const/4 v4, 0x0
    invoke-static/range {v2 .. v7}, Lkotlinx/coroutines/BuildersKt;->launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;
    iget-object v13, v1, Lcom/myscript/nebo/NeboApplication;->applicationState:Lcom/myscript/nebo/whatsnew/ApplicationState;
    if-nez v13, :cond_6da
    invoke-static {v9}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    const/4 v13, 0x0
    :cond_6da
    invoke-virtual {v13}, Lcom/myscript/nebo/whatsnew/ApplicationState;->isFirstInstall()Z
    move-result v0
    if-eqz v0, :cond_70d
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v0
    sget v2, Lcom/myscript/nebo/R$string;->pref_cloudsync_dropbox_key:I
    invoke-virtual {v1, v2}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v2
    invoke-interface {v0, v2, v11}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
    move-result v0
    if-nez v0, :cond_70d
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->getPreferences()Landroid/content/SharedPreferences;
    move-result-object v0
    sget v2, Lcom/myscript/nebo/R$string;->pref_cloudsync_gdrive_key:I
    invoke-virtual {v1, v2}, Lcom/myscript/nebo/NeboApplication;->getString(I)Ljava/lang/String;
    move-result-object v2
    invoke-interface {v0, v2, v11}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
    move-result v0
    if-nez v0, :cond_70d
    new-instance v0, Lcom/myscript/nebo/analytics/firebase/NeboFirebaseProperty$CloudSyncProviderProperty;
    const-string v2, "not_syncing"
    invoke-direct {v0, v2}, Lcom/myscript/nebo/analytics/firebase/NeboFirebaseProperty$CloudSyncProviderProperty;-><init>(Ljava/lang/String;)V
    check-cast v0, Lcom/myscript/nebo/analytics/AnalyticsProperty;
    const/4 v13, 0x0
    invoke-static {v0, v13, v15, v13}, Lcom/myscript/nebo/analytics/AnalyticsProperty;->log$default(Lcom/myscript/nebo/analytics/AnalyticsProperty;Lkotlin/jvm/functions/Function1;ILjava/lang/Object;)V
    :cond_70d
    invoke-virtual {v1}, Lcom/myscript/nebo/NeboApplication;->onPostCreate()V
    return-void
    :catch_711
    move-exception v0
    invoke-virtual {v0}, Lcom/myscript/atk/core/EngineError;->getMessage()Ljava/lang/String;
    move-result-object v1
    move-object v2, v0
    check-cast v2, Ljava/lang/Throwable;
    invoke-static {v4, v1, v2}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    throw v0
.end method
