/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pairip

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val PT_LOAD = 1
private const val PT_DYNAMIC = 2
private const val PF_WRITE = 2
private const val SHT_NOBITS = 8

private const val DT_NULL = 0L
private const val DT_NEEDED = 1L
private const val DT_PLTRELSZ = 2L
private const val DT_HASH = 4L
private const val DT_STRTAB = 5L
private const val DT_SYMTAB = 6L
private const val DT_STRSZ = 10L
private const val DT_INIT = 12L
private const val DT_VERSYM = 0x6FFFFFF0L

private const val MACHINE_ARM = 40
private const val MACHINE_X86_64 = 62
private const val MACHINE_AARCH64 = 183

private const val PAIRIP_RUNTIME_LIBRARY = "libpairipcore.so"
private const val ELF32_HEADER_SIZE = 52
private const val ELF32_PROGRAM_TABLE_OFFSET_FIELD = 0x1C
private const val ELF64_PROGRAM_TABLE_OFFSET_FIELD = 0x20
private const val ELF32_SECTION_TABLE_OFFSET_FIELD = 0x20
private const val ELF64_SECTION_TABLE_OFFSET_FIELD = 0x28
private val ELF_MAGIC = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
private const val PAIRIP_RUNNER_SYMBOL = "ExecuteProgram"
private const val GLOBAL_FUNCTION_SYMBOL = (1 shl 4) or 2
private const val SYMBOL_VERSION_GLOBAL = 1
private const val HASH_ALIGNMENT = 4
private const val HASH_ENTRY_BYTES = 4

private class Segment(
    var type: Int,
    var flags: Int,
    var offset: Long,
    var vaddr: Long,
    var paddr: Long,
    var fileSize: Long,
    var memorySize: Long,
    var align: Long,
)

private class Section(
    var name: Int,
    var type: Int,
    var flags: Long,
    var address: Long,
    var offset: Long,
    var size: Long,
    var link: Int,
    var info: Int,
    var addressAlign: Long,
    var entrySize: Long,
)

private class Relocation(var offset: Long, var info: Long, var addend: Long)

private class DynamicEntry(val tag: Long, var value: Long)

private class Placement(val offset: Long, val address: Long, val size: Long)

internal class AppendedData(val library: ByteArray, val address: Long)

internal class ElfImage(private val bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    private val wide = bytes[4].toInt() == 2
    private val wordSize = if (wide) 8 else 4
    private val machine = buffer.getShort(0x12).toInt() and 0xFFFF
    private val sectionTableOffsetField = if (wide) ELF64_SECTION_TABLE_OFFSET_FIELD else ELF32_SECTION_TABLE_OFFSET_FIELD
    private val programOffset = word(if (wide) ELF64_PROGRAM_TABLE_OFFSET_FIELD else ELF32_PROGRAM_TABLE_OFFSET_FIELD).toInt()
    private val sectionOffset = word(sectionTableOffsetField).toInt()
    private val headerFieldsAt = if (wide) 0x36 else 0x2A
    private val programEntrySize = u16(headerFieldsAt)
    private val programCount = u16(headerFieldsAt + 2)
    private val sectionEntrySize = u16(headerFieldsAt + 4)
    private val sectionCount = u16(headerFieldsAt + 6)
    private val sectionNamesIndex = u16(headerFieldsAt + 8)

    private val programs = List(programCount) { readSegment(programOffset + it * programEntrySize) }
    private val sections = List(sectionCount) { readSection(sectionOffset + it * sectionEntrySize) }
    private val sectionsByName by lazy { sections.associateBy { cString(sections[sectionNamesIndex].offset.toInt() + it.name) } }

    private val jumpSlotType by lazy {
        when (machine) {
            MACHINE_AARCH64 -> 1026L
            MACHINE_X86_64 -> 7L
            MACHINE_ARM -> 22L
            else -> throw IllegalStateException("Unsupported ELF machine $machine")
        }
    }
    private val relocationSection = if (wide) ".rela.plt" else ".rel.plt"
    private val relocationSize = if (wide) 24 else 8
    private val symbolSize = if (wide) 24 else 16
    private val dynamicEntrySize = if (wide) 16 else 8

    fun needsPairipRuntime(): Boolean {
        val dynamic = programs.singleOrNull { it.type == PT_DYNAMIC } ?: return false
        val entries = readDynamicEntries(dynamic.offset.toInt(), (dynamic.fileSize / dynamicEntrySize).toInt())
        val stringTable = entries.singleOrNull { it.tag == DT_STRTAB }?.value ?: return false
        val strings = fileOffset(stringTable) ?: return false
        return entries.any { it.tag == DT_NEEDED && cString(strings + it.value.toInt()) == PAIRIP_RUNTIME_LIBRARY }
    }

    private fun fileOffset(address: Long) = programs
        .firstOrNull { it.type == PT_LOAD && address >= it.vaddr && address < it.vaddr + it.fileSize }
        ?.let { (it.offset + address - it.vaddr).toInt() }

    fun withoutPairipRuntime(imports: List<HiddenImport>): ByteArray {
        val dynamic = programs.single { it.type == PT_DYNAMIC }
        val dynamicCapacity = (dynamic.fileSize / dynamicEntrySize).toInt()
        val dynamicEntries = readDynamicEntries(dynamic.offset.toInt(), dynamicCapacity)
        val originalStrings = section(".dynstr").offset.toInt()
        val runtimeNeeded = dynamicEntries.single {
            it.tag == DT_NEEDED && cString(originalStrings + it.value.toInt()) == PAIRIP_RUNTIME_LIBRARY
        }
        val init = dynamicEntries.single { it.tag == DT_INIT }

        val relocations = readRelocations().toMutableList()
        val runner = relocations.single { symbolName(symbolIndex(it.info)) == PAIRIP_RUNNER_SYMBOL }
        relocations.remove(runner)

        val strings = ByteArrayOutputStream().apply { write(sectionBytes(".dynstr")) }
        val symbols = ByteArrayOutputStream().apply { write(sectionBytes(".dynsym")) }
        val versions = ByteArrayOutputStream().apply { write(sectionBytes(".gnu.version")) }
        val symbolIndexByName = mutableMapOf<String, Int>()
        imports.map { it.name }.distinct().sorted().forEach { name ->
            symbolIndexByName[name] = symbols.size() / symbolSize
            symbols.write(symbolEntry(strings.size()))
            strings.write(name.toByteArray(Charsets.US_ASCII))
            strings.write(0)
            versions.write(byteArrayOf(SYMBOL_VERSION_GLOBAL.toByte(), 0))
        }
        imports.sortedWith(compareBy<HiddenImport> { it.slot }.thenBy { it.name }).forEach {
            relocations += Relocation(it.slot, relocationInfo(symbolIndexByName.getValue(it.name), jumpSlotType), 0)
        }

        val relocationTable = section(relocationSection)
        val relocationLimit = following(relocationTable)
        check(relocationTable.offset + relocations.size * relocationSize <= relocationLimit) {
            "${relocations.size} PLT relocations do not fit before $relocationLimit"
        }
        bytes.fill(0, relocationTable.offset.toInt(), relocationLimit.toInt())
        relocations.forEachIndexed { index, relocation -> writeRelocation(relocationTable.offset.toInt() + index * relocationSize, relocation) }
        relocationTable.size = relocations.size.toLong() * relocationSize

        val lastLoad = programs.filter { it.type == PT_LOAD }.maxBy { it.vaddr }
        val segmentEnd = lastLoad.offset + lastLoad.fileSize
        check(
            lastLoad.flags and PF_WRITE != 0 && lastLoad.offset <= dynamic.offset &&
                dynamic.offset + dynamicCapacity * dynamicEntrySize <= segmentEnd && lastLoad.memorySize == lastLoad.fileSize,
        ) { "The last segment does not hold .dynamic" }
        check(sectionOffset.toLong() == segmentEnd && bytes.size.toLong() == segmentEnd + sectionCount * sectionEntrySize) {
            "Unexpected layout after the last segment"
        }
        check(sections.none { it.offset + it.size > segmentEnd && it.type != SHT_NOBITS }) { "A section lies past the last segment" }

        val tail = ByteArrayOutputStream()
        val placed = mutableMapOf<String, Placement>()
        val blobs = mutableListOf(
            Triple(".dynstr", strings.toByteArray(), 1),
            Triple(".dynsym", symbols.toByteArray(), wordSize),
            Triple(".gnu.version", versions.toByteArray(), 2),
        )
        if (dynamicEntries.any { it.tag == DT_HASH }) {
            blobs += Triple(".hash", extendedHashTable(symbols.size() / symbolSize), HASH_ALIGNMENT)
        }
        blobs.forEach { (name, blob, alignment) ->
            tail.write(ByteArray(((alignment - (segmentEnd + tail.size()) % alignment) % alignment).toInt()))
            placed[name] = Placement(segmentEnd + tail.size(), lastLoad.vaddr + lastLoad.fileSize + tail.size(), blob.size.toLong())
            tail.write(blob)
        }
        placed.forEach { (name, placement) ->
            section(name).apply {
                offset = placement.offset
                address = placement.address
                size = placement.size
            }
        }

        val updates = mapOf(
            DT_STRTAB to placed.getValue(".dynstr").address,
            DT_STRSZ to placed.getValue(".dynstr").size,
            DT_SYMTAB to placed.getValue(".dynsym").address,
            DT_VERSYM to placed.getValue(".gnu.version").address,
            DT_HASH to (placed[".hash"]?.address ?: 0L),
            DT_PLTRELSZ to relocationTable.size,
        )
        val remaining = dynamicEntries.filter { it !== runtimeNeeded && it !== init }
        remaining.forEach { entry -> updates[entry.tag]?.let { entry.value = it } }
        for (index in 0 until dynamicCapacity) {
            val entry = remaining.getOrNull(index)
            writeDynamicEntry(dynamic.offset.toInt() + index * dynamicEntrySize, entry?.tag ?: DT_NULL, entry?.value ?: 0)
        }

        lastLoad.fileSize += tail.size()
        lastLoad.memorySize = lastLoad.fileSize
        val output = ByteArrayOutputStream()
        output.write(bytes, 0, segmentEnd.toInt())
        output.write(tail.toByteArray())
        output.write(ByteArray((wordSize - output.size() % wordSize) % wordSize))
        return appendHeaderTables(output)
    }

    fun address(fileOffset: Int): Long = programs
        .first { it.type == PT_LOAD && fileOffset >= it.offset && fileOffset < it.offset + it.fileSize }
        .let { it.vaddr + fileOffset - it.offset }

    fun appendToWritableSegment(data: ByteArray, alignment: Long): AppendedData {
        val segment = programs.filter { it.type == PT_LOAD }.maxBy { it.vaddr }
        check(segment.flags and PF_WRITE != 0) { "The last segment is not writable" }
        val segmentEnd = segment.offset + segment.fileSize
        val initializedEnd = segment.vaddr + segment.fileSize
        val address = alignUp(initializedEnd, alignment)
        val insertedSize = alignUp(address + data.size - initializedEnd, wordSize.toLong())
        val uninitializedStart = sections
            .filter { it.type == SHT_NOBITS && it.address >= initializedEnd }
            .minOfOrNull { it.address } ?: (segment.vaddr + segment.memorySize)
        check(initializedEnd + insertedSize <= uninitializedStart) {
            "${data.size} bytes do not fit between 0x${initializedEnd.toString(16)} and 0x${uninitializedStart.toString(16)}"
        }
        check(sectionOffset >= segmentEnd && sections.none { it.type != SHT_NOBITS && it.flags != 0L && it.offset >= segmentEnd }) {
            "Allocated section after the last loaded segment"
        }

        sections.filter { it.offset >= segmentEnd }.forEach { it.offset += insertedSize }
        segment.fileSize += insertedSize
        val output = ByteArrayOutputStream()
        output.write(bytes, 0, segmentEnd.toInt())
        output.write(ByteArray((address - initializedEnd).toInt()))
        output.write(data)
        output.write(ByteArray((initializedEnd + insertedSize - address - data.size).toInt()))
        output.write(bytes, segmentEnd.toInt(), sectionOffset - segmentEnd.toInt())
        return AppendedData(appendHeaderTables(output), address)
    }

    private fun appendHeaderTables(output: ByteArrayOutputStream): ByteArray {
        val sectionTableOffset = output.size()
        sections.forEach { output.write(encodeSection(it)) }
        val result = output.toByteArray()
        val resultBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
        writeWord(resultBuffer, sectionTableOffsetField, sectionTableOffset.toLong())
        programs.forEachIndexed { index, segment -> writeSegment(resultBuffer, programOffset + index * programEntrySize, segment) }
        return result
    }

    private fun writeWord(target: ByteBuffer, at: Int, value: Long) {
        if (wide) target.putLong(at, value) else target.putInt(at, value.toInt())
    }

    private fun alignUp(value: Long, alignment: Long) = (value + alignment - 1) / alignment * alignment

    private fun extendedHashTable(symbolCount: Int): ByteArray {
        val table = sectionBytes(".hash")
        val original = ByteBuffer.wrap(table).order(ByteOrder.LITTLE_ENDIAN)
        val chainCount = original.getInt(4)
        check(symbolCount >= chainCount) { "$symbolCount symbols are fewer than the $chainCount in .hash" }
        val extended = ByteBuffer.allocate(table.size + (symbolCount - chainCount) * HASH_ENTRY_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        extended.put(table).putInt(4, symbolCount)
        return extended.array()
    }

    private fun section(name: String) = sectionsByName[name] ?: throw IllegalStateException("No $name section")

    private fun sectionBytes(name: String) = section(name).let { bytes.copyOfRange(it.offset.toInt(), (it.offset + it.size).toInt()) }

    private fun following(section: Section) = sections.filter { it.offset > section.offset && it.flags != 0L }.minOf { it.offset }

    private fun u16(at: Int) = buffer.getShort(at).toInt() and 0xFFFF

    private fun u32(at: Int) = buffer.getInt(at).toLong() and 0xFFFFFFFFL

    private fun word(at: Int) = if (wide) buffer.getLong(at) else u32(at)

    private fun cString(offset: Int): String {
        val end = (offset until bytes.size).first { bytes[it].toInt() == 0 }
        return String(bytes, offset, end - offset, Charsets.US_ASCII)
    }

    private fun symbolIndex(info: Long) = (if (wide) info ushr 32 else info ushr 8).toInt()

    private fun relocationInfo(symbol: Int, type: Long) = if (wide) (symbol.toLong() shl 32) or type else (symbol.toLong() shl 8) or type

    private fun symbolName(index: Int): String {
        val entry = section(".dynsym").offset.toInt() + index * symbolSize
        return cString(section(".dynstr").offset.toInt() + u32(entry).toInt())
    }

    private fun symbolEntry(nameOffset: Int): ByteArray {
        val entry = ByteBuffer.allocate(symbolSize).order(ByteOrder.LITTLE_ENDIAN)
        entry.putInt(nameOffset)
        if (wide) {
            entry.put(GLOBAL_FUNCTION_SYMBOL.toByte())
        } else {
            entry.putInt(0).putInt(0).put(GLOBAL_FUNCTION_SYMBOL.toByte())
        }
        return entry.array()
    }

    private fun readSegment(at: Int) = if (wide) {
        Segment(
            buffer.getInt(at), buffer.getInt(at + 4), buffer.getLong(at + 8), buffer.getLong(at + 16),
            buffer.getLong(at + 24), buffer.getLong(at + 32), buffer.getLong(at + 40), buffer.getLong(at + 48),
        )
    } else {
        Segment(
            buffer.getInt(at), buffer.getInt(at + 24), u32(at + 4), u32(at + 8),
            u32(at + 12), u32(at + 16), u32(at + 20), u32(at + 28),
        )
    }

    private fun writeSegment(target: ByteBuffer, at: Int, segment: Segment) {
        if (wide) {
            target.putInt(at, segment.type).putInt(at + 4, segment.flags).putLong(at + 8, segment.offset)
                .putLong(at + 16, segment.vaddr).putLong(at + 24, segment.paddr).putLong(at + 32, segment.fileSize)
                .putLong(at + 40, segment.memorySize).putLong(at + 48, segment.align)
        } else {
            target.putInt(at, segment.type).putInt(at + 4, segment.offset.toInt()).putInt(at + 8, segment.vaddr.toInt())
                .putInt(at + 12, segment.paddr.toInt()).putInt(at + 16, segment.fileSize.toInt())
                .putInt(at + 20, segment.memorySize.toInt()).putInt(at + 24, segment.flags).putInt(at + 28, segment.align.toInt())
        }
    }

    private fun readSection(at: Int) = if (wide) {
        Section(
            buffer.getInt(at), buffer.getInt(at + 4), buffer.getLong(at + 8), buffer.getLong(at + 16), buffer.getLong(at + 24),
            buffer.getLong(at + 32), buffer.getInt(at + 40), buffer.getInt(at + 44), buffer.getLong(at + 48), buffer.getLong(at + 56),
        )
    } else {
        Section(
            buffer.getInt(at), buffer.getInt(at + 4), u32(at + 8), u32(at + 12), u32(at + 16),
            u32(at + 20), buffer.getInt(at + 24), buffer.getInt(at + 28), u32(at + 32), u32(at + 36),
        )
    }

    private fun encodeSection(section: Section): ByteArray {
        val entry = ByteBuffer.allocate(sectionEntrySize).order(ByteOrder.LITTLE_ENDIAN)
        entry.putInt(section.name).putInt(section.type)
        if (wide) {
            entry.putLong(section.flags).putLong(section.address).putLong(section.offset).putLong(section.size)
                .putInt(section.link).putInt(section.info).putLong(section.addressAlign).putLong(section.entrySize)
        } else {
            entry.putInt(section.flags.toInt()).putInt(section.address.toInt()).putInt(section.offset.toInt())
                .putInt(section.size.toInt()).putInt(section.link).putInt(section.info)
                .putInt(section.addressAlign.toInt()).putInt(section.entrySize.toInt())
        }
        return entry.array()
    }

    private fun readDynamicEntries(at: Int, capacity: Int): List<DynamicEntry> {
        val entries = mutableListOf<DynamicEntry>()
        for (index in 0 until capacity) {
            val entryAt = at + index * dynamicEntrySize
            val tag = if (wide) buffer.getLong(entryAt) else buffer.getInt(entryAt).toLong()
            if (tag == DT_NULL) break
            entries += DynamicEntry(tag, word(entryAt + wordSize))
        }
        return entries
    }

    private fun writeDynamicEntry(at: Int, tag: Long, value: Long) {
        if (wide) {
            buffer.putLong(at, tag).putLong(at + 8, value)
        } else {
            buffer.putInt(at, tag.toInt()).putInt(at + 4, value.toInt())
        }
    }

    private fun readRelocations(): List<Relocation> {
        val table = section(relocationSection)
        return List((table.size / relocationSize).toInt()) {
            val at = table.offset.toInt() + it * relocationSize
            if (wide) Relocation(buffer.getLong(at), buffer.getLong(at + 8), buffer.getLong(at + 16)) else Relocation(u32(at), u32(at + 4), 0)
        }
    }

    private fun writeRelocation(at: Int, relocation: Relocation) {
        if (wide) {
            buffer.putLong(at, relocation.offset).putLong(at + 8, relocation.info).putLong(at + 16, relocation.addend)
        } else {
            buffer.putInt(at, relocation.offset.toInt()).putInt(at + 4, relocation.info.toInt())
        }
    }
}

internal fun ByteArray.isPairipWrappedLibrary() =
    size >= ELF32_HEADER_SIZE && copyOf(ELF_MAGIC.size).contentEquals(ELF_MAGIC) && ElfImage(this).needsPairipRuntime()
