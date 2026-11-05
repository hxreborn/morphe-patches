/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.ai

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val PREFERENCE_CALL_WINDOW = 1

private fun preferenceFilters(key: String) = listOf(
    string(key),
    methodCall(
        parameters = listOf("Ljava/lang/String;", "Z"),
        location = MatchAfterWithin(PREFERENCE_CALL_WINDOW),
    ),
)

internal object ChatSummaryPreferenceFingerprint : Fingerprint(
    filters = preferenceFilters("display_chat_summary_feature_key"),
)

internal object ImageRemixPreferenceFingerprint : Fingerprint(
    filters = preferenceFilters("display_image_remix_feature_key"),
)

internal object AiInChatPreferenceFingerprint : Fingerprint(
    filters = preferenceFilters("display_ai_in_chat_feature_key"),
)

internal object AiReplyPreferenceFingerprint : Fingerprint(
    filters = preferenceFilters("ai_reply_is_display_feature_setting_enabled"),
)

internal object SmartRecapPreferenceFingerprint : Fingerprint(
    filters = preferenceFilters("smart_recap_is_enabled"),
)

internal object AiFeaturesBannerPreferenceFingerprint : Fingerprint(
    filters = preferenceFilters("general_ai_features_banner_dismissed_key"),
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
