/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xtreamplayer.misc.androidauto

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.InstallerType
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.xtreamplayer.misc.premium.unlockPremiumPatch
import org.w3c.dom.Element

private const val EXTENSION_PACKAGE = "app.hxreborn.extension.xtreamplayer"
private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/xtreamplayer/AndroidAutoInstaller;"
private const val SOURCE_APK_PROVIDER = "$EXTENSION_PACKAGE.SourceApkProvider"
private const val AUTHORITY_SUFFIX = ".hxreborn.installer"
private const val PACKAGE_INSTALLER = "com.google.android.packageinstaller"
private const val ANDROID_AUTO = "com.google.android.projection.gearhead"

private val androidAutoResourcesPatch = resourcePatch {
    finalize {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("AndroidManifest.xml has no <application>")
            val provider = document.createElement("provider")
            provider.setAttribute("android:name", SOURCE_APK_PROVIDER)
            provider.setAttribute("android:authorities", document.documentElement.getAttribute("package") + AUTHORITY_SUFFIX)
            provider.setAttribute("android:exported", "false")
            provider.setAttribute("android:grantUriPermissions", "true")
            application.appendChild(provider)
            val queries = document.getElementsByTagName("queries").item(0) as? Element
                ?: document.createElement("queries").also(document.documentElement::appendChild)
            for (packageName in listOf(PACKAGE_INSTALLER, ANDROID_AUTO)) {
                val packageQuery = document.createElement("package")
                packageQuery.setAttribute("android:name", packageName)
                queries.appendChild(packageQuery)
            }
        }
    }
}

@Suppress("unused")
val androidAutoSupportPatch = bytecodePatch(
    name = "Full Android Auto screen",
    description = "Shows full car screen in Android Auto. Requires Unlock premium, Android 16 or newer and one-time installation confirmation.",
) {
    compatibleWith(AppCompatibilities.XTREAM_PLAYER)
    availability { installer, _ ->
        if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.ENABLED
    }
    dependsOn(unlockPremiumPatch, androidAutoResourcesPatch)
    extendWith("extensions/extension.mpe")

    execute {
        mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).replaceResourceIdStubs(
            ResourceType.ATTR,
            mapOf("accentColorAttribute" to "colorAccent"),
        )
        val resumed = MainActivityResumeFingerprint.matchSingle()
        val (superResume) = resumed.instructionMatches
        resumed.method.addInstruction(
            superResume.index + 1,
            "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->promptIfNeeded(Landroid/app/Activity;)V",
        )
    }
}
