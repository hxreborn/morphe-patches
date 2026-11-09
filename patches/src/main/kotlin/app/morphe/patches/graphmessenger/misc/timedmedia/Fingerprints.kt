/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.timedmedia

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val MESSAGE_CLASS = "Lorg/telegram/tgnet/TLRPC\$Message;"
internal const val MESSAGE_MEDIA_CLASS = "Lorg/telegram/tgnet/TLRPC\$MessageMedia;"
private const val READ_MESSAGE_CONTENTS_CLASS = "Lorg/telegram/tgnet/TLRPC\$TL_messages_readMessageContents;"
private const val READ_CHANNEL_MESSAGE_CONTENTS_CLASS = "Lorg/telegram/tgnet/TLRPC\$TL_channels_readMessageContents;"

internal const val MEDIA_TIMER_FIELD = "ttl_seconds"

internal val mediaTimerRead =
    fieldAccess(opcode = Opcode.IGET, definingClass = MESSAGE_MEDIA_CLASS, name = MEDIA_TIMER_FIELD)
internal val messageTimerRead =
    fieldAccess(opcode = Opcode.IGET, definingClass = MESSAGE_CLASS, name = "ttl")

internal object IsSecretMediaFingerprint : Fingerprint(
    name = "isSecretMedia",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(mediaTimerRead),
)

internal object IsVoiceOnceFingerprint : Fingerprint(
    name = "isVoiceOnce",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(mediaTimerRead),
)

internal object IsRoundOnceFingerprint : Fingerprint(
    name = "isRoundOnce",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(mediaTimerRead),
)

internal object NeedDrawBluredPreviewFingerprint : Fingerprint(
    name = "needDrawBluredPreview",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(name = "hasExtendedMediaPreview", returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal object ShouldEncryptPhotoOrVideoFingerprint : Fingerprint(
    name = "shouldEncryptPhotoOrVideo",
    returnType = "Z",
    parameters = listOf("I", MESSAGE_CLASS),
    filters = listOf(mediaTimerRead),
)

internal object MarkMessageContentAsReadFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        newInstance(READ_CHANNEL_MESSAGE_CONTENTS_CLASS),
        newInstance(READ_MESSAGE_CONTENTS_CLASS),
    ),
)
