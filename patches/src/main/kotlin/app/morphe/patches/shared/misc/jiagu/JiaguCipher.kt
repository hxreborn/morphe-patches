/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.jiagu

import app.morphe.patcher.patch.PatchException

private const val STATE_SIZE = 256

internal class JiaguCipher(key: ByteArray) {
    private val schedule: IntArray

    init {
        if (key.size != KEY_SIZE) {
            throw PatchException("Invalid Jiagu key length: expected $KEY_SIZE bytes, got ${key.size}")
        }

        schedule = IntArray(STATE_SIZE) { it }
        var j = 0
        for (i in 0 until STATE_SIZE) {
            j = (j + schedule[i] + (key[i % key.size].toInt() and 0xFF)) and 0xFF
            schedule.swap(i, j)
        }
    }

    fun crypt(data: ByteArray): ByteArray {
        val state = schedule.copyOf()
        var i = FIRST_I
        var j = FIRST_J

        return ByteArray(data.size) { at ->
            i = (i + 2) and 0xFF
            j = (j + state[i] + 1) and 0xFF
            state.swap(i, j)
            (data[at].toInt() xor state[(state[i] + state[j]) and 0xFF]).toByte()
        }
    }

    private companion object {
        const val KEY_SIZE = 16
        const val FIRST_I = 3
        const val FIRST_J = 5

        fun IntArray.swap(a: Int, b: Int) {
            val held = this[a]
            this[a] = this[b]
            this[b] = held
        }
    }
}
