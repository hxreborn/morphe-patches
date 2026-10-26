/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pleco.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops device identifiers and usage counters from reaching Pleco's servers and " +
        "usage events from reaching Google.",
) {
    compatibleWith(AppCompatibilities.PLECO)

    execute {
        ServerRequestDeviceFieldsFingerprint.matchSingle().let {
            it.method.addInstruction(it.instructionMatches.last().index, "return-void")
        }

        TransportEventScheduleFingerprint.matchSingle().let {
            val backendCheckIndex = it.instructionMatches.last().index
            val backendRegister = it.method.getInstruction<OneRegisterInstruction>(backendCheckIndex).registerA
            it.method.addInstruction(backendCheckIndex, "const/16 v$backendRegister, 0x0")
        }
    }
}
