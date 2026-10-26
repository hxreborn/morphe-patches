/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aloa.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

private const val ENTITLEMENT_ID = "subscribed"
private const val ENTITLEMENT_CLASS = "Lcom/superwall/sdk/models/entitlements/Entitlement;"
private const val ACTIVE_STATUS_CLASS = "Lcom/superwall/sdk/models/entitlements/SubscriptionStatus\$Active;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks conversations, tutor lessons and roadmaps.",
) {
    compatibleWith(AppCompatibilities.ALOA)

    dependsOn(removePairipProtectionPatch)

    execute {
        SetSubscriptionStatusFingerprint.matchSingle().method.apply {
            val registers = getFreeRegisterProvider(0, 2)
            val entitlementRegister = registers.getFreeRegister4Bit()
            val statusRegister = registers.getFreeRegister4Bit()

            addInstructions(
                0,
                """
                    new-instance v$entitlementRegister, $ENTITLEMENT_CLASS
                    const-string v$statusRegister, "$ENTITLEMENT_ID"
                    invoke-direct { v$entitlementRegister, v$statusRegister }, $ENTITLEMENT_CLASS-><init>(Ljava/lang/String;)V
                    invoke-static { v$entitlementRegister }, Ljava/util/Collections;->singleton(Ljava/lang/Object;)Ljava/util/Set;
                    move-result-object v$entitlementRegister
                    new-instance v$statusRegister, $ACTIVE_STATUS_CLASS
                    invoke-direct { v$statusRegister, v$entitlementRegister }, $ACTIVE_STATUS_CLASS-><init>(Ljava/util/Set;)V
                    move-object p1, v$statusRegister
                """,
            )
        }
    }
}
