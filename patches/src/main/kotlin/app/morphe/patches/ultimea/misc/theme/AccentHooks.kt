/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.PatchException
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val INSTRUCTION_BYTES = 4
private const val PAINT_ENTRY_HEADER = 0
private const val PACKED_ENTRY_HEADER = 4
private const val GRADIENT_ENTRY_HEADER = 8
private const val IMAGE_ADDRESS_HEADER = 12
private const val CACHE_ADDRESS_HEADER = 16
private const val COLOR_COUNT_HEADER = 20
private const val COLORS_HEADER = 24
private const val PAINT_INSTRUCTION_HEADER = 28
private const val PACKED_INSTRUCTION_HEADER = 32
private const val GRADIENT_INSTRUCTION_HEADER = 36
private const val PAINT_POINTER_HEADER = 40
private const val ADD_REGISTER = 0x8b000000.toInt()
private const val SECOND_REGISTER_SHIFT = 5
private const val THIRD_REGISTER_SHIFT = 16
private const val CALL_INSTRUCTION = 0x94000000.toInt()
private const val CALL_IMMEDIATE_MASK = 0x03ffffff
private const val MAX_CALL_DISTANCE = 0x08000000
private const val RGB_MASK = 0x00ffffff
private const val ASSET_HEADER_BYTES = 8
private const val PALETTE_CACHE_BYTES = 3 * Long.SIZE_BYTES
private const val PALETTE_MAGIC = 0x31434148
private val BRAND_COLORS = listOf(0xff541597.toInt(), 0xffc499ff.toInt(), 0xffb585ee.toInt(), 0xff7b52d4.toInt())

internal object AccentHooks {
    fun apply(image: ThemeElfImage, matches: AccentInstructionMatches): ByteArray {
        val template = AccentHooks::class.java.getResourceAsStream("/ultimea/accent.bin")
            ?.use { it.readBytes() } ?: throw PatchException("Could not read the accent hooks")
        val header = ByteBuffer.wrap(template).order(ByteOrder.LITTLE_ENDIAN)
        val colors = header.getInt(COLORS_HEADER)
        if (colors + INSTRUCTION_BYTES != template.size) {
            throw PatchException("Unexpected accent hook palette layout")
        }
        val hooks = template.copyOf(colors + BRAND_COLORS.size * INSTRUCTION_BYTES)
        val code = ByteBuffer.wrap(hooks).order(ByteOrder.LITTLE_ENDIAN)
        val library = ByteBuffer.wrap(image.bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(IMAGE_ADDRESS_HEADER, image.hookAddress)
        code.putInt(CACHE_ADDRESS_HEADER, image.modeCacheAddress)
        code.putInt(COLOR_COUNT_HEADER, BRAND_COLORS.size)
        code.putInt(code.getInt(PAINT_INSTRUCTION_HEADER), library.getInt(matches.paintColorReturn))
        code.putInt(code.getInt(PACKED_INSTRUCTION_HEADER), library.getInt(matches.packedColorReturn))
        code.putInt(code.getInt(GRADIENT_INSTRUCTION_HEADER), library.getInt(matches.gradientColorReturn))
        code.putInt(code.getInt(PAINT_POINTER_HEADER), ADD_REGISTER or
            (matches.paintBufferRegister shl SECOND_REGISTER_SHIFT) or
            (matches.paintOffsetRegister shl THIRD_REGISTER_SHIFT))
        BRAND_COLORS.forEachIndexed { index, color -> code.putInt(colors + index * INSTRUCTION_BYTES, color and RGB_MASK) }
        image.install(hooks, PALETTE_CACHE_BYTES)
        for ((site, entry) in listOf(
            matches.paintColorReturn to PAINT_ENTRY_HEADER,
            matches.packedColorReturn to PACKED_ENTRY_HEADER,
            matches.gradientColorReturn to GRADIENT_ENTRY_HEADER,
        )) {
            val distance = image.hookAddress + code.getInt(entry) - image.instructionAddress(site)
            if (distance !in -MAX_CALL_DISTANCE until MAX_CALL_DISTANCE || distance % INSTRUCTION_BYTES != 0) {
                throw PatchException("The accent hook is outside the arm64 branch range")
            }
            library.putInt(site, CALL_INSTRUCTION or ((distance / INSTRUCTION_BYTES) and CALL_IMMEDIATE_MASK))
        }
        return ByteBuffer.allocate(ASSET_HEADER_BYTES + BRAND_COLORS.size * INSTRUCTION_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(PALETTE_MAGIC)
                putInt(BRAND_COLORS.size)
                BRAND_COLORS.forEach(::putInt)
            }.array()
    }
}
