/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pleco.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val disableTrackingPatch = patch(
    name = "Disable tracking",
    description = "Stops device identifiers and usage counters from reaching Pleco's servers and " +
        "usage events from reaching Google.",
) {
    compatibleWith(AppCompatibilities.PLECO)

    execute {
        ServerRequestDeviceFieldsFingerprint.match().apply {
            val (_, getContentResolverCall) = instructionMatches
            method.addInstruction(getContentResolverCall.index, "return-void")
        }

        TransportEventScheduleFingerprint.match().apply {
            val (_, _, backendCheck) = instructionMatches
            val backendRegister = backendCheck.getInstruction<OneRegisterInstruction>().registerA
            method.addInstruction(backendCheck.index, "const/16 v$backendRegister, 0x0")
        }
    }
}
