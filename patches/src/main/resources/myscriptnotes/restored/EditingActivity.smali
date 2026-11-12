.class public Lcom/myscript/nebo/editing/EditingActivity;
.super Lcom/myscript/nebo/NeboBaseActivity;

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 13

    invoke-super {p0, p1}, Lcom/myscript/nebo/NeboBaseActivity;->onCreate(Landroid/os/Bundle;)V

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getIntent()Landroid/content/Intent;

    move-result-object p1

    const-string v0, "EditingActivity:PAGE_KEY_ARGUMENT"

    invoke-virtual {p1, v0}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    if-eqz p1, :cond_292

    invoke-static {p1}, Lcom/myscript/snt/core/PageKey;->deserialize(Ljava/lang/String;)Lcom/myscript/snt/core/PageKey;

    move-result-object p1

    if-eqz p1, :cond_292

    iput-object p1, p0, Lcom/myscript/nebo/editing/EditingActivity;->openNoteKey:Lcom/myscript/snt/core/PageKey;

    invoke-virtual {p0}, Lstub/EditorMethods;->NoteEditorViewModel()Lcom/myscript/nebo/editing/NoteEditorViewModel;

    move-result-object p1

    iget-object v0, p0, Lcom/myscript/nebo/editing/EditingActivity;->openNoteKey:Lcom/myscript/snt/core/PageKey;

    const-string v1, "openNoteKey"

    const/4 v2, 0x0

    if-nez v0, :cond_26

    invoke-static {v1}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    move-object v0, v2

    :cond_26
    invoke-virtual {p1, v0}, Lcom/myscript/nebo/editing/NoteEditorViewModel;->openNote(Lcom/myscript/snt/core/PageKey;)V

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getIntent()Landroid/content/Intent;

    move-result-object p1

    const-string v0, "EditingActivity:PAGE_CONTENT_TYPE_ARGUMENT"

    invoke-virtual {p1, v0}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    if-eqz p1, :cond_286

    invoke-virtual {p0, p1}, Lstub/EditorMethods;->setContentType(Ljava/lang/String;)V

    invoke-virtual {p0}, Lstub/EditorMethods;->ChildPagesViewModel()Lcom/myscript/nebo/editing/ChildPagesViewModel;

    move-result-object p1

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    sget v3, Lcom/myscript/nebo/R$dimen;->child_pages_side_panel_width_large:I

    invoke-virtual {v0, v3}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v0

    invoke-virtual {p1, v0}, Lcom/myscript/nebo/editing/ChildPagesViewModel;->setThumbnailWidthInPixels(I)V

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getWindow()Landroid/view/Window;

    move-result-object p1

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getWindow()Landroid/view/Window;

    move-result-object v0

    invoke-virtual {v0}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    move-result-object v0

    invoke-static {p1, v0}, Landroidx/core/view/WindowCompat;->getInsetsController(Landroid/view/Window;Landroid/view/View;)Landroidx/core/view/WindowInsetsControllerCompat;

    move-result-object p1

    const/4 v0, 0x2

    invoke-virtual {p1, v0}, Landroidx/core/view/WindowInsetsControllerCompat;->setSystemBarsBehavior(I)V

    invoke-static {}, Landroidx/core/view/WindowInsetsCompat$Type;->systemBars()I

    move-result v0

    invoke-virtual {p1, v0}, Landroidx/core/view/WindowInsetsControllerCompat;->hide(I)V

    invoke-direct {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getToolbarViewModel()Lcom/myscript/nebo/penpanel/ToolbarViewModel;

    move-result-object p1

    invoke-virtual {p1}, Lcom/myscript/nebo/penpanel/ToolbarViewModel;->getToolbarConfiguration()Landroidx/lifecycle/LiveData;

    move-result-object p1

    move-object v0, p0

    check-cast v0, Landroidx/lifecycle/LifecycleOwner;

    new-instance v3, Lstub/ToolbarCallback;

    invoke-direct {v3, p0}, Lstub/ToolbarCallback;-><init>(Lcom/myscript/nebo/editing/EditingActivity;)V

    new-instance v4, Lstub/EditorObserver;

    invoke-direct {v4, v3}, Lstub/EditorObserver;-><init>(Lkotlin/jvm/functions/Function1;)V

    check-cast v4, Landroidx/lifecycle/Observer;

    invoke-virtual {p1, v0, v4}, Landroidx/lifecycle/LiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    invoke-direct {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getToolbarViewModel()Lcom/myscript/nebo/penpanel/ToolbarViewModel;

    move-result-object p1

    invoke-virtual {p1}, Lcom/myscript/nebo/penpanel/ToolbarViewModel;->getSelectedToolType()Landroidx/lifecycle/MutableLiveData;

    move-result-object p1

    new-instance v3, Lstub/ToolCallback;

    invoke-direct {v3, p0}, Lstub/ToolCallback;-><init>(Lcom/myscript/nebo/editing/EditingActivity;)V

    new-instance v4, Lstub/EditorObserver;

    invoke-direct {v4, v3}, Lstub/EditorObserver;-><init>(Lkotlin/jvm/functions/Function1;)V

    check-cast v4, Landroidx/lifecycle/Observer;

    invoke-virtual {p1, v0, v4}, Landroidx/lifecycle/MutableLiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    invoke-static {v0}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;

    move-result-object p1

    move-object v3, p1

    check-cast v3, Lkotlinx/coroutines/CoroutineScope;

    new-instance p1, Lstub/LaserObserver;

    invoke-direct {p1, p0, v2}, Lstub/LaserObserver;-><init>(Lcom/myscript/nebo/editing/EditingActivity;Lkotlin/coroutines/Continuation;)V

    move-object v6, p1

    check-cast v6, Lkotlin/jvm/functions/Function2;

    const/4 v7, 0x3

    const/4 v8, 0x0

    const/4 v4, 0x0

    const/4 v5, 0x0

    invoke-static/range {v3 .. v8}, Lkotlinx/coroutines/BuildersKt;->launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;

    invoke-static {v0}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;

    move-result-object p1

    move-object v3, p1

    check-cast v3, Lkotlinx/coroutines/CoroutineScope;

    new-instance p1, Lstub/ZoomObserver;

    invoke-direct {p1, p0, v2}, Lstub/ZoomObserver;-><init>(Lcom/myscript/nebo/editing/EditingActivity;Lkotlin/coroutines/Continuation;)V

    move-object v6, p1

    check-cast v6, Lkotlin/jvm/functions/Function2;

    invoke-static/range {v3 .. v8}, Lkotlinx/coroutines/BuildersKt;->launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getSupportFragmentManager()Landroidx/fragment/app/FragmentManager;

    move-result-object p1

    new-instance v3, Lstub/WritingCallback;

    invoke-direct {v3, p0}, Lstub/WritingCallback;-><init>(Lcom/myscript/nebo/editing/EditingActivity;)V

    const-string v4, "ToolConfigurationDialogFragment_RESULT_WRITING_KEY"

    invoke-virtual {p1, v4, v0, v3}, Landroidx/fragment/app/FragmentManager;->setFragmentResultListener(Ljava/lang/String;Landroidx/lifecycle/LifecycleOwner;Landroidx/fragment/app/FragmentResultListener;)V

    invoke-virtual {p0}, Lstub/EditorMethods;->SearchPageViewModel()Lcom/myscript/nebo/editing/search/SearchPageViewModel;

    move-result-object p1

    const/4 v3, 0x1

    invoke-virtual {p1, v3}, Lcom/myscript/nebo/editing/search/SearchPageViewModel;->showCurrentHitNextPageBinding(Z)V

    invoke-virtual {p0}, Lstub/EditorMethods;->SearchPageViewModel()Lcom/myscript/nebo/editing/search/SearchPageViewModel;

    move-result-object p1

    invoke-virtual {p1}, Lcom/myscript/nebo/editing/search/SearchPageViewModel;->getPageIdToShow()Lcom/myscript/android/utils/SingleLiveEvent;

    move-result-object p1

    new-instance v4, Lstub/SearchNavigationCallback;

    invoke-direct {v4, p0}, Lstub/SearchNavigationCallback;-><init>(Ljava/lang/Object;)V

    check-cast v4, Lkotlin/jvm/functions/Function1;

    new-instance v5, Lstub/EditorObserver;

    invoke-direct {v5, v4}, Lstub/EditorObserver;-><init>(Lkotlin/jvm/functions/Function1;)V

    check-cast v5, Landroidx/lifecycle/Observer;

    invoke-virtual {p1, v0, v5}, Lcom/myscript/android/utils/SingleLiveEvent;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    invoke-virtual {p0}, Lstub/EditorMethods;->ChildPagesViewModel()Lcom/myscript/nebo/editing/ChildPagesViewModel;

    move-result-object p1

    invoke-virtual {p1}, Lcom/myscript/nebo/editing/ChildPagesViewModel;->getPaginatedContainer()Lcom/myscript/snt/dms/IPaginatedContainer;

    move-result-object p1

    if-nez p1, :cond_1d0

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;

    move-result-object p1

    iget-object v4, p0, Lcom/myscript/nebo/editing/EditingActivity;->openNoteKey:Lcom/myscript/snt/core/PageKey;

    if-nez v4, :cond_102

    invoke-static {v1}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    move-object v4, v2

    :cond_102
    invoke-virtual {p1, v4}, Lcom/myscript/nebo/dms/core/LibraryRepository;->buildNotebookManager(Lcom/myscript/snt/core/PageKey;)Lcom/myscript/snt/dms/IPaginatedContainer;

    move-result-object v6

    if-eqz v6, :cond_1cf

    invoke-interface {v6}, Lcom/myscript/snt/dms/IPaginatedContainer;->pages()Ljava/util/List;

    move-result-object p1

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;

    move-result-object v4

    iget-object v5, p0, Lcom/myscript/nebo/editing/EditingActivity;->openNoteKey:Lcom/myscript/snt/core/PageKey;

    if-nez v5, :cond_118

    invoke-static {v1}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    move-object v5, v2

    :cond_118
    invoke-virtual {v4, v5}, Lcom/myscript/nebo/dms/core/LibraryRepository;->lastOpenedPage(Lcom/myscript/snt/core/PageKey;)Lcom/myscript/snt/core/PageKey;

    move-result-object v4

    const/4 v5, 0x0

    if-eqz v4, :cond_147

    invoke-static {p1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V

    invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v7

    move v8, v5

    :goto_127
    invoke-interface {v7}, Ljava/util/Iterator;->hasNext()Z

    move-result v9

    if-eqz v9, :cond_145

    invoke-interface {v7}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Lcom/myscript/snt/dms/PageProperties;

    invoke-virtual {v9}, Lcom/myscript/snt/dms/PageProperties;->getPageId()Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v4}, Lcom/myscript/snt/core/PageKey;->uuid()Ljava/lang/String;

    move-result-object v10

    invoke-static {v9, v10}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v9

    if-eqz v9, :cond_142

    goto :goto_148

    :cond_142
    add-int/lit8 v8, v8, 0x1

    goto :goto_127

    :cond_145
    const/4 v8, -0x1

    goto :goto_148

    :cond_147
    move v8, v5

    :goto_148
    add-int/lit8 v4, v8, 0x1

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    move-object v7, v4

    check-cast v7, Ljava/lang/Number;

    invoke-virtual {v7}, Ljava/lang/Number;->intValue()I

    if-ltz v8, :cond_160

    move-object v7, p1

    check-cast v7, Ljava/util/Collection;

    invoke-interface {v7}, Ljava/util/Collection;->size()I

    move-result v7

    if-ge v8, v7, :cond_160

    goto :goto_161

    :cond_160
    move-object v4, v2

    :goto_161
    if-eqz v4, :cond_168

    invoke-virtual {v4}, Ljava/lang/Integer;->intValue()I

    move-result v4

    goto :goto_169

    :cond_168
    move v4, v3

    :goto_169
    invoke-static {p1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V

    check-cast p1, Ljava/util/Collection;

    invoke-interface {p1}, Ljava/util/Collection;->isEmpty()Z

    move-result p1

    if-nez p1, :cond_17d

    int-to-long v7, v4

    invoke-interface {v6, v7, v8}, Lcom/myscript/snt/dms/IPaginatedContainer;->pageAt(J)Ljava/lang/String;

    move-result-object p1

    invoke-interface {v6, p1, v5}, Lcom/myscript/snt/dms/IPaginatedContainer;->selectPage(Ljava/lang/String;Z)V

    goto :goto_1a6

    :cond_17d
    invoke-direct {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getLogger()Lcom/myscript/nebo/log/TechnicalLogger;

    move-result-object p1

    new-instance v4, Ljava/lang/IllegalStateException;

    iget-object v5, p0, Lcom/myscript/nebo/editing/EditingActivity;->openNoteKey:Lcom/myscript/snt/core/PageKey;

    if-nez v5, :cond_18b

    invoke-static {v1}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    move-object v5, v2

    :cond_18b
    invoke-virtual {v5}, Lcom/myscript/snt/core/PageKey;->uuid()Ljava/lang/String;

    move-result-object v5

    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "Notebook has no pages: "

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-direct {v4, v5}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    check-cast v4, Ljava/lang/Exception;

    invoke-interface {p1, v4}, Lcom/myscript/nebo/log/TechnicalLogger;->logError(Ljava/lang/Exception;)V

    :goto_1a6
    invoke-virtual {p0}, Lstub/EditorMethods;->ChildPagesViewModel()Lcom/myscript/nebo/editing/ChildPagesViewModel;

    move-result-object v5

    iget-object p1, p0, Lcom/myscript/nebo/editing/EditingActivity;->openNoteKey:Lcom/myscript/snt/core/PageKey;

    if-nez p1, :cond_1b3

    invoke-static {v1}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    move-object v7, v2

    goto :goto_1b4

    :cond_1b3
    move-object v7, p1

    :goto_1b4
    const/4 v9, 0x4

    const/4 v10, 0x0

    const/4 v8, 0x0

    invoke-static/range {v5 .. v10}, Lcom/myscript/nebo/editing/ChildPagesViewModel;->bindPaginatedContainer$default(Lcom/myscript/nebo/editing/ChildPagesViewModel;Lcom/myscript/snt/dms/IPaginatedContainer;Lcom/myscript/snt/core/PageKey;Ljava/lang/Boolean;ILjava/lang/Object;)V

    invoke-direct {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getCrashCountState()Lcom/myscript/nebo/crash/ICrashCountState;

    move-result-object p1

    iget-object v4, p0, Lcom/myscript/nebo/editing/EditingActivity;->openNoteKey:Lcom/myscript/snt/core/PageKey;

    if-nez v4, :cond_1c6

    invoke-static {v1}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    move-object v4, v2

    :cond_1c6
    invoke-virtual {v4}, Lcom/myscript/snt/core/PageKey;->uuid()Ljava/lang/String;

    move-result-object v1

    invoke-interface {p1, v1}, Lcom/myscript/nebo/crash/ICrashCountState;->setNoteCrashCountKey(Ljava/lang/String;)V

    move-object p1, v6

    goto :goto_1d0

    :cond_1cf
    move-object p1, v2

    :cond_1d0
    :goto_1d0
    instance-of p1, p1, Lcom/myscript/snt/dms/NotebookManager;

    iput-boolean p1, p0, Lcom/myscript/nebo/editing/EditingActivity;->isContainer:Z

    invoke-virtual {p0}, Lstub/EditorMethods;->NoteEditorViewModel()Lcom/myscript/nebo/editing/NoteEditorViewModel;

    move-result-object p1

    invoke-virtual {p1}, Lcom/myscript/nebo/editing/NoteEditorViewModel;->getNoteProperties()Landroidx/lifecycle/LiveData;

    move-result-object p1

    new-instance v1, Lstub/NotePropertiesCallback;

    invoke-direct {v1, p0}, Lstub/NotePropertiesCallback;-><init>(Ljava/lang/Object;)V

    check-cast v1, Lkotlin/jvm/functions/Function1;

    new-instance v4, Lstub/EditorObserver;

    invoke-direct {v4, v1}, Lstub/EditorObserver;-><init>(Lkotlin/jvm/functions/Function1;)V

    check-cast v4, Landroidx/lifecycle/Observer;

    invoke-virtual {p1, v0, v4}, Landroidx/lifecycle/LiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    invoke-virtual {p0}, Lstub/EditorMethods;->NoteEditorViewModel()Lcom/myscript/nebo/editing/NoteEditorViewModel;

    move-result-object p1

    invoke-virtual {p1}, Lcom/myscript/nebo/editing/NoteEditorViewModel;->getDiscoveryTooltip()Landroidx/lifecycle/LiveData;

    move-result-object p1

    new-instance v1, Lstub/TooltipCallback;

    invoke-direct {v1, p0}, Lstub/TooltipCallback;-><init>(Ljava/lang/Object;)V

    check-cast v1, Lkotlin/jvm/functions/Function1;

    new-instance v4, Lstub/EditorObserver;

    invoke-direct {v4, v1}, Lstub/EditorObserver;-><init>(Lkotlin/jvm/functions/Function1;)V

    check-cast v4, Landroidx/lifecycle/Observer;

    invoke-virtual {p1, v0, v4}, Landroidx/lifecycle/LiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    invoke-virtual {p0}, Lstub/EditorMethods;->NoteEditorViewModel()Lcom/myscript/nebo/editing/NoteEditorViewModel;

    move-result-object p1

    invoke-virtual {p1}, Lcom/myscript/nebo/editing/NoteEditorViewModel;->getCloudTokenExpiredNotification()Lcom/myscript/android/utils/SingleLiveEvent;

    move-result-object p1

    new-instance v1, Lstub/EditorCloudCallback;

    invoke-direct {v1, p0}, Lstub/EditorCloudCallback;-><init>(Lcom/myscript/nebo/editing/EditingActivity;)V

    new-instance v4, Lstub/EditorObserver;

    invoke-direct {v4, v1}, Lstub/EditorObserver;-><init>(Lkotlin/jvm/functions/Function1;)V

    check-cast v4, Landroidx/lifecycle/Observer;

    invoke-virtual {p1, v0, v4}, Lcom/myscript/android/utils/SingleLiveEvent;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getSupportFragmentManager()Landroidx/fragment/app/FragmentManager;

    move-result-object p1

    new-instance v1, Lstub/NoteNameCallback;

    invoke-direct {v1, p0}, Lstub/NoteNameCallback;-><init>(Lcom/myscript/nebo/editing/EditingActivity;)V

    const-string v4, "NoteNameDialog_KEY"

    invoke-virtual {p1, v4, v0, v1}, Landroidx/fragment/app/FragmentManager;->setFragmentResultListener(Ljava/lang/String;Landroidx/lifecycle/LifecycleOwner;Landroidx/fragment/app/FragmentResultListener;)V

    iget-boolean p1, p0, Lcom/myscript/nebo/editing/EditingActivity;->isContainer:Z

    if-eqz p1, :cond_23a

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getLibraryRepository()Lcom/myscript/nebo/dms/core/LibraryRepository;

    move-result-object p1

    iget-object v0, p0, Lcom/myscript/nebo/editing/EditingActivity;->syncListener:Lstub/EditorSyncListener;

    check-cast v0, Lcom/myscript/nebo/dms/core/LibraryRepository$SyncListener;

    invoke-virtual {p1, v0}, Lcom/myscript/nebo/dms/core/LibraryRepository;->addSyncListener(Lcom/myscript/nebo/dms/core/LibraryRepository$SyncListener;)V

    :cond_23a
    sget p1, Lcom/myscript/nebo/R$layout;->editing_activity_root:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/editing/EditingActivity;->setContentView(I)V

    invoke-direct {p0}, Lcom/myscript/nebo/editing/EditingActivity;->configurePageSurface()V

    invoke-direct {p0}, Lcom/myscript/nebo/editing/EditingActivity;->observeChildPages()V

    new-instance p1, Lcom/myscript/nebo/HapticFeedbackManagerFactoryImpl;

    invoke-direct {p1}, Lcom/myscript/nebo/HapticFeedbackManagerFactoryImpl;-><init>()V

    invoke-virtual {p1}, Lcom/myscript/nebo/HapticFeedbackManagerFactoryImpl;->createHapticFeedbackManager()Lcom/myscript/nebo/editing/HapticFeedbackManager;

    move-result-object p1

    iput-object p1, p0, Lcom/myscript/nebo/editing/EditingActivity;->hapticFeedbackManager:Lcom/myscript/nebo/editing/HapticFeedbackManager;

    if-nez p1, :cond_258

    const-string p1, "hapticFeedbackManager"

    invoke-static {p1}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    goto :goto_259

    :cond_258
    move-object v2, p1

    :goto_259
    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getApplicationContext()Landroid/content/Context;

    move-result-object p1

    const-string v0, "getApplicationContext(...)"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-interface {v2, p1}, Lcom/myscript/nebo/editing/HapticFeedbackManager;->init(Landroid/content/Context;)V

    sget p1, Lcom/myscript/nebo/R$id;->editing_compose_view:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/editing/EditingActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroidx/compose/ui/platform/ComposeView;

    sget-object v0, Landroidx/compose/ui/platform/ViewCompositionStrategy$DisposeOnViewTreeLifecycleDestroyed;->INSTANCE:Landroidx/compose/ui/platform/ViewCompositionStrategy$DisposeOnViewTreeLifecycleDestroyed;

    check-cast v0, Landroidx/compose/ui/platform/ViewCompositionStrategy;

    invoke-virtual {p1, v0}, Landroidx/compose/ui/platform/ComposeView;->setViewCompositionStrategy(Landroidx/compose/ui/platform/ViewCompositionStrategy;)V

    new-instance v0, Lstub/EditorContentCallback;

    invoke-direct {v0, p0}, Lstub/EditorContentCallback;-><init>(Lcom/myscript/nebo/editing/EditingActivity;)V

    const p0, EDITOR_CONTENT_KEY

    invoke-static {p0, v3, v0}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->composableLambdaInstance(IZLjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambda;

    move-result-object p0

    check-cast p0, Lkotlin/jvm/functions/Function2;

    invoke-virtual {p1, p0}, Landroidx/compose/ui/platform/ComposeView;->setContent(Lkotlin/jvm/functions/Function2;)V

    return-void

    :cond_286
    new-instance p0, Ljava/lang/IllegalArgumentException;

    const-string p1, "Required value was null."

    invoke-virtual {p1}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_292
    new-instance p0, Ljava/lang/IllegalStateException;

    const-string p1, "Page key is required"

    invoke-virtual {p1}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p0
.end method

.method protected onResume()V
    .registers 8

    invoke-super {p0}, Lcom/myscript/nebo/NeboBaseActivity;->onResume()V

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getUserData()Lcom/myscript/nebo/sso/model/UserDataProvider;

    move-result-object v0

    invoke-virtual {v0}, Lcom/myscript/nebo/sso/model/UserDataProvider;->isNewPrivacyAccepted()Z

    move-result v0

    if-nez v0, :cond_21

    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getUserData()Lcom/myscript/nebo/sso/model/UserDataProvider;

    move-result-object v0

    invoke-virtual {v0}, Lcom/myscript/nebo/sso/model/UserDataProvider;->hasUser()Z

    move-result v0

    if-eqz v0, :cond_21

    const/4 v0, 0x1

    sput-boolean v0, Lcom/myscript/nebo/utils/Session;->needToUpdatePrivacyPolicy:Z

    sget-object v0, Lcom/myscript/nebo/common/MainThreadBus;->eventBus:Lcom/squareup/otto/Bus;

    sget-object v1, Lcom/myscript/nebo/events/OnUpdatePrivacyPolicyNeededEvent;->UNIQUE:Lcom/myscript/nebo/events/OnUpdatePrivacyPolicyNeededEvent;

    invoke-virtual {v0, v1}, Lcom/squareup/otto/Bus;->post(Ljava/lang/Object;)V

    :cond_21
    invoke-virtual {p0}, Lcom/myscript/nebo/editing/EditingActivity;->getApplication()Landroid/app/Application;

    move-result-object v0

    const-string v1, "null cannot be cast to non-null type com.myscript.nebo.account.UserProfileManager.Provider"

    invoke-static {v0, v1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V

    check-cast v0, Lcom/myscript/nebo/account/UserProfileManager$Provider;

    invoke-interface {v0}, Lcom/myscript/nebo/account/UserProfileManager$Provider;->provideUserProfileManager()Lcom/myscript/nebo/account/UserProfileManager;

    move-result-object v0

    check-cast p0, Landroidx/lifecycle/LifecycleOwner;

    invoke-static {p0}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;

    move-result-object p0

    move-object v1, p0

    check-cast v1, Lkotlinx/coroutines/CoroutineScope;

    new-instance p0, Lstub/EditorResumeCallback;

    const/4 v2, 0x0

    invoke-direct {p0, v0, v2}, Lstub/EditorResumeCallback;-><init>(Lcom/myscript/nebo/account/UserProfileManager;Lkotlin/coroutines/Continuation;)V

    move-object v4, p0

    check-cast v4, Lkotlin/jvm/functions/Function2;

    const/4 v5, 0x3

    const/4 v6, 0x0

    const/4 v3, 0x0

    invoke-static/range {v1 .. v6}, Lkotlinx/coroutines/BuildersKt;->launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;

    return-void
.end method

.method protected onStop()V
    .registers 3
    const/4 v1, 0x0
    invoke-virtual {p0}, Lstub/EditorMethods;->NoteEditorViewModel()Lcom/myscript/nebo/editing/NoteEditorViewModel;
    move-result-object v0
    invoke-virtual {v0, v1}, Lcom/myscript/nebo/editing/NoteEditorViewModel;->setNoteCommands(Lcom/myscript/nebo/editing/menu/NoteMenuCommands;)V
    iget-object v0, p0, Lcom/myscript/nebo/editing/EditingActivity;->stylusDoubleTapReceiver:Lcom/myscript/nebo/editing/StylusDoubleTapReceiver;
    if-eqz v0, :thumbnails
    invoke-virtual {p0, v0}, Lcom/myscript/nebo/editing/EditingActivity;->unregisterReceiver(Landroid/content/BroadcastReceiver;)V
    invoke-virtual {v0, v1}, Lcom/myscript/nebo/editing/StylusDoubleTapReceiver;->setStylusDoubleTapListener(Lcom/myscript/nebo/editing/IStylusDoubleTapListener;)V
    :thumbnails
    invoke-virtual {p0}, Lstub/EditorMethods;->ChildPagesViewModel()Lcom/myscript/nebo/editing/ChildPagesViewModel;
    move-result-object v0
    invoke-virtual {v0}, Lcom/myscript/nebo/editing/ChildPagesViewModel;->isChildPagesCompatible()Z
    move-result v0
    if-eqz v0, :selection
    invoke-virtual {p0}, Lstub/EditorMethods;->ChildPagesViewModel()Lcom/myscript/nebo/editing/ChildPagesViewModel;
    move-result-object v0
    invoke-virtual {v0}, Lcom/myscript/nebo/editing/ChildPagesViewModel;->cleanOldThumbnails()V
    :selection
    invoke-virtual {p0}, Lstub/EditorMethods;->AIContainerViewModel()Lcom/myscript/nebo/ai/presentation/compose/AIContainerViewModel;
    move-result-object v0
    invoke-virtual {v0, v1}, Lcom/myscript/nebo/ai/presentation/compose/AIContainerViewModel;->setTextSelectionProvider(Lcom/myscript/nebo/ai/presentation/compose/TextSelectionProvider;)V
    invoke-virtual {v0}, Lcom/myscript/nebo/ai/presentation/compose/AIContainerViewModel;->clearLastOperation()V
    invoke-virtual {p0}, Lstub/EditorMethods;->AIQuizViewModel()Lcom/myscript/nebo/ai/presentation/AIQuizViewModel;
    move-result-object v0
    invoke-virtual {v0, v1}, Lcom/myscript/nebo/ai/presentation/AIQuizViewModel;->setTextSelectionProvider(Lcom/myscript/nebo/ai/presentation/compose/TextSelectionProvider;)V
    invoke-super {p0}, Lcom/myscript/nebo/NeboBaseActivity;->onStop()V
    return-void
.end method
