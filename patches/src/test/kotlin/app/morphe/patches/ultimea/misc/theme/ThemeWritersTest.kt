/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.PatchException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThemeWritersTest {
    @Test
    fun installHooksAndReplayDisplacedInstructions() {
        val bytes = library()
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val color = 0x9340fc01.toInt()
        val gradient = 0xaa0203e0.toInt()
        val surface = 0xb8017043.toInt()
        code.putInt(COLOR_SITE, color)
        code.putInt(GRADIENT_SITE, gradient)
        code.putInt(SURFACE_SITE, surface)
        val image = ThemeElfImage(bytes)
        val matches = ThemeInstructionMatches(CODE_OFFSET, COLOR_SITE, GRADIENT_SITE, SURFACE_SITE,
            mapOf(CODE_OFFSET + 100 to ThemeColorRole.INK, CODE_OFFSET + 104 to ThemeColorRole.SURFACE))
        ThemeHooks.apply(image, matches)
        for ((site, header) in listOf(COLOR_SITE to 0, GRADIENT_SITE to 4, SURFACE_SITE to 36)) {
            val instruction = code.getInt(site)
            assertEquals(0x94000000.toInt(), instruction and 0xfc000000.toInt())
            assertEquals(image.hookAddress + code.getInt(image.hookOffset + header),
                image.instructionAddress(site) + (instruction shl 6 shr 4))
        }
        for ((header, instruction) in listOf(8 to color, 12 to gradient, 40 to surface)) {
            assertEquals(instruction, code.getInt(image.hookOffset + code.getInt(image.hookOffset + header)))
        }
        assertEquals(1, code.getInt(image.hookOffset + code.getInt(image.hookOffset + 16)))
        val callers = image.hookOffset + code.getInt(image.hookOffset + 20)
        for ((index, entry) in matches.colorRolesByReturnAddress.entries.withIndex()) {
            assertEquals((image.instructionAddress(entry.key) / Int.SIZE_BYTES shl 3) or entry.value.code,
                code.getInt(callers + index * Int.SIZE_BYTES))
        }
        assertEquals(0, code.getInt(callers + matches.colorRolesByReturnAddress.size * Int.SIZE_BYTES))
        val template = ThemeHooks::class.java.getResourceAsStream("/ultimea/theme.bin")!!.use { it.readBytes() }
        val size = CODE_BYTES + template.size + (matches.colorRolesByReturnAddress.size + 1) * Int.SIZE_BYTES
        assertEquals(size.toLong(), code.getLong(PROGRAM_OFFSET + 32))
        assertEquals(size.toLong(), code.getLong(PROGRAM_OFFSET + 40))
        assertEquals(SEGMENT_BYTES + 8L, code.getLong(PROGRAM_OFFSET + PROGRAM_BYTES * 2 + 40))
        assertEquals(LIBRARY_BYTES, bytes.size)
    }

    @Test
    fun rejectVirtualOverlapBeforeWritingHooks() {
        val bytes = library()
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putLong(PROGRAM_OFFSET + PROGRAM_BYTES + 16, CODE_ADDRESS + CODE_BYTES + 128L)
        val original = bytes.copyOf()
        assertFailsWith<PatchException> { ThemeElfImage(bytes).install(ByteArray(256)) }
        assertContentEquals(original, bytes)
    }

    @Test
    fun allowHooksEndingAtNextVirtualSegment() {
        val bytes = library()
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val hookBytes = PAGE_BYTES - CODE_BYTES
        code.putLong(PROGRAM_OFFSET + PROGRAM_BYTES + 16, CODE_ADDRESS + CODE_BYTES + hookBytes.toLong())
        ThemeElfImage(bytes).install(ByteArray(hookBytes))
        assertEquals(CODE_BYTES + hookBytes.toLong(), code.getLong(PROGRAM_OFFSET + 40))
    }

    @Test
    fun rejectWritableCacheOverlapBeforeWritingHooks() {
        for (cacheBytes in listOf(8, 24)) {
            val bytes = library()
            val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            code.putLong(PROGRAM_OFFSET + PROGRAM_BYTES + 16, WRITABLE_ADDRESS + SEGMENT_BYTES + 4L)
            val original = bytes.copyOf()
            assertFailsWith<PatchException> { ThemeElfImage(bytes).install(ByteArray(128), cacheBytes) }
            assertContentEquals(original, bytes)
        }
    }

    @Test
    fun allowCacheEndingAtNextVirtualSegment() {
        for (cacheBytes in listOf(8, 24)) {
            val bytes = library()
            val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            code.putLong(PROGRAM_OFFSET + PROGRAM_BYTES * 2 + 40, PAGE_BYTES - cacheBytes.toLong())
            code.putLong(PROGRAM_OFFSET + PROGRAM_BYTES + 16, WRITABLE_ADDRESS + PAGE_BYTES.toLong())
            ThemeElfImage(bytes).install(ByteArray(128), cacheBytes)
            assertEquals(PAGE_BYTES.toLong(), code.getLong(PROGRAM_OFFSET + PROGRAM_BYTES * 2 + 40))
        }
    }

    @Test
    fun appendHooksAfterTheExecutableZeroTail() {
        val bytes = library()
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val zeroTail = 64
        code.putLong(PROGRAM_OFFSET + 40, CODE_BYTES + zeroTail.toLong())
        val image = ThemeElfImage(bytes)
        val hooks = ByteArray(128) { 7 }
        image.install(hooks)
        assertEquals(CODE_OFFSET + CODE_BYTES + zeroTail, image.hookOffset)
        assertEquals(CODE_ADDRESS + CODE_BYTES + zeroTail, image.hookAddress)
        assertContentEquals(ByteArray(zeroTail), bytes.copyOfRange(CODE_OFFSET + CODE_BYTES, image.hookOffset))
        assertContentEquals(hooks, bytes.copyOfRange(image.hookOffset, image.hookOffset + hooks.size))
        assertEquals(CODE_BYTES + zeroTail + hooks.size.toLong(), code.getLong(PROGRAM_OFFSET + 32))
        assertEquals(code.getLong(PROGRAM_OFFSET + 32), code.getLong(PROGRAM_OFFSET + 40))
    }

    @Test
    fun rejectHookGrowthIntoAnotherLoadPage() {
        val bytes = library()
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putLong(PROGRAM_OFFSET + PROGRAM_BYTES + 16, CODE_ADDRESS + PAGE_BYTES + 128L)
        val original = bytes.copyOf()
        assertFailsWith<PatchException> { ThemeElfImage(bytes).install(ByteArray(PAGE_BYTES - CODE_BYTES + 64)) }
        assertContentEquals(original, bytes)
    }

    @Test
    fun rejectCacheGrowthIntoAnotherLoadPage() {
        val bytes = library()
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putLong(PROGRAM_OFFSET + PROGRAM_BYTES * 2 + 40, PAGE_BYTES - 16L)
        code.putLong(PROGRAM_OFFSET + PROGRAM_BYTES + 16, WRITABLE_ADDRESS + PAGE_BYTES + 128L)
        val original = bytes.copyOf()
        assertFailsWith<PatchException> { ThemeElfImage(bytes).install(ByteArray(128), 24) }
        assertContentEquals(original, bytes)
    }

    @Test
    fun rewriteImageDecoderRegistrationPriority() {
        val bytes = library()
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val callback = CODE_OFFSET + 128
        val registration = CODE_OFFSET + 512
        val priority = registration + 16
        val slot = WRITABLE_ADDRESS + 64L
        val message = "Failed to set Workers task runner priority".toByteArray()
        message.copyInto(bytes, READ_ONLY_OFFSET)
        code.putInt(callback, 0xd10083ff.toInt())
        addressPair(code, callback + 8, CODE_ADDRESS + 128 + 8L, READ_ONLY_ADDRESS.toLong())
        code.putLong(RELOCATION_OFFSET, slot)
        code.putLong(RELOCATION_OFFSET + 8, 1027)
        code.putLong(RELOCATION_OFFSET + 16, CODE_ADDRESS + 128L)
        addressPair(code, registration, CODE_ADDRESS + 512L, slot)
        code.putInt(priority, 0x12800008)
        code.putInt(priority + 8, 0xb90003e8.toInt())
        assertEquals(priority, FlutterImageDecoder.priorityInstruction(bytes))
        val expected = bytes.copyOf()
        ByteBuffer.wrap(expected).order(ByteOrder.LITTLE_ENDIAN).putInt(priority, 0x52800028)
        FlutterImageDecoder.apply(bytes)
        assertContentEquals(expected, bytes)
    }

    private fun addressPair(code: ByteBuffer, offset: Int, address: Long, target: Long) {
        val pages = ((target shr 12) - (address shr 12)).toInt()
        code.putInt(offset, 0x90000000.toInt() or ((pages and 3) shl 29) or (((pages ushr 2) and 0x7ffff) shl 5))
        code.putInt(offset + Int.SIZE_BYTES, 0x91000000.toInt() or ((target.toInt() and 0xfff) shl 10))
    }

    private fun library(): ByteArray {
        val bytes = ByteArray(LIBRARY_BYTES)
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        bytes[4] = 2
        code.putShort(18, 183)
        code.putLong(32, PROGRAM_OFFSET.toLong())
        code.putShort(54, PROGRAM_BYTES.toShort())
        code.putShort(56, 3)
        val segments = listOf(Triple(CODE_OFFSET, CODE_ADDRESS, 5),
            Triple(READ_ONLY_OFFSET, READ_ONLY_ADDRESS, 4), Triple(WRITABLE_OFFSET, WRITABLE_ADDRESS, 6))
        segments.forEachIndexed { index, (offset, address, flags) ->
            val header = PROGRAM_OFFSET + index * PROGRAM_BYTES
            val size = if (index == 0) CODE_BYTES else SEGMENT_BYTES
            code.putInt(header, 1)
            code.putInt(header + 4, flags)
            code.putLong(header + 8, offset.toLong())
            code.putLong(header + 16, address.toLong())
            code.putLong(header + 32, size.toLong())
            code.putLong(header + 40, size.toLong())
        }
        return bytes
    }

    private companion object {
        const val LIBRARY_BYTES = 32768
        const val PAGE_BYTES = 4096
        const val PROGRAM_OFFSET = 64
        const val PROGRAM_BYTES = 56
        const val CODE_OFFSET = 4096
        const val CODE_ADDRESS = 0x2000
        const val CODE_BYTES = 1024
        const val READ_ONLY_OFFSET = 16384
        const val READ_ONLY_ADDRESS = 0x10000
        const val WRITABLE_OFFSET = 20000
        const val WRITABLE_ADDRESS = 0x20000
        const val SEGMENT_BYTES = 128
        const val RELOCATION_OFFSET = 24000
        const val COLOR_SITE = CODE_OFFSET + 4
        const val GRADIENT_SITE = CODE_OFFSET + 24
        const val SURFACE_SITE = CODE_OFFSET + 44
    }
}
