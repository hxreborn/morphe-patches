.class public Lcom/myscript/nebo/inspector/NeboInspectorActivity;
.super Ljava/lang/Object;

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 8

    invoke-super {p0, p1}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V

    sget v0, Lcom/myscript/nebo/inspector/R$layout;->inspector_activity:I

    invoke-virtual {p0, v0}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->setContentView(I)V

    const/4 v0, 0x0

    if-eqz p1, :cond_12

    const-string v1, "EXTRA_SAVED_CURRENT_DUMPED_CLOUD_FILE_PATH"

    invoke-virtual {p1, v1}, Landroid/os/Bundle;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    goto :goto_13

    :cond_12
    move-object v1, v0

    :goto_13
    iput-object v1, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->currentDumpedCloudFilePath:Ljava/lang/String;

    if-eqz p1, :cond_1e

    const-string v1, "EXTRA_SAVED_CURRENT_DATALOG_FILE_PATH"

    invoke-virtual {p1, v1}, Landroid/os/Bundle;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    goto :goto_1f

    :cond_1e
    move-object p1, v0

    :goto_1f
    iput-object p1, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->currentDatalogFilePath:Ljava/lang/String;

    invoke-virtual {p0}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->getSupportActionBar()Landroidx/appcompat/app/ActionBar;

    move-result-object p1

    const/4 v1, 0x1

    if-eqz p1, :cond_2e

    invoke-virtual {p1, v1}, Landroidx/appcompat/app/ActionBar;->setDisplayHomeAsUpEnabled(Z)V

    invoke-virtual {p1, v1}, Landroidx/appcompat/app/ActionBar;->setDisplayShowHomeEnabled(Z)V

    :cond_2e
    sget p1, Lcom/myscript/nebo/inspector/R$id;->inspector_files_explanation:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroid/widget/TextView;

    invoke-static {}, Landroid/text/method/LinkMovementMethod;->getInstance()Landroid/text/method/MovementMethod;

    move-result-object v2

    invoke-virtual {p1, v2}, Landroid/widget/TextView;->setMovementMethod(Landroid/text/method/MovementMethod;)V

    invoke-virtual {p1, v1}, Landroid/widget/TextView;->setLinksClickable(Z)V

    sget p1, Lcom/myscript/nebo/inspector/R$id;->inspector_info_detail_toggle:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    iput-object p1, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->informationDetailToggle:Landroid/view/View;

    sget p1, Lcom/myscript/nebo/inspector/R$id;->inspector_info_detail_toggle_icon:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroid/widget/ImageView;

    iput-object p1, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->informationDetailToggleIcon:Landroid/widget/ImageView;

    new-instance p1, Lcom/myscript/nebo/inspector/InspectorInformation;

    invoke-virtual {p0}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->getApplication()Landroid/app/Application;

    move-result-object v1

    const-string v2, "getApplication(...)"

    invoke-static {v1, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-static {}, Lkotlin/collections/CollectionsKt;->emptyList()Ljava/util/List;

    move-result-object v2

    invoke-direct {p0}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->getFilesViewModel()Lcom/myscript/nebo/inspector/NeboFilesViewModel;

    move-result-object v3

    invoke-virtual {v3}, Lcom/myscript/nebo/inspector/NeboFilesViewModel;->getStats()Lcom/myscript/nebo/inspector/Stats;

    move-result-object v3

    invoke-direct {p1, v1, v2, v3}, Lcom/myscript/nebo/inspector/InspectorInformation;-><init>(Landroid/app/Application;Ljava/util/List;Lcom/myscript/nebo/inspector/Stats;)V

    sget v1, Lcom/myscript/nebo/inspector/R$id;->inspector_info:I

    invoke-virtual {p0, v1}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->findViewById(I)Landroid/view/View;

    move-result-object v1

    move-object v2, v1

    check-cast v2, Landroid/widget/TextView;

    invoke-static {}, Landroid/text/method/ScrollingMovementMethod;->getInstance()Landroid/text/method/MovementMethod;
    move-result-object v3

    check-cast v3, Landroid/text/method/MovementMethod;

    invoke-virtual {v2, v3}, Landroid/widget/TextView;->setMovementMethod(Landroid/text/method/MovementMethod;)V

    move-object v3, p0

    check-cast v3, Landroidx/lifecycle/LifecycleOwner;

    invoke-static {v3}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;

    move-result-object v4

    new-instance v5, Lstub/InspectorInformationCallback;

    invoke-direct {v5, v2, p1, v0}, Lstub/InspectorInformationCallback;-><init>(Landroid/widget/TextView;Lcom/myscript/nebo/inspector/InspectorInformation;Lkotlin/coroutines/Continuation;)V

    check-cast v5, Lkotlin/jvm/functions/Function2;

    invoke-virtual {v4, v5}, Landroidx/lifecycle/LifecycleCoroutineScope;->launchWhenCreated(Lkotlin/jvm/functions/Function2;)Lkotlinx/coroutines/Job;

    iput-object v1, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->informationLabel:Landroid/view/View;

    sget p1, Lcom/myscript/nebo/inspector/R$id;->inspector_files_reload:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;

    invoke-virtual {p1}, Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;->getContext()Landroid/content/Context;

    move-result-object v0

    const-string v1, "getContext(...)"

    invoke-static {v0, v1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    sget v1, Landroidx/appcompat/R$attr;->colorAccent:I

    invoke-static {v0, v1}, Lcom/myscript/android/utils/ContextExtKt;->resolveColorAttribute(Landroid/content/Context;I)I

    move-result v0

    filled-new-array {v0}, [I

    move-result-object v0

    invoke-virtual {p1, v0}, Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;->setColorSchemeColors([I)V

    iput-object p1, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->filesReload:Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;

    sget p1, Lcom/myscript/nebo/inspector/R$id;->inspector_files_empty_state:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    iput-object p1, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->filesEmptyState:Landroid/view/View;

    sget p1, Lcom/myscript/nebo/inspector/R$id;->inspector_files_list:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroidx/recyclerview/widget/RecyclerView;

    iget-object v0, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->filesAdapter:Lcom/myscript/nebo/inspector/NeboFilesAdapter;

    check-cast v0, Landroidx/recyclerview/widget/RecyclerView$Adapter;

    invoke-virtual {p1, v0}, Landroidx/recyclerview/widget/RecyclerView;->setAdapter(Landroidx/recyclerview/widget/RecyclerView$Adapter;)V

    iput-object p1, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->filesList:Landroidx/recyclerview/widget/RecyclerView;

    sget p1, Lcom/myscript/nebo/inspector/R$id;->inspector_dms_stats:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroid/widget/TextView;

    iput-object p1, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->stats:Landroid/widget/TextView;

    invoke-direct {p0}, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->getFilesViewModel()Lcom/myscript/nebo/inspector/NeboFilesViewModel;

    move-result-object p1

    invoke-virtual {p1}, Lcom/myscript/nebo/inspector/NeboFilesViewModel;->getState()Landroidx/lifecycle/LiveData;

    move-result-object p1

    new-instance v0, Lstub/InspectorStateCallback;

    invoke-direct {v0, p0}, Lstub/InspectorStateCallback;-><init>(Lcom/myscript/nebo/inspector/NeboInspectorActivity;)V

    new-instance p0, Lstub/InspectorObserver;

    invoke-direct {p0, v0}, Lstub/InspectorObserver;-><init>(Lkotlin/jvm/functions/Function1;)V

    check-cast p0, Landroidx/lifecycle/Observer;

    invoke-virtual {p1, v3, p0}, Landroidx/lifecycle/LiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    return-void
.end method

.method protected onStart()V
    .registers 3

    invoke-super {p0}, Landroidx/appcompat/app/AppCompatActivity;->onStart()V

    iget-object v0, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->informationDetailToggle:Landroid/view/View;

    if-eqz v0, :cond_f

    new-instance v1, Lstub/InspectorDetailsCallback;

    invoke-direct {v1, p0}, Lstub/InspectorDetailsCallback;-><init>(Lcom/myscript/nebo/inspector/NeboInspectorActivity;)V

    invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    :cond_f
    iget-object v0, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->informationLabel:Landroid/view/View;

    if-eqz v0, :cond_1b

    new-instance v1, Lstub/InspectorCopyCallback;

    invoke-direct {v1, p0}, Lstub/InspectorCopyCallback;-><init>(Lcom/myscript/nebo/inspector/NeboInspectorActivity;)V

    invoke-virtual {v0, v1}, Landroid/view/View;->setOnLongClickListener(Landroid/view/View$OnLongClickListener;)V

    :cond_1b
    iget-object v0, p0, Lcom/myscript/nebo/inspector/NeboInspectorActivity;->filesReload:Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;

    if-eqz v0, :cond_27

    new-instance v1, Lstub/InspectorReloadCallback;

    invoke-direct {v1, p0}, Lstub/InspectorReloadCallback;-><init>(Lcom/myscript/nebo/inspector/NeboInspectorActivity;)V

    invoke-virtual {v0, v1}, Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;->setOnRefreshListener(Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout$OnRefreshListener;)V

    :cond_27
    return-void
.end method
