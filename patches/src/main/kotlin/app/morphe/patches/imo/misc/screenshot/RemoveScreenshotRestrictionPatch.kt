/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.screenshot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import app.morphe.patches.shared.misc.extension.sharedRemoveScreenshotRestrictionExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/shared/WindowFlags;"

@Suppress("unused")
val removeScreenshotRestrictionPatch = patch(
    name = "Remove screenshot restriction",
    description = "Allows screenshots and screen recording in chats, calls, media and profiles.",
) {
    dependsOn(sharedRemoveScreenshotRestrictionExtensionPatch)
    compatibleWith(AppCompatibilities.IMO)

    execute {
        routeWindowFlagsCalls(AddWindowFlagsFingerprint, "addFlags(Landroid/view/Window;I)V")
        routeWindowFlagsCalls(SetWindowFlagsFingerprint, "setFlags(Landroid/view/Window;II)V")
        SetLayoutParamsSecureFlagFingerprint.matchSingle().method.returnEarly()
    }
}

private fun routeWindowFlagsCalls(fingerprint: Fingerprint, extensionMethod: String) {
    fingerprint.matchAllMethodIndicesForEach { callIndex ->
        val call = getInstruction<FiveRegisterInstruction>(callIndex)
        val registers = listOf(call.registerC, call.registerD, call.registerE)
            .take(call.registerCount)
            .joinToString { "v$it" }
        replaceInstruction(callIndex, "invoke-static { $registers }, $EXTENSION_CLASS->$extensionMethod")
    }
}
