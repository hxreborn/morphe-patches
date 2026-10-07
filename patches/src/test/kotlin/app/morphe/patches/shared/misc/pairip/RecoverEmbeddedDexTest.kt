/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pairip

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class RecoverEmbeddedDexTest {
    private val assetMagic = byteArrayOf(0x00, 0x49, 0x41, 0x50, 0x02)

    private fun dex(size: Int, declaredSize: Int = size): ByteArray {
        val bytes = ByteArray(size)
        byteArrayOf(0x64, 0x65, 0x78, 0x0a).copyInto(bytes)
        for (shift in 0..3) bytes[0x20 + shift] = (declaredSize shr (8 * shift)).toByte()
        return bytes
    }

    private fun asset(vararg parts: ByteArray) = parts.fold(assetMagic) { total, part -> total + part }

    @Test
    fun `recovers the dex behind the asset header`() {
        val embedded = dex(0x200)

        assertContentEquals(embedded, recoverEmbeddedDex(asset(ByteArray(40) { 0x7F }, embedded)))
    }

    @Test
    fun `stops at the declared size when bytes trail the dex`() {
        val embedded = dex(0x200)

        assertContentEquals(embedded, recoverEmbeddedDex(asset(embedded, ByteArray(64) { 0x11 })))
    }

    @Test
    fun `skips an asset without the pairip header`() {
        assertNull(recoverEmbeddedDex(dex(0x200)))
    }

    @Test
    fun `skips a pairip asset that carries no dex`() {
        assertNull(recoverEmbeddedDex(asset(ByteArray(200) { 0x7F })))
    }

    @Test
    fun `rejects a truncated dex header`() {
        assertFailsWith<IllegalStateException> { recoverEmbeddedDex(asset(dex(0x40))) }
    }

    @Test
    fun `rejects a declared size that runs past the asset`() {
        assertFailsWith<IllegalStateException> { recoverEmbeddedDex(asset(dex(0x200, declaredSize = 0x400))) }
    }

    @Test
    fun `rejects a declared size smaller than the header`() {
        assertFailsWith<IllegalStateException> { recoverEmbeddedDex(asset(dex(0x200, declaredSize = 0x10))) }
    }

    @Test
    fun `rejects a negative declared size`() {
        assertFailsWith<IllegalStateException> { recoverEmbeddedDex(asset(dex(0x200, declaredSize = -1))) }
    }
}
