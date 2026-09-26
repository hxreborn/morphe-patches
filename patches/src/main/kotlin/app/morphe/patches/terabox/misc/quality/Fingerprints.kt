/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.quality

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val VIDEO_SOURCE_CLASS = "Lcom/dubox/drive/preview/video/source/IVideoSource;"
internal const val MEDIA_FILE_META_CLASS = "Lcom/dubox/drive/files/domain/job/server/response/MediaFileMetaInfo;"
internal const val VIDEO_PLAY_RESOLUTION_CLASS = "Lcom/dubox/drive/preview/video/VideoPlayerConstants\$VideoPlayResolution;"

internal object PremiumResolutionThresholdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "I",
    parameters = emptyList(),
    strings = listOf("VideoURL"),
)

internal object SetMediaUrlFingerprint : Fingerprint(
    definingClass = "Lcom/dubox/drive/util/VastViewKt;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Lcom/media/vast/VastView;", "Ljava/lang/String;"),
    filters = listOf(string("outset_file_set_media_url")),
)

internal object MediaFileMetaDlinkFingerprint : Fingerprint(
    definingClass = MEDIA_FILE_META_CLASS,
    name = "getDlink",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)

internal object OnlineVideoInfoRequestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = VIDEO_SOURCE_CLASS, name = "getOnlineVideoInfo"),
        methodCall(definingClass = VIDEO_SOURCE_CLASS, name = "getOnlineSmoothPath"),
        methodCall(definingClass = "Landroid/text/TextUtils;", name = "isEmpty"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        fieldAccess(type = "Z", opcode = Opcode.IPUT_BOOLEAN, location = MatchAfterWithin(2)),
    ),
)

internal object ResolutionSwitchFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Lcom/dubox/drive/ui/preview/video/presenter/resolution/IResolution;", VIDEO_PLAY_RESOLUTION_CLASS),
    strings = listOf("sdk_setting_switch_stream_with_new_source"),
    filters = listOf(methodCall(name = "switchStreamWithNewSource")),
)

internal object ResolutionSwitchResultFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("I", "I"),
    strings = listOf("change_video_player_resolution_success"),
)

internal object PlayerReloadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Z", "L", VIDEO_PLAY_RESOLUTION_CLASS),
    strings = listOf("act_create_init_vast_view_in_main"),
)
