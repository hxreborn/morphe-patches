/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.marks.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnBoxedBooleanEarly
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val AUTH_ACTIVITY = "Lcom/scoremarks/marks/ui/onboarding_flow/auth/AuthActivity;"
private const val MEDICAL_ACTIVITY = "Lcom/scoremarks/marks/medical/MedicalActivity;"
private const val MEDICAL_DASHBOARD_DATA = "Lcom/scoremarks/marks/data/models/medical/dashboard/MedicalDashboardResponse\$Data;"
private const val HOME_DASHBOARD_DATA = "Lcom/scoremarks/marks/data/models/dashboardItems/GetNewHomeDashboardResponse\$Data;"
private const val INSTALL_REFERRER_LISTENER_SELECTOR = 0x18

@Suppress("unused")
val unlockPremiumPatch: BytecodePatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium practice features. Paid books, DPPs, NCERT questions, and videos are not unlocked. " +
        "Requires a signed-in account.",
) {
    compatibleWith(AppCompatibilities.MARKS)

    execute {
        val referrerClientType = InstallReferrerClientFingerprint.matchSingle().classDef.type
        val referrerListenerType = InstallReferrerListenerFingerprint.matchSingle().classDef.type
        installReferrerListenerConstructorFingerprint(referrerListenerType).matchSingle()
        val startConnectionName = InstallReferrerStartFingerprint.matchSingle().method.name
        InAppUpdateCheckFingerprint.matchSingle().method.addInstructions(
            0,
            """
                new-instance v0, $referrerClientType
                invoke-direct {v0, p0}, $referrerClientType-><init>(Landroid/content/Context;)V
                new-instance v1, $referrerListenerType
                const/16 v2, $INSTALL_REFERRER_LISTENER_SELECTOR
                invoke-direct {v1, v2, v0, p0}, $referrerListenerType-><init>(ILjava/lang/Object;Ljava/lang/Object;)V
                invoke-virtual {v0, v1}, $referrerClientType->$startConnectionName(Lcom/android/installreferrer/api/InstallReferrerStateListener;)V
                return-void
            """,
        )
        UnverifiedPhoneRoutingFingerprint.matchSingle().apply {
            val (_, _, authActivityClassLoad) = instructionMatches
            val authActivityInstruction = authActivityClassLoad.getInstruction<OneRegisterInstruction>()
            if (authActivityInstruction.getReference<TypeReference>()?.type != AUTH_ACTIVITY) {
                throw PatchException("MARKS auth activity class changed")
            }
            val classRegister = authActivityInstruction.registerA
            method.replaceInstruction(authActivityClassLoad.index, "const-class v$classRegister, $MEDICAL_ACTIVITY")
        }
        PremiumProperty.entries.forEach { property ->
            val count = property.expectedCount(packageMetadata.versionName)
            premiumPropertyFingerprint(property).matchAll(count..count).forEach matches@{ match ->
                if (
                    match.classDef.type.contains("/data/models/marks_selected/") ||
                    match.classDef.type.contains("/data/models/dpp/") ||
                    match.classDef.type == MEDICAL_DASHBOARD_DATA ||
                    match.classDef.type == HOME_DASHBOARD_DATA
                ) return@matches
                match.classDef.methods.single {
                    it.name == property.getterName && it.parameterTypes.isEmpty()
                }.apply {
                    if (returnType == "Z") {
                        returnEarly(property.unlockedValue)
                    } else {
                        returnBoxedBooleanEarly(property.unlockedValue)
                    }
                }
            }
        }
    }
}
