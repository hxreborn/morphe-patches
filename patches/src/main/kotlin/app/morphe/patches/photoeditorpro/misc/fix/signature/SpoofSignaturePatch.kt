/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/photoeditorpro/SpoofSignature;"
private const val PAIRIP_APPLICATION_CLASS = "Lcom/pairip/application/Application;"
private const val ANDROID_APPLICATION_CLASS = "Landroid/app/Application;"

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Restores the AI tools and stops the app killing itself by spoofing the " +
        "original app signature.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    extendWith("extensions/extension.mpe")

    execute {
        var applicationClass = mutableClassDefBy(PAIRIP_APPLICATION_CLASS)
        val walked = mutableSetOf(applicationClass.type)

        while (applicationClass.superclass != ANDROID_APPLICATION_CLASS) {
            val superclass = applicationClass.superclass
                ?: throw PatchException("Application hierarchy ended before android.app.Application")
            if (!walked.add(superclass)) {
                throw PatchException("Application hierarchy loops at $superclass")
            }
            applicationClass = mutableClassDefBy(superclass)
        }

        if (applicationClass.type == EXTENSION_CLASS) {
            throw PatchException("Application hierarchy already extends $EXTENSION_CLASS")
        }

        applicationClass.setSuperClass(EXTENSION_CLASS)

        InitializeLicenseCheckFingerprint.matchSingle().method.returnEarly()

        markPatchInstalled("pep_spoof_signature")
    }
}
