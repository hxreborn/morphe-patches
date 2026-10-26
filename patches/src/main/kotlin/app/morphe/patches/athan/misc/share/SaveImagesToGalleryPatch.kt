/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.misc.share

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.athan.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val SAVE_IMAGE_ACTIVITY = "app.hxreborn.extension.athan.SaveImageActivity"

private val saveImageActivityPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activity = document.createElement("activity")
            activity.setAttribute("android:name", SAVE_IMAGE_ACTIVITY)
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:excludeFromRecents", "true")
            activity.setAttribute("android:theme", "@android:style/Theme.Translucent.NoTitleBar")
            application.appendChild(activity)
        }
    }
}

@Suppress("unused")
val saveImagesToGalleryPatch = bytecodePatch(
    name = "Save images to gallery",
    description = "Adds a Save to gallery option to the share sheet of verse, dua and greeting card images. " +
        "Requires Android 10 or newer.",
) {
    compatibleWith(AppCompatibilities.ATHAN)
    extendWith("extensions/extension.mpe")
    dependsOn(spoofSignaturePatch, saveImageActivityPatch)

    execute {
        val extensionChooser = "L${SAVE_IMAGE_ACTIVITY.replace('.', '/')};->createChooser" +
            "(Landroid/content/Intent;Ljava/lang/CharSequence;)Landroid/content/Intent;"

        CreateChooserFingerprint.matchAll().forEach { match ->
            val method = match.method
            method.instructions.withIndex()
                .filter { (_, instruction) ->
                    instruction.opcode == Opcode.INVOKE_STATIC &&
                        instruction.getReference<MethodReference>()?.toString() == CREATE_CHOOSER
                }
                .forEach { (index, instruction) ->
                    val invoke = instruction as FiveRegisterInstruction
                    method.replaceInstruction(
                        index,
                        "invoke-static { v${invoke.registerC}, v${invoke.registerD} }, $extensionChooser",
                    )
                }
        }
    }
}
