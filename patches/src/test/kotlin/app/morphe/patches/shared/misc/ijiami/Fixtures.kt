/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import java.security.MessageDigest
import java.util.zip.Adler32

internal object Fixtures {
    const val CLASS = "Lsample/Fixture;"

    val substitution: ByteArray = ByteArray(256) { ((it * 167 + 61) and 0xFF).toByte() }
    val inverse: ByteArray = ByteArray(256).also { inv ->
        substitution.forEachIndexed { plain, cipher -> inv[cipher.toInt() and 0xFF] = plain.toByte() }
    }
    val tables = IjiamiSubstitution(substitution, inverse)
    val sampleDex: ByteArray = read("/ijiami/sample.dex")

    private const val OPAQUE_FILL = 0xAA.toByte()

    private fun encrypt(plain: ByteArray) = ByteArray(plain.size) { substitution[plain[it].toInt() and 0xFF] }

    fun payload(copies: Int = 3) = ByteArray(sampleDex.size * copies).also { payload ->
        repeat(copies) { sampleDex.copyInto(payload, it * sampleDex.size) }
    }

    fun recoverableDex(): ByteArray {
        val size = 0x11000
        val dex = ByteArray(size)
        byteArrayOf(0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00).copyInto(dex)
        dex.writeInt(36, 112)
        dex.writeInt(40, 0x12345678)
        dex.writeInt(56, 256)
        dex.writeInt(60, 112)
        for (i in 0 until 256) dex.writeInt(112 + 4 * i, 0x10000 + i)
        return seal(dex)
    }

    fun recoverablePayload(copies: Int = 5): ByteArray {
        val dex = recoverableDex()
        return ByteArray(dex.size * copies).also { p -> repeat(copies) { dex.copyInto(p, it * dex.size) } }
    }

    fun container(payload: ByteArray = payload(), opaque: Boolean = true): ByteArray {
        val header = ByteArray(56)
        "ijiami.dat".toByteArray(Charsets.US_ASCII).copyInto(header)
        header.writeInt(16, 4)
        header.writeInt(20, 40 + payload.size)
        "4929846d4eca7b0c17d37ce4a19ba333".toByteArray(Charsets.US_ASCII).copyInto(header, 24)

        val cipher = encrypt(payload)
        if (opaque) {
            cipher.fill(OPAQUE_FILL, 0, minOf(OPAQUE_BLOCK_SIZE, cipher.size))
            val lastBlock = (cipher.size - 1) / OPAQUE_BLOCK_SIZE * OPAQUE_BLOCK_SIZE
            if (lastBlock >= OPAQUE_BLOCK_SIZE) cipher.fill(OPAQUE_FILL, lastBlock, cipher.size)
        }

        return seal(sampleDex + header + cipher)
    }

    fun seal(dex: ByteArray) = dex.copyOf().apply {
        writeInt(32, size)
        MessageDigest.getInstance("SHA-1").digest(copyOfRange(32, size)).copyInto(this, 12)

        val checksum = Adler32()
        checksum.update(this, 12, size - 12)
        writeInt(8, checksum.value.toInt())
    }

    fun isSealed(dex: ByteArray) = seal(dex).contentEquals(dex)

    fun assertBytesEqual(expected: ByteArray, actual: ByteArray, label: String) {
        if (expected.size != actual.size) {
            throw AssertionError("$label: expected ${expected.size} bytes, got ${actual.size}")
        }

        val at = expected.indices.firstOrNull { expected[it] != actual[it] } ?: return
        throw AssertionError(
            "$label: first differs at $at, expected %02x but got %02x"
                .format(expected[at], actual[at]),
        )
    }

    private fun read(path: String) =
        checkNotNull(Fixtures::class.java.getResourceAsStream(path)) { "missing test resource $path" }
            .use { it.readBytes() }
}
