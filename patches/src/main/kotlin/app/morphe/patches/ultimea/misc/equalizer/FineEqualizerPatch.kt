/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.equalizer

import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.indexOfMasked
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.writeInt
import app.morphe.patches.ultimea.misc.settings.appendNativeCode
import app.morphe.patches.ultimea.misc.settings.patchesSettingsPatch

private const val INSTRUCTION_BYTES = 4
private const val STEP_LOAD_OFFSET = 16
private const val BRANCH_WITH_LINK = 0x94000000.toInt()
private const val BRANCH_IMMEDIATE_MASK = 0x03ffffff

private val SLIDER_RANGE_AND_STEP = "0010731e00f001fc0010631e007002fc00106c1e00f002fc".hexToByteArray()
private val SLIDER_RANGE_AND_STEP_MASK = ByteArray(SLIDER_RANGE_AND_STEP.size) { -1 }
private val LOAD_TENTH_DECIBEL = "4000005cc0035fd69a9999999999b93f".hexToByteArray()

@Suppress("unused")
val fineEqualizerPatch: ResourcePatch = resourcePatch(
    name = "Fine equalizer",
    description = "Adds 0.1 dB adjustment, band values and Undo.",
) {
    compatibleWith(AppCompatibilities.ULTIMEA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, equalizerConnectionPatch)

    execute {
        val library = dartAotLibrary()
        val bytes = library.readBytes()
        val range = bytes.indexOfMasked(SLIDER_RANGE_AND_STEP, SLIDER_RANGE_AND_STEP_MASK)
        if (range < 0 || bytes.indexOfMasked(SLIDER_RANGE_AND_STEP, SLIDER_RANGE_AND_STEP_MASK,
                range + INSTRUCTION_BYTES) >= 0) {
            throw PatchException("Expected one equalizer slider range and step")
        }
        val loader = bytes.appendNativeCode(LOAD_TENTH_DECIBEL)
        val step = range + STEP_LOAD_OFFSET
        bytes.writeInt(step, BRANCH_WITH_LINK or (((loader - step) shr 2) and BRANCH_IMMEDIATE_MASK))
        library.writeBytes(bytes)
    }
}
