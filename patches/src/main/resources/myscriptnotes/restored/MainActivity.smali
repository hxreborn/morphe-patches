.class public Lcom/myscript/nebo/main/ui/MainActivity;
.super Lcom/myscript/nebo/NeboBaseActivity;

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 11
    invoke-super {p0, p1}, Lcom/myscript/nebo/NeboBaseActivity;->onCreate(Landroid/os/Bundle;)V
    move-object v0, p0
    check-cast v0, Landroidx/activity/ComponentActivity;
    sget-object v1, Landroidx/activity/SystemBarStyle;->Companion:Landroidx/activity/SystemBarStyle$Companion;
    const/4 v5, 0x4
    const/4 v6, 0x0
    const/4 v2, 0x0
    const/4 v3, 0x0
    const/4 v4, 0x0
    invoke-static/range {v1 .. v6}, Landroidx/activity/SystemBarStyle$Companion;->auto$default(Landroidx/activity/SystemBarStyle$Companion;IILkotlin/jvm/functions/Function1;ILjava/lang/Object;)Landroidx/activity/SystemBarStyle;
    move-result-object v1
    const/4 v2, 0x0
    const/4 v3, 0x1
    invoke-static {v0, v2, v1, v3, v2}, Landroidx/activity/EdgeToEdge;->enable$default(Landroidx/activity/ComponentActivity;Landroidx/activity/SystemBarStyle;Landroidx/activity/SystemBarStyle;ILjava/lang/Object;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getApplication()Landroid/app/Application;
    move-result-object v1
    const-string v4, "null cannot be cast to non-null type com.myscript.nebo.log.TechnicalLoggerProvider"
    invoke-static {v1, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v1, Lcom/myscript/nebo/log/TechnicalLoggerProvider;
    invoke-interface {v1}, Lcom/myscript/nebo/log/TechnicalLoggerProvider;->provideTechnicalLogger()Lcom/myscript/nebo/log/TechnicalLogger;
    move-result-object v1
    iput-object v1, p0, Lcom/myscript/nebo/main/ui/MainActivity;->logger:Lcom/myscript/nebo/log/TechnicalLogger;
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;
    move-result-object v1
    invoke-virtual {v1}, Lcom/myscript/nebo/dms/core/LibraryRepository;->getDisplayMetrics()Landroid/util/DisplayMetrics;
    move-result-object v1
    if-nez v1, :cond_5b
    move-object v1, p0
    check-cast v1, Landroid/content/Context;
    invoke-static {v1}, Lcom/myscript/android/utils/ScreenUtils;->getFixedDisplayMetrics(Landroid/content/Context;)Landroid/util/DisplayMetrics;
    move-result-object v1
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;
    move-result-object v4
    invoke-virtual {v4, v1}, Lcom/myscript/nebo/dms/core/LibraryRepository;->setDisplayMetrics(Landroid/util/DisplayMetrics;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;
    move-result-object v4
    new-instance v5, Lcom/myscript/nebo/editing/ThumbnailImageCreator;
    invoke-static {v1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V
    new-instance v6, Ljava/io/File;
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getCacheDir()Ljava/io/File;
    move-result-object v7
    const-string v8, "thumbnail_image_cache"
    invoke-direct {v6, v7, v8}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
    invoke-direct {v5, v1, v6}, Lcom/myscript/nebo/editing/ThumbnailImageCreator;-><init>(Landroid/util/DisplayMetrics;Ljava/io/File;)V
    check-cast v5, Lcom/myscript/snt/core/IThumbnailerImageRequester;
    invoke-virtual {v4, v5}, Lcom/myscript/nebo/dms/core/LibraryRepository;->setThumbnailImageRequester(Lcom/myscript/snt/core/IThumbnailerImageRequester;)V
    :cond_5b
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getApplication()Landroid/app/Application;
    move-result-object v1
    const-string v4, "null cannot be cast to non-null type com.myscript.nebo.whatsnew.IApplicationStateProvider"
    invoke-static {v1, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    check-cast v1, Lcom/myscript/nebo/whatsnew/IApplicationStateProvider;
    invoke-interface {v1}, Lcom/myscript/nebo/whatsnew/IApplicationStateProvider;->provideApplicationState()Lcom/myscript/nebo/whatsnew/ApplicationState;
    move-result-object v1
    invoke-static {}, Lcom/myscript/nebo/whatsnew/FancyWhatsNew;->getPdfOptimizationWhatsNewData()Lcom/myscript/nebo/whatsnew/FancyWhatsNewData;
    move-result-object v4
    invoke-virtual {v1}, Lcom/myscript/nebo/whatsnew/ApplicationState;->requiresWhatsNew()Z
    move-result v5
    if-eqz v5, :cond_86
    sget-object v5, Lcom/myscript/nebo/whatsnew/FancyWhatsNewDialog;->Companion:Lcom/myscript/nebo/whatsnew/FancyWhatsNewDialog$Companion;
    invoke-virtual {v5, v4}, Lcom/myscript/nebo/whatsnew/FancyWhatsNewDialog$Companion;->newInstance(Lcom/myscript/nebo/whatsnew/FancyWhatsNewData;)Lcom/myscript/nebo/whatsnew/FancyWhatsNewDialog;
    move-result-object v5
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getSupportFragmentManager()Landroidx/fragment/app/FragmentManager;
    move-result-object v6
    const-string v7, "getSupportFragmentManager(...)"
    invoke-static {v6, v7}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {v5, v6}, Lcom/myscript/nebo/whatsnew/FancyWhatsNewDialog;->show(Landroidx/fragment/app/FragmentManager;)V
    :cond_86
    if-nez p1, :cond_91
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getIntent()Landroid/content/Intent;
    move-result-object v5
    invoke-virtual {v5}, Landroid/content/Intent;->getExtras()Landroid/os/Bundle;
    move-result-object v5
    goto :goto_92
    :cond_91
    move-object v5, p1
    :goto_92
    if-eqz v5, :cond_9b
    const-string v6, "EXTRA_FROM_FRESH_INSTALL"
    invoke-virtual {v5, v6}, Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z
    move-result v5
    goto :goto_9c
    :cond_9b
    const/4 v5, 0x0
    :goto_9c
    iput-boolean v5, p0, Lcom/myscript/nebo/main/ui/MainActivity;->isFromFreshInstall:Z
    if-eqz p1, :cond_ac
    const-string v5, "IMPORT_REQUEST_DATA_FROM_INTENT"
    invoke-virtual {p1, v5}, Landroid/os/Bundle;->getBundle(Ljava/lang/String;)Landroid/os/Bundle;
    move-result-object v5
    invoke-static {v5}, Lcom/myscript/nebo/main/ImportRequestData;->fromBundle(Landroid/os/Bundle;)Lcom/myscript/nebo/main/ImportRequestData;
    move-result-object v5
    if-nez v5, :cond_bb
    :cond_ac
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getIntent()Landroid/content/Intent;
    move-result-object v5
    if-eqz v5, :cond_ba
    move-object v6, p0
    check-cast v6, Landroid/content/Context;
    invoke-static {v6, v5}, Lcom/myscript/nebo/main/ImportRequestData;->fromIntent(Landroid/content/Context;Landroid/content/Intent;)Lcom/myscript/nebo/main/ImportRequestData;
    move-result-object v5
    goto :goto_bb
    :cond_ba
    move-object v5, v2
    :cond_bb
    :goto_bb
    iput-object v5, p0, Lcom/myscript/nebo/main/ui/MainActivity;->importRequestData:Lcom/myscript/nebo/main/ImportRequestData;
    move-object v5, p0
    check-cast v5, Landroidx/appcompat/app/AppCompatActivity;
    new-instance v6, Lstub/CollectionResultCallback;
    invoke-direct {v6, p0}, Lstub/CollectionResultCallback;-><init>(Ljava/lang/Object;)V
    check-cast v6, Lkotlin/jvm/functions/Function2;
    const-string v7, "NEW_COLLECTION_REQUEST_KEY"
    invoke-static {v5, v7, v6}, Lcom/myscript/android/utils/ActivityExtKt;->setFragmentResultListener(Landroidx/appcompat/app/AppCompatActivity;Ljava/lang/String;Lkotlin/jvm/functions/Function2;)V
    new-instance v6, Lstub/CollectionResultCallback;
    invoke-direct {v6, p0}, Lstub/CollectionResultCallback;-><init>(Ljava/lang/Object;)V
    check-cast v6, Lkotlin/jvm/functions/Function2;
    const-string v7, "EDIT_COLLECTION_REQUEST_KEY"
    invoke-static {v5, v7, v6}, Lcom/myscript/android/utils/ActivityExtKt;->setFragmentResultListener(Landroidx/appcompat/app/AppCompatActivity;Ljava/lang/String;Lkotlin/jvm/functions/Function2;)V
    new-instance v6, Lstub/FolderResultCallback;
    invoke-direct {v6, p0}, Lstub/FolderResultCallback;-><init>(Ljava/lang/Object;)V
    check-cast v6, Lkotlin/jvm/functions/Function2;
    const-string v7, "FolderEditorDialogFragment_REQUEST_KEY"
    invoke-static {v5, v7, v6}, Lcom/myscript/android/utils/ActivityExtKt;->setFragmentResultListener(Landroidx/appcompat/app/AppCompatActivity;Ljava/lang/String;Lkotlin/jvm/functions/Function2;)V
    new-instance v6, Lstub/WhatsNewCallback;
    invoke-direct {v6, v1, v4, p0}, Lstub/WhatsNewCallback;-><init>(Lcom/myscript/nebo/whatsnew/ApplicationState;Lcom/myscript/nebo/whatsnew/FancyWhatsNewData;Lcom/myscript/nebo/main/ui/MainActivity;)V
    const-string v4, "FancyWhatsNewDialog_REQUEST_KEY"
    invoke-static {v5, v4, v6}, Lcom/myscript/android/utils/ActivityExtKt;->setFragmentResultListener(Landroidx/appcompat/app/AppCompatActivity;Ljava/lang/String;Lkotlin/jvm/functions/Function2;)V
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryViewModel()Lcom/myscript/nebo/dms/core/LibraryViewModel;
    move-result-object v4
    move-object v5, p0
    check-cast v5, Lcom/myscript/nebo/dms/core/LibraryViewModel$SyncCallback;
    invoke-virtual {v4, v5}, Lcom/myscript/nebo/dms/core/LibraryViewModel;->addSyncCallback(Lcom/myscript/nebo/dms/core/LibraryViewModel$SyncCallback;)V
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryViewModel()Lcom/myscript/nebo/dms/core/LibraryViewModel;
    move-result-object v4
    invoke-virtual {v4}, Lcom/myscript/nebo/dms/core/LibraryViewModel;->getSyncCanceledOverCellularNotification()Lcom/myscript/android/utils/SingleLiveEvent;
    move-result-object v4
    move-object v5, p0
    check-cast v5, Landroidx/lifecycle/LifecycleOwner;
    new-instance v6, Lstub/SyncCanceledCallback;
    invoke-direct {v6, p0}, Lstub/SyncCanceledCallback;-><init>(Lcom/myscript/nebo/main/ui/MainActivity;)V
    new-instance v7, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;
    invoke-direct {v7, v6}, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;-><init>(Lkotlin/jvm/functions/Function1;)V
    check-cast v7, Landroidx/lifecycle/Observer;
    invoke-virtual {v4, v5, v7}, Lcom/myscript/android/utils/SingleLiveEvent;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryViewModel()Lcom/myscript/nebo/dms/core/LibraryViewModel;
    move-result-object v4
    invoke-virtual {v4}, Lcom/myscript/nebo/dms/core/LibraryViewModel;->getCloudTokenExpiredNotification()Lcom/myscript/android/utils/SingleLiveEvent;
    move-result-object v4
    new-instance v6, Lstub/CloudExpiredCallback;
    invoke-direct {v6, p0}, Lstub/CloudExpiredCallback;-><init>(Lcom/myscript/nebo/main/ui/MainActivity;)V
    new-instance v7, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;
    invoke-direct {v7, v6}, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;-><init>(Lkotlin/jvm/functions/Function1;)V
    check-cast v7, Landroidx/lifecycle/Observer;
    invoke-virtual {v4, v5, v7}, Lcom/myscript/android/utils/SingleLiveEvent;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLanguageViewModel()Lcom/myscript/nebo/languagemanager/presentation/LanguageViewModel;
    move-result-object v4
    invoke-virtual {v4}, Lcom/myscript/nebo/languagemanager/presentation/LanguageViewModel;->getDownloadCanceledOverCellularNotification()Lcom/myscript/android/utils/SingleLiveEvent;
    move-result-object v4
    new-instance v6, Lstub/LanguageCanceledCallback;
    invoke-direct {v6, p0}, Lstub/LanguageCanceledCallback;-><init>(Lcom/myscript/nebo/main/ui/MainActivity;)V
    new-instance v7, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;
    invoke-direct {v7, v6}, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;-><init>(Lkotlin/jvm/functions/Function1;)V
    check-cast v7, Landroidx/lifecycle/Observer;
    invoke-virtual {v4, v5, v7}, Lcom/myscript/android/utils/SingleLiveEvent;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryViewModel()Lcom/myscript/nebo/dms/core/LibraryViewModel;
    move-result-object v4
    invoke-virtual {v4}, Lcom/myscript/nebo/dms/core/LibraryViewModel;->getOpenNotebookKey()Landroidx/lifecycle/LiveData;
    move-result-object v4
    new-instance v6, Lstub/FolderChangedCallback;
    invoke-direct {v6, p0}, Lstub/FolderChangedCallback;-><init>(Ljava/lang/Object;)V
    check-cast v6, Lkotlin/jvm/functions/Function1;
    new-instance v7, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;
    invoke-direct {v7, v6}, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;-><init>(Lkotlin/jvm/functions/Function1;)V
    check-cast v7, Landroidx/lifecycle/Observer;
    invoke-virtual {v4, v5, v7}, Landroidx/lifecycle/LiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryViewModel()Lcom/myscript/nebo/dms/core/LibraryViewModel;
    move-result-object v4
    invoke-virtual {v4}, Lcom/myscript/nebo/dms/core/LibraryViewModel;->getState()Landroidx/lifecycle/LiveData;
    move-result-object v4
    new-instance v6, Lstub/LibraryChangedCallback;
    invoke-direct {v6, p0}, Lstub/LibraryChangedCallback;-><init>(Ljava/lang/Object;)V
    check-cast v6, Lkotlin/jvm/functions/Function1;
    new-instance v7, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;
    invoke-direct {v7, v6}, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;-><init>(Lkotlin/jvm/functions/Function1;)V
    check-cast v7, Landroidx/lifecycle/Observer;
    invoke-virtual {v4, v5, v7}, Landroidx/lifecycle/LiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getNotesViewModel()Lcom/myscript/nebo/note/NotesViewModel;
    move-result-object v4
    invoke-virtual {v4}, Lcom/myscript/nebo/note/NotesViewModel;->getImportState()Landroidx/lifecycle/LiveData;
    move-result-object v4
    new-instance v6, Lstub/PdfImportCallback;
    invoke-direct {v6, p0}, Lstub/PdfImportCallback;-><init>(Ljava/lang/Object;)V
    check-cast v6, Lkotlin/jvm/functions/Function1;
    new-instance v7, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;
    invoke-direct {v7, v6}, Lcom/myscript/nebo/main/ui/MainActivity$sam$androidx_lifecycle_Observer$0;-><init>(Lkotlin/jvm/functions/Function1;)V
    check-cast v7, Landroidx/lifecycle/Observer;
    invoke-virtual {v4, v5, v7}, Landroidx/lifecycle/LiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getBackupRestoreViewModel()Lcom/myscript/nebo/backup/BackupRestoreViewModel;
    move-result-object v4
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getMainViewModel()Lcom/myscript/nebo/main/MainViewModel;
    move-result-object v5
    invoke-virtual {v5}, Lcom/myscript/nebo/main/MainViewModel;->getBackupRestoreListener()Lcom/myscript/nebo/backup/BackupRestoreViewModel$BackupRestoreListener;
    move-result-object v5
    invoke-virtual {v4, v5}, Lcom/myscript/nebo/backup/BackupRestoreViewModel;->addBackupRestoreListener(Lcom/myscript/nebo/backup/BackupRestoreViewModel$BackupRestoreListener;)V
    if-nez p1, :cond_1c8
    iget-object p1, p0, Lcom/myscript/nebo/main/ui/MainActivity;->importRequestData:Lcom/myscript/nebo/main/ImportRequestData;
    if-nez p1, :cond_1c8
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;
    move-result-object p1
    invoke-virtual {p1}, Lcom/myscript/nebo/dms/core/LibraryRepository;->getLastOpenedFolderKey()Lcom/myscript/snt/core/NotebookKey;
    move-result-object p1
    if-eqz p1, :cond_1b1
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getNotesViewModel()Lcom/myscript/nebo/note/NotesViewModel;
    move-result-object v4
    invoke-virtual {v4, p1}, Lcom/myscript/nebo/note/NotesViewModel;->openFolder(Lcom/myscript/snt/core/NotebookKey;)V
    :cond_1b1
    invoke-virtual {v1}, Lcom/myscript/nebo/whatsnew/ApplicationState;->requiresWhatsNew()Z
    move-result p1
    if-nez p1, :cond_1c8
    invoke-virtual {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;
    move-result-object p1
    invoke-virtual {p1}, Lcom/myscript/nebo/dms/core/LibraryRepository;->getLastOpenedNoteKey()Lcom/myscript/snt/core/PageKey;
    move-result-object p1
    if-eqz p1, :cond_1c8
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->getNotesViewModel()Lcom/myscript/nebo/note/NotesViewModel;
    move-result-object v1
    invoke-virtual {v1, p1}, Lcom/myscript/nebo/note/NotesViewModel;->openPage(Lcom/myscript/snt/core/PageKey;)V
    :cond_1c8
    invoke-direct {p0}, Lcom/myscript/nebo/main/ui/MainActivity;->displayAutoSyncInformationDialogIfCloudConnected()V
    move-object p1, p0
    check-cast p1, Landroid/content/Context;
    invoke-static {p1}, Landroidx/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object p1
    move-object v1, p0
    check-cast v1, Landroid/content/SharedPreferences$OnSharedPreferenceChangeListener;
    invoke-interface {p1, v1}, Landroid/content/SharedPreferences;->registerOnSharedPreferenceChangeListener(Landroid/content/SharedPreferences$OnSharedPreferenceChangeListener;)V
    new-instance p1, Lstub/MainContentCallback;
    invoke-direct {p1, p0}, Lstub/MainContentCallback;-><init>(Lcom/myscript/nebo/main/ui/MainActivity;)V
    const p0, 0x5dec658f
    invoke-static {p0, v3, p1}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->composableLambdaInstance(IZLjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambda;
    move-result-object p0
    check-cast p0, Lkotlin/jvm/functions/Function2;
    invoke-static {v0, v2, p0, v3, v2}, Landroidx/activity/compose/ComponentActivityKt;->setContent$default(Landroidx/activity/ComponentActivity;Landroidx/compose/runtime/CompositionContext;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)V
    return-void
.end method
