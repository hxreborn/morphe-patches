/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimateguitar.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.ultimateguitar.misc.pairip.restoreVirtualizedMethodsPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/ultimateguitar/ApiResponseRewriter;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Pro, Courses, Sing and Practice features and removes ads. Songbooks are not unlocked.",
) {
    compatibleWith(AppCompatibilities.ULTIMATE_GUITAR)
    extendWith("extensions/extension.mpe")

    dependsOn(restoreVirtualizedMethodsPatch, removePairipProtectionPatch)

    execute {
        ResponseReceivedFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p3, p4 }, $EXTENSION_CLASS->rewriteStatus(Ljava/lang/String;I)I
                move-result p4
            """,
        )
        TextDataReceivedFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p3 }, $EXTENSION_CLASS->rewriteBody(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p3
            """,
        )
        BlobResponseDataFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS->rewriteBody([B)[B
                move-result-object p1
            """,
        )
    }
}
