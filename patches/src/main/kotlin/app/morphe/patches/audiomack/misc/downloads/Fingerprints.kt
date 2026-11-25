/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audiomack.misc.downloads

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
import com.android.tools.smali.dexlib2.iface.Method

internal const val MUSIC_CLASS = "Lcom/audiomack/model/music/Music;"

private const val METADATA_KEY_PREFIX = "android.media.metadata."

internal val DOWNLOAD_FOLDER_SITES = 2..3

private const val MUSIC_READ_TO_DOWNLOAD_ROW_WINDOW = 9

private fun musicStringRead() = fieldAccess(definingClass = MUSIC_CLASS, opcode = Opcode.IGET_OBJECT, type = "Ljava/lang/String;")

private fun metadataKey(key: String) = string(METADATA_KEY_PREFIX + key, location = MatchAfterImmediately())

internal object ApplicationCreateFingerprint : Fingerprint(
    definingClass = "Lcom/audiomack/MainApplication;",
    name = "onCreate",
    parameters = emptyList(),
    returnType = "V",
)

internal object MediaMetadataArtistFingerprint : Fingerprint(
    filters = listOf(
        musicStringRead(),
        metadataKey("MEDIA_ID"),
        musicStringRead(),
        metadataKey("ARTIST"),
    ),
)

internal fun mediaMetadataAlbumFingerprint(metadataMethod: Method) = Fingerprint(
    definingClass = metadataMethod.definingClass,
    name = metadataMethod.name,
    filters = listOf(
        musicStringRead(),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        fieldAccess(
            definingClass = MUSIC_CLASS,
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/String;",
            location = MatchAfterImmediately(),
        ),
        metadataKey("ALBUM"),
    ),
)

internal fun mediaMetadataTitleFingerprint(metadataMethod: Method) = Fingerprint(
    definingClass = metadataMethod.definingClass,
    name = metadataMethod.name,
    filters = listOf(
        musicStringRead(),
        metadataKey("TITLE"),
    ),
)

internal object DownloadFolderFingerprint : Fingerprint(
    returnType = "Ljava/io/File;",
    filters = listOf(
        methodCall(definingClass = "Landroid/content/Context;", name = "getExternalFilesDir"),
        opcode(Opcode.NEW_INSTANCE),
        opcode(Opcode.CONST_STRING, location = MatchAfterImmediately()),
        methodCall(
            definingClass = "Ljava/io/File;",
            name = "<init>",
            parameters = listOf("Ljava/io/File;", "Ljava/lang/String;"),
            location = MatchAfterImmediately(),
        ),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, type = "Ljava/io/File;"),
    ),
)

internal object DownloadRowLabelFingerprint : Fingerprint(
    parameters = emptyList(),
    returnType = "I",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "kebab_action_download_remove"),
        resourceLiteral(ResourceType.STRING, "kebab_action_download"),
    ),
)

internal fun actionRowsFingerprint(downloadRowType: String) = Fingerprint(
    filters = listOf(
        fieldAccess(definingClass = MUSIC_CLASS),
        methodCall(
            definingClass = downloadRowType,
            name = "<init>",
            location = MatchAfterWithin(MUSIC_READ_TO_DOWNLOAD_ROW_WINDOW),
        ),
        methodCall(definingClass = "Ljava/util/ArrayList;", name = "add", location = MatchAfterImmediately()),
    ),
)

internal fun actionClickFingerprint(rowBaseType: String) = Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = rowBaseType),
        string("Kebab Menu"),
        fieldAccess(definingClass = "Lkotlin/Unit;", opcode = Opcode.SGET_OBJECT),
    ),
)
