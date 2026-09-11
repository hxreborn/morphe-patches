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

internal class IjiamiContainerTest {
    @Test
    fun `substitution table is a permutation`() {
        val table = Fixtures.substitution
        assertEquals(256, table.size)
        assertEquals(256, table.toSet().size)
    }

    @Test
    fun `reads a container back byte for byte`() {
        val original = Fixtures.container()
        val container = IjiamiContainer.of(original, Fixtures.tables)

        Fixtures.assertBytesEqual(original, container.rebuild(container.decrypt()), "round trip")
    }

    @Test
    fun `keeps the blocks it cannot decrypt`() {
        val original = Fixtures.container()
        val container = IjiamiContainer.of(original, Fixtures.tables)

        val payload = container.decrypt()
        container.opaqueRanges.forEach { range ->
            payload.fill(0, range.first, range.last + 1)
        }

        Fixtures.assertBytesEqual(original, container.rebuild(payload), "opaque blocks")
    }

    @Test
    fun `covers the first and last block`() {
        val payload = Fixtures.payload(copies = 2)
        val container = IjiamiContainer.of(Fixtures.container(payload))

        assertEquals(
            listOf(0 until 1024, payload.size / 1024 * 1024 until payload.size),
            container.opaqueRanges,
        )
    }

    @Test
    fun `covers a whole last block when the payload divides evenly`() {
        val payload = ByteArray(4 * 1024)
        val container = IjiamiContainer.of(Fixtures.container(payload, opaque = false))

        assertEquals(listOf(0 until 1024, 3072 until 4096), container.opaqueRanges)
    }

    @Test
    fun `covers only the first block when the payload is smaller than one`() {
        val container = IjiamiContainer.of(Fixtures.container(ByteArray(500), opaque = false))

        assertEquals(listOf(0 until 500), container.opaqueRanges)
    }

    @Test
    fun `seals the whole container`() {
        val container = IjiamiContainer.of(Fixtures.container(), Fixtures.tables)
        val rebuilt = container.rebuild(container.decrypt())

        assertTrue(Fixtures.isSealed(rebuilt), "the rebuilt container does not verify its own digests")
        assertEquals(rebuilt.size, rebuilt.readInt(32))
    }

    @Test
    fun `refuses a payload that changed size`() {
        val container = IjiamiContainer.of(Fixtures.container(), Fixtures.tables)

        val message = assertFailsWith<PatchException> {
            container.rebuild(container.decrypt() + 0)
        }.message

        assertTrue(message!!.contains("in place"), message)
    }

    @Test
    fun `rejects an app that is not packed`() {
        assertFailsWith<PatchException> { IjiamiContainer.of(Fixtures.sampleDex) }
    }

    @Test
    fun `rejects something that is not a dex file`() {
        assertFailsWith<PatchException> { IjiamiContainer.of(ByteArray(4096)) }
    }

    @Test
    fun `rejects a container whose declared length disagrees`() {
        val broken = Fixtures.container().also {
            it.writeInt(Fixtures.sampleDex.size + 20, 12345)
        }

        assertFailsWith<PatchException> { IjiamiContainer.of(broken) }
    }

    @Test
    fun `rejects a map offset that overflows when adding its size`() {
        val broken = Fixtures.container().also { it.writeInt(52, Int.MAX_VALUE) }

        assertFailsWith<PatchException> { IjiamiContainer.of(broken) }
    }
}
