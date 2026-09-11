/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException
import java.security.MessageDigest
import java.util.zip.Adler32

private const val CHECKSUM_OFFSET = 8
private const val SIGNATURE_OFFSET = 12
private const val FILE_SIZE_OFFSET = 32
private const val MAP_OFF_OFFSET = 52
private const val MAP_ENTRY_SIZE = 12
private const val DEX_HEADER_SIZE = 112

private const val CONTAINER_HEADER_SIZE = 56
private const val CONTAINER_LENGTH_OFFSET = 20
private const val CONTAINER_FIELDS_SIZE = 8
private const val CONTAINER_DIGEST_SIZE = 32

internal val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0a)
private val CONTAINER_NAME = "ijiami.dat".toByteArray(Charsets.US_ASCII)

// Second cipher on the first and last payload blocks
internal const val OPAQUE_BLOCK_SIZE = 1024

internal fun ByteArray.readInt(offset: Int) =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)

internal fun ByteArray.writeInt(offset: Int, value: Int) {
    this[offset] = value.toByte()
    this[offset + 1] = (value ushr 8).toByte()
    this[offset + 2] = (value ushr 16).toByte()
    this[offset + 3] = (value ushr 24).toByte()
}

internal fun ByteArray.readShort(offset: Int) =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

internal fun ByteArray.sealDex(size: Int = this.size) = sealDexAt(0, size)

internal fun ByteArray.sealDexAt(start: Int, size: Int) = apply {
    writeInt(start + FILE_SIZE_OFFSET, size)
    MessageDigest.getInstance("SHA-1")
        .digest(copyOfRange(start + FILE_SIZE_OFFSET, start + size))
        .copyInto(this, start + SIGNATURE_OFFSET)

    val checksum = Adler32()
    checksum.update(this, start + SIGNATURE_OFFSET, size - SIGNATURE_OFFSET)
    writeInt(start + CHECKSUM_OFFSET, checksum.value.toInt())
}

private fun ByteArray.startsWith(prefix: ByteArray) =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

internal class IjiamiContainer private constructor(
    private val stub: ByteArray,
    private val header: ByteArray,
    private val cipherText: ByteArray,
    tablesOverride: IjiamiSubstitution?,
) {
    private val tables: IjiamiSubstitution by lazy { tablesOverride ?: SubstitutionRecovery.recover(cipherText) }

    val opaqueRanges: List<IntRange> = buildList {
        add(0 until minOf(OPAQUE_BLOCK_SIZE, cipherText.size))

        val lastBlock = (cipherText.size - 1) / OPAQUE_BLOCK_SIZE * OPAQUE_BLOCK_SIZE
        if (lastBlock >= OPAQUE_BLOCK_SIZE) add(lastBlock until cipherText.size)
    }

    fun decrypt() = ByteArray(cipherText.size) { tables.inverse[cipherText[it].toInt() and 0xFF] }

    fun rebuild(payload: ByteArray): ByteArray {
        if (payload.size != cipherText.size) {
            throw PatchException(
                "Payload size changed: expected ${cipherText.size} bytes, got ${payload.size}. " +
                    "Only edits in place are supported.",
            )
        }

        val encrypted = ByteArray(payload.size) { tables.substitution[payload[it].toInt() and 0xFF] }
        opaqueRanges.forEach { range ->
            cipherText.copyInto(encrypted, range.first, range.first, range.last + 1)
        }

        return (stub + header + encrypted).sealDex()
    }

    companion object {
        fun of(classesDex: ByteArray): IjiamiContainer = of(classesDex, null)

        internal fun of(classesDex: ByteArray, tables: IjiamiSubstitution?): IjiamiContainer {
            if (classesDex.size < DEX_HEADER_SIZE || !classesDex.startsWith(DEX_MAGIC)) {
                throw PatchException("classes.dex is not a dex file")
            }

            val mapOffset = classesDex.readInt(MAP_OFF_OFFSET)
            if (mapOffset <= 0 || mapOffset > classesDex.size - 4) {
                throw PatchException("classes.dex has no readable map section")
            }

            val entries = classesDex.readInt(mapOffset)
            val stubSize = mapOffset + 4 + entries.toLong() * MAP_ENTRY_SIZE
            if (entries < 0 || stubSize + CONTAINER_HEADER_SIZE >= classesDex.size) {
                throw PatchException("Ijiami container not found in classes.dex")
            }

            val stubEnd = stubSize.toInt()
            val header = classesDex.copyOfRange(stubEnd, stubEnd + CONTAINER_HEADER_SIZE)
            if (!header.startsWith(CONTAINER_NAME)) {
                throw PatchException("Ijiami container not found in classes.dex")
            }

            val cipherText = classesDex.copyOfRange(stubEnd + CONTAINER_HEADER_SIZE, classesDex.size)
            val declared = header.readInt(CONTAINER_LENGTH_OFFSET)
            val expected = CONTAINER_FIELDS_SIZE + CONTAINER_DIGEST_SIZE + cipherText.size
            if (declared != expected) {
                throw PatchException(
                    "Ijiami container length mismatch: declared $declared bytes, expected $expected",
                )
            }

            return IjiamiContainer(classesDex.copyOfRange(0, stubEnd), header, cipherText, tables)
        }
    }
}
