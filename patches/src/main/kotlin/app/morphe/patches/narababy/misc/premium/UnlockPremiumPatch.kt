/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.narababy.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.narababy.shared.DATABASE_SNAPSHOTS_CLASS
import app.morphe.patches.narababy.shared.rewriteDatabaseSnapshots
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.narababyDatabaseSnapshotsExtensionPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val SUBSCRIPTION_CARDS_CLASS = "Lapp/hxreborn/extension/narababy/SubscriptionCards;"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Nara Lifetime for every family the account belongs to.",
) {
    dependsOn(narababyDatabaseSnapshotsExtensionPatch)
    compatibleWith(AppCompatibilities.NARA_BABY)
    execute {
        ResponseBuilderBuildFingerprint.method.apply {
            val returnIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
            val responseRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA
            addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static { v$responseRegister }, $SUBSCRIPTION_CARDS_CLASS->removeSubscriptionCards(Lokhttp3/Response;)Lokhttp3/Response;
                    move-result-object v$responseRegister
                """,
            )
        }
        rewriteDatabaseSnapshots("$DATABASE_SNAPSHOTS_CLASS->addLifetimeEntitlement(Ljava/lang/Object;)V")
    }
}
