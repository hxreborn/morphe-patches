/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vocat.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.indexOfMasked
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.replaceMasked
import app.morphe.patches.shared.writeInt
import java.io.File

private val PERMISSION_GATE =
    ("fd79bfa9fd030faa502740f9ff0110eb29010054203043b800801c8b01b040b821801c8ba82c0594").hexToByteArray()

private val PERMISSION_GATE_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000fc").hexToByteArray()

private val LOCAL_FEATURE_PERMISSION_CODE =
    ("407040f81f2c00f1c8000054100080d20026c09a60000036c0820091c0035fd6" +
        "203043b800801c8b01b040b821801c8b00000014").hexToByteArray()

private val WIDGET_PERMISSION =
    ("01b040b821801c8b62df459142a845f9b685fa97a1835ff8").hexToByteArray()

private val WIDGET_PERMISSION_MASK =
    ("ffffffffffffffff" + "ff03c0ffff03c0ff000000fc" + "ffffffff").hexToByteArray()

private val RETURN_TRUE = "c0820091".hexToByteArray()

private const val FEATURE_REMOVE_BANNER = 0
private const val FEATURE_WIDGET = 7
private const val FEATURE_SRS = 8
private const val FEATURE_SRS_AUTO_MARKING = 9
private const val FEATURE_SRS_DUE_SORT = 10
private const val FEATURE_SRS_STATISTICS = 11

private val GRANTED_FEATURE_MASK = listOf(
    FEATURE_REMOVE_BANNER,
    FEATURE_WIDGET,
    FEATURE_SRS,
    FEATURE_SRS_AUTO_MARKING,
    FEATURE_SRS_DUE_SORT,
    FEATURE_SRS_STATISTICS,
).fold(0) { mask, feature -> mask or (1 shl feature) }

private const val GRANTED_MASK_OFFSET = 12
private const val ARM64_MOVZ_IMMEDIATE_SHIFT = 5
private const val PERMISSION_CALL_OFFSET = 36
private const val PERMISSION_BRANCH_OFFSET = 48
private const val WIDGET_CONTAINS_CALL_OFFSET = 16
private const val ARM64_INSTRUCTION_BYTES = 4
private const val ARM64_BRANCH_OPCODE_SHIFT = 26
private const val ARM64_BRANCH_SIGN_SHIFT = 6
private const val ARM64_BRANCH_WITH_LINK_OPCODE = 0b100101
private const val ARM64_BRANCH_OPCODE = 0x14000000
private const val ARM64_BRANCH_IMMEDIATE_MASK = 0x03ffffff

private fun File.replaceLocalFeaturePermissionGate() {
    val bytes = readBytes()
    val index = bytes.indexOfMasked(PERMISSION_GATE, PERMISSION_GATE_MASK)
    if (index < 0 || bytes.indexOfMasked(PERMISSION_GATE, PERMISSION_GATE_MASK, index + 1) >= 0) {
        throw PatchException("Could not uniquely find the feature permission gate in $DART_AOT_LIBRARY")
    }

    val callOffset = index + PERMISSION_CALL_OFFSET
    val originalCall = bytes.readInt(callOffset)
    if (originalCall ushr ARM64_BRANCH_OPCODE_SHIFT != ARM64_BRANCH_WITH_LINK_OPCODE) {
        throw PatchException("Unexpected feature permission call in $DART_AOT_LIBRARY")
    }

    val originalTarget = callOffset + (originalCall shl ARM64_BRANCH_SIGN_SHIFT shr ARM64_BRANCH_SIGN_SHIFT) * ARM64_INSTRUCTION_BYTES
    val branchOffset = index + PERMISSION_BRANCH_OFFSET
    val displacement = originalTarget - branchOffset
    val immediate = displacement / ARM64_INSTRUCTION_BYTES
    if (displacement % ARM64_INSTRUCTION_BYTES != 0 || immediate shl ARM64_BRANCH_SIGN_SHIFT shr ARM64_BRANCH_SIGN_SHIFT != immediate) {
        throw PatchException("Feature permission call is out of range in $DART_AOT_LIBRARY")
    }

    val replacement = LOCAL_FEATURE_PERMISSION_CODE.copyOf()
    replacement.writeInt(
        GRANTED_MASK_OFFSET,
        replacement.readInt(GRANTED_MASK_OFFSET) or (GRANTED_FEATURE_MASK shl ARM64_MOVZ_IMMEDIATE_SHIFT),
    )
    replacement.writeInt(
        PERMISSION_BRANCH_OFFSET,
        ARM64_BRANCH_OPCODE or (immediate and ARM64_BRANCH_IMMEDIATE_MASK),
    )
    replacement.copyInto(bytes, index)
    writeBytes(bytes)
}

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks FSRS review, review sorting, FSRS statistics and widgets, and removes banner ads.",
) {
    compatibleWith(AppCompatibilities.VOCAT)
    availability(requireArm64)

    execute {
        val library = dartAotLibrary()
        library.replaceLocalFeaturePermissionGate()
        if (!library.replaceMasked(
                WIDGET_PERMISSION,
                WIDGET_PERMISSION_MASK,
                mapOf(WIDGET_CONTAINS_CALL_OFFSET to RETURN_TRUE),
            )
        ) {
            throw PatchException("Could not find the widget permission check in $DART_AOT_LIBRARY")
        }
        WidgetPremiumExpiredFingerprint.method.returnEarly(false)
    }
}
