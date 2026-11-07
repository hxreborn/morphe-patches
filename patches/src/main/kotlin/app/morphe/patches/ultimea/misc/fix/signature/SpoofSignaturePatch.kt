/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.fix.signature

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.SPOOF_SIGNATURE_CLASS
import app.morphe.patches.shared.misc.signature.spoofSignature
import org.w3c.dom.Element

private const val APPLICATION_NAME = "app.hxreborn.extension.ultimea.UltimeaApplication"
private const val APPLICATION_CLASS = "Lapp/hxreborn/extension/ultimea/UltimeaApplication;"

internal val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.ULTIMEA)
    extendWith("extensions/extension.mpe")
    dependsOn(resourcePatch {
        execute {
            document("AndroidManifest.xml").use { document ->
                val application = document.getElementsByTagName("application").item(0) as Element
                if (application.getAttribute("android:name") != "android.app.Application") {
                    throw PatchException("Expected the framework application in ULTIMEA")
                }
                application.setAttribute("android:name", APPLICATION_NAME)
            }
        }
    })
    execute {
        spoofSignature(APPLICATION_CLASS)
        mutableClassDefBy(APPLICATION_CLASS).methods.single { it.name == "<init>" }.replaceInstruction(
            0,
            "invoke-direct { p0 }, $SPOOF_SIGNATURE_CLASS-><init>()V",
        )
    }
}
