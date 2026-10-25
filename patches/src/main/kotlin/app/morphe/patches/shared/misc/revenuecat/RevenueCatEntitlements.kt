/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.revenuecat

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/shared/RevenueCatUnlock;"
private const val GRANT = "$EXTENSION_CLASS->grantEntitlement(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V"
private const val GRANT_LIFETIME =
    "$EXTENSION_CLASS->grantLifetimeEntitlement(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V"

internal fun BytecodePatchContext.grantRevenueCatEntitlements(vararg entitlements: Pair<String, String>) =
    injectGrants(GRANT, entitlements)

internal fun BytecodePatchContext.grantRevenueCatLifetimeEntitlement(entitlement: String, product: String) =
    injectGrants(GRANT_LIFETIME, arrayOf(entitlement to product))

private fun BytecodePatchContext.injectGrants(grant: String, entitlements: Array<out Pair<String, String>>) {
    BuildCustomerInfoFingerprint.matchSingle().method.apply {
        val registers = getFreeRegisterProvider(0, 3)
        val jsonRegister = registers.getFreeRegister4Bit()
        val entitlementRegister = registers.getFreeRegister4Bit()
        val productRegister = registers.getFreeRegister4Bit()

        val grants = entitlements.joinToString("\n") { (entitlement, product) ->
            """
                const-string v$entitlementRegister, "$entitlement"
                const-string v$productRegister, "$product"
                invoke-static { v$jsonRegister, v$entitlementRegister, v$productRegister }, $grant
            """
        }

        addInstructions(0, "move-object/from16 v$jsonRegister, ${customerInfoJsonParameter()}\n$grants")
    }
}

private fun MutableMethod.customerInfoJsonParameter() =
    if (AccessFlags.STATIC.isSet(accessFlags)) "p0" else "p1"
