/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shttps.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockPlusPatch = patch(
    name = "Unlock PLUS",
    description = "Unlocks remote screen control, control from external apps and auto-shutdown timeouts over 6 hours or off.",
) {
    compatibleWith(AppCompatibilities.SHTTPS_MARKETLESS)

    execute {
        val startupCheck = StartupLicenseCheckFingerprint.matchSingle()
        val (_, licenseKeyCall, _, _, confirmedResult) = startupCheck.instructionMatches

        val licenseKeyCheck = licenseKeyCall.getInstruction<ReferenceInstruction>().getReference<MethodReference>()!!
        mutableClassDefBy(licenseKeyCheck.definingClass).findMutableMethodOf(licenseKeyCheck).returnEarly(true)

        val confirmedRegister = confirmedResult.getInstruction<OneRegisterInstruction>().registerA
        startupCheck.method.replaceInstruction(confirmedResult.index, "const/16 v$confirmedRegister, 0x1")
    }
}
