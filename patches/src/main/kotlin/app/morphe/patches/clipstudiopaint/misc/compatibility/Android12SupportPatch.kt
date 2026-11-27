/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clipstudiopaint.misc.compatibility

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireStandardInstall
import app.morphe.util.getNode
import org.w3c.dom.Element

private const val ANDROID_12_SDK = 31

@Suppress("unused")
val clipStudioPaintAndroid12SupportPatch = patch(
    name = "Lower Android requirement",
    description = "Lowers the minimum Android version to Android 12.",
) {
    compatibleWith(AppCompatibilities.CLIP_STUDIO_PAINT)
    availability(requireStandardInstall)

    execute {
        val firebaseProcessName = FirebaseProcessNameFingerprint.match()
        val (getProcessName) = firebaseProcessName.instructionMatches
        firebaseProcessName.method.replaceInstruction(
            getProcessName.index,
            "invoke-static {}, Landroid/app/Application;->getProcessName()Ljava/lang/String;",
        )

        ProfilePackageInfoFingerprint.method.apply {
            removeInstructions(0, implementation!!.instructions.count())
            addInstructions(
                0,
                """
                    invoke-virtual {p1}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
                    move-result-object p1
                    const/4 v0, 0x0
                    invoke-virtual {p0, p1, v0}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;
                    move-result-object p0
                    return-object p0
                """,
            )
        }

        document("AndroidManifest.xml").use { manifest ->
            val usesSdk = manifest.getNode("uses-sdk") as? Element
                ?: throw PatchException("AndroidManifest.xml is missing uses-sdk")

            usesSdk.setAttribute("android:minSdkVersion", ANDROID_12_SDK.toString())
        }
    }
}
