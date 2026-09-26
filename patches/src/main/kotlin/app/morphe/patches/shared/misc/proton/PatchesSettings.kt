/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.returnEarly
import org.w3c.dom.Element

internal const val SETTINGS_ROW_TITLE = "hxreborn patches"
private const val SETTINGS_ACTIVITY_CLASS = "app.hxreborn.extension.proton.PatchesSettingsActivity"
private const val BUNDLE_VERSION_RESOURCE = "/proton-bundle-version.txt"

internal fun patchesSettingsActivityPatch(themeStyle: String) = resourcePatch {
    finalize {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", SETTINGS_ACTIVITY_CLASS)
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:theme", themeStyle)
            application.appendChild(activity)
        }
    }
}

internal fun BytecodePatchContext.injectBundleVersion() {
    val bundleVersion = PatchesSettingsVersion::class.java
        .getResourceAsStream(BUNDLE_VERSION_RESOURCE)?.bufferedReader()?.use {
            it.readText().trim()
        } ?: throw PatchException("Patch bundle version resource $BUNDLE_VERSION_RESOURCE is unavailable")

    mutableClassDefBy(PATCHES_MENU_CLASS).methods.single { it.name == "bundleVersion" }
        .returnEarly(bundleVersion)
}

private object PatchesSettingsVersion
