/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yuka.misc.pairip

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch

val guardVirtualizedTasksPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.YUKA)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        val offlineStorageResult = OfflineStorageResultFingerprint.matchSingle().method
        offlineStorageResult.addInstructionsWithLabels(
            0,
            """
                if-nez p1, :post
                return-void
            """,
            ExternalLabel("post", offlineStorageResult.getInstruction(0)),
        )

        SecurityProviderTaskFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-static { v0 }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object v0
                return-object v0
            """,
        )
    }
}
