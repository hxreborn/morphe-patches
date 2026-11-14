/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.redreader.layout.filter

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/redreader/PostFilter;"
private const val SETTINGS_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/redreader/PatchSettings;"
private const val SETTINGS_ACTIVITY = "app.hxreborn.extension.redreader.PatchesSettingsActivity"

private val filterResourcesPatch = resourcePatch {
    execute {
        val packageName = document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activity = document.createElement("activity")
            activity.setAttribute("android:name", SETTINGS_ACTIVITY)
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:theme", "@style/RR.Settings")
            application.appendChild(activity)
            document.documentElement.getAttribute("package")
        }
        document("res/xml/prefs_root.xml").use { document ->
            val screen = document.documentElement
            val preference = document.createElement("Preference")
            preference.setAttribute("android:key", "hx_redreader_patches")
            preference.setAttribute("android:title", "hxreborn patches")
            val intent = document.createElement("intent")
            intent.setAttribute("android:targetPackage", packageName)
            intent.setAttribute("android:targetClass", SETTINGS_ACTIVITY)
            preference.appendChild(intent)
            screen.insertBefore(preference, screen.firstChild)
        }
    }
}

@Suppress("unused")
val filterPostsPatch = bytecodePatch(
    name = "Filter posts",
    description = "Filters posts by title keyword, subreddit name or post flair, with regular expression support.",
) {
    compatibleWith(AppCompatibilities.REDREADER, AppCompatibilities.REDREADER_ALPHA)
    dependsOn(filterResourcesPatch)
    extendWith("extensions/extension.mpe")

    execute {
        mutableClassDefBy("Lapp/hxreborn/extension/redreader/SettingsStyle;").replaceResourceIdStubs(
            ResourceType.DRAWABLE, mapOf("backIconDrawable" to "ic_action_back_dark"),
        )
        val listing = PostListingFingerprint
        val (_, postId, _, addResult) = listing.instructionMatches
        listing.method.apply {
            val postRegister = getInstruction<TwoRegisterInstruction>(postId.index).registerB
            val resultRegister = getInstruction<OneRegisterInstruction>(addResult.index).registerA
            addInstructions(addResult.index + 1, """
                invoke-static { v$postRegister, v$resultRegister }, $EXTENSION_CLASS_DESCRIPTOR->keepPost(Ljava/lang/Object;Z)Z
                move-result v$resultRegister
            """)
        }
        ApplicationCreateFingerprint.method.addInstruction(
            ApplicationCreateFingerprint.instructionMatches.single().index + 1,
            "invoke-static/range { p0 .. p0 }, $SETTINGS_CLASS_DESCRIPTOR->initialize(Landroid/content/Context;)V",
        )

        val postClass = PostToStringFingerprint.classDef
        val encodedStringClass = EncodedStringToStringFingerprint.classDef
        val decodedField = encodedStringClass.fields.single { it.name == "decoded" && it.type == "Ljava/lang/String;" }
        val postFilterClass = mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR)
        mapOf("title" to "title", "subreddit" to "subreddit", "flair" to "link_flair_text").forEach { (stubName, fieldName) ->
            val field = postClass.fields.single { it.name == fieldName && it.type == encodedStringClass.type }
            postFilterClass.replaceStub(stubName, 2, """
                check-cast p0, ${postClass.type}
                iget-object v0, p0, ${descriptor(field)}
                if-eqz v0, :missing
                iget-object v0, v0, ${descriptor(decodedField)}
                return-object v0
                :missing
                const/4 v0, 0x0
                return-object v0
            """)
        }
    }
}
