/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import org.w3c.dom.Element

private const val ACTIVITY = "app.hxreborn.extension.viber.PatchesSettingsActivity"

internal val patchesSettingsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
    dependsOn(resourcePatch {
        execute {
            document("res/xml/settings_general.xml").use { document ->
                val row = document.createElement("Preference")
                row.setAttribute("android:layout", "@layout/_ics_custom_preference_layout")
                row.setAttribute("android:title", "hxreborn patches")
                row.setAttribute("android:key", "hx_viber_patches")
                val intent = document.createElement("intent")
                intent.setAttribute("android:targetPackage", "com.viber.voip")
                intent.setAttribute("android:targetClass", ACTIVITY)
                row.appendChild(intent)
                val screen = document.documentElement
                screen.insertBefore(row, screen.firstChild)
            }
            document("AndroidManifest.xml").use { document ->
                val application = document.getElementsByTagName("application").item(0) as Element
                val activity = document.createElement("activity")
                activity.setAttribute("android:name", ACTIVITY)
                activity.setAttribute("android:exported", "false")
                activity.setAttribute("android:theme", "@style/Theme.Viber.NoActionBar")
                application.appendChild(activity)
            }
            get("res/drawable/hx_viber_ic_expand.xml").writeText("""
                <vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
                    <path android:fillColor="@android:color/transparent" android:strokeColor="#FFFFFFFF" android:strokeWidth="2" android:pathData="M6,9 L12,15 L18,9" />
                </vector>
            """.trimIndent())
            mapOf(
                "hide_ads" to "promotions", "hide_more_items" to "more_items",
                "hide_navigation_buttons" to "home_tab", "disable_analytics" to "tracking",
                "reset" to "reset", "info" to "info", "patches" to "patches", "back" to "back",
            ).forEach { (target, source) ->
                val stream = SettingsResources::class.java.getResourceAsStream("/viber/drawable/hx_viber_ic_$source.xml")
                    ?: SettingsResources::class.java.getResourceAsStream("/patreon/drawable/hx_patreon_ic_$source.xml")
                    ?: SettingsResources::class.java.getResourceAsStream("/aliexpress/drawable/hx_aliexpress_ic_$source.xml")!!
                stream.use { get("res/drawable/hx_viber_ic_$target.xml").outputStream().use(it::copyTo) }
            }
        }
    })
    execute {
        ApplicationContextFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static/range { p1 .. p1 }, $VIBER_SETTINGS_CLASS->initialize(Landroid/content/Context;)V",
        )
    }
}

private object SettingsResources
