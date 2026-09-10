/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class PayloadDexTest {
    @Test
    fun `finds every dex in the payload`() {
        val payload = Fixtures.payload(copies = 4)
        val dexes = PayloadDex.split(payload.copyOf())

        assertEquals(4, dexes.size)
        assertEquals(List(4) { it * Fixtures.sampleDex.size }, dexes.map { it.start })
        assertTrue(dexes.all { it.size == Fixtures.sampleDex.size })
    }

    @Test
    fun `recovers the head the packer encrypts twice`() {
        val payload = Fixtures.payload()
        val opaque = payload.copyOf().also { it.fill(0x5A, 0, OPAQUE_BLOCK_SIZE) }

        PayloadDex.split(opaque)

        assertContentEquals(
            payload.copyOfRange(0, OPAQUE_BLOCK_SIZE),
            opaque.copyOfRange(0, OPAQUE_BLOCK_SIZE),
        )
    }

    @Test
    fun `recovers the first block of a synthetic container`() {
        val payload = Fixtures.payload()
        val container = IjiamiContainer.of(Fixtures.container(payload), Fixtures.tables)

        val decrypted = container.decrypt()
        PayloadDex.split(decrypted)

        assertContentEquals(
            payload.copyOfRange(0, OPAQUE_BLOCK_SIZE),
            decrypted.copyOfRange(0, OPAQUE_BLOCK_SIZE),
        )
    }

    @Test
    fun `reads a payload holding a single dex`() {
        val dexes = PayloadDex.split(Fixtures.payload(copies = 1))

        assertEquals(1, dexes.size)
        assertEquals(Fixtures.sampleDex.size, dexes.single().size)
    }

    @Test
    fun `refuses a payload the substitution did not resolve`() {
        val message = assertFailsWith<PatchException> {
            PayloadDex.split(ByteArray(8192) { 0x33 })
        }.message

        assertTrue(message!!.contains("substitution"), message)
    }

    @Test
    fun `refuses a dex that runs past the payload`() {
        val payload = Fixtures.payload(copies = 2)
        payload.writeInt(Fixtures.sampleDex.size + 32, Fixtures.sampleDex.size * 4)

        assertFailsWith<PatchException> { PayloadDex.split(payload) }
    }

    @Test
    fun `refuses a dex size that overflows its end offset`() {
        val payload = Fixtures.payload(copies = 2)
        payload.writeInt(Fixtures.sampleDex.size + 32, Int.MAX_VALUE)

        assertFailsWith<PatchException> { PayloadDex.split(payload) }
    }

    @Test
    fun `keeps the trailer the packer stores after the last dex`() {
        val trailer = 84
        val payload = Fixtures.payload(copies = 2) + ByteArray(trailer) { 0x11 }
        val dexes = PayloadDex.split(payload)

        assertEquals(2, dexes.size)
        assertEquals(Fixtures.sampleDex.size, dexes.last().size)
        assertEquals(trailer, payload.size - (dexes.last().start + dexes.last().size))
    }

    @Test
    fun `refuses a dex that ends before the next one starts`() {
        val payload = Fixtures.payload(copies = 3)
        payload.writeInt(Fixtures.sampleDex.size + 32, Fixtures.sampleDex.size - 16)

        assertFailsWith<PatchException> { PayloadDex.split(payload) }
    }

    @Test
    fun `refuses a class table offset that runs off the payload`() {
        val bytes = Fixtures.payload(copies = 2)
        bytes.writeInt(Fixtures.sampleDex.size + 96 + 4, 0x7000_0000)

        val message = assertFailsWith<PatchException> { PayloadDex.split(bytes) }.message
        assertTrue(message!!.contains("class_defs"), message)
    }

    @Test
    fun `refuses a class table count that overruns the dex`() {
        val bytes = Fixtures.payload(copies = 2)
        bytes.writeInt(Fixtures.sampleDex.size + 96, 0x1000_0000)

        val message = assertFailsWith<PatchException> { PayloadDex.split(bytes) }.message
        assertTrue(message!!.contains("class_defs"), message)
    }

    @Test
    fun `refuses a map list count that overruns the dex`() {
        val bytes = Fixtures.payload(copies = 2)
        bytes.writeInt(Fixtures.sampleDex.readInt(52), 0x1000_0000)

        val message = assertFailsWith<PatchException> { PayloadDex.split(bytes) }.message
        assertTrue(message!!.contains("out of range"), message)
    }

    @Test
    fun `refuses to recover a head that disagrees past the block`() {
        val payload = Fixtures.payload()
        payload.writeInt(OPAQUE_BLOCK_SIZE + 64, 0x41414141)

        val message = assertFailsWith<PatchException> { PayloadDex.split(payload) }.message

        assertTrue(message!!.contains("past the block"), message)
    }

    @Test
    fun `refuses a class table that starts at the dex boundary`() {
        val bytes = Fixtures.payload(copies = 3)
        bytes.writeInt(Fixtures.sampleDex.size + 96 + 4, Fixtures.sampleDex.size)

        val message = assertFailsWith<PatchException> { PayloadDex.split(bytes) }.message
        assertTrue(message!!.contains("out-of-range"), message)
    }

    @Test
    fun `refuses a code item whose instructions run past the dex`() {
        val bytes = Fixtures.payload(copies = 1)
        val body = PayloadDex.split(bytes.copyOf()).first().bodiesOf(Fixtures.CLASS, "flag").single()
        bytes.writeInt(body.range.first + 12, 0x1000_0000)

        val message = assertFailsWith<PatchException> {
            PayloadDex.split(bytes.copyOf()).first().bodiesOf(Fixtures.CLASS, "flag")
        }.message
        assertTrue(message!!.contains("code item"), message)
    }

    @Test
    fun `refuses a name with a truncated multi-byte sequence`() {
        val bytes = Fixtures.payload(copies = 1)
        bytes[stringData(10)] = 0xC2.toByte()

        val message = assertFailsWith<PatchException> {
            PayloadDex.split(bytes).first().bodiesOf(Fixtures.CLASS, "flag")
        }.message
        assertTrue(message!!.contains("MUTF-8"), message)
    }

    @Test
    fun `refuses a name whose declared length disagrees with its bytes`() {
        val bytes = Fixtures.payload(copies = 1)
        bytes[stringHeader(10)] = 0x0F

        val message = assertFailsWith<PatchException> {
            PayloadDex.split(bytes).first().bodiesOf(Fixtures.CLASS, "flag")
        }.message
        assertTrue(message!!.contains("declares"), message)
    }

    @Test
    fun `refuses a name whose length overflows 32 bits`() {
        val bytes = Fixtures.payload(copies = 1)
        byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x10)
            .copyInto(bytes, stringHeader(10))

        val message = assertFailsWith<PatchException> {
            PayloadDex.split(bytes).first().bodiesOf(Fixtures.CLASS, "flag")
        }.message
        assertTrue(message!!.contains("LEB128"), message)
    }

    @Test
    fun `decodes a supplementary character name`() {
        val bytes = Fixtures.payload(copies = 1)
        bytes[stringHeader(0)] = 0x02
        byteArrayOf(0xED.toByte(), 0xA0.toByte(), 0xBD.toByte(), 0xED.toByte(), 0xB8.toByte(), 0x80.toByte())
            .copyInto(bytes, stringData(0))

        val bodies = PayloadDex.split(bytes).first().bodiesOf(Fixtures.CLASS, "😀")
        assertEquals(1, bodies.size)
    }

    @Test
    fun `refuses a class_data count larger than its table`() {
        val bytes = Fixtures.payload(copies = 1)
        bytes[fixtureClassData()] = 0xFF.toByte()
        bytes[fixtureClassData() + 1] = 0x7F

        val message = assertFailsWith<PatchException> {
            PayloadDex.split(bytes).first().bodiesOf(Fixtures.CLASS, "flag")
        }.message
        assertTrue(message!!.contains("class_data"), message)
    }

    @Test
    fun `refuses a method index delta that wraps past the table`() {
        val bytes = Fixtures.payload(copies = 1)
        byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x0F)
            .copyInto(bytes, lastMethodDiff())

        val message = assertFailsWith<PatchException> {
            PayloadDex.split(bytes).first().bodiesOf(Fixtures.CLASS, "tiny")
        }.message
        assertTrue(message!!.contains("method index"), message)
    }

    private fun lastMethodDiff(): Int {
        var cursor = fixtureClassData()
        val counts = IntArray(4)
        for (section in counts.indices) {
            val (count, next) = fixtureUleb(cursor)
            counts[section] = count
            cursor = next
        }
        repeat(counts[0] + counts[1]) { cursor = fixtureUleb(fixtureUleb(cursor).second).second }

        var diff = cursor
        repeat(counts[2] + counts[3]) {
            diff = cursor
            cursor = fixtureUleb(fixtureUleb(fixtureUleb(cursor).second).second).second
        }
        return diff
    }

    private fun fixtureUleb(offset: Int): Pair<Int, Int> {
        var value = 0
        var shift = 0
        var cursor = offset
        while (true) {
            val byte = Fixtures.sampleDex[cursor++].toInt()
            value = value or ((byte and 0x7F) shl shift)
            if (byte and 0x80 == 0) return value to cursor
            shift += 7
        }
    }

    private fun stringHeader(index: Int) =
        Fixtures.sampleDex.readInt(Fixtures.sampleDex.readInt(60) + 4 * index)

    private fun stringData(index: Int): Int {
        var offset = stringHeader(index)
        while (Fixtures.sampleDex[offset].toInt() and 0x80 != 0) offset++
        return offset + 1
    }

    private fun fixtureClassData(): Int {
        val dex = Fixtures.sampleDex
        val typeIds = dex.readInt(68)
        val classDefs = dex.readInt(100)
        for (index in 0 until dex.readInt(96)) {
            val base = classDefs + 32 * index
            val name = readAsciiString(dex.readInt(typeIds + 4 * dex.readInt(base)))
            if (name == Fixtures.CLASS) return dex.readInt(base + 24)
        }
        error("Fixture class_data not found")
    }

    private fun readAsciiString(index: Int): String {
        val chars = StringBuilder()
        var offset = stringData(index)
        while (Fixtures.sampleDex[offset] != 0.toByte()) {
            chars.append((Fixtures.sampleDex[offset].toInt() and 0xFF).toChar())
            offset++
        }
        return chars.toString()
    }

    @Test
    fun `refuses a string section that overruns the dex`() {
        val bytes = Fixtures.payload()
        val mapOff = Fixtures.sampleDex.readInt(52)
        val entries = Fixtures.sampleDex.readInt(mapOff)
        val stringIds = (0 until entries).map { mapOff + 4 + it * 12 }
            .first { Fixtures.sampleDex.readShort(it) == 0x0001 }
        bytes.writeInt(stringIds + 8, 0x7000_0000)

        val message = assertFailsWith<PatchException> { PayloadDex.split(bytes) }.message
        assertTrue(message!!.contains("String section"), message)
    }
}
