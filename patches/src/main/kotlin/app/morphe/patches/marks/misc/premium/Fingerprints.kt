/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.marks.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal enum class PremiumProperty(
    val fieldName: String,
    val getterName: String,
    val unlockedValue: Boolean,
    private val count208: Int,
    private val count207: Int = count208,
) {
    MEMBERSHIP("isUserPremium", "isUserPremium", true, 79, 75),
    CUSTOM_TEST_MEMBERSHIP("isPremiumUser", "isPremiumUser", true, 1),
    COURSE_ACCESS("isCourseAccessible", "isCourseAccessible", true, 7),
    QUESTION_ACCESS("hasAccess", "getHasAccess", true, 7),
    PRACTICE_ACCESS("isAccessible", "isAccessible", true, 4),
    SUBJECT_ACCESS("isSubjectUnlockedForUser", "isSubjectUnlockedForUser", true, 2),
    QUESTION_LIMIT("isPremiumLocked", "isPremiumLocked", false, 1),
    CONTENT_LOCK("isLocked", "isLocked", false, 12, 8),
    COURSE_CONTENT_LOCK("locked", "getLocked", false, 2),
    UPGRADE_BUTTON("showPremiumBt", "getShowPremiumBt", false, 12, 11),
    ;

    fun expectedCount(version: String): Int = when (version) {
        "2.0.8" -> count208
        "2.0.7" -> count207
        else -> throw PatchException("Unsupported MARKS version $version")
    }
}

internal fun premiumPropertyFingerprint(property: PremiumProperty) = Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    custom = { method, classDef ->
        val stringLiterals = method.implementation?.instructions?.mapNotNull {
            it.getReference<StringReference>()?.string
        }.orEmpty()
        classDef.type.startsWith("Lcom/scoremarks/marks/") &&
            classDef.methods.any {
                it.name == property.getterName && it.parameterTypes.isEmpty() &&
                    (it.returnType == "Z" || it.returnType == "Ljava/lang/Boolean;")
            } &&
            stringLiterals.any { it.endsWith("${property.fieldName}=") } &&
            stringLiterals.none { it.startsWith("LeagueQuest(") || it.startsWith("Badge(") }
    },
)

internal object UnverifiedPhoneRoutingFingerprint : Fingerprint(
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        string("splash_auth"),
        methodCall(definingClass = "Lcom/scoremarks/marks/data/models/auth/User;", name = "isProfileCompleted"),
        opcode(Opcode.CONST_CLASS),
        methodCall(definingClass = "Lcom/scoremarks/marks/data/models/auth/User;", name = "isPhoneVerified"),
        newInstance("Landroid/content/Intent;"),
    ),
)

internal object InAppUpdateCheckFingerprint : Fingerprint(
    definingClass = "Lcom/scoremarks/marks/ui/activities/SplashActivity;",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(methodCall(definingClass = "Landroid/content/Context;", name = "getApplicationContext")),
)

internal object InstallReferrerClientFingerprint : Fingerprint(
    superclass = "Lcom/android/installreferrer/api/InstallReferrerClient;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal object InstallReferrerListenerFingerprint : Fingerprint(
    name = "onInstallReferrerSetupFinished",
    returnType = "V",
    parameters = listOf("I"),
    strings = listOf("Raw Referrer: "),
)

internal object InstallReferrerStartFingerprint : Fingerprint(
    definingClass = "Lcom/android/installreferrer/api/InstallReferrerClient;",
    returnType = "V",
    parameters = listOf("Lcom/android/installreferrer/api/InstallReferrerStateListener;"),
)

internal fun installReferrerListenerConstructorFingerprint(listenerType: String) = Fingerprint(
    definingClass = listenerType,
    name = "<init>",
    returnType = "V",
    parameters = listOf("I", "Ljava/lang/Object;", "Ljava/lang/Object;"),
)
