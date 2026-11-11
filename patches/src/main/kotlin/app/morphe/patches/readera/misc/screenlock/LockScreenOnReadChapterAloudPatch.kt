/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.screenlock

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.readera.misc.background.backgroundListeningPatch
import app.morphe.patches.readera.misc.chapterspeech.readChapterAloudPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/SpeechScreenLock;"
private const val PATCH_NAME = "Lock screen on Read chapter aloud"
private const val ADMIN_RECEIVER = "app.hxreborn.extension.readera.ScreenLockAdmin"

@Suppress("unused")
val lockScreenOnReadChapterAloudPatch = bytecodePatch(
    name = PATCH_NAME,
    description = "Locks the phone when Read chapter aloud starts, and the chapter keeps playing. " +
        "Turned on in the patches menu. " +
        "Needs device admin access.",
) {
    compatibleWith(AppCompatibilities.READERA)
    availability(requireArm64)
    dependsOn(
        readChapterAloudPatch,
        backgroundListeningPatch,
        resourcePatch {
            execute {
                copyResources("readera", ResourceGroup("drawable", "hx_readera_lock.xml"))
                get("res/xml/hx_readera_screen_lock_admin.xml").writeText(
                    """<device-admin xmlns:android="http://schemas.android.com/apk/res/android"><uses-policies><force-lock/></uses-policies></device-admin>""",
                )
                document("AndroidManifest.xml").use { document ->
                    val receiver = document.createElement("receiver").apply {
                        setAttribute("android:name", ADMIN_RECEIVER)
                        setAttribute("android:label", PATCH_NAME)
                        setAttribute("android:permission", "android.permission.BIND_DEVICE_ADMIN")
                        setAttribute("android:exported", "false")
                    }
                    receiver.appendChild(document.createElement("meta-data").apply {
                        setAttribute("android:name", "android.app.device_admin")
                        setAttribute("android:resource", "@xml/hx_readera_screen_lock_admin")
                    })
                    receiver.appendChild(document.createElement("intent-filter").apply {
                        appendChild(document.createElement("action").apply {
                            setAttribute("android:name", "android.app.action.DEVICE_ADMIN_ENABLED")
                        })
                    })
                    document.getElementsByTagName("application").item(0).appendChild(receiver)
                }
            }
        },
    )

    execute {
        mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).methods.single { it.name == "applied" }.returnEarly(true)
        registerReadEraPatch(PATCH_NAME, "Locks the phone when Read chapter aloud starts. The chapter keeps playing.")
    }
}
