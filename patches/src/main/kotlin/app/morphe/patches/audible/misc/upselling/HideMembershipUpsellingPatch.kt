/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audible.misc.upselling

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val hideMembershipUpsellingPatch = bytecodePatch(
    name = "Hide membership upselling",
    description = "Hides the membership promotion on the Home screen and the free trial bottom sheet.",
) {
    compatibleWith(AppCompatibilities.AUDIBLE)

    execute {
        BookwallSectionMapperFingerprint.matchSingle().method.returnEarly(null)

        AppHomeBottomSheetCoordinatorFingerprint.matchSingle().method
            .addInstruction(0, "const/4 p1, 0x1")
    }
}
