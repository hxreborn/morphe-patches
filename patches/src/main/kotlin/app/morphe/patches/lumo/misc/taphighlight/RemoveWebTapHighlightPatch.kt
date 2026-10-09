/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.taphighlight

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.lumo.misc.PageFinishedFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/shared/WebTapHighlight;"

@Suppress("unused")
val removeWebTapHighlightPatch = bytecodePatch(
    name = "Remove tap highlight",
    description = "Removes the highlight flash on tapped buttons and links.",
) {
    compatibleWith(AppCompatibilities.LUMO)
    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)
    availability(requireArm64)
    extendWith("extensions/extension.mpe")

    execute {
        PageFinishedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->remove(Landroid/webkit/WebView;)V",
        )
    }
}
