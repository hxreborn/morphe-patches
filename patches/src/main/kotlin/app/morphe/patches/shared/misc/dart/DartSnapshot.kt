/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dart

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.indexOfMasked
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.readShort
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val SNAPSHOT_SYMBOL = "_kDartIsolateSnapshotData"
private const val SNAPSHOT_VERSION = "97ff04a728735e6b6b098bdf983faaba"
private const val SNAPSHOT_HEADER_BYTES = 20
private const val SNAPSHOT_VERSION_BYTES = 32
private const val SNAPSHOT_LENGTH = 4
private const val SNAPSHOT_MAGIC = 0xdcdcf5f5.toInt()
private const val STRING_CLASS = 93
private const val OBJECT_POOL_CLASS = 23
private const val CLASS_SHIFT = 12
private const val CANONICAL_BIT = 2
private const val PREDEFINED_CLASSES = 175
private const val OBJECT_HEADER_WORDS = 2
private const val IMMUTABLE_ARRAY_CLASS = 91
private const val ARRAY_PROBE_COUNT = 32
private const val UNBOXED_WORD_PARTS = 2
private const val BITMAP_BITS = 64
private const val CLASS_CLASS = 5
private const val CODE_CLASS = 18
private const val INTEGER_CLASS = 61
private const val INSTANCE_CLASS = 45
private const val TYPE_ARGUMENTS_CLASS = 47
private const val FIRST_FFI_CLASS = 96
private const val LAST_FFI_CLASS = 110
private const val FIRST_TYPED_DATA_CLASS = 112
private const val LAST_TYPED_DATA_CLASS = 167
private const val TYPED_DATA_CLASS_STRIDE = 4
private val LENGTH_CLASSES = setOf(17, 23, 24, 25, 26, 28, 29, 30, 47, 67, 90, 91, 93)
private val CANONICAL_LAYOUT_CLASSES = setOf(47, 49, 50, 51, 52, 93)
private const val POOL_ENTRY_BYTES = 8
private const val POOL_HEADER_BYTES = 16
private const val POOL_TYPE_MASK = 15
private const val POOL_BEHAVIOR_SHIFT = 5
private const val POOL_TAGGED_OBJECT = 1
private const val POOL_IMMEDIATE = 0
private const val POOL_NATIVE_FUNCTION = 2
private const val POOL_NOT_SNAPSHOTABLE = 1
private const val POOL_MAX_BEHAVIOR = 4
private const val MAX_REFERENCE_BYTES = 4
private const val DATA_BITS = 7
private const val BYTE_TERMINATOR = 128
private const val SIGNED_TERMINATOR = 192
private const val MAX_INTEGER_SHIFT = 63
private const val ELF_SECTION_TABLE = 40
private const val ELF_SECTION_ENTRY_SIZE = 58
private const val ELF_SECTION_COUNT = 60
private const val ELF_PROGRAM_TABLE = 32
private const val ELF_PROGRAM_ENTRY_SIZE = 54
private const val ELF_PROGRAM_COUNT = 56
private const val ELF_SECTION_OFFSET = 24
private const val ELF_SECTION_SIZE = 32
private const val ELF_SECTION_LINK = 40
private const val ELF_SECTION_ITEM_SIZE = 56
private const val ELF_SECTION_TYPE = 4
private const val ELF_DYNAMIC_SYMBOLS = 11
private const val ELF_SYMBOL_VALUE = 8
private const val ELF_LOAD = 1
private const val ELF_EXECUTE = 1
private const val ELF_SEGMENT_FLAGS = 4
private const val ELF_SEGMENT_OFFSET = 8
private const val ELF_SEGMENT_ADDRESS = 16
private const val ELF_SEGMENT_SIZE = 32

internal class DartSnapshot(private val bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    private val snapshot = findSnapshot()
    private val limit = snapshot + buffer.getLong(snapshot + SNAPSHOT_LENGTH).toInt() + Int.SIZE_BYTES
    private val start = bytes.indexOf(0.toByte(), snapshot + SNAPSHOT_HEADER_BYTES + SNAPSHOT_VERSION_BYTES) + 1
    private val header = Cursor(start).run { List(5) { unsigned().toInt() } }
    private val objects = header[1]
    private val allocations: List<Allocation>
    private val stringFillEnd: Int
    private val poolFill: Int
    private val poolReferences: Map<Int, Int>
    private val nonReferenceRanges = mutableListOf<IntRange>()
    private val canonicalObjects by lazy { readCanonicalObjects() }

    val namesByPoolOffset: Map<Int, String>
        get() = poolReferences.mapNotNull { (offset, id) ->
            (stringsById[id] ?: canonicalObjects.names[id])?.let { offset to it }
        }.toMap()
    private val stringsById: Map<Int, String>

    val stringsByPoolOffset: Map<Int, String>

    val executableOffsets: IntRange
        get() {
            val programs = buffer.getLong(ELF_PROGRAM_TABLE).toInt()
            val programSize = bytes.readShort(ELF_PROGRAM_ENTRY_SIZE)
            val executable = (0 until bytes.readShort(ELF_PROGRAM_COUNT)).map { programs + it * programSize }.single {
                bytes.readInt(it) == ELF_LOAD && bytes.readInt(it + ELF_SEGMENT_FLAGS) and ELF_EXECUTE != 0
            }
            val offset = buffer.getLong(executable + ELF_SEGMENT_OFFSET).toInt()
            return offset until offset + buffer.getLong(executable + ELF_SEGMENT_SIZE).toInt()
        }

    init {
        if (bytes.readInt(snapshot) != SNAPSHOT_MAGIC || limit > bytes.size || start <= snapshot) {
            throw PatchException("Invalid Dart snapshot header")
        }
        val version = String(bytes, snapshot + SNAPSHOT_HEADER_BYTES, SNAPSHOT_VERSION_BYTES, Charsets.US_ASCII)
        if (version != SNAPSHOT_VERSION) throw PatchException("Unsupported Dart snapshot version $version")
        val features = String(bytes, snapshot + SNAPSHOT_HEADER_BYTES + SNAPSHOT_VERSION_BYTES,
            start - snapshot - SNAPSHOT_HEADER_BYTES - SNAPSHOT_VERSION_BYTES - 1, Charsets.US_ASCII)
        if ("compressed-pointers" !in features.split(' ')) throw PatchException("Dart snapshot requires compressed pointers")
        val allocationCursor = Cursor(start).apply { repeat(header.size) { unsigned() } }
        allocations = readAllocations(allocationCursor)
        val strings = allocations.first()
        if (strings.cid != STRING_CLASS || !strings.canonical || strings.lengths == null) {
            throw PatchException("Expected leading canonical Dart string cluster")
        }
        stringsById = strings.lengths.mapIndexed { index, length ->
            if (allocationCursor.unsigned().toInt() != length) {
                throw PatchException("Dart string fill differs from allocation")
            }
            val size = stringBytes(length)
            allocationCursor.requireBytes(size)
            val value = String(bytes, allocationCursor.position, size,
                if (length and 1 == 0) Charsets.ISO_8859_1 else Charsets.UTF_16LE)
            allocationCursor.advance(size)
            strings.firstId + index to value
        }.toMap()
        stringFillEnd = allocationCursor.position
        nonReferenceRanges += snapshot until stringFillEnd
        val pool = allocations.singleOrNull { it.cid == OBJECT_POOL_CLASS && it.count == 1 }
            ?: throw PatchException("Expected one Dart global object pool allocation")
        val poolLength = pool.lengths?.single() ?: throw PatchException("Missing Dart object pool length")
        val needle = encoded(poolLength)
        val pools = positions(needle, stringFillEnd, limit).mapNotNull { offset ->
            parseCandidate { readPool(Cursor(offset + needle.size), poolLength)?.let { offset to it } }
        }
        val (fill, entries) = pools.singleOrNull()
            ?: throw PatchException("Expected one Dart global object pool fill")
        poolFill = fill
        poolReferences = entries.references
        nonReferenceRanges += fill until fill + needle.size
        nonReferenceRanges += entries.nonReferenceRanges
        stringsByPoolOffset = poolReferences.mapNotNull { (offset, id) -> stringsById[id]?.let { offset to it } }.toMap()
    }

    private fun readAllocations(cursor: Cursor): List<Allocation> {
        var nextId = header.first() + 1
        val result = List(header[2]) {
            val tags = cursor.signed().toInt()
            val cid = tags ushr CLASS_SHIFT
            val canonical = tags and CANONICAL_BIT != 0
            var count = cursor.unsigned().toInt()
            if (count !in 0..objects) throw PatchException("Invalid Dart cluster allocation count")
            var fields = 0
            var lengths: IntArray? = null
            var lengthPositions: IntArray? = null
            when {
                cid >= PREDEFINED_CLASSES || cid == INSTANCE_CLASS || cid in FIRST_FFI_CLASS..LAST_FFI_CLASS -> {
                    fields = cursor.signed().toInt() - OBJECT_HEADER_WORDS
                    val size = cursor.signed().toInt()
                    if (fields < 0 || size < fields + OBJECT_HEADER_WORDS) {
                        throw PatchException("Invalid Dart instance allocation layout")
                    }
                }
                cid == CLASS_CLASS -> {
                    repeat(count) { cursor.signed() }
                    count += cursor.unsigned().toInt()
                }
                cid == CODE_CLASS -> {
                    repeat(count) { cursor.signed() }
                    val deferred = cursor.unsigned().toInt()
                    repeat(deferred) { cursor.signed() }
                    count += deferred
                }
                cid == INTEGER_CLASS -> repeat(count) { cursor.signed() }
                cid in LENGTH_CLASSES || cid in FIRST_TYPED_DATA_CLASS..LAST_TYPED_DATA_CLASS &&
                    (cid - FIRST_TYPED_DATA_CLASS) % TYPED_DATA_CLASS_STRIDE == 0 -> {
                    lengthPositions = IntArray(count)
                    lengths = IntArray(count) { index ->
                        lengthPositions[index] = cursor.position
                        cursor.unsigned().toInt()
                    }
                }
            }
            if (canonical && cid in CANONICAL_LAYOUT_CLASSES) {
                cursor.unsigned()
                val prefix = cursor.unsigned().toInt()
                if (prefix !in 0..count) throw PatchException("Invalid Dart canonical cluster layout")
                repeat(count - prefix) { cursor.unsigned() }
            }
            Allocation(cid, canonical, nextId, count, fields, lengths, lengthPositions).also { nextId += count }
        }
        if (nextId != objects + 1) throw PatchException("Dart allocation reference count differs from snapshot header")
        return result
    }

    fun objectPoolOffsets(name: String): List<Int> = namesByPoolOffset.filterValues { it == name }.keys.toList()

    fun widgetArrayPoolOffsets(): Set<Int> = poolReferences.filterValues { id ->
        val elements = canonicalObjects.arrays[id]?.elements
        elements != null && elements.size == 3 && elements.all { element ->
            (allocations.firstOrNull { element >= it.firstId && element < it.firstId + it.count }?.cid ?: 0) >= PREDEFINED_CLASSES
        }
    }.keys

    fun removeFirstArrayElement(poolOffset: Int): List<Pair<Int, ByteArray>> {
        val id = poolReferences[poolOffset] ?: throw PatchException("Expected Dart array pool reference")
        val array = canonicalObjects.arrays[id] ?: throw PatchException("Expected canonical Dart array")
        if (array.elements.size != 3 || array.elements.any { element ->
            (allocations.firstOrNull { element >= it.firstId && element < it.firstId + it.count }?.cid ?: 0) < PREDEFINED_CLASSES
        }) throw PatchException("Expected three user-defined Dart widgets")
        val encodedReference = referenceBytes(id)
        val references = positions(encodedReference, snapshot, limit).filterNot { offset ->
            nonReferenceRanges.any { offset in it && offset + encodedReference.size - 1 in it }
        }
        if (references.size != 1) throw PatchException("Dart widget array has additional serialized references")
        val allocation = allocations.single { id >= it.firstId && id < it.firstId + it.count }
        val allocationPosition = allocation.lengthPositions?.get(id - allocation.firstId)
            ?: throw PatchException("Missing Dart array allocation position")
        val values = listOf(array.typeArguments) + array.elements.drop(1)
        val payload = values.map { referenceBytes(it).toMutableList() }
        var padding = array.referencesEnd - array.referencesStart - payload.sumOf { it.size }
        payload.forEach { value ->
            while (padding > 0 && value.size < MAX_REFERENCE_BYTES) {
                value.add(0, 0)
                padding--
            }
        }
        if (padding != 0 || encoded(3).size != encoded(2).size) {
            throw PatchException("Dart widget array replacement changes snapshot size")
        }
        return listOf(allocationPosition to encoded(2), array.lengthPosition to encoded(2),
            array.referencesStart to payload.flatten().toByteArray())
    }

    private fun readCanonicalObjects(): CanonicalObjects {
        val clusterIndex = allocations.indexOfLast { it.cid == IMMUTABLE_ARRAY_CLASS && it.canonical }
        if (clusterIndex < 0) throw PatchException("Missing canonical Dart array cluster")
        val cluster = allocations[clusterIndex]
        val lengths = cluster.lengths ?: throw PatchException("Missing Dart array lengths")
        val needle = encoded(lengths.first())
        val candidates = positions(needle, stringFillEnd, poolFill).filter { offset ->
            parseCandidate {
                val cursor = Cursor(offset)
                lengths.take(ARRAY_PROBE_COUNT).all { length ->
                    if (cursor.unsigned().toInt() != length) false else {
                        repeat(length + 1) { cursor.checkedReference() }
                        true
                    }
                }
            } == true
        }
        val start = candidates.singleOrNull() ?: throw PatchException("Expected one canonical Dart array fill")
        val cursor = Cursor(start)
        val arrays = lengths.mapIndexed { index, length ->
            val lengthPosition = cursor.position
            if (cursor.unsigned().toInt() != length) throw PatchException("Dart array fill differs from allocation")
            val referencesStart = cursor.position
            nonReferenceRanges += lengthPosition until referencesStart
            val typeArguments = cursor.checkedReference()
            val elements = IntArray(length) { cursor.checkedReference() }
            cluster.firstId + index to SnapshotArray(lengthPosition, referencesStart, cursor.position, typeArguments, elements)
        }.toMap()
        val names = mutableMapOf<Int, String>()
        for (allocation in allocations.drop(clusterIndex + 1).takeWhile { it.canonical }) {
            if (allocation.fields < 0) throw PatchException("Expected canonical Dart instance layout")
            val bitmapStart = cursor.position
            val bitmap = cursor.unsigned()
            nonReferenceRanges += bitmapStart until cursor.position
            repeat(allocation.count) { index ->
                repeat(allocation.fields) { field ->
                    val word = field + OBJECT_HEADER_WORDS
                    if (word < BITMAP_BITS && bitmap ushr word and 1L != 0L) {
                        val valueStart = cursor.position
                        repeat(UNBOXED_WORD_PARTS) { cursor.signed() }
                        nonReferenceRanges += valueStart until cursor.position
                    } else {
                        val id = cursor.checkedReference()
                        if (field == 0) stringsById[id]?.let { names[allocation.firstId + index] = it }
                    }
                }
            }
        }
        if (cursor.position > poolFill) throw PatchException("Dart canonical instances overlap object pool")
        return CanonicalObjects(arrays, names)
    }

    private fun referenceBytes(value: Int): ByteArray {
        var remaining = value
        val reversed = mutableListOf((remaining and (BYTE_TERMINATOR - 1) or BYTE_TERMINATOR).toByte())
        remaining = remaining ushr DATA_BITS
        while (remaining > 0) {
            reversed += (remaining and (BYTE_TERMINATOR - 1)).toByte()
            remaining = remaining ushr DATA_BITS
        }
        return reversed.reversed().toByteArray()
    }

    private fun readPool(cursor: Cursor, length: Int): PoolFill? {
        val result = mutableMapOf<Int, Int>()
        val nonReferences = mutableListOf<IntRange>()
        repeat(length) { index ->
            val tagPosition = cursor.position
            val bits = cursor.byte()
            nonReferences += tagPosition until cursor.position
            val type = bits and POOL_TYPE_MASK
            val behavior = bits ushr POOL_BEHAVIOR_SHIFT
            if (type > POOL_NATIVE_FUNCTION || behavior == POOL_NOT_SNAPSHOTABLE || behavior > POOL_MAX_BEHAVIOR) {
                throw InvalidSnapshot()
            }
            if (behavior == 0) {
                when (type) {
                    POOL_TAGGED_OBJECT -> {
                        val id = cursor.reference()
                        if (id !in 1..objects) throw InvalidSnapshot()
                        result[index * POOL_ENTRY_BYTES + POOL_HEADER_BYTES] = id
                    }
                    POOL_IMMEDIATE -> {
                        val valueStart = cursor.position
                        cursor.signed()
                        nonReferences += valueStart until cursor.position
                    }
                }
            }
        }
        return result.takeIf { it.isNotEmpty() }?.let { PoolFill(it, nonReferences) }
    }

    private fun findSnapshot(): Int {
        val sections = buffer.getLong(ELF_SECTION_TABLE).toInt()
        val sectionSize = bytes.readShort(ELF_SECTION_ENTRY_SIZE)
        val symbols = (0 until bytes.readShort(ELF_SECTION_COUNT)).map { sections + it * sectionSize }
            .single { bytes.readInt(it + ELF_SECTION_TYPE) == ELF_DYNAMIC_SYMBOLS }
        val namesSection = sections + bytes.readInt(symbols + ELF_SECTION_LINK) * sectionSize
        val names = buffer.getLong(namesSection + ELF_SECTION_OFFSET).toInt()
        val symbolsStart = buffer.getLong(symbols + ELF_SECTION_OFFSET).toInt()
        val symbolSize = buffer.getLong(symbols + ELF_SECTION_ITEM_SIZE).toInt()
        val symbolCount = (buffer.getLong(symbols + ELF_SECTION_SIZE) / symbolSize).toInt()
        val symbol = (0 until symbolCount).map { symbolsStart + it * symbolSize }.single { entry ->
            val name = names + bytes.readInt(entry)
            String(bytes, name, bytes.indexOf(0.toByte(), name) - name, Charsets.US_ASCII) == SNAPSHOT_SYMBOL
        }
        val address = buffer.getLong(symbol + ELF_SYMBOL_VALUE)
        val programs = buffer.getLong(ELF_PROGRAM_TABLE).toInt()
        val programSize = bytes.readShort(ELF_PROGRAM_ENTRY_SIZE)
        val segment = (0 until bytes.readShort(ELF_PROGRAM_COUNT)).map { programs + it * programSize }.single { entry ->
            bytes.readInt(entry) == ELF_LOAD && address >= buffer.getLong(entry + ELF_SEGMENT_ADDRESS) &&
                address < buffer.getLong(entry + ELF_SEGMENT_ADDRESS) + buffer.getLong(entry + ELF_SEGMENT_SIZE)
        }
        return (address - buffer.getLong(segment + ELF_SEGMENT_ADDRESS) + buffer.getLong(segment + ELF_SEGMENT_OFFSET)).toInt()
    }

    private fun positions(needle: ByteArray, from: Int, to: Int): List<Int> {
        val result = mutableListOf<Int>()
        val mask = ByteArray(needle.size) { -1 }
        var position = bytes.indexOfMasked(needle, mask, from)
        while (position >= 0 && position + needle.size <= to) {
            result += position
            position = bytes.indexOfMasked(needle, mask, position + 1)
        }
        return result
    }

    private fun stringBytes(encoded: Int): Int {
        if (encoded < 0 || encoded > limit) throw InvalidSnapshot()
        return (encoded ushr 1) * (if (encoded and 1 == 0) 1 else Char.SIZE_BYTES)
    }

    private fun encoded(value: Int, marker: Int = BYTE_TERMINATOR): ByteArray {
        var remaining = value
        val result = mutableListOf<Byte>()
        val maximum = if (marker == SIGNED_TERMINATOR) SIGNED_TERMINATOR - BYTE_TERMINATOR else BYTE_TERMINATOR
        while (remaining >= maximum) {
            result += (remaining and (BYTE_TERMINATOR - 1)).toByte()
            remaining = remaining ushr DATA_BITS
        }
        result += (remaining + marker).toByte()
        return result.toByteArray()
    }

    private fun <T> parseCandidate(parse: () -> T): T? = try {
        parse()
    } catch (_: InvalidSnapshot) {
        null
    }

    private inner class Cursor(var position: Int) {
        fun requireBytes(count: Int) {
            if (count < 0 || position < snapshot || position > limit - count) throw InvalidSnapshot()
        }

        fun advance(count: Int) {
            requireBytes(count)
            position += count
        }

        fun byte(): Int {
            requireBytes(1)
            return bytes[position++].toInt() and 0xff
        }

        fun unsigned() = integer(BYTE_TERMINATOR)
        fun signed() = integer(SIGNED_TERMINATOR)

        private fun integer(marker: Int): Long {
            var result = 0L
            var shift = 0
            while (true) {
                val byte = byte()
                if (byte >= BYTE_TERMINATOR) return result or ((byte - marker).toLong() shl shift)
                result = result or (byte.toLong() shl shift)
                shift += DATA_BITS
                if (shift > MAX_INTEGER_SHIFT) throw InvalidSnapshot()
            }
        }

        fun checkedReference(): Int = reference().also { if (it !in 1..objects) throw InvalidSnapshot() }

        fun reference(): Int {
            var result = 0
            repeat(MAX_REFERENCE_BYTES) {
                val value = byte().toByte().toInt()
                result = (result shl DATA_BITS) + value
                if (value < 0) return result + BYTE_TERMINATOR
            }
            throw InvalidSnapshot()
        }
    }

    private class SnapshotArray(
        val lengthPosition: Int,
        val referencesStart: Int,
        val referencesEnd: Int,
        val typeArguments: Int,
        val elements: IntArray,
    )

    private class PoolFill(val references: Map<Int, Int>, val nonReferenceRanges: List<IntRange>)

    private class CanonicalObjects(val arrays: Map<Int, SnapshotArray>, val names: Map<Int, String>)

    private class Allocation(
        val cid: Int,
        val canonical: Boolean,
        val firstId: Int,
        val count: Int,
        val fields: Int,
        val lengths: IntArray?,
        val lengthPositions: IntArray?,
    )

    private class InvalidSnapshot : RuntimeException()
}

private fun ByteArray.indexOf(value: Byte, from: Int): Int = (from until size).firstOrNull { this[it] == value } ?: -1
