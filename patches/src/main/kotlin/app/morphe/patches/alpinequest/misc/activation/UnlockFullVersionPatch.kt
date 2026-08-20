/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.alpinequest.misc.activation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.alpinequest.misc.fix.signature.bypassSignatureCheckPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findFreeRegister
import app.morphe.util.matchSingle

private const val ACTIVATED_STATUS = 'R'

@Suppress("unused")
val unlockFullVersionPatch = bytecodePatch(
    name = "Unlock full version",
    description = "Unlocks the Off-Road Explorer features gated behind activation.",
) {
    dependsOn(bypassSignatureCheckPatch)
    compatibleWith(AppCompatibilities.ALPINEQUEST)

    execute {
        val restoreActivation = RestoreActivationFingerprint.matchSingle()
        val statusField = restoreActivation.classDef.fields.single { it.type == "C" }

        restoreActivation.method.apply {
            val statusRegister = findFreeRegister(0)

            addInstructions(
                0,
                """
                    const/16 v$statusRegister, ${ACTIVATED_STATUS.code}
                    iput-char v$statusRegister, p0, ${restoreActivation.classDef.type}->${statusField.name}:C
                    return-void
                """,
            )
        }
    }
}
