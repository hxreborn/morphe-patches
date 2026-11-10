/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.arm64

private const val INSTRUCTION_GROUP_SHIFT = 25
private const val INSTRUCTION_GROUP_MASK = 15
private const val FLOAT_MEMORY_BIT = 0x04000000
private const val CONDITIONAL_COMPARE_MASK = 0x1fe00000
private const val CONDITIONAL_COMPARE = 0x1a400000
private const val ATOMIC_UPDATE_MASK = 0x3b200c00
private const val ATOMIC_UPDATE = 0x38200000
private const val COMPARE_AND_SWAP_MASK = 0x3fa07c00
private const val COMPARE_AND_SWAP = 0x08a07c00
private const val COMPARE_REGISTER_SHIFT = 16
private const val REGISTER_MASK = 31
private const val PAIR_LOAD_MASK = 0x3a400000
private const val PAIR_LOAD = 0x28400000
private const val EXCLUSIVE_PAIR_LOAD_MASK = 0x3fff0000
private const val EXCLUSIVE_PAIR_LOAD = 0x087f0000
private const val EXCLUSIVE_STORE_MASK = 0x3fe00000
private const val EXCLUSIVE_STORE = 0x08000000
private const val EXCLUSIVE_PAIR_STORE_MASK = 0xbfe00000.toInt()
private const val EXCLUSIVE_PAIR_STORE = 0x88200000.toInt()
private const val SECOND_LOAD_REGISTER_SHIFT = 10
private const val MEMORY_LOAD_BIT = 0x00400000
private const val SIGN_EXTENDING_LOAD_MASK = 0x3a800000
private const val SIGN_EXTENDING_LOAD = 0x38800000
private const val PREFETCH_MASK = 0xffc00000.toInt()
private val PREFETCHES = setOf(0xf9800000.toInt(), 0xf8800000.toInt())
private const val INTEGER_LITERAL_MASK = 0x3f000000
private const val INTEGER_LITERAL_LOAD = 0x18000000
private const val LITERAL_PREFETCH = 0xc0000000.toInt()
private const val SYSTEM_REGISTER_MASK = 0xfff00000.toInt()
private const val SYSTEM_REGISTER_READ = 0xd5300000.toInt()
private const val SYSTEM_RESULT_MASK = 0xfff80000.toInt()
private const val SYSTEM_RESULT_READ = 0xd5280000.toInt()
private const val FLOAT_INTEGER_MASK = 0x7f20fc00
private const val FLOAT_INTEGER = 0x1e200000
private const val FLOAT_FIXED_MASK = 0x7f200000
private const val FLOAT_FIXED = 0x1e000000
private const val FLOAT_OPCODE_SHIFT = 16
private const val FLOAT_OPCODE_MASK = 31
private const val SIMD_COPY_MASK = 0xbfe0fc00.toInt()
private const val SIMD_SIGNED_COPY = 0x0e002c00
private const val SIMD_UNSIGNED_COPY = 0x0e003c00
private val DATA_PROCESSING_GROUPS = setOf(5, 8, 9, 13)
private val LOAD_STORE_GROUPS = setOf(4, 6, 12, 14)
private val FLOAT_INTEGER_RESULTS = setOf(0, 1, 4, 5, 6, 8, 9, 14, 16, 17, 24, 25, 30)
private val FLOAT_FIXED_RESULTS = setOf(24, 25)
private val SIMD_GENERAL_RESULTS = setOf(SIMD_SIGNED_COPY, SIMD_UNSIGNED_COPY)

internal fun arm64CompareAndSwapResultRegister(instruction: Int): Int? =
    if (instruction and COMPARE_AND_SWAP_MASK == COMPARE_AND_SWAP) instruction ushr COMPARE_REGISTER_SHIFT and REGISTER_MASK else null

internal fun arm64ExclusiveStoreStatusRegister(instruction: Int): Int? =
    if (instruction and EXCLUSIVE_STORE_MASK == EXCLUSIVE_STORE ||
        instruction and EXCLUSIVE_PAIR_STORE_MASK == EXCLUSIVE_PAIR_STORE)
        instruction ushr COMPARE_REGISTER_SHIFT and REGISTER_MASK else null

internal fun arm64SecondLoadRegister(instruction: Int): Int? =
    if (instruction and PAIR_LOAD_MASK == PAIR_LOAD && instruction and FLOAT_MEMORY_BIT == 0 ||
        instruction and EXCLUSIVE_PAIR_LOAD_MASK == EXCLUSIVE_PAIR_LOAD)
        instruction ushr SECOND_LOAD_REGISTER_SHIFT and REGISTER_MASK else null

internal fun writesArm64GeneralRegister(instruction: Int): Boolean {
    val group = instruction ushr INSTRUCTION_GROUP_SHIFT and INSTRUCTION_GROUP_MASK
    val floatOpcode = instruction ushr FLOAT_OPCODE_SHIFT and FLOAT_OPCODE_MASK
    return group in DATA_PROCESSING_GROUPS && instruction and CONDITIONAL_COMPARE_MASK != CONDITIONAL_COMPARE ||
        instruction and ATOMIC_UPDATE_MASK == ATOMIC_UPDATE ||
        group in LOAD_STORE_GROUPS && instruction and FLOAT_MEMORY_BIT == 0 &&
            instruction and INTEGER_LITERAL_MASK != INTEGER_LITERAL_LOAD &&
            (instruction and MEMORY_LOAD_BIT != 0 || instruction and SIGN_EXTENDING_LOAD_MASK == SIGN_EXTENDING_LOAD) &&
            instruction and PREFETCH_MASK !in PREFETCHES ||
        instruction and INTEGER_LITERAL_MASK == INTEGER_LITERAL_LOAD && instruction and LITERAL_PREFETCH != LITERAL_PREFETCH ||
        instruction and SYSTEM_REGISTER_MASK == SYSTEM_REGISTER_READ ||
        instruction and SYSTEM_RESULT_MASK == SYSTEM_RESULT_READ ||
        instruction and FLOAT_INTEGER_MASK == FLOAT_INTEGER && floatOpcode in FLOAT_INTEGER_RESULTS ||
        instruction and FLOAT_FIXED_MASK == FLOAT_FIXED && floatOpcode in FLOAT_FIXED_RESULTS ||
        instruction and SIMD_COPY_MASK in SIMD_GENERAL_RESULTS
}
