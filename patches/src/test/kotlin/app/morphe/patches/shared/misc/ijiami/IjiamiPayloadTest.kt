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
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class IjiamiPayloadTest {
    private fun payloadOf(bytes: ByteArray) =
        IjiamiPayload(PayloadDex.split(bytes), IjiamiContainer.of(Fixtures.container(bytes)).opaqueRanges)

    private fun firstInstructions(bytes: ByteArray, name: String, count: Int): ByteArray {
        val dex = PayloadDex.split(bytes.copyOf()).first()
        val body = dex.bodiesOf(Fixtures.CLASS, name).single()
        return bytes.copyOfRange(body.range.first + 16, body.range.first + 16 + count)
    }

    @Test
    fun `returns a constant in place`() {
        val bytes = Fixtures.payload(copies = 1)
        payloadOf(bytes).method(Fixtures.CLASS, "flag").returnEarly(true)

        assertContentEquals(byteArrayOf(0x12, 0x10, 0x0f, 0x00), firstInstructions(bytes, "flag", 4))
    }

    @Test
    fun `returns void in place`() {
        val bytes = Fixtures.payload(copies = 1)
        payloadOf(bytes).method(Fixtures.CLASS, "run").returnEarly()

        assertContentEquals(byteArrayOf(0x0e, 0x00), firstInstructions(bytes, "run", 2))
    }

    @Test
    fun `returns null in place`() {
        val bytes = Fixtures.payload(copies = 1)
        payloadOf(bytes).method(Fixtures.CLASS, "name").returnNull()

        assertContentEquals(byteArrayOf(0x12, 0x00, 0x11, 0x00), firstInstructions(bytes, "name", 4))
    }

    @Test
    fun `emits const-16`() {
        val bytes = Fixtures.payload(copies = 1)
        payloadOf(bytes).method(Fixtures.CLASS, "count").returnEarly(1234)

        assertContentEquals(
            byteArrayOf(0x13, 0x00, 0xd2.toByte(), 0x04, 0x0f, 0x00),
            firstInstructions(bytes, "count", 6),
        )
    }

    @Test
    fun `leaves the payload the same size`() {
        val bytes = Fixtures.payload(copies = 1)
        val before = bytes.size
        payloadOf(bytes).method(Fixtures.CLASS, "flag").returnEarly(false)

        assertTrue(bytes.size == before)
    }

    @Test
    fun `survives a container round trip`() {
        val bytes = Fixtures.payload()
        val container = IjiamiContainer.of(Fixtures.container(bytes), Fixtures.tables)

        val decrypted = container.decrypt()
        IjiamiPayload(PayloadDex.split(decrypted), container.opaqueRanges)
            .methods(Fixtures.CLASS, "flag")
            .returnEarly(true)

        val reread = IjiamiContainer.of(container.rebuild(decrypted), Fixtures.tables).decrypt()
        PayloadDex.split(reread)

        assertContentEquals(
            byteArrayOf(0x12, 0x10, 0x0f, 0x00),
            firstInstructions(reread, "flag", 4),
        )
    }

    @Test
    fun `refuses a body too short to hold the replacement`() {
        val bytes = Fixtures.payload(copies = 1)
        val body = PayloadDex.split(bytes.copyOf()).first().bodiesOf(Fixtures.CLASS, "tiny").single()
        bytes.writeInt(body.range.first + 12, 1)

        assertFailsWith<PatchException> {
            payloadOf(bytes).method(Fixtures.CLASS, "tiny").returnEarly(true)
        }
    }

    @Test
    fun `refuses a body with no register to return from`() {
        val bytes = Fixtures.payload(copies = 1)
        val body = PayloadDex.split(bytes.copyOf()).first().bodiesOf(Fixtures.CLASS, "flag").single()
        bytes[body.range.first] = 0
        bytes[body.range.first + 1] = 0

        assertFailsWith<PatchException> {
            payloadOf(bytes).method(Fixtures.CLASS, "flag").returnEarly(true)
        }
    }

    @Test
    fun `returns a full word constant in place`() {
        val bytes = Fixtures.payload(copies = 1)
        payloadOf(bytes).method(Fixtures.CLASS, "count").returnEarly(100000)

        assertContentEquals(
            byteArrayOf(0x14, 0x00, 0xa0.toByte(), 0x86.toByte(), 0x01, 0x00, 0x0f, 0x00),
            firstInstructions(bytes, "count", 8),
        )
    }

    @Test
    fun `returns a boxed integer in place`() {
        val bytes = Fixtures.payload(copies = 1)
        val method = PayloadDex.split(bytes.copyOf()).first()
            .methodIndexOf("Ljava/lang/Integer;", "valueOf", "LI")!!
        payloadOf(bytes).method(Fixtures.CLASS, "boxedCount").returnBoxed(1234)

        val written = firstInstructions(bytes, "boxedCount", 14)
        assertContentEquals(byteArrayOf(0x13, 0x00, 0xd2.toByte(), 0x04), written.copyOfRange(0, 4))
        assertContentEquals(
            byteArrayOf(0x71, 0x10, method.toByte(), (method shr 8).toByte(), 0x00, 0x00),
            written.copyOfRange(4, 10),
        )
        assertContentEquals(byteArrayOf(0x0c, 0x00, 0x11, 0x00), written.copyOfRange(10, 14))
    }

    @Test
    fun `refuses a constant too wide for the body it replaces`() {
        val bytes = Fixtures.payload(copies = 1)
        val body = PayloadDex.split(bytes.copyOf()).first().bodiesOf(Fixtures.CLASS, "count").single()
        bytes.writeInt(body.range.first + 12, 1)

        val message = assertFailsWith<PatchException> {
            payloadOf(bytes).method(Fixtures.CLASS, "count").returnEarly(100000)
        }.message
        assertTrue(message!!.contains("requires"), message)
    }

    @Test
    fun `refuses a boxed integer with no outgoing argument space`() {
        val bytes = Fixtures.payload(copies = 1)
        val body = PayloadDex.split(bytes.copyOf()).first().bodiesOf(Fixtures.CLASS, "boxedCount").single()
        bytes.writeInt(body.range.first + 4, 0)

        val message = assertFailsWith<PatchException> {
            payloadOf(bytes).method(Fixtures.CLASS, "boxedCount").returnBoxed(1)
        }.message
        assertTrue(message!!.contains("outgoing"), message)
    }

    @Test
    fun `returns a boxed boolean in place`() {
        val bytes = Fixtures.payload(copies = 1)
        val field = PayloadDex.split(bytes.copyOf()).first()
            .fieldIndexOf("Ljava/lang/Boolean;", "TRUE")!!
        payloadOf(bytes).method(Fixtures.CLASS, "boxed").returnBoxed(true)

        val written = firstInstructions(bytes, "boxed", 6)
        assertContentEquals(
            byteArrayOf(0x62, 0x00, field.toByte(), (field shr 8).toByte()),
            written.copyOfRange(0, 4),
        )
        assertContentEquals(byteArrayOf(0x11, 0x00), written.copyOfRange(4, 6))
    }

    @Test
    fun `names a method the app does not declare`() {
        val message = assertFailsWith<PatchException> {
            payloadOf(Fixtures.payload(copies = 1)).method(Fixtures.CLASS, "absent")
        }.message

        assertTrue(message!!.contains("absent"), message)
    }

    @Test
    fun `reports a method the app does not declare`() {
        assertNull(payloadOf(Fixtures.payload(copies = 1)).methodOrNull(Fixtures.CLASS, "absent"))
    }

    @Test
    fun `refuses an ambiguous method name`() {
        val message = assertFailsWith<PatchException> {
            payloadOf(Fixtures.payload(copies = 1)).method(Fixtures.CLASS, "over")
        }.message

        assertTrue(message!!.contains("overload"), message)
    }

    @Test
    fun `reports an ambiguous method name as an error not a null`() {
        assertFailsWith<PatchException> {
            payloadOf(Fixtures.payload(copies = 1)).methodOrNull(Fixtures.CLASS, "over")
        }
    }

    @Test
    fun `edits every overload of a name`() {
        val bytes = Fixtures.payload(copies = 1)
        payloadOf(bytes).methods(Fixtures.CLASS, "over").returnEarly(true)

        val bodies = PayloadDex.split(bytes.copyOf()).first().bodiesOf(Fixtures.CLASS, "over")
        assertEquals(2, bodies.size)
        bodies.forEach {
            assertContentEquals(
                byteArrayOf(0x12, 0x10, 0x0f, 0x00),
                bytes.copyOfRange(it.range.first + 16, it.range.first + 20),
            )
        }
    }

    @Test
    fun `refuses a return type it cannot produce`() {
        val payload = payloadOf(Fixtures.payload(copies = 1))

        assertFailsWith<PatchException> { payload.method(Fixtures.CLASS, "flag").returnEarly() }
        assertFailsWith<PatchException> { payload.method(Fixtures.CLASS, "run").returnEarly(true) }
        assertFailsWith<PatchException> { payload.method(Fixtures.CLASS, "count").returnNull() }
        assertFailsWith<PatchException> { payload.method(Fixtures.CLASS, "flag").returnBoxed(true) }
    }
}
