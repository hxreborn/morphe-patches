/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dotnet

import app.morphe.patcher.patch.PatchException
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

private const val PAYLOAD_OFFSET = 0x100
private val COMPRESSED_TEXT = ABC_REPEATED_THEN_X.toByteArray()
private val RAW_IMAGE = "MZ raw image".toByteArray()

class AssemblyStoreLibraryTest {
    @Test
    fun readCompressedAndRawAssemblies() {
        for (is64Bit in listOf(true, false)) {
            val library = AssemblyStoreLibrary(library(is64Bit))

            assertContentEquals(COMPRESSED_TEXT, library.readAssembly("First.dll"))
            assertContentEquals(RAW_IMAGE, library.readAssembly("Second.dll"))
        }
    }

    @Test
    fun appendReplacementAndKeepOtherAssemblies() {
        for (is64Bit in listOf(true, false)) {
            val replacement = "MZ patched image".toByteArray()
            val output = tempFile()

            AssemblyStoreLibrary(library(is64Bit)).writeWithAssembly("First.dll", replacement, output)
            val patched = AssemblyStoreLibrary(output.readBytes())

            assertContentEquals(replacement, patched.readAssembly("First.dll"))
            assertContentEquals(RAW_IMAGE, patched.readAssembly("Second.dll"))
        }
    }

    @Test
    fun rejectMissingAssembly() {
        assertFailsWith<PatchException> { AssemblyStoreLibrary(library(is64Bit = true)).readAssembly("Third.dll") }
    }

    private fun store(): ByteArray {
        val names = listOf("First.dll", "Second.dll")
        val compressed = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x5a4c4158).putInt(0).putInt(COMPRESSED_TEXT.size).array() + ABC_REPEATED_THEN_X_LZ4
        val images = listOf(compressed, RAW_IMAGE)
        val indexSize = 0
        val namesSize = names.sumOf { Int.SIZE_BYTES + it.length }
        var dataOffset = 20 + indexSize + names.size * 28 + namesSize
        val buffer = ByteBuffer.allocate(dataOffset + images.sumOf { it.size }).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0x41424158).putInt(0x80010003.toInt()).putInt(names.size).putInt(0).putInt(indexSize)
        images.forEachIndexed { index, image ->
            buffer.putInt(index).putInt(dataOffset).putInt(image.size).putInt(0).putInt(0).putInt(0).putInt(0)
            dataOffset += image.size
        }
        names.forEach { buffer.putInt(it.length).put(it.toByteArray()) }
        images.forEach { buffer.put(it) }
        return buffer.array()
    }

    private fun library(is64Bit: Boolean): ByteArray {
        val payload = store()
        val sectionNames = "\u0000.shstrtab\u0000payload\u0000".toByteArray()
        val sectionNamesOffset = PAYLOAD_OFFSET - sectionNames.size
        val entrySize = if (is64Bit) 64 else 40
        val sectionTable = PAYLOAD_OFFSET + payload.size + 7 and 7.inv()
        val elf = ByteBuffer.allocate(sectionTable + 3 * entrySize).order(ByteOrder.LITTLE_ENDIAN)
        elf.put(4, if (is64Bit) 2 else 1)
        if (is64Bit) {
            elf.putLong(0x28, sectionTable.toLong()).putShort(0x3a, entrySize.toShort()).putShort(0x3c, 3).putShort(0x3e, 1)
        } else {
            elf.putInt(0x20, sectionTable).putShort(0x2e, entrySize.toShort()).putShort(0x30, 3).putShort(0x32, 1)
        }
        elf.position(sectionNamesOffset)
        elf.put(sectionNames)
        elf.put(payload)
        section(elf, sectionTable + entrySize, is64Bit, name = 1, offset = sectionNamesOffset, size = sectionNames.size)
        section(elf, sectionTable + 2 * entrySize, is64Bit, name = 11, offset = PAYLOAD_OFFSET, size = payload.size)
        return elf.array()
    }

    private fun section(elf: ByteBuffer, header: Int, is64Bit: Boolean, name: Int, offset: Int, size: Int) {
        elf.putInt(header, name).putInt(header + 4, 1)
        if (is64Bit) {
            elf.putLong(header + 24, offset.toLong()).putLong(header + 32, size.toLong())
        } else {
            elf.putInt(header + 16, offset).putInt(header + 20, size)
        }
    }

    private fun tempFile(): File = createTempFile(suffix = ".so").toFile().apply { deleteOnExit() }
}
