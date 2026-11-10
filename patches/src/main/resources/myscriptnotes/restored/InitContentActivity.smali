.class public Lcom/myscript/nebo/init/InitContentActivity;
.super Landroidx/appcompat/app/AppCompatActivity;

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 8
    invoke-super {p0, p1}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/init/InitContentActivity;->getLayoutInflater()Landroid/view/LayoutInflater;
    move-result-object v0
    invoke-static {v0}, Lcom/myscript/nebo/databinding/InitContentActivityBinding;->inflate(Landroid/view/LayoutInflater;)Lcom/myscript/nebo/databinding/InitContentActivityBinding;
    move-result-object v0
    const-string v1, "inflate(...)"
    invoke-static {v0, v1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    iput-object v0, p0, Lcom/myscript/nebo/init/InitContentActivity;->binding:Lcom/myscript/nebo/databinding/InitContentActivityBinding;
    if-nez v0, :cond_1a
    const-string v0, "binding"
    invoke-static {v0}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V
    const/4 v0, 0x0
    :cond_1a
    invoke-virtual {v0}, Lcom/myscript/nebo/databinding/InitContentActivityBinding;->getRoot()Landroid/widget/FrameLayout;
    move-result-object v0
    check-cast v0, Landroid/view/View;
    invoke-virtual {p0, v0}, Lcom/myscript/nebo/init/InitContentActivity;->setContentView(Landroid/view/View;)V
    invoke-direct {p0}, Lcom/myscript/nebo/init/InitContentActivity;->getViewModel()Lcom/myscript/nebo/init/InitContentViewModel;
    move-result-object v0
    invoke-virtual {v0}, Lcom/myscript/nebo/init/InitContentViewModel;->getState()Landroidx/lifecycle/LiveData;
    move-result-object v0
    move-object v1, p0
    check-cast v1, Landroidx/lifecycle/LifecycleOwner;
    new-instance v2, Lcom/myscript/nebo/init/InitContentActivity$$ExternalSyntheticLambda0;
    invoke-direct {v2, p0}, Lcom/myscript/nebo/init/InitContentActivity$$ExternalSyntheticLambda0;-><init>(Lcom/myscript/nebo/init/InitContentActivity;)V
    new-instance v3, Lcom/myscript/nebo/init/InitContentActivity$sam$androidx_lifecycle_Observer$0;
    invoke-direct {v3, v2}, Lcom/myscript/nebo/init/InitContentActivity$sam$androidx_lifecycle_Observer$0;-><init>(Lkotlin/jvm/functions/Function1;)V
    check-cast v3, Landroidx/lifecycle/Observer;
    invoke-virtual {v0, v1, v3}, Landroidx/lifecycle/LiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V
    if-nez p1, :cond_7f
    sget p1, Lcom/myscript/nebo/onboarding/R$string;->onboarding_sample_my_collection:I
    invoke-virtual {p0, p1}, Lcom/myscript/nebo/init/InitContentActivity;->getString(I)Ljava/lang/String;
    move-result-object v1
    const-string p1, "getString(...)"
    invoke-static {v1, p1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    sget v0, Lcom/myscript/nebo/onboarding/R$string;->onboarding_sample_folder_name:I
    invoke-virtual {p0, v0}, Lcom/myscript/nebo/init/InitContentActivity;->getString(I)Ljava/lang/String;
    move-result-object v2
    invoke-static {v2, p1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/init/InitContentActivity;->getResources()Landroid/content/res/Resources;
    move-result-object v0
    sget v3, Lcom/myscript/nebo/common/R$color;->default_folder_cover_color:I
    invoke-virtual {p0}, Lcom/myscript/nebo/init/InitContentActivity;->getTheme()Landroid/content/res/Resources$Theme;
    move-result-object v4
    invoke-static {v0, v3, v4}, Landroidx/core/content/res/ResourcesCompat;->getColor(Landroid/content/res/Resources;ILandroid/content/res/Resources$Theme;)I
    move-result v3
    sget v0, Lcom/myscript/nebo/onboarding/R$string;->onboarding_sample_notebook2024_name:I
    invoke-virtual {p0, v0}, Lcom/myscript/nebo/init/InitContentActivity;->getString(I)Ljava/lang/String;
    move-result-object v4
    invoke-static {v4, p1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {p0}, Lcom/myscript/nebo/init/InitContentActivity;->getResources()Landroid/content/res/Resources;
    move-result-object p1
    sget v0, Lcom/myscript/nebo/common/R$color;->default_notebook_cover_color:I
    invoke-virtual {p0}, Lcom/myscript/nebo/init/InitContentActivity;->getTheme()Landroid/content/res/Resources$Theme;
    move-result-object v5
    invoke-static {p1, v0, v5}, Landroidx/core/content/res/ResourcesCompat;->getColor(Landroid/content/res/Resources;ILandroid/content/res/Resources$Theme;)I
    move-result v5
    invoke-direct {p0}, Lcom/myscript/nebo/init/InitContentActivity;->getViewModel()Lcom/myscript/nebo/init/InitContentViewModel;
    move-result-object v0
    invoke-virtual/range {v0 .. v5}, Lcom/myscript/nebo/init/InitContentViewModel;->createInitialContent(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;I)V
    :cond_7f
    return-void
.end method
