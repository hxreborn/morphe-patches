.class public Lcom/myscript/nebo/sso/ui/WebViewActivity;
.super Ljava/lang/Object;

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 9

    invoke-super {p0, p1}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V

    invoke-virtual {p0}, Lcom/myscript/nebo/sso/ui/WebViewActivity;->getLayoutInflater()Landroid/view/LayoutInflater;

    move-result-object v0

    invoke-static {v0}, Lcom/myscript/nebo/sso/databinding/WebViewLayoutBinding;->inflate(Landroid/view/LayoutInflater;)Lcom/myscript/nebo/sso/databinding/WebViewLayoutBinding;

    move-result-object v0

    const-string v1, "inflate(...)"

    invoke-static {v0, v1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    iput-object v0, p0, Lcom/myscript/nebo/sso/ui/WebViewActivity;->binding:Lcom/myscript/nebo/sso/databinding/WebViewLayoutBinding;

    const/4 v1, 0x0

    const-string v2, "binding"

    if-nez v0, :cond_1b

    invoke-static {v2}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    move-object v0, v1

    :cond_1b
    invoke-virtual {v0}, Lcom/myscript/nebo/sso/databinding/WebViewLayoutBinding;->getRoot()Landroid/widget/FrameLayout;

    move-result-object v0

    check-cast v0, Landroid/view/View;

    invoke-virtual {p0, v0}, Lcom/myscript/nebo/sso/ui/WebViewActivity;->setContentView(Landroid/view/View;)V

    invoke-virtual {p0}, Lcom/myscript/nebo/sso/ui/WebViewActivity;->getActionBar()Landroid/app/ActionBar;

    move-result-object v0

    const/4 v3, 0x1

    if-eqz v0, :cond_2e

    invoke-virtual {v0, v3}, Landroid/app/ActionBar;->setDisplayHomeAsUpEnabled(Z)V

    :cond_2e
    invoke-virtual {p0, v3}, Lcom/myscript/nebo/sso/ui/WebViewActivity;->setChromeVisible(Z)V

    iget-object v0, p0, Lcom/myscript/nebo/sso/ui/WebViewActivity;->binding:Lcom/myscript/nebo/sso/databinding/WebViewLayoutBinding;

    if-nez v0, :cond_39

    invoke-static {v2}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    move-object v0, v1

    :cond_39
    iget-object v0, v0, Lcom/myscript/nebo/sso/databinding/WebViewLayoutBinding;->webViewWebView:Landroid/webkit/WebView;

    new-instance v4, Lstub/WebViewClient;

    invoke-direct {v4, p0}, Lstub/WebViewClient;-><init>(Lcom/myscript/nebo/sso/ui/WebViewActivity;)V

    check-cast v4, Landroid/webkit/WebViewClient;

    invoke-virtual {v0, v4}, Landroid/webkit/WebView;->setWebViewClient(Landroid/webkit/WebViewClient;)V

    invoke-virtual {p0}, Lcom/myscript/nebo/sso/ui/WebViewActivity;->clearCache()V

    const-string v4, "http.agent"

    invoke-static {v4}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    move-object v5, v4

    check-cast v5, Ljava/lang/CharSequence;

    if-eqz v5, :cond_59

    invoke-interface {v5}, Ljava/lang/CharSequence;->length()I

    move-result v5

    if-nez v5, :cond_5b

    :cond_59
    const-string v4, "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.101 Mobile Safari/537.36"

    :cond_5b
    invoke-virtual {v0}, Landroid/webkit/WebView;->getSettings()Landroid/webkit/WebSettings;

    move-result-object v5

    invoke-virtual {v5, v4}, Landroid/webkit/WebSettings;->setUserAgentString(Ljava/lang/String;)V

    invoke-virtual {v0}, Landroid/webkit/WebView;->getSettings()Landroid/webkit/WebSettings;

    move-result-object v4

    invoke-virtual {v4, v3}, Landroid/webkit/WebSettings;->setJavaScriptEnabled(Z)V

    const-string v4, "FORCE_DARK"

    invoke-static {v4}, Landroidx/webkit/WebViewFeature;->isFeatureSupported(Ljava/lang/String;)Z

    move-result v4

    const/4 v5, 0x0

    if-eqz v4, :cond_97

    invoke-virtual {v0}, Landroid/webkit/WebView;->getSettings()Landroid/webkit/WebSettings;

    move-result-object v4

    invoke-static {v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V

    move-object v6, v0

    check-cast v6, Landroid/view/View;

    invoke-static {v6}, Lcom/myscript/android/utils/DarkModeUtils;->isDarkMode(Landroid/view/View;)Z

    move-result v6

    if-eqz v6, :cond_84

    const/4 v6, 0x2

    goto :goto_85

    :cond_84
    move v6, v5

    :goto_85
    invoke-static {v4, v6}, Landroidx/webkit/WebSettingsCompat;->setForceDark(Landroid/webkit/WebSettings;I)V

    const-string v4, "FORCE_DARK_STRATEGY"

    invoke-static {v4}, Landroidx/webkit/WebViewFeature;->isFeatureSupported(Ljava/lang/String;)Z

    move-result v4

    if-eqz v4, :cond_97

    invoke-virtual {v0}, Landroid/webkit/WebView;->getSettings()Landroid/webkit/WebSettings;

    move-result-object v0

    invoke-static {v0, v3}, Landroidx/webkit/WebSettingsCompat;->setForceDarkStrategy(Landroid/webkit/WebSettings;I)V

    :cond_97
    invoke-direct {p0}, Lcom/myscript/nebo/sso/ui/WebViewActivity;->updateNavigationButton()V

    if-eqz p1, :cond_ad

    iget-object v0, p0, Lcom/myscript/nebo/sso/ui/WebViewActivity;->binding:Lcom/myscript/nebo/sso/databinding/WebViewLayoutBinding;

    if-nez v0, :cond_a4

    invoke-static {v2}, Lkotlin/jvm/internal/Intrinsics;->throwUninitializedPropertyAccessException(Ljava/lang/String;)V

    goto :goto_a5

    :cond_a4
    move-object v1, v0

    :goto_a5
    iget-object v0, v1, Lcom/myscript/nebo/sso/databinding/WebViewLayoutBinding;->webViewWebView:Landroid/webkit/WebView;

    invoke-virtual {v0, p1}, Landroid/webkit/WebView;->restoreState(Landroid/os/Bundle;)Landroid/webkit/WebBackForwardList;

    move-result-object p1

    if-nez p1, :cond_b6

    :cond_ad
    invoke-virtual {p0}, Lcom/myscript/nebo/sso/ui/WebViewActivity;->shouldLoadPageAtStartup()Z

    move-result p1

    if-eqz p1, :cond_b6

    invoke-direct {p0}, Lcom/myscript/nebo/sso/ui/WebViewActivity;->loadUrl()V

    :cond_b6
    iput-boolean v5, p0, Lcom/myscript/nebo/sso/ui/WebViewActivity;->isLeaving:Z

    return-void
.end method
