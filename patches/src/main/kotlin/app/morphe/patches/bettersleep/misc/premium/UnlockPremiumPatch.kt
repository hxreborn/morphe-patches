/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bettersleep.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.bettersleep.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.ReferenceUtil

internal const val PURCHASE_CLASS = "Lcom/ipnossoft/api/purchasemanager/purchaserepo/localdb/Purchase;"
internal const val DATA_SOURCE_NAME_CLASS = "Lcom/ipnossoft/api/purchasemanager/datasource/DataSourceName;"
internal const val PURCHASE_TYPE_CLASS = "Lcom/ipnossoft/api/purchasemanager/data/PurchaseType;"

private const val LIFETIME_PURCHASE_METHOD = "lifetimePurchase"
private const val PURCHASE_TIME = 1735689600000L
private const val EXPIRY_TIME = 4891363200000L

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks all premium content and skips the free trial screen.",
) {
    compatibleWith(AppCompatibilities.BETTERSLEEP)

    dependsOn(removePairipVirtualizationPatch, spoofSignaturePatch)

    execute {
        IsContentUnlockedFingerprint.matchSingle().method.returnEarly(true)

        val hasSubscription = OnAskForUserPremiumFingerprint.matchSingle()
            .instructionMatches.first().getMethodCalled()
        hasSubscription.returnEarly(true)

        val featureManager = mutableClassDefBy(hasSubscription.definingClass)
        featureManager.methods.single { it.parameters.isEmpty() && it.returnType == PURCHASE_CLASS }
            .addInstructions(
                0,
                """
                    invoke-static {}, ${featureManager.type}->$LIFETIME_PURCHASE_METHOD()$PURCHASE_CLASS
                    move-result-object v0
                    return-object v0
                """,
            )

        listOf(DATA_SOURCE_NAME_CLASS to "GOOGLE", PURCHASE_TYPE_CLASS to "PURCHASE").forEach { (type, name) ->
            check(mutableClassDefBy(type).staticFields.any { it.name == name && it.type == type }) { "$type->$name not found" }
        }

        featureManager.directMethods.add(
            lifetimePurchaseMethod(featureManager.type, PurchaseConstructorFingerprint.matchSingle().originalMethod),
        )
    }
}

private fun lifetimePurchaseMethod(definingClass: String, constructor: MethodReference) = ImmutableMethod(
    definingClass,
    LIFETIME_PURCHASE_METHOD,
    emptyList<ImmutableMethodParameter>(),
    PURCHASE_CLASS,
    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
    null,
    null,
    MutableMethodImplementation(20),
).toMutable().apply {
    addInstructions(
        0,
        """
            new-instance v0, $PURCHASE_CLASS
            const-string v1, "lifetime"
            const-string v2, ""
            const-string v3, ""
            const-wide v4, ${PURCHASE_TIME}L
            const-wide v6, ${EXPIRY_TIME}L
            const-string v8, "GOOGLE"
            sget-object v9, $DATA_SOURCE_NAME_CLASS->GOOGLE:$DATA_SOURCE_NAME_CLASS
            sget-object v10, $PURCHASE_TYPE_CLASS->PURCHASE:$PURCHASE_TYPE_CLASS
            const/4 v11, 0x0
            const/4 v12, 0x0
            const/4 v13, 0x0
            const/4 v14, 0x0
            const/4 v15, 0x0
            const/16 v16, 0x1
            const/16 v17, 0x0
            const/16 v18, 0x0
            const/16 v19, 0x0
            invoke-direct/range { v0 .. v19 }, ${ReferenceUtil.getMethodDescriptor(constructor)}
            return-object v0
        """,
    )
}
