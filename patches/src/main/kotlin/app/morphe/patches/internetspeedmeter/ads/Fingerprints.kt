/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.internetspeedmeter.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object LoadAdsFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/internet/speed/meter/lite/MainActivity;"),
    strings = listOf("ads_enabled", "firebase_ads_enabled", "gdpr_can_request_ads"),
)

internal object AllowAdsPromptFingerprint : Fingerprint(
    definingClass = "Lcom/internet/speed/meter/lite/MainActivity;",
    name = "onCreate",
    filters = listOf(
        string("ad_enable_attempt"),
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "getInt"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal object FirstRunAdNoticeFingerprint : Fingerprint(
    definingClass = "Lcom/internet/speed/meter/lite/MainActivity;",
    name = "onCreate",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "first_run_ad_info_title"),
        methodCall(definingClass = "Landroid/app/Dialog;", name = "show", location = MatchAfterWithin(30)),
    ),
)

internal object ApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/internet/speed/meter/lite/ISMApplication;",
    name = "onCreate",
)
