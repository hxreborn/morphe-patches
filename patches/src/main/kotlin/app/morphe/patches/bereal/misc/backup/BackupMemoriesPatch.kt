/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.backup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.asSequence
import app.morphe.util.setExtensionIsPatchIncluded
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}backup/MemoriesBackup;"
private const val API_CLASS = "${BEREAL_EXTENSION_PACKAGE}backup/BeRealApi;"

private const val SERVICE_CLASS = "app.hxreborn.extension.bereal.backup.BackupService"

private val backupPermissions = listOf(
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
    "android.permission.POST_NOTIFICATIONS",
)

private val addBackupServicePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            val declared = manifest.childNodes.asSequence().filterIsInstance<Element>()
                .filter { it.tagName == "uses-permission" }
                .map { it.getAttribute("android:name") }
                .toSet()
            backupPermissions.filter { it !in declared }.forEach { permission ->
                manifest.appendChild(
                    document.createElement("uses-permission").apply {
                        setAttribute("android:name", permission)
                    },
                )
            }

            (document.getElementsByTagName("application").item(0) as Element).appendChild(
                document.createElement("service").apply {
                    setAttribute("android:name", SERVICE_CLASS)
                    setAttribute("android:exported", "false")
                    setAttribute("android:foregroundServiceType", "dataSync")
                },
            )
        }
    }
}

@Suppress("unused")
val backupMemoriesPatch = bytecodePatch(
    name = "Back up memories",
    description = "Adds Back up all memories to the patch settings. Saves every memory as separate back and front " +
        "photos with the time, place and caption, plus the BTS video. The JSON sidecars hold location, friends' " +
        "usernames and comments, and go to Download/BeReal unless a folder is picked.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch, addBackupServicePatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        RetrofitCreateFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p0, p1 }, $API_CLASS->onCreate(Ljava/lang/Object;Ljava/lang/Class;)V",
        )
    }
}
