/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val accountBeans = setOf(
    "${BEAN_PACKAGE}BffGetVipUserInfoData;",
    "${BEAN_PACKAGE}BffUserInfoData;",
)

private fun MutableMethod.returnBoolean(value: Boolean) {
    if (returnType == "Z") return returnEarly(value)
    addInstructions(
        0,
        """
            sget-object v0, $BOOLEAN->${value.toString().uppercase()}:$BOOLEAN
            return-object v0
        """,
    )
}

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks full-length playback and VIP titles, and removes region blocks.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)

    execute {
        val (stateRead) = ServiceLocatorIsVipFingerprint.instructionMatches
        val state = stateRead.methodReference.definingClass

        val initialState = vipStateInitialValueFingerprint(state)
        val (initialValue) = initialState.instructionMatches
        val initialRegister = initialValue.getInstruction<OneRegisterInstruction>().registerA
        initialState.method.replaceInstruction(initialValue.index, "sget-object v$initialRegister, $BOOLEAN->TRUE:$BOOLEAN")
        vipStateWriterFingerprint(state).method.addInstruction(0, "const/4 p1, 0x1")

        val vipGetters = VipGetterFingerprint.matchAll()
        val missingAccounts = accountBeans - vipGetters.map { it.classDef.type }.toSet()
        if (missingAccounts.isNotEmpty()) throw PatchException("No isVip getter in $missingAccounts")
        vipGetters.forEach { it.method.returnBoolean(it.classDef.type in accountBeans) }
        VipHeadersFingerprint.method.returnEarly()
        RegionBlockFingerprint.method.returnBoolean(false)
    }
}
