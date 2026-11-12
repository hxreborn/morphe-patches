.class public Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;
.super Ljava/lang/Object;

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 11

    const/4 v0, 0x1

    invoke-virtual {p0, v0}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;->requestWindowFeature(I)Z

    invoke-super {p0, p1}, Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V

    move-object v1, p0

    check-cast v1, Landroid/content/Context;

    invoke-static {v1, p1}, Lcom/revenuecat/purchases/ui/revenuecatui/helpers/SdkConfigurationStateKt;->restoreSdkConfigurationIfNeeded(Landroid/content/Context;Landroid/os/Bundle;)V

    invoke-direct {p0}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;->getArgs()Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityArgs;

    move-result-object v5

    if-eqz v5, :cond_18

    invoke-virtual {v5}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityArgs;->getWasLaunchedThroughSDK()Z

    move-result p1

    goto :goto_19

    :cond_18
    const/4 p1, 0x0

    :goto_19
    if-nez p1, :cond_2e

    sget-object p1, Lcom/revenuecat/purchases/Purchases;->Companion:Lcom/revenuecat/purchases/Purchases$Companion;

    invoke-virtual {p1}, Lcom/revenuecat/purchases/Purchases$Companion;->isConfigured()Z

    move-result p1

    if-nez p1, :cond_2e

    sget-object p1, Lcom/revenuecat/purchases/ui/revenuecatui/helpers/Logger;->INSTANCE:Lcom/revenuecat/purchases/ui/revenuecatui/helpers/Logger;

    const-string v0, "PaywallActivity was launched incorrectly. Please use PaywallActivityLauncher, or Paywall/PaywallDialog/PaywallFooter composables to display the Paywall."

    invoke-virtual {p1, v0}, Lcom/revenuecat/purchases/ui/revenuecatui/helpers/Logger;->e(Ljava/lang/String;)V

    invoke-virtual {p0}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;->finish()V

    return-void

    :cond_2e
    const/4 p1, 0x0

    if-eqz v5, :cond_44

    invoke-virtual {v5}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityArgs;->getNonSerializableArgsKey()Ljava/lang/Integer;

    move-result-object v1

    if-eqz v1, :cond_44

    check-cast v1, Ljava/lang/Number;

    invoke-virtual {v1}, Ljava/lang/Number;->intValue()I

    move-result v1

    sget-object v2, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityNonSerializableArgsStore;->INSTANCE:Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityNonSerializableArgsStore;

    invoke-virtual {v2, v1}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityNonSerializableArgsStore;->get(I)Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityNonSerializableArgs;

    move-result-object v1

    goto :goto_45

    :cond_44
    move-object v1, p1

    :goto_45
    if-eqz v5, :cond_4c

    invoke-virtual {v5}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityArgs;->getNonSerializableArgsKey()Ljava/lang/Integer;

    move-result-object v2

    goto :goto_4d

    :cond_4c
    move-object v2, p1

    :goto_4d
    if-eqz v2, :cond_68

    if-nez v1, :cond_68

    sget-object p1, Lcom/revenuecat/purchases/ui/revenuecatui/helpers/Logger;->INSTANCE:Lcom/revenuecat/purchases/ui/revenuecatui/helpers/Logger;

    const-string v0, "PaywallActivity was recreated after process death causing PurchaseLogic and/or PaywallListener to be lost. Finishing activity."

    invoke-virtual {p1, v0}, Lcom/revenuecat/purchases/ui/revenuecatui/helpers/Logger;->w(Ljava/lang/String;)V

    sget-object p1, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallResult$Cancelled;->INSTANCE:Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallResult$Cancelled;

    check-cast p1, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallResult;

    invoke-direct {p0, p1}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;->createResultIntent(Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallResult;)Landroid/content/Intent;

    move-result-object p1

    const/4 v0, -0x1

    invoke-virtual {p0, v0, p1}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;->setResult(ILandroid/content/Intent;)V

    invoke-virtual {p0}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;->finish()V

    return-void

    :cond_68
    if-eqz v1, :cond_6f

    invoke-virtual {v1}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityNonSerializableArgs;->getListener()Lcom/revenuecat/purchases/ui/revenuecatui/PaywallListener;

    move-result-object v2

    goto :goto_70

    :cond_6f
    move-object v2, p1

    :goto_70
    if-eqz v1, :cond_78

    invoke-virtual {v1}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityNonSerializableArgs;->getPurchaseLogic()Lcom/revenuecat/purchases/ui/revenuecatui/PaywallPurchaseLogic;

    move-result-object v1

    move-object v7, v1

    goto :goto_79

    :cond_78
    move-object v7, p1

    :goto_79
    new-instance v6, Lstub/PaywallListener;

    invoke-direct {v6, v2, p0}, Lstub/PaywallListener;-><init>(Lcom/revenuecat/purchases/ui/revenuecatui/PaywallListener;Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;)V

    if-eqz v5, :cond_8d

    invoke-virtual {v5}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityArgs;->getEdgeToEdge()Z

    move-result v1

    if-ne v1, v0, :cond_8d

    move-object v1, p0

    check-cast v1, Landroidx/activity/ComponentActivity;

    const/4 v2, 0x3

    invoke-static {v1, p1, p1, v2, p1}, Landroidx/activity/EdgeToEdge;->enable$default(Landroidx/activity/ComponentActivity;Landroidx/activity/SystemBarStyle;Landroidx/activity/SystemBarStyle;ILjava/lang/Object;)V

    :cond_8d
    if-eqz v5, :cond_95

    invoke-virtual {v5}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityArgs;->getOfferingIdAndPresentedOfferingContext()Lcom/revenuecat/purchases/ui/revenuecatui/OfferingSelection$IdAndPresentedOfferingContext;

    move-result-object v1

    move-object v4, v1

    goto :goto_96

    :cond_95
    move-object v4, p1

    :goto_96
    if-eqz v5, :cond_a4

    invoke-virtual {v5}, Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityArgs;->getRequiredEntitlementIdentifier()Ljava/lang/String;

    move-result-object v1

    if-eqz v1, :cond_a4

    invoke-static {v1}, Lcom/revenuecat/purchases/ui/revenuecatui/helpers/HelperFunctionsKt;->shouldDisplayBlockForEntitlementIdentifier(Ljava/lang/String;)Lkotlin/jvm/functions/Function1;

    move-result-object v1

    move-object v8, v1

    goto :goto_a5

    :cond_a4
    move-object v8, p1

    :goto_a5
    move-object v1, p0

    check-cast v1, Landroidx/activity/ComponentActivity;

    new-instance v2, Lstub/PaywallContent;

    move-object v3, p0

    invoke-direct/range {v2 .. v8}, Lstub/PaywallContent;-><init>(Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;Lcom/revenuecat/purchases/ui/revenuecatui/OfferingSelection$IdAndPresentedOfferingContext;Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityArgs;Lstub/PaywallListener;Lcom/revenuecat/purchases/ui/revenuecatui/PaywallPurchaseLogic;Lkotlin/jvm/functions/Function1;)V

    const p0, 0x792120a4

    invoke-static {p0, v0, v2}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->composableLambdaInstance(IZLjava/lang/Object;)Landroidx/compose/runtime/internal/ComposableLambda;

    move-result-object p0

    check-cast p0, Lkotlin/jvm/functions/Function2;

    invoke-static {v1, p1, p0, v0, p1}, Landroidx/activity/compose/ComponentActivityKt;->setContent$default(Landroidx/activity/ComponentActivity;Landroidx/compose/runtime/CompositionContext;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)V

    return-void
.end method
