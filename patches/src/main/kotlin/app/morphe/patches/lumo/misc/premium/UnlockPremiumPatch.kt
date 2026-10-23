/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.lumo.misc.PageStartedFingerprint
import app.morphe.patches.lumo.misc.pairip.restoreWidgetActionsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/lumo/LumoPlus;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks unlimited projects and removes upgrade prompts. " +
        "Usage limits for models, messages and images still apply.",
) {
    compatibleWith(AppCompatibilities.LUMO)
    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, restoreWidgetActionsPatch)
    availability(requireArm64)
    extendWith("extensions/extension.mpe")

    execute {
        PageStartedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->unlock(Landroid/webkit/WebView;)V",
        )
    }
}
