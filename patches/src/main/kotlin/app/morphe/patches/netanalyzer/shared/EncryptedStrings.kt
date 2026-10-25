/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.Base64

private const val STRING_LIBRARY = "lib/arm64-v8a/libnative.so"
private const val STRING_DECODER_SYMBOL = "Java_net_techet_netanalyzershared_net_NativeNetUtils_ds"
private const val STRING_KEY_PARTS = 2

private const val SPLITMIX_GAMMA = 0x9e3779b97f4a7c15UL
private const val SPLITMIX_MULTIPLIER_1 = 0xbf58476d1ce4e5b9UL
private const val SPLITMIX_MULTIPLIER_2 = 0x94d049bb133111ebUL
private const val SPLITMIX_SHIFT_1 = 30
private const val SPLITMIX_SHIFT_2 = 27
private const val SPLITMIX_SHIFT_3 = 31
private const val SEED_BYTES = ULong.SIZE_BYTES
private const val TRAILING_PLAINTEXT_BYTE = '.'.code

private var stringKey: ULong? = null
private val decrypted = HashMap<String, String?>()

internal val stringKeyPatch = rawResourcePatch {
    execute {
        val library = get(STRING_LIBRARY, true)
        if (!library.exists()) throw PatchException("$STRING_LIBRARY is missing")

        val elf = Arm64ElfReader(library.readBytes())
        val keyParts = elf.pageRelativeLoads(elf.symbol(STRING_DECODER_SYMBOL)).take(STRING_KEY_PARTS)
        if (keyParts.size != STRING_KEY_PARTS) {
            throw PatchException("$STRING_DECODER_SYMBOL loads ${keyParts.size} page-relative qwords, expected $STRING_KEY_PARTS")
        }
        stringKey = keyParts.map(elf::readQword).reduce(ULong::xor)
        decrypted.clear()
    }
}

internal fun decryptString(encoded: String): String? = decrypted.getOrPut(encoded) {
    val key = stringKey ?: throw PatchException("The string key was not read before decrypting")
    val bytes = try {
        Base64.getUrlDecoder().decode(encoded.replace(" ", "").replace('(', '-').replace(')', '_'))
    } catch (_: IllegalArgumentException) {
        return@getOrPut null
    }
    if (bytes.size < SEED_BYTES) return@getOrPut null

    var state = (0 until SEED_BYTES).fold(0UL) { seed, i ->
        seed or (bytes[i].toUByte().toULong() shl (Byte.SIZE_BITS * i))
    } xor key
    var block = 0UL
    val plain = ByteArray(bytes.size - SEED_BYTES)
    for (i in plain.indices) {
        if (i % SEED_BYTES == 0) {
            state += SPLITMIX_GAMMA
            block = state
            block = (block xor (block shr SPLITMIX_SHIFT_1)) * SPLITMIX_MULTIPLIER_1
            block = (block xor (block shr SPLITMIX_SHIFT_2)) * SPLITMIX_MULTIPLIER_2
            block = block xor (block shr SPLITMIX_SHIFT_3)
        }
        val next = bytes.getOrNull(i + SEED_BYTES + 1)?.toInt() ?: TRAILING_PLAINTEXT_BYTE
        val keystream = (block shr (Byte.SIZE_BITS * (i % SEED_BYTES))).toInt()
        plain[i] = (bytes[i + SEED_BYTES].toInt() xor next xor keystream).toByte()
    }
    String(plain, Charsets.UTF_8)
}

internal fun Instruction.decryptedString(): String? {
    if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) return null
    return decryptString(getReference<StringReference>()!!.string)
}

internal fun decryptedString(value: String) = InstructionFilter { _, instruction ->
    instruction.decryptedString() == value
}
