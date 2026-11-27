/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.arm64.Arm64
import app.morphe.patches.shared.misc.dart.NativeMethods
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt

private const val MAP_KEY_REGISTER = 2
private const val DART_NULL_REGISTER = 22
private const val DART_TRUE_OFFSET = 0x20
private const val BASE_REGISTER_SHIFT = 5
private const val IMMEDIATE_SHIFT = 10
private const val ADD_IMMEDIATE = 0x91000000.toInt()
private const val MOVE_FROM_RESULT_MASK = 0xffffffe0.toInt()
private const val MOVE_FROM_RESULT = 0xaa0003e0.toInt()

internal fun ByteArray.forceProfilePremium() {
    val methods = NativeMethods(this)
    val keyLoad = methods.matching("premium", "premium_cancelled", "free_trial")
        .flatMap { it.loads("premium") }
        .filter { Arm64.destination(readInt(it)) == MAP_KEY_REGISTER }
        .singleOrNull() ?: throw PatchException("Expected one profile premium read")
    val moveAddress = methods.firstCallAfter(keyLoad, methods.containing(keyLoad).end) + Arm64.INSTRUCTION_BYTES
    val move = readInt(moveAddress)
    if (move and MOVE_FROM_RESULT_MASK != MOVE_FROM_RESULT) {
        throw PatchException("Expected the profile premium value to be moved out of the call result")
    }
    writeInt(
        moveAddress,
        ADD_IMMEDIATE or (DART_TRUE_OFFSET shl IMMEDIATE_SHIFT) or
            (DART_NULL_REGISTER shl BASE_REGISTER_SHIFT) or Arm64.destination(move),
    )
}
