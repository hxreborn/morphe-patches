/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.deshtelugu.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val SUPABASE_REST_URL = "https://tofjpqpjvukyhdolcyic.supabase.co/rest/v1/"

private fun remoteConfigUrl(key: String) = listOf(
    string(key),
    methodCall(
        opcode = Opcode.INVOKE_STATIC,
        returnType = "Ljava/lang/String;",
        parameters = listOf("Ljava/lang/String;"),
        location = MatchAfterImmediately(),
    ),
)

internal object FullSentenceSenderFingerprint : Fingerprint(
    returnType = "V",
    filters = remoteConfigUrl("full_sentence_analytics_url") + listOf(
        string("FullSentenceDataCollection"),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, returnType = "V", parameters = listOf("L")),
    ),
)

internal object EventBatchSenderFingerprint : Fingerprint(
    returnType = "V",
    filters = remoteConfigUrl("analytics_url") + string("/v5/submit"),
)

internal object InstallSenderFingerprint : Fingerprint(
    filters = remoteConfigUrl("install_analytics_url"),
)

internal object KeyboardActiveSenderFingerprint : Fingerprint(
    filters = remoteConfigUrl("keyboard_active_analytics_url"),
)

internal object PerformanceSenderFingerprint : Fingerprint(
    filters = remoteConfigUrl("performance_analytics_url"),
)

internal object PlayStoreSearchInstallSenderFingerprint : Fingerprint(
    filters = remoteConfigUrl("playstore_search_install_analytics_url"),
)

internal object QuickMessageEventSenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "L"),
    filters = listOf(string("quick_messages")),
)

internal object ContentShareEventSenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;", "L"),
    filters = listOf(string("StickerAnalytics")),
)

internal object HeartbeatPingerFingerprint : Fingerprint(
    filters = remoteConfigUrl("heartbeat_analytics_url") + methodCall(
        name = "<init>",
        parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "L"),
    ),
)

internal fun heartbeatPingFingerprint(pingerClass: String) = Fingerprint(
    definingClass = pingerClass,
    returnType = "V",
    parameters = listOf("J", "L", "L"),
)

internal object DiagnosticsSenderFingerprint : Fingerprint(
    filters = remoteConfigUrl("feedback_diagnostics_url"),
)

internal object RawEventSenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    filters = listOf(string("/v4/event")),
)

internal object LearnedWordsSenderFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(string("log_words"), string("learnedData")),
)

internal object VoiceInputSenderFingerprint : Fingerprint(
    filters = listOf(string("https://voice-analytics.desh.app/v7/submit")),
)

internal object StickerStatsSenderFingerprint : Fingerprint(
    filters = listOf(string("https://sticker-analytics.desh.app/v3/telugu/sticker/stats/")),
)

internal object KeystrokeSuggestionSenderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    filters = listOf(
        string(SUPABASE_REST_URL + "keystroke_suggestions"),
        string("KeystrokeSuggestionAnalytics"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object EmojiSuggestionSenderFingerprint : Fingerprint(
    filters = listOf(string(SUPABASE_REST_URL + "emoji_suggestion_analytics")),
)

internal object EmojiSearchSenderFingerprint : Fingerprint(
    filters = listOf(string(SUPABASE_REST_URL + "emoji_search_analytics")),
)

internal object ReviewPromptSenderFingerprint : Fingerprint(
    filters = listOf(string(SUPABASE_REST_URL + "review_prompt_analytics")),
)

internal object AppSizeSenderFingerprint : Fingerprint(
    filters = listOf(string(SUPABASE_REST_URL + "app_size_collection")),
)

internal object UserHistorySenderFingerprint : Fingerprint(
    filters = listOf(string(SUPABASE_REST_URL + "user_history_native_analytics")),
)

internal object SetupDeviceInfoSenderFingerprint : Fingerprint(
    filters = listOf(string(SUPABASE_REST_URL + "easy_config_gegevensverzameling_v2?installation_id=eq.")),
)

internal object MetaRetentionPingFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("J", "L", "L"),
    strings = listOf("facebook_sdk_logger_crash", "fb_mobile_level_achieved"),
)
