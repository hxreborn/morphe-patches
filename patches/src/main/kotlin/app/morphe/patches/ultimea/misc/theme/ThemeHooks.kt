/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.PatchException
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val INSTRUCTION_BYTES = 4
private const val COLOR_ENTRY_HEADER = 0
private const val GRADIENT_ENTRY_HEADER = 4
private const val COLOR_INSTRUCTION_HEADER = 8
private const val GRADIENT_INSTRUCTION_HEADER = 12
private const val MODE_HEADER = 16
private const val CALLERS_HEADER = 20
private const val IMAGE_ADDRESS_HEADER = 24
private const val CACHE_ADDRESS_HEADER = 28
private const val SURFACE_ENTRY_HEADER = 36
private const val SURFACE_INSTRUCTION_HEADER = 40
private const val SURFACE_INPUT_HEADER = 44
private const val SURFACE_OUTPUT_HEADER = 48
private const val SURFACE_RESTORE_HEADER = 52
private const val REGISTER_MASK = 31
private const val MOVE_ARGUMENT = 0xaa0003e0.toInt()
private const val MOVE_SOURCE_SHIFT = 16
private const val CALLER_ROLE_SHIFT = 3
private const val CALL_INSTRUCTION = 0x94000000.toInt()
private const val CALL_IMMEDIATE_MASK = 0x03ffffff
private const val MAX_CALL_DISTANCE = 0x08000000
private const val DARK_MODE = 1

internal object ThemeHooks {
    fun apply(image: ThemeElfImage, matches: ThemeInstructionMatches, mode: Int = DARK_MODE) {
        val template = ThemeHooks::class.java.getResourceAsStream("/ultimea/theme.bin")
            ?.use { it.readBytes() } ?: throw PatchException("Could not read the theme hooks")
        val hooks = template.copyOf(template.size + (matches.colorRolesByReturnAddress.size + 1) * INSTRUCTION_BYTES)
        val code = ByteBuffer.wrap(hooks).order(ByteOrder.LITTLE_ENDIAN)
        val library = ByteBuffer.wrap(image.bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(IMAGE_ADDRESS_HEADER, image.hookAddress)
        code.putInt(CACHE_ADDRESS_HEADER, image.modeCacheAddress)
        code.putInt(code.getInt(MODE_HEADER), mode)
        code.putInt(code.getInt(COLOR_INSTRUCTION_HEADER), library.getInt(matches.colorComponents))
        code.putInt(code.getInt(GRADIENT_INSTRUCTION_HEADER), library.getInt(matches.gradientReturn))
        var position = code.getInt(CALLERS_HEADER)
        for ((caller, role) in matches.colorRolesByReturnAddress.toSortedMap()) {
            code.putInt(position, (image.instructionAddress(caller) / INSTRUCTION_BYTES shl CALLER_ROLE_SHIFT) or role.code)
            position += INSTRUCTION_BYTES
        }
        val surfaceStore = library.getInt(matches.containerColorStore)
        val colorRegister = surfaceStore and REGISTER_MASK
        code.putInt(code.getInt(SURFACE_INSTRUCTION_HEADER), surfaceStore)
        code.putInt(code.getInt(SURFACE_INPUT_HEADER), MOVE_ARGUMENT or (colorRegister shl MOVE_SOURCE_SHIFT))
        val outputOffset = code.getInt(SURFACE_OUTPUT_HEADER)
        code.putInt(outputOffset, (code.getInt(outputOffset) and REGISTER_MASK.inv()) or colorRegister)
        val restoreOffset = code.getInt(SURFACE_RESTORE_HEADER)
        code.putInt(restoreOffset, (code.getInt(restoreOffset) and REGISTER_MASK.inv()) or colorRegister)
        image.install(hooks)
        fun installCall(site: Int, header: Int) {
            val distance = image.hookAddress + code.getInt(header) - image.instructionAddress(site)
            if (distance !in -MAX_CALL_DISTANCE until MAX_CALL_DISTANCE || distance % INSTRUCTION_BYTES != 0) {
                throw PatchException("The theme hook is outside the arm64 branch range")
            }
            library.putInt(site, CALL_INSTRUCTION or ((distance / INSTRUCTION_BYTES) and CALL_IMMEDIATE_MASK))
        }
        installCall(matches.colorComponents, COLOR_ENTRY_HEADER)
        installCall(matches.gradientReturn, GRADIENT_ENTRY_HEADER)
        installCall(matches.containerColorStore, SURFACE_ENTRY_HEADER)
    }
}
