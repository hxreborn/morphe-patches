/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.jiagu

import app.morphe.patcher.patch.PatchException
import java.util.zip.Adler32

private const val CHECKSUM_OFFSET = 8
private const val FILE_SIZE_OFFSET = 32
private const val STRING_IDS_OFFSET = 56
private const val TYPE_IDS_OFFSET = 64
private const val PROTO_IDS_OFFSET = 72
private const val METHOD_IDS_OFFSET = 88
private const val PROTO_ID_SIZE = 12
private const val CLASS_DEFS_OFFSET = 96

private const val CLASS_DEF_SIZE = 32
private const val CLASS_DATA_FIELD = 24

private const val CODE_TRIES_SIZE = 6
private const val CODE_DEBUG_INFO_OFF = 8
private const val CODE_INSNS_SIZE = 12
private const val CODE_INSNS = 16

private const val ACCESS_STATIC = 0x8
private const val ACCESS_NATIVE = 0x100
private const val ACCESS_ABSTRACT = 0x400

private const val RETURN_VOID = 0x000E
private const val RETURN_V0 = 0x000F
private const val CONST_4_V0 = 0x12
private const val INVOKE_INTERFACE = 0x72

private val CATEGORY_ONE = setOf("Z", "B", "S", "C", "I", "F")

// Fixed dex layout during instruction replacement
internal class PayloadDexEditor(dex: ByteArray) {
    private val bytes = dex.copyOf()

    var isModified = false
        private set

    private val strings: List<String>
    private val types: List<String>
    private val methods: List<String>
    private val methodClasses: List<String>
    private val returnTypes: List<String>
    private val parameterTypes: List<List<String>>
    private val classCount = bytes.readInt(CLASS_DEFS_OFFSET)
    private val classOffset = bytes.readInt(CLASS_DEFS_OFFSET + 4)

    init {
        val stringCount = bytes.readInt(STRING_IDS_OFFSET)
        val stringOffset = bytes.readInt(STRING_IDS_OFFSET + 4)
        strings = List(stringCount) {
            val reader = Uleb(bytes, bytes.readInt(stringOffset + it * 4))
            reader.next()
            val start = reader.cursor
            var end = start
            while (bytes[end] != 0.toByte()) end++
            String(bytes, start, end - start, Charsets.UTF_8)
        }

        val typeCount = bytes.readInt(TYPE_IDS_OFFSET)
        val typeOffset = bytes.readInt(TYPE_IDS_OFFSET + 4)
        types = List(typeCount) { strings[bytes.readInt(typeOffset + it * 4)] }

        val protoOffset = bytes.readInt(PROTO_IDS_OFFSET + 4)

        val methodCount = bytes.readInt(METHOD_IDS_OFFSET)
        val methodOffset = bytes.readInt(METHOD_IDS_OFFSET + 4)
        methods = List(methodCount) { strings[bytes.readInt(methodOffset + it * 8 + 4)] }
        methodClasses = List(methodCount) { types[bytes.readShort(methodOffset + it * 8)] }
        returnTypes = List(methodCount) {
            val proto = bytes.readShort(methodOffset + it * 8 + 2)
            types[bytes.readInt(protoOffset + proto * PROTO_ID_SIZE + 4)]
        }
        parameterTypes = List(methodCount) {
            val proto = bytes.readShort(methodOffset + it * 8 + 2)
            val list = bytes.readInt(protoOffset + proto * PROTO_ID_SIZE + 8)
            if (list == 0) {
                emptyList()
            } else {
                List(bytes.readInt(list)) { at -> types[bytes.readShort(list + 4 + at * 2)] }
            }
        }
    }

    fun defines(descriptor: String) = classDataOf(descriptor) != null

    fun forceReturn(descriptor: String, name: String, value: Int) =
        replace(descriptor, name, intArrayOf(CONST_4_V0 or (value shl 12), RETURN_V0), CATEGORY_ONE)

    fun forceReturnVoid(descriptor: String, name: String) =
        replace(descriptor, name, intArrayOf(RETURN_VOID), setOf("V"))

    fun buildDex(): ByteArray {
        bytes.writeInt(FILE_SIZE_OFFSET, bytes.size)
        bytes.writeInt(
            CHECKSUM_OFFSET,
            Adler32().apply { update(bytes, 12, bytes.size - 12) }.value.toInt(),
        )
        return bytes
    }

    fun declares(descriptor: String, name: String, returns: String) =
        classDataOf(descriptor)?.let { data ->
            methodsOf(data).any { it.name == name && it.returns == returns && it.code != 0 }
        } ?: false

    fun invokeBooleanCallback(
        descriptor: String,
        name: String,
        callback: String,
        result: String,
        value: Boolean,
    ) {
        val target = methods.indices.firstOrNull {
            methodClasses[it] == callback && methods[it] == result
        } ?: throw PatchException("$callback.$result is not referenced by this payload dex")

        rewrite(descriptor, name, setOf("V")) { code, flags ->
            val parameters = parameterTypes[methodIndexOf(descriptor, name, "V")]
            val slot = parameters.indexOf(callback)
            if (slot < 0) throw PatchException("$descriptor.$name takes no $callback")

            var register = bytes.readShort(code) - bytes.readShort(code + 2)
            if (flags and ACCESS_STATIC == 0) register++
            parameters.take(slot).forEach { register += if (it == "J" || it == "D") 2 else 1 }

            if (register > 0xF) {
                throw PatchException("Callback register v$register exceeds invoke-interface encoding limit: $descriptor.$name")
            }

            intArrayOf(
                CONST_4_V0 or ((if (value) 1 else 0) shl 12),
                INVOKE_INTERFACE or (2 shl 12),
                target,
                register,
                RETURN_VOID,
            )
        }
    }

    private fun methodIndexOf(descriptor: String, name: String, returns: String) =
        methods.indices.first {
            methodClasses[it] == descriptor && methods[it] == name && returnTypes[it] == returns
        }

    private fun classDataOf(descriptor: String): Int? {
        for (index in 0 until classCount) {
            val entry = classOffset + index * CLASS_DEF_SIZE
            if (types[bytes.readInt(entry)] != descriptor) continue
            return bytes.readInt(entry + CLASS_DATA_FIELD).takeIf { it != 0 }
        }
        return null
    }

    private fun methodsOf(classData: Int): List<Method> {
        val reader = Uleb(bytes, classData)

        val staticFields = reader.next()
        val instanceFields = reader.next()
        val directMethods = reader.next()
        val virtualMethods = reader.next()

        repeat(staticFields + instanceFields) { reader.next(); reader.next() }

        val found = mutableListOf<Method>()
        var index = 0
        for (position in 0 until directMethods + virtualMethods) {
            if (position == directMethods) index = 0
            index += reader.next()
            val flags = reader.next()
            val code = reader.next()
            found += Method(methods[index], flags, code, returnTypes[index])
        }
        return found
    }

    private fun replace(
        descriptor: String,
        name: String,
        units: IntArray,
        allowedReturns: Set<String>,
    ) = rewrite(descriptor, name, allowedReturns) { _, _ -> units }

    private fun rewrite(
        descriptor: String,
        name: String,
        allowedReturns: Set<String>,
        body: (code: Int, flags: Int) -> IntArray,
    ) {
        val classData = classDataOf(descriptor)
            ?: throw PatchException("$descriptor is not defined in this payload dex")

        var replaced = 0
        for ((method, flags, code, returns) in methodsOf(classData)) {
            if (method != name) continue

            if (returns !in allowedReturns) continue

            if (flags and (ACCESS_NATIVE or ACCESS_ABSTRACT) != 0 || code == 0) {
                throw PatchException("$descriptor.$name has no body to replace")
            }

            val registers = bytes.readShort(code)
            val parameters = bytes.readShort(code + 2)
            if (registers - parameters < 1) {
                throw PatchException("$descriptor.$name has no local register")
            }
            if (bytes.readShort(code + CODE_TRIES_SIZE) != 0) {
                throw PatchException("Exception handlers unsupported: $descriptor.$name")
            }

            val units = body(code, flags)
            val instructions = bytes.readInt(code + CODE_INSNS_SIZE)
            if (instructions < units.size) {
                throw PatchException("Replacement for $descriptor.$name requires ${units.size} code units, available: $instructions")
            }

            bytes.writeInt(code + CODE_DEBUG_INFO_OFF, 0)
            for (at in 0 until instructions) {
                bytes.writeShort(code + CODE_INSNS + at * 2, units.getOrElse(at) { 0 })
            }
            isModified = true
            replaced++
        }

        if (replaced == 0) {
            throw PatchException("$descriptor.$name has no ${allowedReturns.joinToString("/")} overload")
        }
    }
}

private fun ByteArray.readInt(offset: Int) =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)

private fun ByteArray.readShort(offset: Int) =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

private fun ByteArray.writeInt(offset: Int, value: Int) {
    repeat(4) { this[offset + it] = (value ushr (it * 8)).toByte() }
}

private fun ByteArray.writeShort(offset: Int, value: Int) {
    repeat(2) { this[offset + it] = (value ushr (it * 8)).toByte() }
}

private data class Method(val name: String, val flags: Int, val code: Int, val returns: String)

private class Uleb(private val bytes: ByteArray, var cursor: Int) {
    fun next(): Int {
        var value = 0
        var shift = 0
        while (true) {
            val byte = bytes[cursor++].toInt() and 0xFF
            value = value or ((byte and 0x7F) shl shift)
            if (byte < 0x80) return value
            shift += 7
        }
    }
}
