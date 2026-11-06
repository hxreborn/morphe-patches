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

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val SAVE_BUTTON_READ_WINDOW = 2
private const val SAVE_FILE_PATH_WINDOW = 2
private const val DOWNLOADER_LOOKUP_WINDOW = 1
private const val DOWNLOAD_CALL_WINDOW = 3

internal object ImageSaveButtonFingerprint : Fingerprint(
    filters = listOf(
        string("hideSaveButton"),
        methodCall(
            definingClass = "Landroid/os/Bundle;",
            name = "getBoolean",
            parameters = listOf("Ljava/lang/String;", "Z"),
            returnType = "Z",
            location = MatchAfterWithin(SAVE_BUTTON_READ_WINDOW),
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)

internal object ViewerShareButtonFingerprint : Fingerprint(
    name = "onCreateView",
    returnType = "Landroid/view/View;",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "rl_share"),
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "findViewById",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object RemoteImageUrlFingerprint : Fingerprint(
    definingClass = "Lcom/alibaba/aliexpress/painter/widget/RemoteImageView;",
    name = "getImageUrl",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
)

internal object ImageDownloadJobFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, type = "Lcom/aliexpress/app/AEApp;"),
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = "Ljava/io/File;", location = MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/io/File;", name = "getAbsolutePath", location = MatchAfterWithin(SAVE_FILE_PATH_WINDOW)),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf(), location = MatchAfterWithin(DOWNLOADER_LOOKUP_WINDOW)),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("Ljava/io/File;", "Ljava/lang/String;"),
            returnType = "Z",
            location = MatchAfterWithin(DOWNLOAD_CALL_WINDOW),
        ),
        string("PicViewFragment"),
    ),
)
