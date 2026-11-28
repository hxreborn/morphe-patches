/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.inputspacing

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patches.lumo.misc.pairip.restoreVirtualizedMethodsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.parameterRegister
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val fixInputSpacingPatch = patch(
    name = "Fix input spacing",
    description = "Removes excess space above the text input.",
) {
    compatibleWith(AppCompatibilities.LUMO)
    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, restoreVirtualizedMethodsPatch)
    availability(requireArm64)

    execute {
        val rootInsets = RootWindowInsetsFingerprint.match().originalMethod
        val safeAreaInsets = SafeAreaInsetsFingerprint.match()
        val implementationField = safeAreaInsets.instructionMatches.single {
            it.filter === insetsImplementationRead
        }.fieldReference
        val imeInsets = safeAreaInsets.instructionMatches.single {
            it.filter === imeInsetsRead
        }
        val receiver = imeInsets.getInstruction<FiveRegisterInstruction>().registerC
        val view = safeAreaInsets.method.parameterRegister(0)

        safeAreaInsets.method.addInstructions(
            imeInsets.index,
            """
                invoke-static { $view }, ${descriptor(rootInsets)}
                move-result-object v$receiver
                iget-object v$receiver, v$receiver, ${descriptor(implementationField)}
            """,
        )
    }
}
