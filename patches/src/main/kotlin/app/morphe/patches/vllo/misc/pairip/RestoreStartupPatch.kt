/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vllo.misc.pairip

import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedMethod
import app.morphe.patches.shared.misc.smali.assembleDex
import app.morphe.util.inputStreamFromBundledResource
import java.io.ByteArrayInputStream
import java.util.function.Supplier

private const val APPLICATION = "Lcom/darinsoft/vimo/VLLOApplication;"
private const val ACTIVITY = "Lcom/darinsoft/vimo/VLLOMainActivity;"
private const val RESTORED = "LVlloRestored;"
private const val VERSION_CODE = "140002"
private var needsRestoration = false

private val selectRestorationPatch = patch {
    compatibleWith(AppCompatibilities.VLLO)

    execute {
        needsRestoration = packageMetadata.versionCode == VERSION_CODE
    }
}

internal val restoreStartupPatch = patch {
    compatibleWith(AppCompatibilities.VLLO)
    dependsOn(removePairipVirtualizationPatch, selectRestorationPatch)

    extendWithAll {
        if (!needsRestoration) {
            emptyList()
        } else {
            val source = inputStreamFromBundledResource("vllo/restored", "VlloRestored.smali")!!.use {
                it.readBytes().decodeToString()
            }
            listOf(Supplier { ByteArrayInputStream(assembleDex(listOf(source))) })
        }
    }

    execute {
        if (!needsRestoration) return@execute

        fun restore(type: String, name: String, registers: Int, body: String) {
            mutableClassDefBy(type).replaceVirtualizedMethod(name, registers, body)
        }

        fun receiver(type: String) {
            restore(type, "onReceive", 3, """
                invoke-static {p1, p2}, $RESTORED->ignoreBroadcast(Landroid/content/Context;Landroid/content/Intent;)V
                return-void
            """)
        }
        receiver("Landroidx/profileinstaller/ProfileInstallReceiver;")
        val foregroundParent = classDefBy("Landroidx/work/impl/foreground/SystemForegroundService;").superclass
        restore("Landroidx/work/impl/foreground/SystemForegroundService;", "onStartCommand", 5, """
            invoke-super {p0, p1, p2, p3}, $foregroundParent->onStartCommand(Landroid/content/Intent;II)I
            invoke-static {p0, p1}, $RESTORED->foregroundWorkStarted(Landroidx/work/impl/foreground/SystemForegroundService;Landroid/content/Intent;)I
            move-result v0
            return v0
        """)
        restore("Landroidx/work/impl/workers/DiagnosticsWorker;", "doWork", 2, """
            invoke-static {}, $RESTORED->diagnosticsDone()Lql5;
            move-result-object v0
            return-object v0
        """)

        listOf(
            "Lcom/android/billingclient/api/ProxyBillingActivity;",
            "Lcom/android/billingclient/api/ProxyBillingActivityV2;",
            "Lcom/facebook/CustomTabMainActivity;",
            "Lcom/facebook/FacebookActivity;",
            "Lcom/google/android/gms/common/api/GoogleApiActivity;",
        ).forEach { type ->
            val parent = classDefBy(type).superclass
            restore(type, "onCreate", 2, """
                invoke-super {p0, p1}, $parent->onCreate(Landroid/os/Bundle;)V
                invoke-static {p0}, $RESTORED->finishActivity(Landroid/app/Activity;)V
                return-void
            """)
        }

        receiver("Lcom/appsflyer/MultipleInstallBroadcastReceiver;")
        receiver("Lcom/facebook/internal/d;")
        receiver("Lcom/google/android/play/core/assetpacks/SessionStateBroadcastReceiver;")
        receiver("Lrdf;")

        restore(APPLICATION, "onCreate", 1, """
            invoke-super {p0}, Landroid/app/Application;->onCreate()V
            invoke-static {p0}, $RESTORED->applicationCreated($APPLICATION)V
            return-void
        """)

        val layout = resourceId(ResourceType.LAYOUT, "activity_vimo_main")
        val auxiliary = resourceId(ResourceType.ID, "aux_controller_container")
        val main = resourceId(ResourceType.ID, "controller_container")
        val margin = resourceId(ResourceType.ID, "dummy_margin")
        val toast = resourceId(ResourceType.ID, "vl_toast_layer")
        val portraitOnly = resourceId(ResourceType.BOOL, "portrait_only")
        val activityParent = classDefBy(ACTIVITY).superclass
        restore(ACTIVITY, "onCreate", 10, """
            invoke-static {p0}, $RESTORED->activitySplashInstalled($ACTIVITY)V
            invoke-super {p0, p1}, $activityParent->onCreate(Landroid/os/Bundle;)V
            move-object v0, p0
            move-object v1, p1
            const v2, $layout
            const v3, $auxiliary
            const v4, $main
            const v5, $margin
            const v6, $toast
            const v7, $portraitOnly
            invoke-static/range {v0 .. v7}, $RESTORED->activityCreated(${ACTIVITY}Landroid/os/Bundle;IIIIII)V
            return-void
        """)
        mapOf(
            "onPause" to "invoke-static {}, $RESTORED->activityPaused()V",
            "onResume" to "invoke-static {p0}, $RESTORED->activityResumed($ACTIVITY)V",
            "onStop" to "invoke-static {}, $RESTORED->activityStopped()V",
            "onDestroy" to "invoke-static {}, $RESTORED->activityDestroyed()V",
        ).forEach { (name, helper) ->
            restore(ACTIVITY, name, 1, """
                invoke-super {p0}, $activityParent->$name()V
                $helper
                return-void
            """)
        }
        restore("Lcom/vimosoft/vimomodule/tts/supertonic/TTSSupertonicService;", "onStartCommand", 5, """
            invoke-static {p0, p1}, $RESTORED->textToSpeechStarted(Landroid/app/Service;Landroid/content/Intent;)I
            move-result v0
            return v0
        """)
    }
}
