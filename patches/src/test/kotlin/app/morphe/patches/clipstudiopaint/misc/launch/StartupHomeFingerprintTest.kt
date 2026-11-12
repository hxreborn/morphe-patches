/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clipstudiopaint.misc.launch

import app.morphe.patcher.patch.PatchException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class StartupHomeFingerprintTest {
    @Test
    fun `matches automatic home display through event name`() {
        assertEquals(HOME_CALL, createLibraryBuffer().let(StartupHomeFingerprint::match))
    }

    @Test
    fun `continues past unrelated add that preserves page register`() {
        val buffer = createLibraryBuffer()
        buffer.putInt(CODE + WORD_SIZE, 0x91000128.toInt())
        buffer.putInt(CODE + 2 * WORD_SIZE, 0x910c0129.toInt())
        buffer.putInt(CODE + 3 * WORD_SIZE, 0xd000000a.toInt())
        buffer.putInt(DESCRIPTOR_STORE, 0x9100014a.toInt())
        buffer.putInt(DESCRIPTOR_STORE + WORD_SIZE, 0xa900a548.toInt())

        assertEquals(HOME_CALL, StartupHomeFingerprint.match(buffer))
    }

    @Test
    fun `rejects unrelated add that overwrites page register`() {
        val buffer = createLibraryBuffer()
        buffer.putInt(CODE + WORD_SIZE, 0x91000129.toInt())
        buffer.putInt(CODE + 2 * WORD_SIZE, 0x910c0129.toInt())

        assertFailsWith<PatchException> { StartupHomeFingerprint.match(buffer) }
    }

    @Test
    fun `ignores home calls outside matching event handler`() {
        val buffer = createLibraryBuffer()
        putHomeCall(buffer, UNRELATED_CALL)
        assertEquals(HOME_CALL, StartupHomeFingerprint.match(buffer))
    }

    @Test
    fun `rejects home call beyond event branch`() {
        val buffer = createLibraryBuffer()
        buffer.putInt(EVENT_BRANCH, branchNotEqual(EVENT_BRANCH, HOME_CALL - 2 * WORD_SIZE))
        assertFailsWith<PatchException> { StartupHomeFingerprint.match(buffer) }
    }

    @Test
    fun `rejects duplicated matching handlers`() {
        val buffer = createLibraryBuffer()
        putHandler(buffer, SECOND_HANDLER)
        assertFailsWith<PatchException> { StartupHomeFingerprint.match(buffer) }
    }

    @Test
    fun `rejects overwritten event name register`() {
        val buffer = createLibraryBuffer()
        buffer.putInt(DESCRIPTOR_STORE, 0xaa0003e9.toInt())
        buffer.putInt(DESCRIPTOR_STORE + WORD_SIZE, 0xa900a548.toInt())
        assertFailsWith<PatchException> { StartupHomeFingerprint.match(buffer) }
    }

    @Test
    fun `rejects missing event name`() {
        val buffer = createLibraryBuffer()
        buffer.put(STRINGS, 'x'.code.toByte())
        assertFailsWith<PatchException> { StartupHomeFingerprint.match(buffer) }
    }

    private fun createLibraryBuffer(): ByteBuffer = ByteBuffer.allocate(LIBRARY_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
        putLong(0x28, SECTION_TABLE.toLong())
        putShort(0x3a, SECTION_HEADER_SIZE.toShort())
        putShort(0x3c, 4.toShort())
        putShort(0x3e, 3.toShort())
        putString(SECTION_NAMES, "\u0000.text\u0000.rodata\u0000.shstrtab\u0000")
        putSection(1, 1, CODE, CODE_ADDRESS, CODE_SIZE)
        putSection(2, 7, STRINGS, STRINGS_ADDRESS, STRINGS_SIZE)
        putSection(3, 15, SECTION_NAMES, SECTION_NAMES.toLong(), SECTION_NAMES_SIZE)
        putString(STRINGS, "applicationshowicewindowifnoworkview\u0000")
        putInt(CODE, 0xb0000009.toInt())
        putInt(CODE + WORD_SIZE, 0x910c0129.toInt())
        putInt(CODE + 2 * WORD_SIZE, 0xd000000a.toInt())
        putInt(CODE + 3 * WORD_SIZE, 0x9100014a.toInt())
        putInt(DESCRIPTOR_STORE, 0xa900a548.toInt())
        putHandler(this, HANDLER)
    }

    private fun ByteBuffer.putSection(index: Int, name: Int, offset: Int, address: Long, size: Int) {
        val header = SECTION_TABLE + index * SECTION_HEADER_SIZE
        putInt(header, name)
        putLong(header + 0x10, address)
        putLong(header + 0x18, offset.toLong())
        putLong(header + 0x20, size.toLong())
    }

    private fun ByteBuffer.putString(offset: Int, value: String) {
        value.toByteArray(Charsets.US_ASCII).forEachIndexed { index, byte -> put(offset + index, byte) }
    }

    private fun putHandler(buffer: ByteBuffer, offset: Int) {
        buffer.putInt(offset, 0xd0000000.toInt())
        buffer.putInt(offset + WORD_SIZE, 0x91000000.toInt())
        buffer.putInt(offset + 2 * WORD_SIZE, 0x94000000.toInt())
        buffer.putInt(offset + 3 * WORD_SIZE, 0x6b0002bf)
        buffer.putInt(offset + 4 * WORD_SIZE, branchNotEqual(offset + 4 * WORD_SIZE, offset + HANDLER_SIZE))
        putHomeCall(buffer, offset + 5 * WORD_SIZE)
    }

    private fun putHomeCall(buffer: ByteBuffer, offset: Int) {
        buffer.putInt(offset, 0x12800000)
        buffer.putInt(offset + WORD_SIZE, 0x2a1f03e1)
        buffer.putInt(offset + 2 * WORD_SIZE, 0x94000000.toInt())
        buffer.putInt(offset + 3 * WORD_SIZE, 0x14000008)
    }

    private fun branchNotEqual(from: Int, target: Int) = 0x54000001 or ((target - from) / WORD_SIZE shl 5)

    private companion object {
        const val WORD_SIZE = 4
        const val LIBRARY_SIZE = 0x600
        const val CODE = 0x100
        const val CODE_ADDRESS = 0x1100L
        const val CODE_SIZE = 0x180
        const val DESCRIPTOR_STORE = CODE + 4 * WORD_SIZE
        const val HANDLER = 0x180
        const val HANDLER_SIZE = 0x40
        const val SECOND_HANDLER = HANDLER + HANDLER_SIZE
        const val EVENT_BRANCH = HANDLER + 4 * WORD_SIZE
        const val HOME_CALL = HANDLER + 7 * WORD_SIZE
        const val UNRELATED_CALL = 0x240
        const val STRINGS = 0x300
        const val STRINGS_ADDRESS = 0x2300L
        const val STRINGS_SIZE = 0x40
        const val SECTION_NAMES = 0x350
        const val SECTION_NAMES_SIZE = 0x20
        const val SECTION_TABLE = 0x400
        const val SECTION_HEADER_SIZE = 0x40
    }
}
