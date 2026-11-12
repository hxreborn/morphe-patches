.class public Lcom/myscript/nebo/sso/ui/login/LoginActivity;
.super Ljava/lang/Object;

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 9

    invoke-super {p0, p1}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V

    invoke-virtual {p0}, Lcom/myscript/nebo/sso/ui/login/LoginActivity;->getIntent()Landroid/content/Intent;

    move-result-object p1

    const/4 v0, 0x0

    if-eqz p1, :cond_11

    const-string v1, "COMPATIBLE_BROWSER_PACKAGE_NAME"

    invoke-virtual {p1, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    goto :goto_12

    :cond_11
    move-object p1, v0

    :goto_12
    if-eqz p1, :cond_2f

    invoke-direct {p0}, Lcom/myscript/nebo/sso/ui/login/LoginActivity;->getLoginViewModel()Lcom/myscript/nebo/sso/ui/login/LoginViewModel;

    move-result-object v0

    invoke-virtual {v0}, Lcom/myscript/nebo/sso/ui/login/LoginViewModel;->getOpenLoginTab()Lcom/myscript/android/utils/SingleLiveEvent;

    move-result-object v0

    move-object v1, p0

    check-cast v1, Landroidx/lifecycle/LifecycleOwner;

    new-instance v2, Lstub/LoginResultCallback;

    invoke-direct {v2, p0, p1}, Lstub/LoginResultCallback;-><init>(Lcom/myscript/nebo/sso/ui/login/LoginActivity;Ljava/lang/String;)V

    new-instance p0, Lstub/LoginObserver;

    invoke-direct {p0, v2}, Lstub/LoginObserver;-><init>(Lkotlin/jvm/functions/Function1;)V

    check-cast p0, Landroidx/lifecycle/Observer;

    invoke-virtual {v0, v1, p0}, Lcom/myscript/android/utils/SingleLiveEvent;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    return-void

    :cond_2f
    move-object p1, p0

    check-cast p1, Landroidx/lifecycle/LifecycleOwner;

    invoke-static {p1}, Landroidx/lifecycle/LifecycleOwnerKt;->getLifecycleScope(Landroidx/lifecycle/LifecycleOwner;)Landroidx/lifecycle/LifecycleCoroutineScope;

    move-result-object p1

    move-object v1, p1

    check-cast v1, Lkotlinx/coroutines/CoroutineScope;

    new-instance p1, Lstub/LoginStateCallback;

    invoke-direct {p1, p0, v0}, Lstub/LoginStateCallback;-><init>(Lcom/myscript/nebo/sso/ui/login/LoginActivity;Lkotlin/coroutines/Continuation;)V

    move-object v4, p1

    check-cast v4, Lkotlin/jvm/functions/Function2;

    const/4 v5, 0x3

    const/4 v6, 0x0

    const/4 v2, 0x0

    const/4 v3, 0x0

    invoke-static/range {v1 .. v6}, Lkotlinx/coroutines/BuildersKt;->launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;

    return-void
.end method
