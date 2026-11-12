.class public Lcom/myscript/nebo/about/CreditsActivity;
.super Ljava/lang/Object;

.method public onCreate(Landroid/os/Bundle;)V
    .registers 5

    invoke-super {p0, p1}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V

    invoke-virtual {p0}, Lcom/myscript/nebo/about/CreditsActivity;->getWindow()Landroid/view/Window;

    move-result-object p1

    const/16 v0, 0x8

    invoke-virtual {p1, v0}, Landroid/view/Window;->requestFeature(I)Z

    sget p1, Lcom/myscript/nebo/R$string;->about_credits:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/about/CreditsActivity;->setTitle(I)V

    sget p1, Lcom/myscript/nebo/R$layout;->credits_activity:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/about/CreditsActivity;->setContentView(I)V

    sget p1, Lcom/myscript/nebo/R$id;->credit_detail_container:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/about/CreditsActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    iput-object p1, p0, Lcom/myscript/nebo/about/CreditsActivity;->mDetailPane:Landroid/view/View;

    sget p1, Lcom/myscript/nebo/R$id;->credits_list:I

    invoke-virtual {p0, p1}, Lcom/myscript/nebo/about/CreditsActivity;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroidx/recyclerview/widget/RecyclerView;

    new-instance v0, Lcom/myscript/nebo/about/CreditsActivity$CreditsAdapter;

    iget-object v1, p0, Lcom/myscript/nebo/about/CreditsActivity;->mDetailPane:Landroid/view/View;

    const/4 v2, 0x1

    if-eqz v1, :cond_2f

    move v1, v2

    goto :goto_30

    :cond_2f
    const/4 v1, 0x0

    :goto_30
    invoke-direct {v0, p0, v1}, Lcom/myscript/nebo/about/CreditsActivity$CreditsAdapter;-><init>(Lcom/myscript/nebo/about/OnCreditClickListener;Z)V

    invoke-virtual {p1, v0}, Landroidx/recyclerview/widget/RecyclerView;->setAdapter(Landroidx/recyclerview/widget/RecyclerView$Adapter;)V

    new-instance v0, Landroidx/recyclerview/widget/DividerItemDecoration;

    invoke-direct {v0, p0, v2}, Landroidx/recyclerview/widget/DividerItemDecoration;-><init>(Landroid/content/Context;I)V

    invoke-virtual {p1, v0}, Landroidx/recyclerview/widget/RecyclerView;->addItemDecoration(Landroidx/recyclerview/widget/RecyclerView$ItemDecoration;)V

    invoke-virtual {p0}, Lcom/myscript/nebo/about/CreditsActivity;->getSupportActionBar()Landroidx/appcompat/app/ActionBar;

    move-result-object p0

    if-eqz p0, :cond_47

    invoke-virtual {p0, v2}, Landroidx/appcompat/app/ActionBar;->setDisplayHomeAsUpEnabled(Z)V

    :cond_47
    return-void
.end method
