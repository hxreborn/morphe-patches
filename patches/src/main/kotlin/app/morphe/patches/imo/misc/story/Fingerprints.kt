/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.story

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.imo.shared.RemoteSettingFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object StoryGalleryDurationFingerprint : RemoteSettingFingerprint("getStoryGalleryDurationLimit")

internal object StoryRecordDurationFingerprint : RemoteSettingFingerprint("storyRecordDuration")

internal object DisableRecordStoryInCallFingerprint : RemoteSettingFingerprint("getDisableRecordStoryInCall")

internal object CameraCallConflictFingerprint : Fingerprint(
    name = "onResume",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT),
        methodCall(returnType = "Z", parameters = emptyList(), location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.SGET_OBJECT),
        methodCall(returnType = "Z", parameters = emptyList(), location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        string("av_conflict"),
    ),
)
