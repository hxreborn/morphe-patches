/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.ai

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import app.morphe.patches.viber.shared.preferenceFilters
import com.android.tools.smali.dexlib2.Opcode

private const val INLINE_TRANSLATE_VIEW_NULL_CHECK_WINDOW = 1
private const val CHATGPT_SETTINGS_ROW_CREATION_WINDOW = 2

internal object SearchChatGptEntryPointProviderFingerprint : Fingerprint(
    filters = listOf(
        string("getEntryPointProvider()Lcom/viber/voip/feature/search/data/experiments/SearchChatGptEntryPointExperimentsProvider;"),
    ),
)

internal object SearchChatGptEntryPointAvailabilityFingerprint : Fingerprint(
    classFingerprint = SearchChatGptEntryPointProviderFingerprint,
    returnType = "Z",
    filters = listOf(methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue")),
)

internal object SettingsChatGptRowFingerprint : Fingerprint(
    filters = listOf(
        methodCall(parameters = listOf("Z"), returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        resourceLiteral(
            ResourceType.STRING,
            "pref_category_manage_ai_features_key",
            location = MatchAfterWithin(CHATGPT_SETTINGS_ROW_CREATION_WINDOW),
        ),
    ),
)

internal object InlineTranslateBinderFingerprint : Fingerprint(
    filters = listOf(
        string("getKeyboardLanguagesProvider()Lcom/viber/voip/feature/translate/message/domain/TranslateKeyboardLanguagesProvider;"),
    ),
)

internal object InlineTranslateVisibilityFingerprint : Fingerprint(
    classFingerprint = InlineTranslateBinderFingerprint,
    filters = listOf(
        methodCall(
            parameters = listOf("L", "L"),
            returnType = "Z",
            opcodes = listOf(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE),
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        fieldAccess(type = "Landroid/view/View;", opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "getTag",
            parameters = emptyList(),
            returnType = "Ljava/lang/Object;",
            location = MatchAfterWithin(INLINE_TRANSLATE_VIEW_NULL_CHECK_WINDOW),
        ),
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

internal object NewsBriefBannerManagerFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("getBannerSettingsRepository()Lcom/viber/voip/feature/ai/news/brief/domain/BannerSettingsRepository;"),
    ),
)

internal object NewsBriefBannerEligibilityFingerprint : Fingerprint(
    classFingerprint = NewsBriefBannerManagerFingerprint,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lkotlin/coroutines/jvm/internal/ContinuationImpl;"),
)
