.class public Lcom/myscript/nebo/editing/StylusDoubleTapReceiverImpl;
.super Ljava/lang/Object;

.method public onReceive(Landroid/content/Context;Landroid/content/Intent;)V
    .registers 6

    const-string v0, "context"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const/4 v0, 0x0

    if-eqz p2, :cond_d

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v1

    goto :goto_e

    :cond_d
    move-object v1, v0

    :goto_e
    const-string v2, "com.huawei.stylus.action.BUTTON_DOUBLE_PRESSED"

    invoke-static {v2, v1}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_39

    invoke-virtual {p1}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;

    move-result-object p1

    invoke-virtual {p1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object p1

    const-string p2, "double_click_switch_mode"

    const/4 v0, 0x0

    invoke-static {p1, p2, v0}, Landroid/provider/Settings$Global;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result p1

    if-eqz p1, :cond_36

    const/4 p2, 0x1

    if-eq p1, p2, :cond_33

    const/4 p2, 0x2

    if-eq p1, p2, :cond_30

    sget-object p1, Lcom/myscript/nebo/editing/StylusSwitchBehavior;->NONE:Lcom/myscript/nebo/editing/StylusSwitchBehavior;

    goto :goto_4c

    :cond_30
    sget-object p1, Lcom/myscript/nebo/editing/StylusSwitchBehavior;->COLOR_PALETTE:Lcom/myscript/nebo/editing/StylusSwitchBehavior;

    goto :goto_4c

    :cond_33
    sget-object p1, Lcom/myscript/nebo/editing/StylusSwitchBehavior;->CURRENT_AND_TOOL:Lcom/myscript/nebo/editing/StylusSwitchBehavior;

    goto :goto_4c

    :cond_36
    sget-object p1, Lcom/myscript/nebo/editing/StylusSwitchBehavior;->CURRENT_AND_ERASER:Lcom/myscript/nebo/editing/StylusSwitchBehavior;

    goto :goto_4c

    :cond_39
    if-eqz p2, :cond_3f

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v0

    :cond_3f
    const-string p1, "com.hihonor.stylus.action.STYLUS_DOUBLE_CLICK"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_4a

    sget-object p1, Lcom/myscript/nebo/editing/StylusSwitchBehavior;->CURRENT_AND_ERASER:Lcom/myscript/nebo/editing/StylusSwitchBehavior;

    goto :goto_4c

    :cond_4a
    sget-object p1, Lcom/myscript/nebo/editing/StylusSwitchBehavior;->NONE:Lcom/myscript/nebo/editing/StylusSwitchBehavior;

    :goto_4c
    iget-object p0, p0, Lcom/myscript/nebo/editing/StylusDoubleTapReceiverImpl;->stylusDoubleTapListener:Lcom/myscript/nebo/editing/IStylusDoubleTapListener;

    if-eqz p0, :cond_53

    invoke-interface {p0, p1}, Lcom/myscript/nebo/editing/IStylusDoubleTapListener;->stylusDoubleTap(Lcom/myscript/nebo/editing/StylusSwitchBehavior;)V

    :cond_53
    return-void
.end method
