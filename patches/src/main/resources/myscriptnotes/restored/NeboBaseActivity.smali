.class public Lcom/myscript/nebo/NeboBaseActivity;
.super Landroidx/appcompat/app/AppCompatActivity;

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 7
    invoke-super {p0, p1}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V
    iget-object v0, p0, Lcom/myscript/nebo/NeboBaseActivity;->appUpdateHandler:Lcom/myscript/nebo/AppUpdateHandler;
    move-object v1, p0
    check-cast v1, Landroidx/appcompat/app/AppCompatActivity;
    invoke-virtual {v0, v1}, Lcom/myscript/nebo/AppUpdateHandler;->registerActivity(Landroidx/appcompat/app/AppCompatActivity;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getSupportFragmentManager()Landroidx/fragment/app/FragmentManager;
    move-result-object v0
    const-string v2, "getSupportFragmentManager(...)"
    invoke-static {v0, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {p0, v0}, Lcom/myscript/nebo/NeboBaseActivity;->setFragmentManager(Landroidx/fragment/app/FragmentManager;)V
    if-eqz p1, :cond_27
    sget v0, Lcom/myscript/nebo/R$string;->first_intent:I
    invoke-virtual {p0, v0}, Lcom/myscript/nebo/NeboBaseActivity;->getString(I)Ljava/lang/String;
    move-result-object v0
    invoke-virtual {p1, v0}, Landroid/os/Bundle;->getParcelable(Ljava/lang/String;)Landroid/os/Parcelable;
    move-result-object p1
    check-cast p1, Landroid/content/Intent;
    iput-object p1, p0, Lcom/myscript/nebo/NeboBaseActivity;->firstLaunchingIntent:Landroid/content/Intent;
    :cond_27
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getApplication()Landroid/app/Application;
    move-result-object p1
    const-string v0, "null cannot be cast to non-null type com.myscript.nebo.dms.core.LibraryRepository.Provider"
    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast p1, Lcom/myscript/nebo/dms/core/LibraryRepository$Provider;
    invoke-interface {p1}, Lcom/myscript/nebo/dms/core/LibraryRepository$Provider;->provideLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;
    move-result-object p1
    invoke-virtual {p0, p1}, Lcom/myscript/nebo/NeboBaseActivity;->setLibraryRepository(Lcom/myscript/nebo/dms/core/LibraryRepository;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getApplication()Landroid/app/Application;
    move-result-object p1
    const-string v0, "null cannot be cast to non-null type com.myscript.nebo.dms.core.IAutoSyncTimerManager.Provider"
    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast p1, Lcom/myscript/nebo/dms/core/IAutoSyncTimerManager$Provider;
    invoke-interface {p1}, Lcom/myscript/nebo/dms/core/IAutoSyncTimerManager$Provider;->provideAutoSyncTimerManager()Lcom/myscript/nebo/dms/core/IAutoSyncTimerManager;
    move-result-object p1
    invoke-virtual {p0, p1}, Lcom/myscript/nebo/NeboBaseActivity;->setAutoSyncTimerManager(Lcom/myscript/nebo/dms/core/IAutoSyncTimerManager;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getApplication()Landroid/app/Application;
    move-result-object p1
    const-string v0, "null cannot be cast to non-null type com.myscript.nebo.common.IMyScriptEngineContextProvider"
    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast p1, Lcom/myscript/nebo/common/IMyScriptEngineContextProvider;
    invoke-interface {p1}, Lcom/myscript/nebo/common/IMyScriptEngineContextProvider;->getEngineContext()Lcom/myscript/atk/core/Context;
    move-result-object p1
    if-nez p1, :cond_7f
    new-instance v0, Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;
    move-object v2, p0
    check-cast v2, Landroid/content/Context;
    invoke-direct {v0, v2}, Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;-><init>(Landroid/content/Context;)V
    sget v2, Lcom/myscript/nebo/R$string;->certificate_dialog_title:I
    invoke-virtual {v0, v2}, Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;->setTitle(I)Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;
    move-result-object v0
    sget v2, Lcom/myscript/nebo/R$string;->certificate_dialog_label:I
    invoke-virtual {v0, v2}, Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;->setMessage(I)Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;
    move-result-object v0
    const/4 v2, 0x0
    invoke-virtual {v0, v2}, Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;->setCancelable(Z)Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;
    move-result-object v0
    sget v2, Lcom/myscript/nebo/common/R$string;->dialog_ok:I
    const/4 v3, 0x0
    invoke-virtual {v0, v2, v3}, Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;->setPositiveButton(ILandroid/content/DialogInterface$OnClickListener;)Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;
    move-result-object v0
    invoke-virtual {v0}, Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;->show()Landroidx/appcompat/app/AlertDialog;
    :cond_7f
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getApplication()Landroid/app/Application;
    move-result-object v0
    const-string v2, "null cannot be cast to non-null type com.myscript.nebo.cloudsync.CloudManager.Provider"
    invoke-static {v0, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v0, Lcom/myscript/nebo/cloudsync/CloudManager$Provider;
    invoke-interface {v0}, Lcom/myscript/nebo/cloudsync/CloudManager$Provider;->provideCloudManager()Lcom/myscript/nebo/cloudsync/CloudManager;
    move-result-object v0
    invoke-virtual {p0, v0}, Lcom/myscript/nebo/NeboBaseActivity;->setCloudManager(Lcom/myscript/nebo/cloudsync/CloudManager;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getApplication()Landroid/app/Application;
    move-result-object v0
    const-string v2, "null cannot be cast to non-null type com.myscript.nebo.sso.model.UserDataProvider.Provider"
    invoke-static {v0, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v0, Lcom/myscript/nebo/sso/model/UserDataProvider$Provider;
    invoke-interface {v0}, Lcom/myscript/nebo/sso/model/UserDataProvider$Provider;->provideUserData()Lcom/myscript/nebo/sso/model/UserDataProvider;
    move-result-object v0
    invoke-virtual {p0, v0}, Lcom/myscript/nebo/NeboBaseActivity;->setUserData(Lcom/myscript/nebo/sso/model/UserDataProvider;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getApplication()Landroid/app/Application;
    move-result-object v0
    const-string v2, "null cannot be cast to non-null type com.myscript.nebo.sso.api.ISSOManager.Provider"
    invoke-static {v0, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v0, Lcom/myscript/nebo/sso/api/ISSOManager$Provider;
    invoke-interface {v0}, Lcom/myscript/nebo/sso/api/ISSOManager$Provider;->provideSSOManager()Lcom/myscript/nebo/sso/api/ISSOManager;
    move-result-object v0
    invoke-virtual {p0, v0}, Lcom/myscript/nebo/NeboBaseActivity;->setSsoManager(Lcom/myscript/nebo/sso/api/ISSOManager;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getSsoManager()Lcom/myscript/nebo/sso/api/ISSOManager;
    move-result-object v0
    iget-object v2, p0, Lcom/myscript/nebo/NeboBaseActivity;->loginListener:Lcom/myscript/nebo/NeboBaseActivity$loginListener$1;
    check-cast v2, Lcom/myscript/nebo/sso/api/ISSOManager$OnLoginListener;
    invoke-interface {v0, v2}, Lcom/myscript/nebo/sso/api/ISSOManager;->addLoginListener(Lcom/myscript/nebo/sso/api/ISSOManager$OnLoginListener;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getApplication()Landroid/app/Application;
    move-result-object v0
    const-string v2, "null cannot be cast to non-null type com.myscript.nebo.common.network.NeboNetworkStateRepositoryProvider"
    invoke-static {v0, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v0, Lcom/myscript/nebo/common/network/NeboNetworkStateRepositoryProvider;
    invoke-interface {v0}, Lcom/myscript/nebo/common/network/NeboNetworkStateRepositoryProvider;->provideNeboNetworkStateRepository()Lcom/myscript/nebo/common/network/NeboNetworkStateRepository;
    move-result-object v0
    iput-object v0, p0, Lcom/myscript/nebo/NeboBaseActivity;->networkStateRepository:Lcom/myscript/nebo/common/network/NeboNetworkStateRepository;
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getSupportFragmentManager()Landroidx/fragment/app/FragmentManager;
    move-result-object v0
    move-object v2, p0
    check-cast v2, Landroidx/lifecycle/LifecycleOwner;
    new-instance v3, Lcom/myscript/nebo/NeboBaseActivity$$ExternalSyntheticLambda9;
    invoke-direct {v3, p0}, Lcom/myscript/nebo/NeboBaseActivity$$ExternalSyntheticLambda9;-><init>(Lcom/myscript/nebo/NeboBaseActivity;)V
    const-string v4, "cloud_token_invalid_result"
    invoke-virtual {v0, v4, v2, v3}, Landroidx/fragment/app/FragmentManager;->setFragmentResultListener(Ljava/lang/String;Landroidx/lifecycle/LifecycleOwner;Landroidx/fragment/app/FragmentResultListener;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getResources()Landroid/content/res/Resources;
    move-result-object v0
    sget v2, Lcom/myscript/nebo/R$bool;->show_debug_view:I
    invoke-virtual {v0, v2}, Landroid/content/res/Resources;->getBoolean(I)Z
    move-result v0
    if-eqz v0, :cond_f7
    new-instance v0, Lcom/myscript/nebo/common/debug/UOCController;
    sget v2, Lcom/myscript/nebo/R$id;->app_debug_userObjectCount:I
    invoke-direct {v0, v1, v2, p1}, Lcom/myscript/nebo/common/debug/UOCController;-><init>(Landroidx/appcompat/app/AppCompatActivity;ILcom/myscript/atk/core/Context;)V
    iput-object v0, p0, Lcom/myscript/nebo/NeboBaseActivity;->uocController:Lcom/myscript/nebo/common/debug/UOCController;
    :cond_f7
    return-void
.end method

.method protected onStart()V
    .registers 8
    invoke-super {p0}, Landroidx/appcompat/app/AppCompatActivity;->onStart()V
    sget-object v0, Lcom/myscript/nebo/common/MainThreadBus;->eventBus:Lcom/squareup/otto/Bus;
    iget-object v1, p0, Lcom/myscript/nebo/NeboBaseActivity;->busEventListener:Lcom/myscript/nebo/NeboBaseActivity$busEventListener$1;
    invoke-virtual {v0, v1}, Lcom/squareup/otto/Bus;->register(Ljava/lang/Object;)V
    invoke-direct {p0}, Lcom/myscript/nebo/NeboBaseActivity;->checkForwardCompatibilityIssue()V
    invoke-direct {p0}, Lcom/myscript/nebo/NeboBaseActivity;->checkSSOTokenInvalidated()V
    invoke-direct {p0}, Lcom/myscript/nebo/NeboBaseActivity;->checkUpdatePrivacyPolicy()V
    move-object v0, p0
    check-cast v0, Landroidx/lifecycle/LifecycleOwner;
    invoke-static {v0}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;
    move-result-object v0
    move-object v1, v0
    check-cast v1, Lkotlinx/coroutines/CoroutineScope;
    new-instance v0, Lcom/myscript/nebo/NeboBaseActivity$onStart$1;
    const/4 v2, 0x0
    invoke-direct {v0, p0, v2}, Lcom/myscript/nebo/NeboBaseActivity$onStart$1;-><init>(Lcom/myscript/nebo/NeboBaseActivity;Lkotlin/coroutines/Continuation;)V
    move-object v4, v0
    check-cast v4, Lkotlin/jvm/functions/Function2;
    const/4 v5, 0x3
    const/4 v6, 0x0
    const/4 v3, 0x0
    invoke-static/range {v1 .. v6}, Lkotlinx/coroutines/BuildersKt;->launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;
    sget-boolean v0, Lcom/myscript/nebo/utils/Session;->needToDisplaySSOTokenInvalidated:Z
    if-eqz v0, :cond_45
    invoke-virtual {p0}, Lcom/myscript/nebo/NeboBaseActivity;->getLifecycle()Landroidx/lifecycle/Lifecycle;
    move-result-object v0
    invoke-virtual {v0}, Landroidx/lifecycle/Lifecycle;->getCurrentState()Landroidx/lifecycle/Lifecycle$State;
    move-result-object v0
    sget-object v1, Landroidx/lifecycle/Lifecycle$State;->RESUMED:Landroidx/lifecycle/Lifecycle$State;
    invoke-virtual {v0, v1}, Landroidx/lifecycle/Lifecycle$State;->isAtLeast(Landroidx/lifecycle/Lifecycle$State;)Z
    move-result v0
    if-eqz v0, :cond_45
    sget-object v0, Lcom/myscript/nebo/cloudsync/CloudManager$CloudProviderType;->Dropbox:Lcom/myscript/nebo/cloudsync/CloudManager$CloudProviderType;
    invoke-direct {p0, v0}, Lcom/myscript/nebo/NeboBaseActivity;->openCloudTokenInvalidatedDialog(Lcom/myscript/nebo/cloudsync/CloudManager$CloudProviderType;)V
    :cond_45
    check-cast p0, Landroid/content/Context;
    invoke-static {p0}, Lcom/myscript/nebo/export/ExportHelper;->wasProbablyGoneExporting(Landroid/content/Context;)Z
    move-result v0
    if-eqz v0, :cond_50
    invoke-static {p0}, Lcom/myscript/nebo/export/ExportHelper;->resetProbablyGoneExporting(Landroid/content/Context;)V
    :cond_50
    return-void
.end method
