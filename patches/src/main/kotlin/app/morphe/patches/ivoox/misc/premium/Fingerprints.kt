/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ivoox.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val SUBSCRIBER_SKILL_KEYS = listOf(
    "adsfree",
    "skill_boost_volume",
    "skill_car_mode",
    "skill_unlimited_smartlists",
)

internal object UserSkillEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = emptyList(),
    strings = SUBSCRIBER_SKILL_KEYS,
)

internal object StoredUserSkillsFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
    filters = listOf(
        string("USER_KEY_SKILLS"),
        methodCall(definingClass = "Landroid/content/SharedPreferences;", name = "getStringSet"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object PremiumPurchasedFingerprint : Fingerprint(
    classFingerprint = StoredUserSkillsFingerprint,
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("pref_premium_purchased"),
)

internal object ProContractFingerprint : Fingerprint(
    classFingerprint = StoredUserSkillsFingerprint,
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("pref_has_pro_contract"),
)

internal object HidePlusFingerprint : Fingerprint(
    classFingerprint = StoredUserSkillsFingerprint,
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("HIDE_PLUS"),
)

private const val CTA_BANNER_TYPE = "Lcom/ivoox/app/premium/data/model/CtaBannerType;"

internal object PlayerCtaBannerFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        fieldAccess(definingClass = CTA_BANNER_TYPE, name = "PLUS"),
        fieldAccess(definingClass = CTA_BANNER_TYPE, name = "FAN"),
    ),
)

internal object ActiveSplashAdFingerprint : Fingerprint(
    returnType = "Lcom/ivoox/app/splashScreenAd/domain/SplashAdDto;",
    parameters = listOf("Ljava/util/List;"),
    strings = listOf("splashAds"),
)

internal object PlayerMediaItemFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/net/Uri;"),
    filters = listOf(
        methodCall(definingClass = "Landroid/net/Uri;", name = "getPath"),
        string("_encrypted"),
    ),
)

internal object DynamicAlertClosedFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Lcom/ivoox/app/dynamiccontent/data/model/alertmodels/DynamicAlertDto;"),
    filters = listOf(
        string("alert"),
        methodCall(name = "getCapping"),
    ),
)
