/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flipaclip.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch

private const val LIBRARY = "lib/arm64-v8a/libfc.so"
private const val PROJECT_EXPORT_METHOD_OFFSET = 0xde114c
private const val PROJECT_EXPORT_GATE_OFFSET = 0x30
private const val PROJECT_EXPORT_BRANCH_OFFSET = 0x3c
private const val PROJECT_EXPORT_ERROR_OFFSET = 0xc4
private val PROJECT_EXPORT_PROLOGUE = "fd7bbda9f50b00f9f44f02a9fd030091".hexToByteArray()
private val PROJECT_EXPORT_GATE = "a80240b908b944111f4d213141040054".hexToByteArray()
private val PROJECT_EXPORT_ERROR = "20068012".hexToByteArray()
private val ARM64_NOP = "1f2003d5".hexToByteArray()

internal val restoreProjectExportPatch = patch {
    compatibleWith(AppCompatibilities.FLIPACLIP)
    dependsOn(removePairipVirtualizationPatch)

    execute {
        val library = resourceFile(LIBRARY, true)
        if (!library.exists()) throw PatchException("$LIBRARY is missing")

        val bytes = library.readBytes()
        fun requireProjectExportBytes(offset: Int, expected: ByteArray) {
            val start = PROJECT_EXPORT_METHOD_OFFSET + offset
            if (start + expected.size > bytes.size || !bytes.copyOfRange(start, start + expected.size).contentEquals(expected)) {
                throw PatchException("ProjectExportGlue::startExport differs at $LIBRARY offset 0x${start.toString(16)}")
            }
        }

        requireProjectExportBytes(0, PROJECT_EXPORT_PROLOGUE)
        requireProjectExportBytes(PROJECT_EXPORT_GATE_OFFSET, PROJECT_EXPORT_GATE)
        requireProjectExportBytes(PROJECT_EXPORT_ERROR_OFFSET, PROJECT_EXPORT_ERROR)
        ARM64_NOP.copyInto(bytes, PROJECT_EXPORT_METHOD_OFFSET + PROJECT_EXPORT_BRANCH_OFFSET)
        library.writeBytes(bytes)
    }
}
