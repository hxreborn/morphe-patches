/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.pairip

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.RestoredMethod
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.restoreVirtualizedMethods

internal const val APP = "Lcom/vpn/lib/App;"
internal const val SPLASH = "Lcom/vpn/lib/feature/splash/SplashActivity;"
internal const val PREPARING_STEPS = "Lcom/vpn/lib/feature/splash/SplashActivity\$PreparingSteps;"
internal const val NAVIGATION = "Lcom/vpn/lib/feature/naviagation/NavigationActivity;"
internal const val STATUS_SERVICE = "Lcom/vpn/lib/status/VpnStatusService;"
internal const val LAUNCH_VPN = "Lde/blinkt/openvpn/LaunchVPN;"
internal const val DEVICE_STATE_RECEIVER = "Lde/blinkt/openvpn/core/DeviceStateReceiver;"
internal const val SETTINGS = "Lcom/vpn/lib/SettingPreferences;"
internal const val SHARED_PREFERENCES = "Landroid/content/SharedPreferences;"
internal const val FIND_VIEW = "Landroidx/appcompat/app/AppCompatActivity;->findViewById(I)Landroid/view/View;"

private const val INDIA_APP = "Lindia/vpn_tap2free/MyApp;"

private const val INMOBI_AD_ACTIVITY = "Lcom/inmobi/ads/rendering/InMobiAdActivity;"

private const val ON_CREATE = "onCreate"
private const val ON_RESUME = "onResume"
private const val ON_RECEIVE = "onReceive"
private const val ON_START_COMMAND = "onStartCommand"

private const val FINISH_ON_CREATE = """
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
    return-void
"""

private const val FINISH_ON_RESUME = """
    invoke-super {p0}, Landroidx/fragment/app/FragmentActivity;->onResume()V
    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
    return-void
"""

private const val IGNORE_BROADCAST = "return-void"

private fun callActivitySuper(name: String) = """
    invoke-super {p0}, Landroid/app/Activity;->$name()V
    return-void
"""

internal fun resourceIdHex(type: ResourceType, name: String) = "0x" + resourceId(type, name).toString(16)

internal fun stringId(name: String) = resourceIdHex(ResourceType.STRING, name)

internal fun viewId(name: String) = resourceIdHex(ResourceType.ID, name)

private fun restoredMethods() = listOf(
    RestoredMethod(APP, ON_CREATE, 6, appOnCreate()),
    RestoredMethod(SPLASH, ON_CREATE, 12, splashOnCreate()),
    RestoredMethod(NAVIGATION, ON_CREATE, 9, navigationOnCreate()),
    RestoredMethod(
        "Lcom/vpn/lib/feature/paywall/PaywallActivity;", ON_CREATE, 2,
        """
            invoke-static {p0}, Ldagger/android/AndroidInjection;->a(Landroid/app/Activity;)V
            invoke-super {p0, p1}, Landroidx/fragment/app/FragmentActivity;->onCreate(Landroid/os/Bundle;)V
            invoke-virtual {p0}, Landroid/app/Activity;->finish()V
            return-void
        """,
    ),
    RestoredMethod(STATUS_SERVICE, ON_START_COMMAND, 11, statusServiceOnStartCommand()),
    RestoredMethod(LAUNCH_VPN, ON_CREATE, 8, launchVpnOnCreate()),
    RestoredMethod("Lcom/android/billingclient/api/ProxyBillingActivity;", ON_CREATE, 2, FINISH_ON_CREATE),
    RestoredMethod(
        "Lcom/android/billingclient/api/ProxyBillingActivityV2;", ON_CREATE, 2,
        """
            invoke-super {p0, p1}, Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V
            invoke-virtual {p0}, Landroid/app/Activity;->finish()V
            return-void
        """,
    ),
    RestoredMethod("Lcom/android/billingclient/api/zzaa;", ON_RECEIVE, 3, IGNORE_BROADCAST),
    RestoredMethod("Lcom/google/firebase/auth/internal/GenericIdpActivity;", ON_RESUME, 1, FINISH_ON_RESUME),
    RestoredMethod("Lcom/google/firebase/auth/internal/RecaptchaActivity;", ON_RESUME, 1, FINISH_ON_RESUME),
    RestoredMethod(INMOBI_AD_ACTIVITY, ON_CREATE, 2, FINISH_ON_CREATE),
    RestoredMethod(INMOBI_AD_ACTIVITY, "onStart", 1, callActivitySuper("onStart")),
    RestoredMethod(INMOBI_AD_ACTIVITY, ON_RESUME, 1, callActivitySuper("onResume")),
    RestoredMethod(INMOBI_AD_ACTIVITY, "onDestroy", 1, callActivitySuper("onDestroy")),
    RestoredMethod("Lcom/vungle/ads/internal/ui/l;", ON_CREATE, 2, FINISH_ON_CREATE),
    RestoredMethod("Landroidx/media3/common/util/NetworkTypeObserver\$Receiver;", ON_RECEIVE, 3, IGNORE_BROADCAST),
    RestoredMethod("Landroidx/profileinstaller/ProfileInstallReceiver;", ON_RECEIVE, 3, IGNORE_BROADCAST),
    RestoredMethod(
        "Landroidx/media3/exoplayer/offline/DownloadService;", ON_START_COMMAND, 4,
        """
            invoke-virtual {p0}, Landroid/app/Service;->stopSelf()V
            const/4 v0, 0x2
            return v0
        """,
    ),
)

private fun appRestoredMethods(packageName: String) = when (packageName) {
    AppCompatibilities.USA_VPN.packageName -> listOf(
        RestoredMethod(DEVICE_STATE_RECEIVER, ON_RECEIVE, 8, deviceStateOnReceive()),
    )
    AppCompatibilities.VPN_INDIA.packageName -> listOf(
        RestoredMethod(
            INDIA_APP, ON_CREATE, 2,
            """
                invoke-super {p0}, $APP->onCreate()V
                sput-object p0, $APP->F:$APP
                invoke-virtual {p0}, $INDIA_APP->m()V
                invoke-static {}, Lcom/vpn/lib/injection/DaggerAppComponent;->a()Lcom/vpn/lib/injection/AppComponent${'$'}Builder;
                move-result-object v0
                invoke-interface {v0, p0}, Lcom/vpn/lib/injection/AppComponent${'$'}Builder;->a($APP)Lcom/vpn/lib/injection/AppComponent${'$'}Builder;
                invoke-interface {v0}, Lcom/vpn/lib/injection/AppComponent${'$'}Builder;->build()Lcom/vpn/lib/injection/DaggerAppComponent;
                move-result-object v0
                invoke-virtual {v0, p0}, Lcom/vpn/lib/injection/DaggerAppComponent;->c($APP)V
                return-void
            """,
        ),
    )
    else -> throw PatchException("No restored methods for $packageName")
}

val restoreVirtualizedMethodsPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.USA_VPN, AppCompatibilities.VPN_INDIA)

    dependsOn(removePairipVirtualizationPatch)
    extendWith("extensions/extension.mpe")

    execute {
        checkMergedLambdaBindings()
        restoreVirtualizedMethods(restoredMethods() + appRestoredMethods(packageMetadata.packageName))
    }
}
