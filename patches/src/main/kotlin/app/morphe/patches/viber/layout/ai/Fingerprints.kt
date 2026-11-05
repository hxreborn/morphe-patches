/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.ai

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object AppPreferenceDescriptionFingerprint : Fingerprint(
    filters = listOf(string("BaseAppPreference(key=")),
)

internal object AppPreferenceValueFingerprint : Fingerprint(
    classFingerprint = AppPreferenceDescriptionFingerprint,
    name = "get",
    returnType = "Ljava/lang/Object;",
    parameters = listOf(),
    filters = listOf(opcode(Opcode.RETURN_OBJECT)),
)

internal object AiReplyPreferencesFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("ai_reply_is_display_feature_setting_enabled"),
        methodCall(name = "<init>", parameters = listOf("Ljava/lang/String;", "Z")),
    ),
)

internal fun booleanPreferenceValueFingerprint(type: String) = Fingerprint(
    definingClass = type,
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(type = "Ljava/lang/String;", opcode = Opcode.IGET_OBJECT),
        methodCall(name = "getBoolean", parameters = listOf("Ljava/lang/String;", "Z"), returnType = "Z"),
        opcode(Opcode.RETURN),
    ),
)

internal object ImageRemixIntroductionFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "L", "L", "L"),
    filters = listOf(string("image_remix_ftue_dialog")),
)

internal object ImageRemixPresetsFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("com.viber.voip.custom_cam_media_preview.show_image_remix_presets"),
        methodCall(
            definingClass = "Landroid/os/BaseBundle;",
            name = "getBoolean",
            parameters = listOf("Ljava/lang/String;", "Z"),
            returnType = "Z",
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)
