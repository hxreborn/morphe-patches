/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.chapterspeech

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.readera.misc.highlightcolors.READ_ACTIVITY_CLASS
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ContentsRowClickFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.SYNTHETIC),
    returnType = "V",
    parameters = listOf("L", "Landroid/view/View;"),
    filters = listOf(
        methodCall(returnType = "Ljava/lang/Object;", parameters = emptyList()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = READ_ACTIVITY_CLASS),
        methodCall(definingClass = READ_ACTIVITY_CLASS, parameters = emptyList(), location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroidx/fragment/app/Fragment;"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;"), returnType = "V"),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = emptyList(), returnType = "V"),
    ),
)

internal object ContentsRowInitFingerprint : Fingerprint(
    classFingerprint = ContentsRowClickFingerprint,
    name = "<init>",
    filters = listOf(methodCall(definingClass = "Landroid/view/View;", name = "setOnClickListener")),
)

internal fun contentsPlaceholderFingerprint(fragment: String, message: Long) = Fingerprint(
    definingClass = fragment,
    returnType = "Landroid/view/View;",
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/widget/TextView;"),
        literal(message, location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/widget/TextView;", name = "setText", parameters = listOf("I"), location = MatchAfterImmediately()),
    ),
)

internal object SpeechStartTaskFingerprint : Fingerprint(
    name = "run",
    strings = listOf("SpeechStartTask clearSpeechUtters", "SpeechStartTask pos:%s, gen:%d"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN),
        opcode(Opcode.NEW_INSTANCE),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(name = "<init>", location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_INTERFACE, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET, type = "I"),
    ),
)

internal object ContentsTreeFingerprint : Fingerprint(strings = listOf("createTocTree"))

internal fun contentsTreeEventFingerprint(tree: String) = Fingerprint(
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_INTERFACE, parameters = emptyList(), returnType = "Ljava/util/List;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchAfterImmediately()),
        methodCall(definingClass = tree, name = "<init>", location = MatchAfterImmediately()),
    ),
)

internal object NextTrackFingerprint : Fingerprint(
    strings = listOf("SpeechService getAndPlayNextTrack"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = emptyList(), location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
    ),
)

internal fun spokenPositionFingerprint(track: String) = Fingerprint(
    definingClass = "Lorg/readera/SpeechService;",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = track, type = "Ljava/lang/Object;"),
        opcode(Opcode.INSTANCE_OF, MatchAfterImmediately()),
    ),
)

internal object SkipToNextFingerprint : Fingerprint(strings = listOf("SpeechService onSkipToNext"))

internal fun queuedSpeechLookAheadFingerprint(utterance: String) = Fingerprint(
    strings = listOf("ReadTextPlayer speak next lang:%s, text:[%s], uri:%s"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = utterance),
        opcode(Opcode.IF_EQZ, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/util/Set;", location = MatchAfterImmediately()),
        methodCall(definingClass = utterance, parameters = emptyList(), returnType = "Ljava/lang/String;", location = MatchAfterImmediately()),
    ),
)

internal fun speakDirectFallbackFingerprint(utterance: String) = Fingerprint(
    strings = listOf("ReadTextPlayer addToSpeakDirect [%s]"),
    filters = listOf(methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L", utterance), returnType = "V")),
)

internal const val SPEECH_RELAYOUT_DECODE_MATCH = 7

internal fun speechRelayoutFingerprint(position: String) = Fingerprint(
    name = "run",
    strings = listOf("SpeechPlayTask "),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET, definingClass = position, type = "I"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_INTERFACE, parameters = emptyList(), returnType = "I", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.IF_EQ, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_INTERFACE, parameters = listOf(position), returnType = "V", location = MatchAfterImmediately()),
    ),
)

internal fun positionDecoderFingerprint(position: String, decoder: String) = Fingerprint(
    name = decoder,
    returnType = "V",
    parameters = listOf(position),
    filters = listOf(methodCall(definingClass = position, parameters = emptyList(), returnType = "Z", location = MatchFirst())),
)

internal fun pageSpeechFingerprint(task: String) = Fingerprint(
    definingClass = task,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Ljava/util/List;",
    parameters = listOf("I"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchAfterImmediately()),
        opcode(Opcode.IF_GE, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.AGET_OBJECT, MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = emptyList(), returnType = "Ljava/util/List;", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_INTERFACE, returnType = "Ljava/util/List;", location = MatchAfterImmediately()),
    ),
)

internal object SpeechFinishPauseFingerprint : Fingerprint(
    strings = listOf("SpeechService getAndPlayNextTrack"),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT),
        methodCall(opcode = Opcode.INVOKE_DIRECT, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/support/v4/media/session/MediaSessionCompat\$b;", location = MatchAfterImmediately()),
        methodCall(parameters = emptyList(), returnType = "V", location = MatchAfterImmediately()),
    ),
)

internal fun firstSpeechTrackFingerprint(service: String, track: String) = Fingerprint(
    strings = listOf("SpeechService onPlay"),
    filters = listOf(methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf(service, track), returnType = "V")),
)
