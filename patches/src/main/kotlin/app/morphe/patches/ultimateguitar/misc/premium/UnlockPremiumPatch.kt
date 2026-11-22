/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimateguitar.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.ultimateguitar.misc.pairip.restoreVirtualizedMethodsPatch
import app.morphe.patches.shared.misc.extension.ultimateguitarUnlockPremiumExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/ultimateguitar/ApiResponseRewriter;"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Pro, Sing and Practice features and removes ads. Paid course lessons and Songbooks are not unlocked.",
) {
    dependsOn(ultimateguitarUnlockPremiumExtensionPatch)
    compatibleWith(AppCompatibilities.ULTIMATE_GUITAR)

    dependsOn(restoreVirtualizedMethodsPatch, removePairipProtectionPatch)

    execute {
        ResponseReceivedFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p3, p4 }, $EXTENSION_CLASS->rewriteStatus(Ljava/lang/String;I)I
                move-result p4
            """,
        )
        TextDataReceivedFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p3 }, $EXTENSION_CLASS->rewriteBody(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p3
            """,
        )
        BlobResponseDataFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS->rewriteBody([B)[B
                move-result-object p1
            """,
        )
    }
}
