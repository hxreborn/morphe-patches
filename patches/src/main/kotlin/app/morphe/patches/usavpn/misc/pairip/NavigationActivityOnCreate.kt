/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.pairip

import app.morphe.patcher.resource.ResourceType

internal fun navigationOnCreate(): String {
    val drawerItems = listOf("l", "m", "k", "o", "j", "p", "t", "u", "n", "q", "v").joinToString("") { field ->
        """
            iget-object v0, p0, $NAVIGATION->$field:Landroid/widget/TextView;
            if-eqz v0, :item_$field
            invoke-virtual {v0, p0}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
            :item_$field
        """
    }
    val textViews = listOf(
        "h" to "activity_navigation_version_navigation_view",
        "i" to "toolbar_title",
        "j" to "activity_navigation_connection_item",
        "k" to "activity_navigation_remove_ad_item",
        "l" to "activity_navigation_server_list_item",
        "m" to "activity_navigation_history_list_item",
        "o" to "activity_navigation_settings_item",
        "p" to "activity_navigation_diagnostic_item",
        "t" to "activity_navigation_support_us_item",
        "u" to "activity_navigation_feedback_item",
        "n" to "activity_navigation_filter_item",
        "q" to "activity_navigation_faq_item",
        "v" to "activity_navigation_activate_code",
    ).joinToString("") { (field, name) ->
        """
            const v0, ${viewId(name)}
            invoke-virtual {p0, v0}, $FIND_VIEW
            move-result-object v0
            check-cast v0, Landroid/widget/TextView;
            iput-object v0, p0, $NAVIGATION->$field:Landroid/widget/TextView;
        """
    }

    return """
        invoke-static {p0}, Ldagger/android/AndroidInjection;->a(Landroid/app/Activity;)V
        invoke-super {p0, p1}, Landroidx/fragment/app/FragmentActivity;->onCreate(Landroid/os/Bundle;)V
        invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
        move-result-object v0
        invoke-virtual {v0}, Landroid/content/Intent;->getAction()Ljava/lang/String;
        move-result-object v0
        const-string v1, "orientation"
        invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
        move-result v0
        iput-boolean v0, p0, $NAVIGATION->J:Z
        const v0, ${resourceIdHex(ResourceType.LAYOUT, "activity_navigation")}
        invoke-virtual {p0, v0}, Landroidx/appcompat/app/AppCompatActivity;->setContentView(I)V
        new-instance v0, $SETTINGS
        new-instance v1, Lcom/google/gson/Gson;
        invoke-direct {v1}, Lcom/google/gson/Gson;-><init>()V
        invoke-direct {v0, p0, v1}, $SETTINGS-><init>(Landroid/content/Context;Lcom/google/gson/Gson;)V
        iput-object v0, p0, $NAVIGATION->C:$SETTINGS
        const v0, ${viewId("searchReserve")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        iput-object v0, p0, $NAVIGATION->K:Landroid/view/View;
        invoke-virtual {p0}, Landroidx/appcompat/app/AppCompatActivity;->getResources()Landroid/content/res/Resources;
        move-result-object v0
        invoke-virtual {v0}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;
        move-result-object v0
        iget v0, v0, Landroid/content/res/Configuration;->orientation:I
        const/4 v1, 0x2
        if-ne v0, v1, :views
        invoke-virtual {p0}, $NAVIGATION->s1()V
        invoke-static {p0}, Lcom/vpn/lib/util/ViewUtils;->a(Landroidx/appcompat/app/AppCompatActivity;)I
        move-result v3
        const v0, ${viewId("statusBar")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        if-eqz v0, :landscape_drawer
        invoke-virtual {v0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
        move-result-object v0
        iput v3, v0, Landroid/view/ViewGroup${'$'}LayoutParams;->height:I
        :landscape_drawer
        const v0, ${viewId("statusBarDrawer")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        if-eqz v0, :landscape_toolbar
        invoke-virtual {v0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
        move-result-object v0
        iput v3, v0, Landroid/view/ViewGroup${'$'}LayoutParams;->height:I
        :landscape_toolbar
        const v0, ${viewId("toolbar")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        if-eqz v0, :landscape_share
        const/4 v1, 0x0
        invoke-virtual {v0, v1, v3, v1, v1}, Landroid/view/View;->setPadding(IIII)V
        :landscape_share
        const v0, ${viewId("shareReserve")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        if-eqz v0, :landscape_search
        new-instance v1, Lcom/vpn/lib/feature/naviagation/e;
        const/4 v2, 0x2
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/e;-><init>(${NAVIGATION}I)V
        invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        :landscape_search
        iget-object v0, p0, $NAVIGATION->K:Landroid/view/View;
        if-eqz v0, :views
        new-instance v1, Lcom/vpn/lib/feature/naviagation/e;
        const/4 v2, 0x3
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/e;-><init>(${NAVIGATION}I)V
        invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        :views
        const v0, ${viewId("drawer_layout")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        check-cast v0, Landroidx/drawerlayout/widget/DrawerLayout;
        iput-object v0, p0, $NAVIGATION->e:Landroidx/drawerlayout/widget/DrawerLayout;
        const v0, ${viewId("toolbar")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        check-cast v0, Landroidx/appcompat/widget/Toolbar;
        iput-object v0, p0, $NAVIGATION->f:Landroidx/appcompat/widget/Toolbar;
        $textViews
        iget-object v0, p0, $NAVIGATION->v:Landroid/widget/TextView;
        const/16 v1, 0x8
        if-eqz v0, :activation_icon
        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
        :activation_icon
        const v0, ${viewId("activity_navigation_activate_code_icon")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        check-cast v0, Landroid/widget/ImageView;
        iput-object v0, p0, $NAVIGATION->w:Landroid/widget/ImageView;
        if-eqz v0, :buttons
        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
        :buttons
        const v0, ${viewId("bgDots")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        iput-object v0, p0, $NAVIGATION->y:Landroid/view/View;
        const v0, ${viewId("btnGetPro")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        iput-object v0, p0, $NAVIGATION->z:Landroid/view/View;
        if-eqz v0, :share
        new-instance v1, Lcom/vpn/lib/feature/naviagation/e;
        const/4 v2, 0x5
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/e;-><init>(${NAVIGATION}I)V
        invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        :share
        const v0, ${viewId("btnShare")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        iput-object v0, p0, $NAVIGATION->A:Landroid/view/View;
        if-eqz v0, :home
        new-instance v1, Lcom/vpn/lib/feature/naviagation/e;
        const/4 v2, 0x7
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/e;-><init>(${NAVIGATION}I)V
        invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        :home
        const v0, ${viewId("home")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        iput-object v0, p0, $NAVIGATION->x:Landroid/view/View;
        const v0, ${viewId("drawerCloseView")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        iput-object v0, p0, $NAVIGATION->B:Landroid/view/View;
        $drawerItems
        invoke-static {}, $APP->r()Z
        move-result v3
        iget-object v1, p0, $NAVIGATION->e:Landroidx/drawerlayout/widget/DrawerLayout;
        iget-object v2, p0, $NAVIGATION->f:Landroidx/appcompat/widget/Toolbar;
        if-eqz v3, :phone_toggle
        new-instance v0, Lcom/vpn/lib/feature/naviagation/NavigationActivity${'$'}1;
        invoke-direct {v0, p0, p0, v1, v2}, Lcom/vpn/lib/feature/naviagation/NavigationActivity${'$'}1;-><init>(${NAVIGATION}${NAVIGATION}Landroidx/drawerlayout/widget/DrawerLayout;Landroidx/appcompat/widget/Toolbar;)V
        goto :toggle
        :phone_toggle
        new-instance v0, Lcom/vpn/lib/feature/naviagation/NavigationActivity${'$'}2;
        invoke-direct {v0, p0, p0, v1, v2}, Lcom/vpn/lib/feature/naviagation/NavigationActivity${'$'}2;-><init>(${NAVIGATION}${NAVIGATION}Landroidx/drawerlayout/widget/DrawerLayout;Landroidx/appcompat/widget/Toolbar;)V
        :toggle
        iput-object v0, p0, $NAVIGATION->g:Landroidx/appcompat/app/ActionBarDrawerToggle;
        iget-object v2, v1, Landroidx/drawerlayout/widget/DrawerLayout;->t:Ljava/util/ArrayList;
        if-nez v2, :listeners
        new-instance v2, Ljava/util/ArrayList;
        invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V
        iput-object v2, v1, Landroidx/drawerlayout/widget/DrawerLayout;->t:Ljava/util/ArrayList;
        :listeners
        invoke-virtual {v2, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
        invoke-virtual {v0}, Landroidx/appcompat/app/ActionBarDrawerToggle;->g()V
        if-nez v3, :emergency
        iget-object v0, p0, $NAVIGATION->f:Landroidx/appcompat/widget/Toolbar;
        const v1, ${resourceIdHex(ResourceType.MENU, "navigation")}
        invoke-virtual {v0, v1}, Landroidx/appcompat/widget/Toolbar;->n(I)V
        new-instance v1, Lcom/vpn/lib/feature/naviagation/c;
        const/4 v2, 0x4
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/c;-><init>(${NAVIGATION}I)V
        invoke-virtual {v0, v1}, Landroidx/appcompat/widget/Toolbar;->setOnMenuItemClickListener(Landroidx/appcompat/widget/Toolbar${'$'}OnMenuItemClickListener;)V
        :emergency
        sget-object v0, Lcom/vpn/lib/troubleshooting/EmergencyModeProvider;->a:Lcom/vpn/lib/troubleshooting/EmergencyMode;
        iput-object v0, p0, $NAVIGATION->H:Lcom/vpn/lib/troubleshooting/EmergencyMode;
        if-eqz v0, :presenter
        new-instance v0, Lcom/github/shadowsocks/utils/StartService;
        invoke-direct {v0}, Landroidx/activity/result/contract/ActivityResultContract;-><init>()V
        new-instance v1, Lcom/vpn/lib/feature/naviagation/c;
        const/4 v2, 0x5
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/c;-><init>(${NAVIGATION}I)V
        invoke-virtual {p0, v0, v1}, Landroidx/activity/ComponentActivity;->registerForActivityResult(Landroidx/activity/result/contract/ActivityResultContract;Landroidx/activity/result/ActivityResultCallback;)Landroidx/activity/result/ActivityResultLauncher;
        move-result-object v0
        iput-object v0, p0, $NAVIGATION->I:Landroidx/activity/result/ActivityResultLauncher;
        :presenter
        iget-object v0, p0, $NAVIGATION->c:Lcom/vpn/lib/feature/naviagation/NavigationPresenter;
        invoke-interface {v0, p0}, Lcom/vpn/lib/feature/base/Presenter;->e0(Lcom/vpn/lib/feature/base/BaseView;)V
        invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
        move-result-object v0
        if-eqz v0, :pro
        const-string v1, "open_code_activation"
        const/4 v2, 0x0
        invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->getBooleanExtra(Ljava/lang/String;Z)Z
        move-result v4
        if-eqz v4, :intent_action
        invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Z)Landroid/content/Intent;
        invoke-virtual {p0, v0}, Landroid/app/Activity;->setIntent(Landroid/content/Intent;)V
        invoke-virtual {p0}, $NAVIGATION->m1()V
        :intent_action
        const-string v1, "action"
        invoke-virtual {v0, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
        move-result-object v1
        if-eqz v1, :intent_code
        iget-object v2, p0, $NAVIGATION->c:Lcom/vpn/lib/feature/naviagation/NavigationPresenter;
        invoke-interface {v2, v1}, Lcom/vpn/lib/feature/naviagation/NavigationPresenter;->f(Ljava/lang/String;)V
        :intent_code
        const-string v1, "code"
        invoke-virtual {v0, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
        move-result-object v1
        if-eqz v1, :pro
        invoke-virtual {p0}, $NAVIGATION->A()V
        iget-object v0, p0, $NAVIGATION->F:Lcom/vpn/lib/dialog/CodeActivationDialog;
        invoke-virtual {v0, v1}, Lcom/vpn/lib/dialog/CodeActivationDialog;->setCode(Ljava/lang/String;)V
        :pro
        const/4 v4, 0x1
        sput-boolean v4, $APP->P:Z
        if-eqz v3, :post
        iget-object v0, p0, $NAVIGATION->x:Landroid/view/View;
        if-eqz v0, :tv_close
        invoke-virtual {v0}, Landroid/view/View;->requestFocus()Z
        invoke-virtual {p0, v4}, $NAVIGATION->r1(Z)V
        new-instance v1, Lcom/vpn/lib/feature/naviagation/d;
        const/4 v2, 0x0
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/d;-><init>(${NAVIGATION}I)V
        invoke-virtual {v0, v1}, Landroid/view/View;->setOnFocusChangeListener(Landroid/view/View${'$'}OnFocusChangeListener;)V
        new-instance v1, Lcom/vpn/lib/feature/naviagation/e;
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/e;-><init>(${NAVIGATION}I)V
        invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        const v0, ${viewId("forceHome")}
        invoke-virtual {p0, v0}, $FIND_VIEW
        move-result-object v0
        new-instance v1, Lcom/vpn/lib/feature/naviagation/d;
        invoke-direct {v1, p0, v4}, Lcom/vpn/lib/feature/naviagation/d;-><init>(${NAVIGATION}I)V
        invoke-virtual {v0, v1}, Landroid/view/View;->setOnFocusChangeListener(Landroid/view/View${'$'}OnFocusChangeListener;)V
        :tv_close
        iget-object v0, p0, $NAVIGATION->B:Landroid/view/View;
        if-eqz v0, :post
        invoke-virtual {v0, p0}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
        new-instance v1, Lcom/vpn/lib/feature/naviagation/d;
        const/4 v2, 0x2
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/d;-><init>(${NAVIGATION}I)V
        invoke-virtual {v0, v1}, Landroid/view/View;->setOnFocusChangeListener(Landroid/view/View${'$'}OnFocusChangeListener;)V
        :post
        invoke-static {p0}, $APP->q(Landroid/content/Context;)V
        new-instance v0, Landroid/os/Handler;
        invoke-direct {v0}, Landroid/os/Handler;-><init>()V
        new-instance v1, Lcom/vpn/lib/feature/naviagation/a;
        const/4 v2, 0x4
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/a;-><init>(${NAVIGATION}I)V
        const-wide/16 v5, 0x1f4
        invoke-virtual {v0, v1, v5, v6}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z
        iget-boolean v0, p0, $NAVIGATION->J:Z
        if-eqz v0, :welcome
        invoke-virtual {p0}, Landroid/app/Activity;->getApplication()Landroid/app/Application;
        move-result-object v0
        check-cast v0, $APP
        iget-object v0, v0, $APP->d:Lcom/vpn/lib/AppOpenManager;
        invoke-virtual {v0}, Lcom/vpn/lib/AppOpenManager;->a()V
        invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
        move-result-object v0
        const-string v1, ""
        invoke-virtual {v0, v1}, Landroid/content/Intent;->setAction(Ljava/lang/String;)Landroid/content/Intent;
        :welcome
        if-nez p1, :refresh
        invoke-virtual {p0}, $NAVIGATION->w1()V
        :refresh
        new-instance v0, Landroid/os/Handler;
        invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
        move-result-object v1
        invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
        new-instance v1, Lcom/vpn/lib/feature/naviagation/a;
        const/16 v2, 0x8
        invoke-direct {v1, p0, v2}, Lcom/vpn/lib/feature/naviagation/a;-><init>(${NAVIGATION}I)V
        const-wide/16 v5, 0x3e8
        invoke-virtual {v0, v1, v5, v6}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z
        sget v0, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
        const/16 v1, 0x21
        if-lt v0, v1, :done
        const-string v0, "android.permission.POST_NOTIFICATIONS"
        invoke-static {p0, v0}, Landroidx/core/content/ContextCompat;->a(Landroid/content/Context;Ljava/lang/String;)I
        move-result v1
        if-eqz v1, :done
        filled-new-array {v0}, [Ljava/lang/String;
        move-result-object v0
        const/16 v1, 0x65
        invoke-virtual {p0, v0, v1}, Landroid/app/Activity;->requestPermissions([Ljava/lang/String;I)V
        :done
        const/4 v0, 0x0
        iput-boolean v0, p0, $NAVIGATION->J:Z
        return-void
    """
}
