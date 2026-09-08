/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.jiagu

import app.morphe.patcher.patch.PatchException
import java.io.ByteArrayOutputStream
import java.util.zip.Adler32

private const val CHECKSUM_OFFSET = 8
private const val FILE_SIZE_OFFSET = 32
private const val MAP_OFF_OFFSET = 52
private const val MAP_ENTRY_SIZE = 12
private const val PART_HEADER_SIZE = 12

private val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0a)
private val SUPPORTED_PAYLOAD_MAGIC = byteArrayOf(0x71, 0x68, 0x00, 0x01)

private fun ByteArray.readInt(offset: Int) =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)

private fun ByteArray.writeInt(offset: Int, value: Int) {
    this[offset] = value.toByte()
    this[offset + 1] = (value ushr 8).toByte()
    this[offset + 2] = (value ushr 16).toByte()
    this[offset + 3] = (value ushr 24).toByte()
}

private fun ByteArray.sealDex() = apply {
    writeInt(FILE_SIZE_OFFSET, size)
    writeInt(CHECKSUM_OFFSET, Adler32().apply { update(this@sealDex, 12, size - 12) }.value.toInt())
}

internal class JiaguPart(private val header: ByteArray, val cipherText: ByteArray) {
    val bytes get() = header + cipherText

    fun withCipherText(replacement: ByteArray) =
        JiaguPart(
            header.copyOf().apply {
                writeInt(0, replacement.size + 8)
                writeInt(4, replacement.size + 4)
            },
            replacement,
        )
}

internal class JiaguDex(private val bytes: ByteArray) {
    val stubSize = stubSizeOf(bytes)

    val stub: ByteArray get() = bytes.copyOfRange(0, stubSize)

    private val payload by lazy { bytes.copyOfRange(stubSize, bytes.size) }

    private val partsOffset: Int
        get() {
            val magic = payload.copyOf(SUPPORTED_PAYLOAD_MAGIC.size)
            if (!magic.contentEquals(SUPPORTED_PAYLOAD_MAGIC)) {
                throw PatchException(
                    "The jiagu payload starts with ${magic.joinToString("") { "%02x".format(it) }}, " +
                        "not the ${SUPPORTED_PAYLOAD_MAGIC.joinToString("") { "%02x".format(it) }} " +
                        "layout this reads",
                )
            }
            return PART_HEADER_SIZE + payload.readInt(8)
        }

    fun parts(): List<JiaguPart> {
        var offset = partsOffset
        val count = payload.readInt(offset)
        offset += 4

        return List(count) {
            val length = payload.readInt(offset)
            if (length < 8 || offset + 4 + length > payload.size) {
                throw PatchException("The jiagu payload ends inside a part")
            }

            JiaguPart(
                payload.copyOfRange(offset, offset + PART_HEADER_SIZE),
                payload.copyOfRange(offset + PART_HEADER_SIZE, offset + 4 + length),
            ).also { offset += 4 + length }
        }
    }

    fun withParts(parts: List<JiaguPart>): ByteArray {
        val head = partsOffset + 4
        if (parts.size != payload.readInt(head - 4)) {
            throw PatchException("The rebuilt payload does not carry the original part count")
        }

        val rebuilt = ByteArrayOutputStream().apply {
            write(payload, 0, head)
            parts.forEach { write(it.bytes) }
        }.toByteArray()
        rebuilt.writeInt(4, rebuilt.size - 8)

        return (stub + rebuilt).sealDex()
    }

    fun withStub(replacement: ByteArray): ByteArray {
        if (replacement.size != stubSize) {
            throw PatchException(
                "The replacement stub is ${replacement.size} bytes, but the packer reads its " +
                    "payload at $stubSize",
            )
        }

        if (stubSizeOf(replacement) != stubSize) {
            throw PatchException("The replacement stub does not end where the payload begins")
        }

        if (mapEntryCount(replacement) != mapEntryCount(bytes)) {
            throw PatchException("The replacement stub does not carry the original map sections")
        }

        return (replacement + payload).sealDex()
    }

    private companion object {
        fun mapEntryCount(dex: ByteArray) = dex.readInt(dex.readInt(MAP_OFF_OFFSET))

        fun stubSizeOf(dex: ByteArray): Int {
            if (dex.size < 112 || !dex.copyOf(DEX_MAGIC.size).contentEquals(DEX_MAGIC)) {
                throw PatchException("classes.dex is not a dex file")
            }

            val mapOffset = dex.readInt(MAP_OFF_OFFSET)
            if (mapOffset <= 0 || mapOffset >= dex.size) {
                throw PatchException("classes.dex has no readable map section")
            }

            return mapOffset + 4 + mapEntryCount(dex) * MAP_ENTRY_SIZE
        }
    }
}

internal fun ByteArray.asJiaguDex(): JiaguDex {
    val dex = JiaguDex(this)

    if (dex.stubSize >= size) {
        throw PatchException("The app being patched is not packed with jiagu")
    }

    return dex
}
