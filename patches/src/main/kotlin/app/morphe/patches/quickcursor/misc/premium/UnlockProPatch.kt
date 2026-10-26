/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.quickcursor.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val LIFETIME_STATE = "lifetime"

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock pro",
    description = "Unlocks trigger, tracker and edge actions, floating tracker and cursor customization.",
) {
    compatibleWith(AppCompatibilities.QUICK_CURSOR)

    execute {
        val getter = ProStateFingerprint.matchSingle().method
        val proStateType = getter.returnType

        fun loadLifetimeState(nameRegister: Int, resultRegister: String) = """
            const-string v$nameRegister, "$LIFETIME_STATE"
            invoke-static { v$nameRegister }, $proStateType->valueOf(Ljava/lang/String;)$proStateType
            move-result-object $resultRegister
        """

        getter.apply {
            val stateRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructions(0, loadLifetimeState(stateRegister, "v$stateRegister") + "return-object v$stateRegister")
        }

        proStateDispatchFingerprint(proStateType).matchSingle().method.apply {
            val nameRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructions(0, loadLifetimeState(nameRegister, "p1"))
        }
    }
}
