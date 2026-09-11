/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException
import java.security.MessageDigest
import java.util.zip.Adler32

private const val DEX_HEADER_SIZE = 112
private const val MAGIC_NULL_OFFSET = 7
private const val CHECKSUM_OFFSET = 8
private const val SIGNATURE_OFFSET = 12
private const val SIGNATURE_END = 32
private const val FILE_SIZE_OFFSET = 32
private const val HEADER_SIZE_OFFSET = 36
private const val ENDIAN_OFFSET = 40
private const val STRING_IDS_START = 112

private const val MIN_BOUNDARIES = 3
private const val MAX_BOUNDARIES = 64

internal class IjiamiSubstitution(val substitution: ByteArray, val inverse: ByteArray)

internal object SubstitutionRecovery {
    fun recover(cipherText: ByteArray): IjiamiSubstitution {
        val n = cipherText.size
        val opaque = opaqueRanges(n)
        val zero = mostFrequentByte(cipherText)
        val candidates = candidateBoundaries(cipherText, zero, opaque, n)
        if (candidates.isEmpty()) {
            throw PatchException("No candidate dex headers found in the Ijiami payload")
        }

        var solved = false
        for (starts in candidates) {
            val inverse = solveOrNull(cipherText, starts, zero, n) ?: continue
            solved = true
            if (!verifies(cipherText, starts, inverse, opaque)) continue
            val substitution = ByteArray(256)
            for (cipher in 0..255) substitution[inverse[cipher].toInt() and 0xFF] = cipher.toByte()
            return IjiamiSubstitution(substitution, inverse)
        }
        throw PatchException(
            if (solved) "Ijiami substitution rejected: payload checksum or signature mismatch"
            else "Ijiami substitution ordering is not unique",
        )
    }

    private fun opaqueRanges(n: Int): List<IntRange> = buildList {
        add(0 until minOf(OPAQUE_BLOCK_SIZE, n))
        val lastBlock = (n - 1) / OPAQUE_BLOCK_SIZE * OPAQUE_BLOCK_SIZE
        if (lastBlock >= OPAQUE_BLOCK_SIZE) add(lastBlock until n)
    }

    private fun List<IntRange>.overlap(start: Int, length: Int) =
        any { start < it.last + 1 && start + length > it.first }

    private fun mostFrequentByte(bytes: ByteArray): Int {
        val counts = IntArray(256)
        for (b in bytes) counts[b.toInt() and 0xFF]++
        var best = 0
        for (v in 1..255) if (counts[v] > counts[best]) best = v
        return best
    }

    private fun distinct4(bytes: ByteArray, at: Int): Boolean {
        val a = bytes[at]
        val b = bytes[at + 1]
        val c = bytes[at + 2]
        val d = bytes[at + 3]
        return a != b && a != c && a != d && b != c && b != d && c != d
    }

    private fun key8(bytes: ByteArray, at: Int): Long {
        var k = 0L
        for (i in 0 until 8) k = (k shl 8) or (bytes[at + i].toLong() and 0xFF)
        return k
    }

    private fun candidateBoundaries(cipher: ByteArray, zero: Int, opaque: List<IntRange>, n: Int): List<IntArray> {
        val z = zero.toByte()
        val groups = HashMap<Long, MutableList<Int>>()
        var i = OPAQUE_BLOCK_SIZE
        val limit = n - DEX_HEADER_SIZE
        while (i <= limit) {
            if (cipher[i + 37] == z && cipher[i + 38] == z && cipher[i + 39] == z &&
                cipher[i + MAGIC_NULL_OFFSET] == z && cipher[i + HEADER_SIZE_OFFSET] != z &&
                distinct4(cipher, i) && distinct4(cipher, i + ENDIAN_OFFSET) &&
                !opaque.overlap(i, DEX_HEADER_SIZE)
            ) {
                groups.getOrPut(key8(cipher, i + HEADER_SIZE_OFFSET)) { mutableListOf() }.add(i)
            }
            i++
        }

        return groups.values.mapNotNull { positions ->
            if (positions.size < MIN_BOUNDARIES || positions.size > MAX_BOUNDARIES) return@mapNotNull null
            positions.sort()
            if (positions.any { !sameMagic(cipher, it, positions[0]) }) return@mapNotNull null
            if (!tiles(cipher, positions)) return@mapNotNull null
            positions.toIntArray()
        }.sortedByDescending { it.last() - it.first() }
    }

    private fun sameMagic(cipher: ByteArray, a: Int, b: Int) =
        cipher[a] == cipher[b] && cipher[a + 1] == cipher[b + 1] &&
            cipher[a + 2] == cipher[b + 2] && cipher[a + 3] == cipher[b + 3]

    private fun tiles(cipher: ByteArray, starts: List<Int>): Boolean {
        val map = HashMap<Int, Int>()
        for (i in 0 until starts.size - 1) {
            val gap = starts[i + 1] - starts[i]
            for (k in 0 until 4) {
                val cipherByte = cipher[starts[i] + FILE_SIZE_OFFSET + k].toInt() and 0xFF
                val plainByte = (gap ushr (8 * k)) and 0xFF
                val prev = map.putIfAbsent(cipherByte, plainByte)
                if (prev != null && prev != plainByte) return false
            }
        }
        return true
    }

    private fun solveOrNull(cipher: ByteArray, starts: IntArray, zero: Int, n: Int): ByteArray? {
        val z = zero.toByte()
        val adjacency = Array(256) { BooleanArray(256) }
        val present = BooleanArray(256)
        for (index in starts.indices) {
            val cap = if (index + 1 < starts.size) starts[index + 1] else n
            var p = starts[index] + STRING_IDS_START
            var prev = -1
            while (p + 4 <= cap) {
                if (cipher[p + 3] != z || cipher[p + 2] == z) break
                if (prev >= 0) {
                    var x = -1
                    var y = -1
                    for (col in 2 downTo 0) {
                        if (cipher[prev + col] != cipher[p + col]) {
                            x = cipher[prev + col].toInt() and 0xFF
                            y = cipher[p + col].toInt() and 0xFF
                            break
                        }
                    }
                    if (x >= 0) {
                        adjacency[x][y] = true
                        present[x] = true
                        present[y] = true
                    }
                }
                prev = p
                p += 4
            }
        }

        val indegree = IntArray(256)
        for (x in 0..255) for (y in 0..255) if (adjacency[x][y]) indegree[y]++
        val done = BooleanArray(256)
        val inverse = ByteArray(256)
        for (rank in 0..255) {
            var ready = -1
            var count = 0
            for (v in 0..255) {
                if (present[v] && !done[v] && indegree[v] == 0) {
                    ready = v
                    if (++count > 1) break
                }
            }
            if (count != 1) return null
            inverse[ready] = rank.toByte()
            done[ready] = true
            for (w in 0..255) if (adjacency[ready][w] && !done[w]) indegree[w]--
        }
        return inverse
    }

    private fun verifies(cipher: ByteArray, starts: IntArray, inverse: ByteArray, opaque: List<IntRange>): Boolean {
        var verified = 0
        for (index in 0 until starts.size - 1) {
            val start = starts[index]
            val size = starts[index + 1] - start
            if (opaque.overlap(start, size)) continue
            val dex = ByteArray(size) { inverse[cipher[start + it].toInt() and 0xFF] }
            val checksum = Adler32().apply { update(dex, SIGNATURE_OFFSET, size - SIGNATURE_OFFSET) }
            if (checksum.value.toInt() != dex.readInt(CHECKSUM_OFFSET)) return false
            val signature = MessageDigest.getInstance("SHA-1").digest(dex.copyOfRange(SIGNATURE_END, size))
            if (!signature.contentEquals(dex.copyOfRange(SIGNATURE_OFFSET, SIGNATURE_END))) return false
            verified++
        }
        return verified > 0
    }
}
