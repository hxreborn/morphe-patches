/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.notification

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val INVOKE_RESULT_WINDOW = 1
private const val PREVIOUS_MEDIA_ACTION = 0x10
private const val NEXT_MEDIA_ACTION = 0x20
private const val STOP_MEDIA_ACTION = 1

internal object SpeechNotificationFingerprint : Fingerprint(
    returnType = "Landroid/app/Notification;",
    filters = listOf(
        literal(PREVIOUS_MEDIA_ACTION),
        literal(NEXT_MEDIA_ACTION),
        opcode(Opcode.FILLED_NEW_ARRAY),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(parameters = emptyList(), returnType = "Landroid/app/Notification;"),
    ),
)

internal object SpeechStopIntentFingerprint : Fingerprint(
    classFingerprint = SpeechNotificationFingerprint,
    returnType = "Landroid/app/Notification;",
    filters = listOf(
        literal(STOP_MEDIA_ACTION),
        methodCall(parameters = listOf("Landroid/content/Context;", "J"), returnType = "Landroid/app/PendingIntent;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal object SleepTimerFingerprint : Fingerprint(
    filters = listOf(
        methodCall(parameters = emptyList(), opcode = Opcode.INVOKE_STATIC),
        fieldAccess(type = "I", opcode = Opcode.IGET, location = MatchAfterWithin(INVOKE_RESULT_WINDOW)),
        string("00:00", location = MatchAfterImmediately()),
        methodCall(parameters = emptyList(), opcode = Opcode.INVOKE_STATIC),
        methodCall(parameters = emptyList(), returnType = "J", opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterWithin(INVOKE_RESULT_WINDOW)),
    ),
)

internal fun addNotificationActionFingerprint(builder: String) = Fingerprint(
    definingClass = builder,
    parameters = listOf("I", "Ljava/lang/CharSequence;", "Landroid/app/PendingIntent;"),
    returnType = builder,
)

internal object SleepTimerUpdatedFingerprint : Fingerprint(
    strings = listOf("SpeechMediaController EventTtsTimerUpdated"),
)

internal fun sleepTimerHandlerFingerprint(service: String, event: String) = Fingerprint(
    definingClass = service,
    name = "onEventMainThread",
    parameters = listOf(event),
    returnType = "V",
)

internal object RefreshSpeechNotificationFingerprint : Fingerprint(
    strings = listOf("SpeechService refreshNotificationAndForegroundStatus GO"),
)

internal object SetPlaybackStateFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Landroid/media/session/MediaSession;",
            name = "setPlaybackState",
            parameters = listOf("Landroid/media/session/PlaybackState;"),
            returnType = "V",
        ),
    ),
)

internal object CustomActionCallbackFingerprint : Fingerprint(
    superclass = "Landroid/media/session/MediaSession\$Callback;",
    name = "onCustomAction",
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
    returnType = "V",
)
