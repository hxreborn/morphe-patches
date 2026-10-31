/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.weather24.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val VOUCHER_EXPIRE_DATE = "2099-12-31T12:00:00Z"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Removes banner, interstitial and video ads and unlocks the premium weather icons.",
) {
    compatibleWith(AppCompatibilities.WEATHER24)

    execute {
        val expireDateRead = VoucherExpireDateFingerprint.matchSingle()
        val moveResult = expireDateRead.instructionMatches.last()
        val expireDateRegister = moveResult.getInstruction<OneRegisterInstruction>().registerA
        expireDateRead.method.addInstruction(
            moveResult.index + 1,
            "const-string v$expireDateRegister, \"$VOUCHER_EXPIRE_DATE\"",
        )
    }
}
