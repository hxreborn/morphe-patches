/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.anytracker.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.BuildCustomerInfoFingerprint
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.matchAllMethodIndicesForEach
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/shared/RevenueCatUnlock;"
private const val ENTITLEMENT = "android-platinum"
private const val PRODUCT = "android_platinum_yearly"

@Suppress("unused")
val unlockPlatinumPatch = bytecodePatch(
    name = "Unlock Platinum",
    description = "Unlocks the Platinum plan with unlimited tracked items, every-minute updates, widgets, " +
        "watchlists and backups.",
) {
    compatibleWith(AppCompatibilities.ANYTRACKER)
    dependsOn(removePairipProtectionPatch)
    extendWith("extensions/extension.mpe")

    execute {
        BuildCustomerInfoFingerprint.matchSingle().method.apply {
            val registers = getFreeRegisterProvider(0, 3)
            val jsonRegister = registers.getFreeRegister4Bit()
            val entitlementRegister = registers.getFreeRegister4Bit()
            val productRegister = registers.getFreeRegister4Bit()

            addInstructions(
                0,
                """
                    move-object/from16 v$jsonRegister, p1
                    const-string v$entitlementRegister, "$ENTITLEMENT"
                    const-string v$productRegister, "$PRODUCT"
                    invoke-static { v$jsonRegister, v$entitlementRegister, v$productRegister }, $EXTENSION_CLASS->grantEntitlement(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                """,
            )
        }

        RequestCustomerInfoFingerprint.matchSingle().let { match ->
            match.method.useCachedCustomerInfo(match.instructionMatches.first().index)

            planStateConstructorFingerprint(match.classDef.type).matchSingle().let { constructor ->
                constructor.method.loadPlatinumPlan(constructor.instructionMatches.single().index)
            }
        }

        FetchPlanFingerprint.matchSingle().let { match ->
            val (invalidateCacheIndex, fetchPolicyIndex, _, basicFallbackIndex) =
                match.instructionMatches.map { it.index }
            match.method.apply {
                loadPlatinumPlan(basicFallbackIndex)
                useCachedCustomerInfo(fetchPolicyIndex)
                removeInstruction(invalidateCacheIndex)
            }
        }

        StoredPlanDefaultFingerprint.matchAllMethodIndicesForEach { defaultIndex ->
            val register = getInstruction<OneRegisterInstruction>(defaultIndex).registerA
            replaceInstruction(defaultIndex, "const-string v$register, \"PLATINUM\"")
        }
    }
}

private fun MutableMethod.useCachedCustomerInfo(policyIndex: Int) {
    val register = getInstruction<OneRegisterInstruction>(policyIndex).registerA
    replaceInstruction(
        policyIndex,
        "sget-object v$register, $CACHE_FETCH_POLICY_CLASS->CACHED_OR_FETCHED:$CACHE_FETCH_POLICY_CLASS",
    )
}

private fun MutableMethod.loadPlatinumPlan(planReadIndex: Int) {
    val register = getInstruction<OneRegisterInstruction>(planReadIndex).registerA
    replaceInstruction(planReadIndex, "const-string v$register, \"PLATINUM\"")
    addInstructions(
        planReadIndex + 1,
        """
            invoke-static/range { v$register .. v$register }, $PLAN_CLASS->valueOf(Ljava/lang/String;)$PLAN_CLASS
            move-result-object v$register
        """,
    )
}
