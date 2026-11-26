.class public final LVlloRestored;
.super Ljava/lang/Object;

# interfaces
.implements Landroid/view/View$OnApplyWindowInsetsListener;


# static fields
.field private static final APP_CONFIG_PREFERENCES:Ljava/lang/String; = "com.vimosoft.vimo.appConfig"

.field private static final AUXILIARY_ROUTER_MODE:I = 0x3

.field private static final CACHE_FACTORY_VARIANT:I = 0x0

.field private static final COROUTINE_DEFAULT_ARGUMENTS:I = 0x3

.field private static final DATABASE_VERSION:I = 0x1

.field private static final DEFAULT_SAMPLE_RATE:I = 0xac44

.field private static final DEVICE_CHECK_VARIANT:I = 0x8

.field private static final FILE_DATA_SOURCE_VARIANT:I = 0x0

.field private static final GIF_IMAGE_HANDLER_VARIANT:I = 0x0

.field private static final GIF_PLAYER_FACTORY_VARIANT:I = 0x2

.field private static final GROUND_SURFACE_AUXILIARY_VARIANT:I = 0x1

.field private static final GROUND_SURFACE_MAIN_VARIANT:I = 0x0

.field private static final GROUND_SURFACE_SIZE:I = 0x10

.field private static final HTTP_DATA_SOURCE_VARIANT:I = 0x11

.field private static final IMMERSIVE_FLAGS_BEFORE_API_30:I = 0x1307

.field private static final IMMERSIVE_REAPPLY_DELAY_MS:J = 0x64L

.field private static final KEY_INSTALL_TIME:Ljava/lang/String; = "kAPP_CONFIG_INSTALL_TIME"

.field private static final KEY_LAUNCH_COUNT:Ljava/lang/String; = "kAPP_CONFIG_LAUNCH_COUNT"

.field private static final KEY_USER_ID:Ljava/lang/String; = "userId"

.field private static final LAYOUT_LISTENER_ACTIVITY_VARIANT:I = 0x3

.field private static final MEDIA_CACHE_DATABASE:Ljava/lang/String; = "media-cache.db"

.field private static final MEDIA_CACHE_DIRECTORY:Ljava/lang/String; = "media-cache"

.field private static final MERGED_CLASS_VARIANT:I = 0x0

.field private static final SPLASH_TRANSACTION_INDEX:I = -0x1

.field private static final STOP_ACTION:Ljava/lang/String; = "ACTION_STOP"

.field private static final UNKNOWN_VERSION:Ljava/lang/String; = "unknown"


# instance fields
.field private final statusBarMargin:Landroid/view/View;


# direct methods
.method private constructor <init>(Landroid/view/View;)V
    .registers 2

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, LVlloRestored;->statusBarMargin:Landroid/view/View;

    return-void
.end method

.method public static activityCreated(Lcom/darinsoft/vimo/VLLOMainActivity;Landroid/os/Bundle;IIIIII)V
    .registers 21

    sget-boolean v0, Lcom/darinsoft/vimo/VLLOMainActivity;->h0:Z

    const/4 v1, 0x1

    const/4 v2, 0x0

    if-nez v0, :cond_0

    if-eqz p1, :cond_0

    move p1, v1

    goto :goto_0

    :cond_0
    move p1, v2

    :goto_0
    iput-boolean p1, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->f0:Z

    sput-boolean v2, Lcom/darinsoft/vimo/VLLOMainActivity;->h0:Z

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getApplicationContext()Landroid/content/Context;

    move-result-object p1

    invoke-static {p0, p1}, LVlloRestored;->configureMediaCache(Lcom/darinsoft/vimo/VLLOMainActivity;Landroid/content/Context;)V

    invoke-static {p0}, LVlloRestored;->configureGifProvider(Lcom/darinsoft/vimo/VLLOMainActivity;)V

    invoke-static {p1}, Llx;->b(Landroid/content/Context;)Lgvb;

    move-result-object v0

    sput-object v0, Lepg;->r:Lgvb;

    invoke-static {p0}, LVlloRestored;->configureWindow(Lcom/darinsoft/vimo/VLLOMainActivity;)V

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    move/from16 v3, p7

    invoke-virtual {v0, v3}, Landroid/content/res/Resources;->getBoolean(I)Z

    move-result v0

    if-eqz v0, :cond_1

    goto :goto_1

    :cond_1
    const/4 v1, -0x1

    :goto_1
    invoke-virtual {p0, v1}, Lcom/darinsoft/vimo/VLLOMainActivity;->setRequestedOrientation(I)V

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getLayoutInflater()Landroid/view/LayoutInflater;

    move-result-object v0

    const/4 v1, 0x0

    invoke-virtual {v0, p2, v1, v2}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v4

    move/from16 p2, p3

    invoke-virtual {v4, p2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p2

    move-object v5, p2

    check-cast v5, Lcom/bluelinelabs/conductor/ChangeHandlerFrameLayout;

    move/from16 p2, p4

    invoke-virtual {v4, p2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p2

    move-object v6, p2

    check-cast v6, Lcom/bluelinelabs/conductor/ChangeHandlerFrameLayout;

    move/from16 p2, p5

    invoke-virtual {v4, p2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v7

    move/from16 p2, p6

    invoke-virtual {v4, p2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p2

    move-object v9, p2

    check-cast v9, Landroid/widget/FrameLayout;

    new-instance v3, Ldy4;

    const/4 v10, 0x1

    move-object v8, v4

    invoke-direct/range {v3 .. v10}, Ldy4;-><init>(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)V

    iput-object v3, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->e0:Ldy4;

    invoke-virtual {p0, v4}, Lcom/darinsoft/vimo/VLLOMainActivity;->setContentView(Landroid/view/View;)V

    new-instance p2, Lny0;

    const/4 v0, 0x3

    invoke-direct {p2, p0, v0}, Lny0;-><init>(Ljava/lang/Object;I)V

    invoke-virtual {v4, p2}, Landroid/view/View;->addOnLayoutChangeListener(Landroid/view/View$OnLayoutChangeListener;)V

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getResources()Landroid/content/res/Resources;

    move-result-object p2

    invoke-virtual {p2}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;

    move-result-object p2

    iget v1, p2, Landroid/util/DisplayMetrics;->widthPixels:I

    if-lez v1, :cond_2

    iget v1, p2, Landroid/util/DisplayMetrics;->heightPixels:I

    if-lez v1, :cond_2

    new-instance v1, Lcom/vimosoft/vimoutil/util/CGSize;

    iget v2, p2, Landroid/util/DisplayMetrics;->widthPixels:I

    iget p2, p2, Landroid/util/DisplayMetrics;->heightPixels:I

    invoke-direct {v1, v2, p2}, Lcom/vimosoft/vimoutil/util/CGSize;-><init>(II)V

    sput-object v1, Lso2;->h:Lcom/vimosoft/vimoutil/util/CGSize;

    :cond_2
    new-instance p2, LVlloRestored;

    invoke-direct {p2, v7}, LVlloRestored;-><init>(Landroid/view/View;)V

    invoke-virtual {v4, p2}, Landroid/view/View;->setOnApplyWindowInsetsListener(Landroid/view/View$OnApplyWindowInsetsListener;)V

    sput-object p1, Lby4;->g:Landroid/content/Context;

    sput-object v9, Lqm9;->a:Landroid/widget/FrameLayout;

    sget-boolean p2, Lso2;->d:Z

    if-eqz p2, :cond_3

    sget-boolean p2, Lso2;->e:Z

    if-eqz p2, :cond_3

    sget-boolean p2, Lso2;->f:Z

    if-nez p2, :cond_4

    :cond_3
    invoke-static {p0}, LVlloRestored;->setUpDeviceManager(Lcom/darinsoft/vimo/VLLOMainActivity;)V

    :cond_4
    check-cast v4, Landroid/view/ViewGroup;

    invoke-static {p1, v4}, LVlloRestored;->addGroundSurfaces(Landroid/content/Context;Landroid/view/ViewGroup;)V

    invoke-static {p0, v6}, Libg;->a(Lcom/darinsoft/vimo/VLLOMainActivity;Lcom/bluelinelabs/conductor/ChangeHandlerFrameLayout;)Lg8;

    move-result-object p1

    iput-object p1, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->c0:Lg8;

    iget-object p1, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->c0:Lg8;

    sput-object p1, Ljs1;->s0:Lg8;

    iget-object p1, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->c0:Lg8;

    invoke-virtual {p1}, Lg8;->k()Z

    move-result p1

    if-nez p1, :cond_5

    iget-object p1, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->c0:Lg8;

    new-instance v6, Lp28;

    new-instance v7, Lrt8;

    invoke-direct {v7}, Lrt8;-><init>()V

    const/4 v11, 0x0

    const/4 v12, -0x1

    const/4 v8, 0x0

    const/4 v9, 0x0

    const/4 v10, 0x0

    invoke-direct/range {v6 .. v12}, Lp28;-><init>(Lwr1;Ljava/lang/String;Lus1;Lus1;ZI)V

    invoke-virtual {p1, v6}, Lg8;->K(Lp28;)V

    :cond_5
    invoke-static {p0, v5}, Libg;->a(Lcom/darinsoft/vimo/VLLOMainActivity;Lcom/bluelinelabs/conductor/ChangeHandlerFrameLayout;)Lg8;

    move-result-object p1

    iput-object p1, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->d0:Lg8;

    iget-object p1, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->d0:Lg8;

    iput v0, p1, Lg8;->e:I

    iget-object p0, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->d0:Lg8;

    sput-object p0, Ljs1;->t0:Lg8;

    return-void
.end method

.method public static activityDestroyed()V
    .registers 2

    sget-object v0, Lky7;->a:Lky7;

    monitor-enter v0

    const/4 v1, 0x1

    :try_start_0
    sput-boolean v1, Lky7;->f:Z

    monitor-exit v0

    return-void

    :catchall_0
    move-exception v1

    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw v1
.end method

.method public static activityPaused()V
    .registers 0

    invoke-static {}, LVlloRestored;->markDeviceCheckAborted()V

    return-void
.end method

.method public static activityResumed(Lcom/darinsoft/vimo/VLLOMainActivity;)V
    .registers 5

    iget-object v0, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->Z:Landroid/os/Handler;

    iget-object v1, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->b0:Lrma;

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    iget-object v0, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->Z:Landroid/os/Handler;

    iget-object v1, p0, Lcom/darinsoft/vimo/VLLOMainActivity;->b0:Lrma;

    const-wide/16 v2, 0x64

    invoke-virtual {v0, v1, v2, v3}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    sget-object v0, Ldi2;->g:Ldi2;

    invoke-static {v0}, Lhcg;->a(Lkotlin/coroutines/CoroutineContext;)Llr1;

    move-result-object v0

    new-instance v1, Lrh6;

    const/16 v2, 0x8

    const/4 v3, 0x0

    invoke-direct {v1, p0, v3, v2}, Lrh6;-><init>(Landroid/content/Context;Lor1;I)V

    const/4 p0, 0x3

    invoke-static {v0, v3, v3, v1, p0}, Ll4g;->d(Lvz1;Lkotlin/coroutines/CoroutineContext;Lyz1;Lkotlin/jvm/functions/Function2;I)Ljv8;

    return-void
.end method

.method public static activitySplashInstalled(Lcom/darinsoft/vimo/VLLOMainActivity;)V
    .registers 2

    new-instance v0, Ltt8;

    invoke-direct {v0, p0}, Ltt8;-><init>(Lcom/darinsoft/vimo/VLLOMainActivity;)V

    invoke-virtual {v0}, Ltt8;->a0()V

    return-void
.end method

.method public static activityStopped()V
    .registers 0

    invoke-static {}, LVlloRestored;->markDeviceCheckAborted()V

    return-void
.end method

.method private static addGroundSurfaces(Landroid/content/Context;Landroid/view/ViewGroup;)V
    .registers 4

    const/4 v0, 0x0

    invoke-static {p0, v0}, LVlloRestored;->groundSurface(Landroid/content/Context;I)Landroid/view/TextureView;

    move-result-object v1

    invoke-virtual {p1, v1, v0}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V

    const/4 v1, 0x1

    invoke-static {p0, v1}, LVlloRestored;->groundSurface(Landroid/content/Context;I)Landroid/view/TextureView;

    move-result-object p0

    invoke-virtual {p1, p0, v0}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V

    return-void
.end method

.method public static applicationCreated(Lcom/darinsoft/vimo/VLLOApplication;)V
    .registers 6

    new-instance v0, Lv81;

    new-instance v1, Lng2;

    const/4 v2, 0x0

    invoke-direct {v1, v2}, Lng2;-><init>(I)V

    new-instance v3, Ln2a;

    invoke-direct {v3, v2}, Ln2a;-><init>(I)V

    new-instance v4, Ln71;

    invoke-direct {v4, v2}, Ln71;-><init>(I)V

    invoke-direct {v0, v1, v3, v4, p0}, Lv81;-><init>(Lng2;Ln2a;Ln71;Lcom/darinsoft/vimo/VLLOApplication;)V

    sput-object v0, Lcom/darinsoft/vimo/VLLOApplication;->d:Lv81;

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOApplication;->getApplicationContext()Landroid/content/Context;

    move-result-object v0

    sput-object v0, Llgb;->a:Landroid/content/Context;

    sput-object v0, Ldeg;->a:Landroid/content/Context;

    const-string v1, "connectivity"

    invoke-virtual {v0, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/net/ConnectivityManager;

    sput-object v1, Lam6;->a:Landroid/net/ConnectivityManager;

    sget-object v2, Lam6;->e:Landroid/net/NetworkRequest;

    sget-object v3, Lam6;->f:Lzl6;

    invoke-virtual {v1, v2, v3}, Landroid/net/ConnectivityManager;->registerNetworkCallback(Landroid/net/NetworkRequest;Landroid/net/ConnectivityManager$NetworkCallback;)V

    invoke-static {v0}, LVlloRestored;->recordInstallation(Landroid/content/Context;)V

    invoke-static {}, Lcom/appsflyer/AppsFlyerLib;->getInstance()Lcom/appsflyer/AppsFlyerLib;

    move-result-object v0

    new-instance v1, Lcom/vimosoft/vllovllo/VlloVllo;

    invoke-direct {v1}, Ljava/lang/Object;-><init>()V

    invoke-virtual {v1}, Lcom/vimosoft/vllovllo/VlloVllo;->a()Ljava/lang/String;

    move-result-object v1

    const/4 v2, 0x0

    invoke-virtual {v0, v1, v2, p0}, Lcom/appsflyer/AppsFlyerLib;->init(Ljava/lang/String;Lcom/appsflyer/AppsFlyerConversionListener;Landroid/content/Context;)Lcom/appsflyer/AppsFlyerLib;

    invoke-static {v0, p0}, Lcom/darinsoft/vimo/VLLOApplication;->start$035(Lcom/appsflyer/AppsFlyerLib;Landroid/content/Context;)V

    return-void
.end method

.method private static configureGifProvider(Lcom/darinsoft/vimo/VLLOMainActivity;)V
    .registers 4

    sget-object v0, Lj44;->a:Lj44;

    monitor-enter v0

    :try_start_0
    new-instance v1, Lfzg;

    const/4 v2, 0x0

    invoke-direct {v1, v2}, Lfzg;-><init>(I)V

    sput-object v1, Lj44;->e:Lfzg;

    new-instance v1, Lcom/vimosoft/vllovllo/VlloVllo;

    invoke-direct {v1}, Ljava/lang/Object;-><init>()V

    invoke-virtual {v1}, Lcom/vimosoft/vllovllo/VlloVllo;->i()Ljava/lang/String;

    move-result-object v1

    new-instance v2, Ljava/util/HashMap;

    invoke-direct {v2}, Ljava/util/HashMap;-><init>()V

    invoke-static {p0, v1, v2}, Lj44;->a(Lcom/darinsoft/vimo/VLLOMainActivity;Ljava/lang/String;Ljava/util/HashMap;)V

    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    new-instance p0, Lgj1;

    const/4 v0, 0x2

    invoke-direct {p0, v0}, Lgj1;-><init>(I)V

    sput-object p0, Lj44;->f:Lgj1;

    return-void

    :catchall_0
    move-exception p0

    :try_start_1
    monitor-exit v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw p0
.end method

.method private static configureMediaCache(Lcom/darinsoft/vimo/VLLOMainActivity;Landroid/content/Context;)V
    .registers 10

    sget-object v0, Lhjg;->b:Lebg;

    if-eqz v0, :cond_0

    return-void

    :cond_0
    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getApplicationInfo()Landroid/content/pm/ApplicationInfo;

    move-result-object v1

    iget v1, v1, Landroid/content/pm/ApplicationInfo;->labelRes:I

    invoke-virtual {v0, v1}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;

    move-result-object v0

    new-instance v1, Lkl8;

    new-instance v2, Ljava/io/File;

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getFilesDir()Ljava/io/File;

    move-result-object v3

    const-string v4, "media-cache"

    invoke-direct {v2, v3, v4}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    new-instance v3, Ltg5;

    invoke-direct {v3}, Ltg5;-><init>()V

    new-instance v4, Lkv8;

    const/4 v5, 0x0

    const/4 v6, 0x1

    const-string v7, "media-cache.db"

    invoke-direct {v4, p1, v7, v5, v6}, Landroid/database/sqlite/SQLiteOpenHelper;-><init>(Landroid/content/Context;Ljava/lang/String;Landroid/database/sqlite/SQLiteDatabase$CursorFactory;I)V

    invoke-direct {v1, v2, v3, v4}, Lkl8;-><init>(Ljava/io/File;Ltg5;Lkv8;)V

    const/4 p1, 0x0

    :try_start_0
    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v2

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getPackageName()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v2, p0, p1}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object p0

    iget-object p0, p0, Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    const-string p0, "?"

    :goto_0
    new-instance v2, Lx3d;

    const/16 v3, 0x11

    invoke-direct {v2, v3}, Lx3d;-><init>(I)V

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v0, "/"

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    iput-object p0, v2, Lx3d;->g:Ljava/lang/Object;

    new-instance p0, Lebg;

    invoke-direct {p0, p1}, Lebg;-><init>(I)V

    iput-object v1, p0, Lebg;->e:Ljava/lang/Object;

    new-instance v0, Ltd8;

    invoke-direct {v0, p1}, Ltd8;-><init>(I)V

    iput-object v0, p0, Lebg;->g:Ljava/lang/Object;

    iput-object v2, p0, Lebg;->i:Ljava/lang/Object;

    sput-object p0, Lhjg;->b:Lebg;

    return-void
.end method

.method private static configureWindow(Lcom/darinsoft/vimo/VLLOMainActivity;)V
    .registers 3

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getWindow()Landroid/view/Window;

    move-result-object p0

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1e

    if-ge v0, v1, :cond_0

    invoke-virtual {p0}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    move-result-object p0

    const/16 v0, 0x1307

    invoke-virtual {p0, v0}, Landroid/view/View;->setSystemUiVisibility(I)V

    return-void

    :cond_0
    const/4 v0, 0x0

    invoke-virtual {p0, v0}, Landroid/view/Window;->setDecorFitsSystemWindows(Z)V

    invoke-virtual {p0}, Landroid/view/Window;->getInsetsController()Landroid/view/WindowInsetsController;

    move-result-object p0

    if-eqz p0, :cond_1

    const/4 v0, 0x2

    invoke-interface {p0, v0}, Landroid/view/WindowInsetsController;->setSystemBarsBehavior(I)V

    invoke-static {}, Landroid/view/WindowInsets$Type;->systemBars()I

    move-result v0

    invoke-interface {p0, v0}, Landroid/view/WindowInsetsController;->hide(I)V

    :cond_1
    return-void
.end method

.method public static diagnosticsDone()Lql5;
    .registers 2

    new-instance v0, Lpl5;

    sget-object v1, Lo52;->c:Lo52;

    invoke-direct {v0, v1}, Lpl5;-><init>(Lo52;)V

    return-object v0
.end method

.method public static finishActivity(Landroid/app/Activity;)V
    .registers 1

    invoke-virtual {p0}, Landroid/app/Activity;->finish()V

    return-void
.end method

.method public static foregroundWorkStarted(Landroidx/work/impl/foreground/SystemForegroundService;Landroid/content/Intent;)I
    .registers 3

    iget-boolean v0, p0, Landroidx/work/impl/foreground/SystemForegroundService;->g:Z

    if-eqz v0, :cond_0

    invoke-virtual {p0}, Landroidx/work/impl/foreground/SystemForegroundService;->a()V

    const/4 v0, 0x0

    iput-boolean v0, p0, Landroidx/work/impl/foreground/SystemForegroundService;->g:Z

    :cond_0
    if-eqz p1, :cond_1

    iget-object p0, p0, Landroidx/work/impl/foreground/SystemForegroundService;->i:Lq89;

    invoke-virtual {p0, p1}, Lq89;->e(Landroid/content/Intent;)V

    :cond_1
    const/4 p0, 0x3

    return p0
.end method

.method private static groundSurface(Landroid/content/Context;I)Landroid/view/TextureView;
    .registers 4

    new-instance v0, Landroid/view/TextureView;

    invoke-direct {v0, p0}, Landroid/view/TextureView;-><init>(Landroid/content/Context;)V

    new-instance p0, Landroid/view/ViewGroup$LayoutParams;

    const/16 v1, 0x10

    invoke-direct {p0, v1, v1}, Landroid/view/ViewGroup$LayoutParams;-><init>(II)V

    invoke-virtual {v0, p0}, Landroid/view/TextureView;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    new-instance p0, Lh04;

    invoke-direct {p0, p1}, Lh04;-><init>(I)V

    invoke-virtual {v0, p0}, Landroid/view/TextureView;->setSurfaceTextureListener(Landroid/view/TextureView$SurfaceTextureListener;)V

    return-object v0
.end method

.method public static ignoreBroadcast(Landroid/content/Context;Landroid/content/Intent;)V
    .registers 2

    return-void
.end method

.method private static markDeviceCheckAborted()V
    .registers 1

    sget-object v0, Lso2;->i:Lyo0;

    if-eqz v0, :cond_0

    sget-object v0, Lso2;->i:Lyo0;

    invoke-virtual {v0}, Lyo0;->q()V

    :cond_0
    return-void
.end method

.method private static recordInstallation(Landroid/content/Context;)V
    .registers 8

    const/4 v0, 0x0

    const/4 v1, 0x0

    :try_start_0
    invoke-virtual {p0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v2

    invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3, v1}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object v2
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-object v2, v0

    :goto_0
    const-string v3, "com.vimosoft.vimo.appConfig"

    invoke-virtual {p0, v3, v1}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object p0

    invoke-interface {p0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    move-result-object v3

    if-eqz v2, :cond_1

    invoke-virtual {v2}, Landroid/content/pm/PackageInfo;->getLongVersionCode()J

    move-result-wide v4

    sput-wide v4, Leu;->e:J

    iget-object v4, v2, Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;

    if-eqz v4, :cond_0

    iget-object v4, v2, Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;

    goto :goto_1

    :cond_0
    const-string v4, "unknown"

    :goto_1
    sput-object v4, Leu;->d:Ljava/lang/String;

    const-string v4, "kAPP_CONFIG_INSTALL_TIME"

    iget-wide v5, v2, Landroid/content/pm/PackageInfo;->firstInstallTime:J

    invoke-interface {v3, v4, v5, v6}, Landroid/content/SharedPreferences$Editor;->putLong(Ljava/lang/String;J)Landroid/content/SharedPreferences$Editor;

    const-string v2, "kAPP_CONFIG_LAUNCH_COUNT"

    invoke-interface {p0, v2, v1}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I

    move-result v1

    add-int/lit8 v1, v1, 0x1

    invoke-interface {v3, v2, v1}, Landroid/content/SharedPreferences$Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences$Editor;

    :cond_1
    const-string v1, "userId"

    invoke-interface {p0, v1, v0}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    sput-object p0, Leu;->f:Ljava/lang/String;

    sget-object p0, Leu;->f:Ljava/lang/String;

    if-nez p0, :cond_2

    invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;

    move-result-object p0

    invoke-virtual {p0}, Ljava/util/UUID;->toString()Ljava/lang/String;

    move-result-object p0

    sput-object p0, Leu;->f:Ljava/lang/String;

    sget-object p0, Leu;->f:Ljava/lang/String;

    invoke-interface {v3, v1, p0}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;

    :cond_2
    invoke-interface {v3}, Landroid/content/SharedPreferences$Editor;->apply()V

    return-void
.end method

.method private static setUpDeviceManager(Lcom/darinsoft/vimo/VLLOMainActivity;)V
    .registers 10

    invoke-virtual {p0}, Lcom/darinsoft/vimo/VLLOMainActivity;->getApplication()Landroid/app/Application;

    move-result-object v0

    check-cast v0, Lcom/darinsoft/vimo/VLLOApplication;

    new-instance v1, Lyo0;

    invoke-direct {v1, p0}, Lyo0;-><init>(Lcom/darinsoft/vimo/VLLOMainActivity;)V

    sput-object v1, Lso2;->i:Lyo0;

    new-instance p0, Lrv9;

    invoke-direct {p0, v0}, Lrv9;-><init>(Lcom/darinsoft/vimo/VLLOApplication;)V

    sput-object p0, Lso2;->a:Lrv9;

    new-instance v1, Lqh1;

    invoke-direct {v1}, Ljava/lang/Object;-><init>()V

    iput-object v0, v1, Lqh1;->g:Ljava/lang/Object;

    sget-object v2, Lxd1;->a:Lvo2;

    iput-object v2, v1, Lqh1;->i:Ljava/lang/Object;

    new-instance v2, Ljava/util/ArrayList;

    const/4 v3, 0x1

    new-array v4, v3, [Lvo2;

    sget-object v5, Lxd1;->a:Lvo2;

    const/4 v6, 0x0

    aput-object v5, v4, v6

    invoke-static {v4}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    move-result-object v4

    invoke-direct {v2, v4}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V

    iput-object v2, v1, Lqh1;->r:Ljava/io/Serializable;

    sget-object v2, Lxd1;->b:Lvo2;

    iput-object v2, v1, Lqh1;->v:Ljava/lang/Object;

    new-instance v2, Ljava/util/ArrayList;

    const/4 v4, 0x2

    new-array v4, v4, [Lvo2;

    sget-object v5, Lxd1;->b:Lvo2;

    aput-object v5, v4, v6

    sget-object v5, Lxd1;->a:Lvo2;

    aput-object v5, v4, v3

    invoke-static {v4}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    move-result-object v4

    invoke-direct {v2, v4}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V

    iput-object v2, v1, Lqh1;->w:Ljava/lang/Object;

    sput-object v1, Lso2;->b:Lqh1;

    new-instance v1, Ly;

    invoke-direct {v1, v6, v6}, Ly;-><init>(IZ)V

    sput-object v1, Lso2;->c:Ly;

    const-string v2, "window"

    invoke-virtual {v0, v2}, Lcom/darinsoft/vimo/VLLOApplication;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/view/WindowManager;

    invoke-interface {v2}, Landroid/view/WindowManager;->getDefaultDisplay()Landroid/view/Display;

    move-result-object v2

    new-instance v4, Landroid/graphics/Point;

    invoke-direct {v4}, Landroid/graphics/Point;-><init>()V

    invoke-virtual {v2, v4}, Landroid/view/Display;->getRealSize(Landroid/graphics/Point;)V

    new-instance v5, Lcom/vimosoft/vimoutil/util/CGSize;

    iget v7, v4, Landroid/graphics/Point;->x:I

    iget v8, v4, Landroid/graphics/Point;->y:I

    invoke-direct {v5, v7, v8}, Lcom/vimosoft/vimoutil/util/CGSize;-><init>(II)V

    iput-object v5, p0, Lrv9;->g:Ljava/lang/Object;

    new-instance v5, Landroid/util/DisplayMetrics;

    invoke-direct {v5}, Landroid/util/DisplayMetrics;-><init>()V

    invoke-virtual {v2, v5}, Landroid/view/Display;->getMetrics(Landroid/util/DisplayMetrics;)V

    new-instance v2, Lcom/vimosoft/vimoutil/util/CGSize;

    iget v7, v4, Landroid/graphics/Point;->x:I

    int-to-float v7, v7

    iget v8, v5, Landroid/util/DisplayMetrics;->xdpi:F

    div-float/2addr v7, v8

    iget v4, v4, Landroid/graphics/Point;->y:I

    int-to-float v4, v4

    iget v5, v5, Landroid/util/DisplayMetrics;->ydpi:F

    div-float/2addr v4, v5

    invoke-direct {v2, v7, v4}, Lcom/vimosoft/vimoutil/util/CGSize;-><init>(FF)V

    iput-object v2, p0, Lrv9;->i:Ljava/lang/Object;

    sput-boolean v3, Lso2;->d:Z

    const-string p0, "audio"

    invoke-virtual {v0, p0}, Lcom/darinsoft/vimo/VLLOApplication;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Landroid/media/AudioManager;

    const-string v0, "android.media.property.OUTPUT_SAMPLE_RATE"

    invoke-virtual {p0, v0}, Landroid/media/AudioManager;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    if-nez p0, :cond_0

    goto :goto_0

    :cond_0
    invoke-static {p0}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v6

    :goto_0
    if-nez v6, :cond_1

    const v6, 0xac44

    :cond_1
    iput v6, v1, Ly;->d:I

    sput-boolean v3, Lso2;->f:Z

    return-void
.end method

.method public static textToSpeechStarted(Landroid/app/Service;Landroid/content/Intent;)I
    .registers 3

    if-eqz p1, :cond_0

    const-string v0, "ACTION_STOP"

    invoke-virtual {p1}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_0

    invoke-virtual {p0}, Landroid/app/Service;->stopSelf()V

    :cond_0
    const/4 p0, 0x1

    return p0
.end method


# virtual methods
.method public onApplyWindowInsets(Landroid/view/View;Landroid/view/WindowInsets;)Landroid/view/WindowInsets;
    .registers 5

    iget-object p1, p0, LVlloRestored;->statusBarMargin:Landroid/view/View;

    invoke-virtual {p1}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object p1

    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1e

    if-lt v0, v1, :cond_0

    invoke-static {}, Landroid/view/WindowInsets$Type;->statusBars()I

    move-result v0

    invoke-static {}, Landroid/view/WindowInsets$Type;->displayCutout()I

    move-result v1

    or-int/2addr v0, v1

    invoke-virtual {p2, v0}, Landroid/view/WindowInsets;->getInsetsIgnoringVisibility(I)Landroid/graphics/Insets;

    move-result-object v0

    iget v0, v0, Landroid/graphics/Insets;->top:I

    iput v0, p1, Landroid/view/ViewGroup$LayoutParams;->height:I

    goto :goto_0

    :cond_0
    invoke-virtual {p2}, Landroid/view/WindowInsets;->getSystemWindowInsetTop()I

    move-result v0

    iput v0, p1, Landroid/view/ViewGroup$LayoutParams;->height:I

    :goto_0
    iget-object p0, p0, LVlloRestored;->statusBarMargin:Landroid/view/View;

    invoke-virtual {p0, p1}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    return-object p2
.end method
