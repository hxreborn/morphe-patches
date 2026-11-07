/*
 * Copyright (C) 2026 Quantro100
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from Quantro100/Morphe-patches:
 * https://github.com/Quantro100/Morphe-patches/commit/f384d233384d2bc3d63d9f5011507f774d586b6f
 * Commit f384d233384d2bc3d63d9f5011507f774d586b6f (2026-06-02),
 * patches/src/main/kotlin/app/morphe/patches/aliexpress/media/EnableImageSavingPatch.kt
 */
package app.morphe.patches.aliexpress.misc.images

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.aliexpress.misc.settings.markPatchApplied
import app.morphe.patches.aliexpress.misc.signin.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val PATCH_SETTINGS_CLASS = "Lapp/hxreborn/extension/aliexpress/PatchSettings;"
private const val VIEWER_SAVE_BUTTON_CLASS = "Lapp/hxreborn/extension/aliexpress/ViewerSaveButton;"
private const val SAVE_BUTTON_ID = "@id/rl_save"
private const val DRAWABLE_PREFIX = "@drawable/"
private const val IMAGE_URL_REGISTERS = 2
private const val IMAGE_DOWNLOAD_REGISTERS = 3
private val PRODUCT_PHOTO_VIEWERS = 2..2

private lateinit var saveIconName: String

private val stockSaveIconPatch = resourcePatch {
    execute {
        val iconNames = decode("res/layout")
            .flatMap { path -> get(path).inputStream().use { saveButtonIconNames(document(it)) } }
            .distinct()
        saveIconName = iconNames.singleOrNull()
            ?: throw PatchException("Expected one save button icon in the layouts, found $iconNames")
    }
}

private fun saveButtonIconNames(layout: Document): List<String> {
    val elements = layout.getElementsByTagName("*")
    return (0 until elements.length).map { elements.item(it) as Element }
        .filter { it.getAttribute("android:id") == SAVE_BUTTON_ID }
        .flatMap { saveButton ->
            (0 until saveButton.childNodes.length).map(saveButton.childNodes::item)
                .filterIsInstance<Element>()
                .filter { it.tagName == "ImageView" }
                .map { it.getAttribute("android:src") }
        }
        .filter { it.startsWith(DRAWABLE_PREFIX) }
        .map { it.removePrefix(DRAWABLE_PREFIX) }
}

@Suppress("unused")
val enableImageSavingPatch: BytecodePatch = bytecodePatch(
    name = "Enable image saving",
    description = "Shows a save button on full-screen product and review photos.",
) {
    compatibleWith(AppCompatibilities.ALIEXPRESS)
    dependsOn(spoofSignaturePatch, stockSaveIconPatch)

    execute {
        markPatchApplied("enableImageSaving", enableImageSavingPatch)
        ImageSaveButtonFingerprint.matchAll(3..3).forEach { match ->
            val (_, _, saveButtonState) = match.instructionMatches
            val register = match.method.getInstruction<OneRegisterInstruction>(saveButtonState.index).registerA
            match.method.addInstructions(
                saveButtonState.index + 1,
                """
                    invoke-static/range { v$register .. v$register }, $PATCH_SETTINGS_CLASS->saveButtonHidden(Z)Z
                    move-result v$register
                """.trimIndent(),
            )
        }
        ViewerShareButtonFingerprint.matchAll(PRODUCT_PHOTO_VIEWERS).forEach { match ->
            val (_, _, shareButton) = match.instructionMatches
            val register = match.method.getInstruction<OneRegisterInstruction>(shareButton.index).registerA
            match.method.addInstructions(
                shareButton.index + 1,
                "invoke-static/range { v$register .. v$register }, $VIEWER_SAVE_BUTTON_CLASS->attach(Landroid/view/View;)V",
            )
        }
        val imageUrlGetter = RemoteImageUrlFingerprint.matchSingle().method
        val saveJobCalls = ImageDownloadJobFingerprint.matchAll()
            .map { match ->
                match.instructionMatches.map {
                    match.method.getInstruction<ReferenceInstruction>(it.index).reference.toString()
                }
            }
            .distinct()
        val (application, saveFile, _, downloader, download) = saveJobCalls.singleOrNull()
            ?: throw PatchException("Image save jobs call different save methods: $saveJobCalls")
        with(mutableClassDefBy(VIEWER_SAVE_BUTTON_CLASS)) {
            replaceResourceIdStubs(ResourceType.DRAWABLE, mapOf("saveIconId" to saveIconName))
            replaceStub(
                "imageUrlOf",
                IMAGE_URL_REGISTERS,
                """
                    instance-of v0, p0, ${imageUrlGetter.definingClass}
                    if-eqz v0, :not_remote_image
                    check-cast p0, ${imageUrlGetter.definingClass}
                    invoke-virtual { p0 }, ${descriptor(imageUrlGetter)}
                    move-result-object v0
                    return-object v0
                    :not_remote_image
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            replaceStub(
                "downloadImage",
                IMAGE_DOWNLOAD_REGISTERS,
                """
                    sget-object v0, $application
                    invoke-static { v0, p0 }, $saveFile
                    move-result-object v0
                    if-eqz v0, :failed
                    invoke-static { }, $downloader
                    move-result-object v1
                    invoke-virtual { v1, v0, p0 }, $download
                    move-result v1
                    if-eqz v1, :failed
                    return-object v0
                    :failed
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
        }
    }
}
