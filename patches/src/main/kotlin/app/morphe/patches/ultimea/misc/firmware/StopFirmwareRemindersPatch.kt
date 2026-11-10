/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.firmware

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.readInt
import app.morphe.patches.ultimea.misc.settings.markPatchApplied
import app.morphe.patches.ultimea.misc.settings.nativeMethods
import app.morphe.patches.ultimea.misc.settings.patchesSettingsPatch
import app.morphe.patches.ultimea.misc.settings.writeNativeEdits

private const val INSTRUCTION_BYTES = 4
private const val BRANCH = 0x14000000
private const val BRANCH_MASK = 0xfc000000.toInt()
private const val RETURN_FALSE = 0x9100c2c0.toInt()

@Suppress("unused")
val stopFirmwareRemindersPatch: BytecodePatch = bytecodePatch(
    name = "Stop firmware reminders",
    description = "Stops automatic firmware reminders. Keeps manual update checks.",
) {
    compatibleWith(AppCompatibilities.ULTIMEA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, resourcePatch {
        execute {
            val library = dartAotLibrary().readBytes()
            val firmware = nativeMethods.single("/device-detail", "Do not show dialog")
            val resume = nativeMethods.single("common_new_version", "device_ota_hdmi_module_sotware", "common_upgrade_now") {
                "language_english" !in it.stringLoads.values
            }
            val edits = listOf(firmware, resume).map { function ->
                val initialized = nativeMethods.asyncBodyStart(function)
                val completedFalse = (initialized until function.end step INSTRUCTION_BYTES).firstOrNull {
                    library.readInt(it) == RETURN_FALSE &&
                        library.readInt(it + INSTRUCTION_BYTES) and BRANCH_MASK == BRANCH
                } ?: throw PatchException("Could not find completed firmware reminder result")
                val branch = BRANCH or ((completedFalse - initialized) / INSTRUCTION_BYTES)
                initialized to ByteArray(INSTRUCTION_BYTES) { (branch ushr (it * Byte.SIZE_BITS)).toByte() }
            }
            writeNativeEdits("stop_firmware_reminders", library, edits)
        }
    })
    execute { markPatchApplied("stopFirmwareReminders", stopFirmwareRemindersPatch) }
}
