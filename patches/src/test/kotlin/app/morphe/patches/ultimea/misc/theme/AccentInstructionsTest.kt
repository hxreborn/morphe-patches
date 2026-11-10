/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

private const val INSTRUCTION_BYTES = 4
private const val FRAME_ENTRY = 0xa9bf79fd.toInt()
private const val REGISTER_MASK = 31
private const val SOURCE_REGISTER_SHIFT = 5
private const val MOVE_SOURCE_SHIFT = 16
private const val UNSCALED_MEMORY_MASK = 0xffe00c00.toInt()
private const val DOUBLE_LOAD = 0xfc400000.toInt()
private const val SINGLE_STORE = 0xbc000000.toInt()
private const val DIRECT_CALL = 0x94000000.toInt()
private const val MEMORY_OFFSET_BIT = 0x1000
private const val ALTERNATE_PAINT_REGISTER = 5
private const val ALTERNATE_PACKED_REGISTER = 6

class AccentInstructionsTest {
    private data class Stock(val path: String, val paintReturn: Int, val packedReturn: Int, val gradientReturn: Int)

    private data class Methods(
        val bytes: ByteArray,
        val paintReturn: Int,
        val packedStart: Int,
        val packedReturn: Int,
        val gradientStart: Int,
        val gradientReturn: Int,
    ) {
        fun match() = AccentInstructions(bytes, 0, bytes.size).match()
    }

    @Test
    fun matchColorSerializersAcrossStockVersions() {
        stocks().forEach { stock ->
            val image = ThemeElfImage(File(stock.path).readBytes())
            val matches = AccentInstructions(image.bytes, image.codeStart, image.codeEnd).match()
            assertEquals(stock.paintReturn, image.instructionAddress(matches.paintColorReturn), stock.path)
            assertEquals(stock.packedReturn, image.instructionAddress(matches.packedColorReturn), stock.path)
            assertEquals(stock.gradientReturn, image.instructionAddress(matches.gradientColorReturn), stock.path)
            assertEquals(2, matches.paintBufferRegister, stock.path)
            assertEquals(1, matches.paintOffsetRegister, stock.path)
            assertEquals(2, matches.packedColorRegister, stock.path)
        }
    }

    @Test
    fun matchGradientAfterThemeHookAndRejectChangedPayload() {
        val fixture = syntheticMethods()
        fixture.bytes.writeInt(fixture.gradientReturn - INSTRUCTION_BYTES, DIRECT_CALL)
        assertEquals(fixture.gradientReturn, fixture.match().gradientColorReturn)
        val payloadStore = (fixture.gradientStart until fixture.gradientReturn step INSTRUCTION_BYTES).first {
            fixture.bytes.readInt(it) and UNSCALED_MEMORY_MASK == SINGLE_STORE
        }
        fixture.bytes.writeInt(payloadStore, fixture.bytes.readInt(payloadStore) xor MEMORY_OFFSET_BIT)
        val changed = assertFailsWith<PatchException> { fixture.match() }
        assertTrue(changed.message.orEmpty().contains("gradient Color converter, found 0"))
    }

    @Test
    fun bindRegistersFromColorSerializerDataflow() {
        val fixture = syntheticMethods()
        val pointerLoad = fixture.paintReturn - 2 * INSTRUCTION_BYTES
        val pointer = fixture.bytes.readInt(pointerLoad)
        fixture.bytes.writeInt(pointerLoad, pointer and REGISTER_MASK.inv() or ALTERNATE_PAINT_REGISTER)
        val bufferStore = fixture.paintReturn - INSTRUCTION_BYTES
        val store = fixture.bytes.readInt(bufferStore)
        fixture.bytes.writeInt(
            bufferStore,
            store and (REGISTER_MASK shl SOURCE_REGISTER_SHIFT).inv() or
                (ALTERNATE_PAINT_REGISTER shl SOURCE_REGISTER_SHIFT),
        )
        val packedCombine = fixture.packedReturn - INSTRUCTION_BYTES
        val combine = fixture.bytes.readInt(packedCombine)
        fixture.bytes.writeInt(packedCombine, combine and REGISTER_MASK.inv() or ALTERNATE_PACKED_REGISTER)
        val packedMove = fixture.bytes.readInt(fixture.packedReturn)
        fixture.bytes.writeInt(
            fixture.packedReturn,
            packedMove and (REGISTER_MASK shl MOVE_SOURCE_SHIFT).inv() or
                (ALTERNATE_PACKED_REGISTER shl MOVE_SOURCE_SHIFT),
        )
        val matches = fixture.match()
        assertEquals(fixture.paintReturn, matches.paintColorReturn)
        assertEquals(fixture.packedReturn, matches.packedColorReturn)
        assertEquals(fixture.gradientReturn, matches.gradientColorReturn)
        assertEquals(ALTERNATE_PAINT_REGISTER, matches.paintBufferRegister)
        assertEquals(ALTERNATE_PACKED_REGISTER, matches.packedColorRegister)
    }

    @Test
    fun rejectChangedComponentLayoutAndDuplicateSerializers() {
        val fixture = syntheticMethods()
        val componentLoad = (fixture.packedStart until fixture.packedReturn step INSTRUCTION_BYTES).first {
            fixture.bytes.readInt(it) and UNSCALED_MEMORY_MASK == DOUBLE_LOAD
        }
        val original = fixture.bytes.readInt(componentLoad)
        fixture.bytes.writeInt(componentLoad, original xor MEMORY_OFFSET_BIT)
        val changed = assertFailsWith<PatchException> { fixture.match() }
        assertTrue(changed.message.orEmpty().contains("packed Color converter, found 0"))
        fixture.bytes.writeInt(componentLoad, original)
        val duplicated = fixture.bytes + fixture.bytes
        val duplicate = assertFailsWith<PatchException> {
            AccentInstructions(duplicated, 0, duplicated.size).match()
        }
        assertTrue(duplicate.message.orEmpty().contains("Paint color serializer, found 2"))
    }

    private fun syntheticMethods(): Methods {
        val code = Arm64Fixture()
        code.enterFrame()
        code.storeRegister(4, 29, -32)
        code.storeRegister(2, 29, -24)
        for (component in 1..4) {
            if (component == 4) code.subtractDouble(2, 0, 1)
            code.loadRegister(2, 29, -24)
            code.addImmediate(0, 2, component * 4)
            code.convertDoubleToSingle(1, if (component == 4) 2 else 0)
            code.loadRegister(3, 29, -32)
            code.loadRegister(1, 3, 7)
            code.storeSingleIndexed(1, 1, 0)
        }
        code.loadRegister(2, 29, -24)
        code.addImmediate(1, 2, 20)
        code.loadRegister(3, 29, -32)
        code.loadRegister(2, 3, 7)
        code.storeWordIndexed(4, 2, 1)
        val paintReturn = code.position
        code.moveRegister(0, 22)
        code.leaveFrame()

        val packedStart = code.position
        code.enterFrame()
        code.storeRegister(1, 29, -8)
        for (component in 0..3) {
            code.loadRegister(1, 29, -8)
            code.loadDouble(0, 1, 7 + component * 8)
            code.call("channelToByte")
            val shift = (3 - component) * 8
            if (shift == 0) code.moveRegister(1, 0) else code.shiftLeft(1, 0, shift)
            if (component == 0) {
                code.storeRegister(1, 29, -16)
            } else {
                code.loadRegister(0, 29, -16)
                code.orRegisters(2, 0, 1)
                if (component < 3) code.storeRegister(2, 29, -16)
            }
        }
        val packedReturn = code.position
        code.moveRegister(0, 2)
        code.leaveFrame()

        val gradientStart = code.position
        code.enterFrame()
        code.label("gradientLoop")
        code.compareRegisters(1, 2)
        code.branchGreaterEqual("gradientResult")
        repeat(4) {
            code.convertDoubleToSingle(1, 0)
            code.storeSingle(1, 0, 23)
        }
        code.branch("gradientLoop")
        code.label("gradientResult")
        code.moveRegister(0, 2)
        val gradientReturn = code.position
        code.leaveFrame()
        code.label("channelToByte")
        code.returnToCaller()
        return Methods(code.bytes(), paintReturn, packedStart, packedReturn, gradientStart, gradientReturn)
    }

    private fun stocks(): List<Stock> {
        val directory = System.getenv("ULTIMEA_STOCK_DIRECTORY")
        assumeTrue(directory != null, "Set ULTIMEA_STOCK_DIRECTORY to verify stock libraries")
        return listOf(
            Stock("$directory/1.4.4-play/x/lib/arm64-v8a/libapp.so", 0x656148, 0x58eeb0, 0x7d4fcc),
            Stock("$directory/1.4.1-arm64/x/lib/arm64-v8a/libapp.so", 0x4be170, 0x4bcf74, 0x78a620),
        )
    }
}

private class Arm64Fixture {
    private data class Branch(val index: Int, val label: String, val opcode: Int, val bits: Int, val shift: Int)

    private val instructions = mutableListOf<Int>()
    private val labels = mutableMapOf<String, Int>()
    private val branches = mutableListOf<Branch>()
    val position get() = instructions.size * INSTRUCTION_BYTES

    fun label(name: String) {
        check(labels.put(name, position) == null)
    }

    fun enterFrame() = emit(FRAME_ENTRY)

    fun leaveFrame() {
        moveRegister(15, 29)
        emit(0xa8c179fd.toInt())
        returnToCaller()
    }

    fun returnToCaller() = emit(0xd65f03c0.toInt())

    fun moveRegister(destination: Int, source: Int) = emit(0xaa0003e0.toInt() or (source shl 16) or destination)

    fun loadRegister(destination: Int, base: Int, offset: Int) = memory(0xf8400000.toInt(), destination, base, offset)

    fun storeRegister(source: Int, base: Int, offset: Int) = memory(0xf8000000.toInt(), source, base, offset)

    fun loadDouble(destination: Int, base: Int, offset: Int) = memory(DOUBLE_LOAD, destination, base, offset)

    fun storeSingle(source: Int, base: Int, offset: Int) = memory(SINGLE_STORE, source, base, offset)

    fun addImmediate(destination: Int, source: Int, value: Int) =
        emit(0x91000000.toInt() or (value shl 10) or (source shl 5) or destination)

    fun convertDoubleToSingle(destination: Int, source: Int) = emit(0x1e624000 or (source shl 5) or destination)

    fun subtractDouble(destination: Int, left: Int, right: Int) =
        emit(0x1e603800 or (right shl 16) or (left shl 5) or destination)

    fun storeSingleIndexed(source: Int, base: Int, index: Int) =
        emit(0xbc206800.toInt() or (index shl 16) or (base shl 5) or source)

    fun storeWordIndexed(source: Int, base: Int, index: Int) =
        emit(0xb8206800.toInt() or (index shl 16) or (base shl 5) or source)

    fun shiftLeft(destination: Int, source: Int, amount: Int) =
        emit(0xd3400000.toInt() or ((64 - amount) shl 16) or ((63 - amount) shl 10) or (source shl 5) or destination)

    fun orRegisters(destination: Int, left: Int, right: Int) =
        emit(0xaa000000.toInt() or (right shl 16) or (left shl 5) or destination)

    fun compareRegisters(left: Int, right: Int) = emit(0xeb00001f.toInt() or (right shl 16) or (left shl 5))

    fun call(label: String) = branch(label, DIRECT_CALL, 26, 0)

    fun branch(label: String) = branch(label, 0x14000000, 26, 0)

    fun branchGreaterEqual(label: String) = branch(label, 0x5400000a, 19, 5)

    fun bytes(): ByteArray {
        val bytes = ByteArray(position)
        instructions.forEachIndexed { index, instruction -> bytes.writeInt(index * INSTRUCTION_BYTES, instruction) }
        branches.forEach { branch ->
            val position = branch.index * INSTRUCTION_BYTES
            val distance = (labels.getValue(branch.label) - position) / INSTRUCTION_BYTES
            val mask = (1 shl branch.bits) - 1
            bytes.writeInt(position, branch.opcode or ((distance and mask) shl branch.shift))
        }
        return bytes
    }

    private fun memory(opcode: Int, register: Int, base: Int, offset: Int) =
        emit(opcode or ((offset and 0x1ff) shl 12) or (base shl 5) or register)

    private fun branch(label: String, opcode: Int, bits: Int, shift: Int) {
        branches += Branch(instructions.size, label, opcode, bits, shift)
        emit(opcode)
    }

    private fun emit(instruction: Int) {
        instructions += instruction
    }
}
