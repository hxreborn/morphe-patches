/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException
import kotlin.math.abs

private const val REGISTERS_SIZE_OFFSET = 0
private const val OUTS_SIZE_OFFSET = 4
private const val TRIES_SIZE_OFFSET = 6
private const val INSTRUCTIONS_SIZE_OFFSET = 12
private const val INSTRUCTIONS_OFFSET = 16
private const val TRY_ITEM_SIZE = 8

private const val OP_SGET_OBJECT = 0x62
private const val OP_INVOKE_STATIC = 0x71
private const val OP_MOVE_RESULT_OBJECT = 0x0c
private const val OP_RETURN_VOID = 0x0e
private const val OP_RETURN = 0x0f
private const val OP_RETURN_OBJECT = 0x11
private const val OP_CONST_4 = 0x12
private const val OP_CONST_16 = 0x13
private const val OP_CONST = 0x14
private const val OP_CONST_STRING = 0x1a

private const val STRING = "Ljava/lang/String;"
private const val BOXED_BOOLEAN = "Ljava/lang/Boolean;"
private const val BOXED_INTEGER = "Ljava/lang/Integer;"
private const val VALUE_OF = "valueOf"
private const val VALUE_OF_INT_SHORTY = "LI"

internal class IjiamiPayload(
    private val dexes: List<PayloadDex>,
    private val opaqueRanges: List<IntRange>,
) {
    fun method(classDescriptor: String, name: String) =
        methodOrNull(classDescriptor, name)
            ?: throw PatchException("Method not found in payload: $classDescriptor->$name")

    fun methodOrNull(classDescriptor: String, name: String): PayloadMethods? {
        val bodies = bodiesOf(classDescriptor, name)
        if (bodies.isEmpty()) return null
        if (bodies.size > 1) {
            throw PatchException(
                "$classDescriptor->$name matches ${bodies.size} methods, use methods() to edit every overload",
            )
        }

        return PayloadMethods(bodies)
    }

    fun methods(classDescriptor: String, name: String): PayloadMethods {
        val bodies = bodiesOf(classDescriptor, name)
        if (bodies.isEmpty()) {
            throw PatchException("Method not found in payload: $classDescriptor->$name")
        }

        return PayloadMethods(bodies)
    }

    fun methodWithString(classDescriptor: String, value: String) =
        methodWithStringOrNull(classDescriptor, value)
            ?: throw PatchException("No method in $classDescriptor loads \"$value\"")

    fun methodWithStringOrNull(classDescriptor: String, value: String): PayloadMethods? {
        val bodies = bodiesMatching(classDescriptor, LoadsString(value))
        if (bodies.isEmpty()) return null
        if (bodies.size > 1) {
            throw PatchException(
                "Expected one method in $classDescriptor loading \"$value\", found ${bodies.size}",
            )
        }

        return PayloadMethods(bodies)
    }

    fun methodsCalling(classDescriptor: String, calleeDescriptor: String, name: String, returning: String): PayloadMethods {
        val selector = CallsMethod(calleeDescriptor, name, returning)
        val bodies = bodiesMatching(classDescriptor, selector)
        if (bodies.isEmpty()) {
            throw PatchException("No method in $classDescriptor ${selector.criterion}")
        }

        return PayloadMethods(bodies)
    }

    private fun bodiesMatching(classDescriptor: String, selector: MethodSelector): List<MethodBody> {
        val bodies = dexes.flatMap { it.bodiesMatching(classDescriptor, selector, opaqueRanges) }
        if (bodies.any { body -> opaqueRanges.any { body.range.overlaps(it) } }) {
            throw PatchException(
                "Cannot rewrite $classDescriptor: a method that ${selector.criterion} " +
                    "overlaps an opaque payload block",
            )
        }

        return bodies
    }

    fun seal() = dexes.forEach { dex ->
        val range = dex.start until dex.start + dex.size
        if (opaqueRanges.none { range.overlaps(it) }) dex.reseal()
    }

    private fun bodiesOf(classDescriptor: String, name: String): List<MethodBody> {
        val bodies = dexes.flatMap { it.bodiesOf(classDescriptor, name, opaqueRanges) }

        if (bodies.any { body -> opaqueRanges.any { body.range.overlaps(it) } }) {
            throw PatchException(
                "Cannot rewrite $classDescriptor->$name: method overlaps an opaque payload block",
            )
        }

        return bodies
    }
}

private fun IntRange.overlaps(other: IntRange) = first <= other.last && other.first <= last

internal class PayloadMethods(private val bodies: List<MethodBody>) {
    fun returnEarly() = bodies.forEach { it.returnVoid() }

    fun returnEarly(value: Boolean) = bodies.forEach { it.returnConstant(if (value) 1 else 0, "Z") }

    fun returnEarly(value: Int) = bodies.forEach { it.returnConstant(value, "I") }

    fun returnNull() = bodies.forEach { it.returnNull() }

    fun returnString(value: String) = bodies.forEach { it.returnString(value) }

    fun returnBoxed(value: Boolean) = bodies.forEach { it.returnBoxed(value) }

    fun returnBoxed(value: Int) = bodies.forEach { it.returnBoxed(value) }
}

internal class MethodBody(
    private val dex: PayloadDex,
    private val payload: ByteArray,
    private val codeOffset: Int,
    private val returnType: String,
    private val description: String,
    private val opaqueRanges: List<IntRange>,
) {
    private val registers = payload.readShort(codeOffset + REGISTERS_SIZE_OFFSET)
    private val outgoing = payload.readShort(codeOffset + OUTS_SIZE_OFFSET)
    private val tryCount = payload.readShort(codeOffset + TRIES_SIZE_OFFSET)
    private val instructionCodeUnits = payload.readInt(codeOffset + INSTRUCTIONS_SIZE_OFFSET)
    private val instructionsOffset = codeOffset + INSTRUCTIONS_OFFSET
    private val tryItemsOffset =
        dex.start + ((instructionsOffset + instructionCodeUnits * 2 - dex.start + 3) and 3.inv())

    val range = codeOffset until instructionsOffset + instructionCodeUnits * 2

    fun returnVoid() {
        requireReturnType("V")
        write(byteArrayOf(OP_RETURN_VOID.toByte(), 0))
    }

    fun returnNull() {
        if (!returnType.startsWith("L") && !returnType.startsWith("[")) {
            throw PatchException("Cannot return null from $description: return type is $returnType")
        }
        writeConstant(0, OP_RETURN_OBJECT)
    }

    fun returnString(value: String) {
        requireReturnType(STRING)
        requireRegister()

        val string = dex.stringIndexOf(value)
            ?: throw PatchException("Missing string \"$value\" in dex for $description")
        if (string > 0xFFFF) {
            throw PatchException("String index $string exceeds 0xFFFF for \"$value\" in $description")
        }

        write(
            byteArrayOf(
                OP_CONST_STRING.toByte(),
                0,
                string.toByte(),
                (string shr 8).toByte(),
                OP_RETURN_OBJECT.toByte(),
                0,
            ),
        )
    }

    fun returnBoxed(value: Boolean) {
        requireReturnType(BOXED_BOOLEAN)
        requireRegister()

        val name = if (value) "TRUE" else "FALSE"
        val field = dex.fieldIndexOf(BOXED_BOOLEAN, name)
            ?: throw PatchException("Missing field reference $BOXED_BOOLEAN.$name in dex for $description")
        if (field > 0xFFFF) {
            throw PatchException("Field index $field exceeds 0xFFFF for $BOXED_BOOLEAN.$name in $description")
        }

        write(
            byteArrayOf(
                OP_SGET_OBJECT.toByte(),
                0,
                field.toByte(),
                (field shr 8).toByte(),
                OP_RETURN_OBJECT.toByte(),
                0,
            ),
        )
    }

    fun returnBoxed(value: Int) {
        requireReturnType(BOXED_INTEGER)
        requireRegister()
        requireOutgoing()

        val method = dex.methodIndexOf(BOXED_INTEGER, VALUE_OF, VALUE_OF_INT_SHORTY)
            ?: throw PatchException("Missing method reference $BOXED_INTEGER.$VALUE_OF in dex for $description")
        requireIndex(method, "$BOXED_INTEGER.$VALUE_OF")

        write(
            constantFor(value) + byteArrayOf(
                OP_INVOKE_STATIC.toByte(),
                0x10,
                method.toByte(),
                (method shr 8).toByte(),
                0,
                0,
                OP_MOVE_RESULT_OBJECT.toByte(),
                0,
                OP_RETURN_OBJECT.toByte(),
                0,
            ),
        )
    }

    fun returnConstant(value: Int, declared: String) {
        requireReturnType(declared)
        writeConstant(value, OP_RETURN)
    }

    private fun requireReturnType(declared: String) {
        if (returnType != declared) {
            throw PatchException("$description returns $returnType, not $declared")
        }
    }

    private fun requireIndex(index: Int, what: String) {
        if (index > 0xFFFF) {
            throw PatchException("Method index $index exceeds 0xFFFF for $what in $description")
        }
    }

    private fun requireRegister() {
        if (registers < 1) {
            throw PatchException("Cannot return a value from $description: no registers available")
        }
    }

    private fun requireOutgoing() {
        if (outgoing < 1) {
            throw PatchException("Cannot invoke from $description: no outgoing argument space")
        }
    }

    private fun constantFor(value: Int) = when (value) {
        in -8..7 -> byteArrayOf(OP_CONST_4.toByte(), (value shl 4).toByte())
        in Short.MIN_VALUE..Short.MAX_VALUE ->
            byteArrayOf(OP_CONST_16.toByte(), 0, value.toByte(), (value shr 8).toByte())
        else -> ByteArray(6).apply {
            this[0] = OP_CONST.toByte()
            writeInt(2, value)
        }
    }

    private fun writeConstant(value: Int, returnOpcode: Int) {
        requireRegister()
        write(constantFor(value) + byteArrayOf(returnOpcode.toByte(), 0))
    }

    private fun write(code: ByteArray) {
        val replacementCodeUnits = code.size / 2
        if (instructionCodeUnits * 2 < code.size) {
            throw PatchException(
                "Replacement for $description requires ${code.size} bytes, available: ${instructionCodeUnits * 2}",
            )
        }
        if (tryCount > 0 && replacementCodeUnits + tryCount > instructionCodeUnits) {
            throw PatchException(
                "Replacement for $description leaves no room for its $tryCount try blocks",
            )
        }
        payload.fill(0, instructionsOffset, instructionsOffset + instructionCodeUnits * 2)
        code.copyInto(payload, instructionsOffset)
        if (tryCount > 0) relocateExceptionTableToNopTail(replacementCodeUnits)
    }

    private fun relocateExceptionTableToNopTail(replacementCodeUnits: Int) {
        repeat(tryCount) { index ->
            payload.writeInt(tryItemsOffset + index * TRY_ITEM_SIZE, replacementCodeUnits + index)
            payload.writeShort(tryItemsOffset + index * TRY_ITEM_SIZE + 4, 1)
        }

        var cursor = tryItemsOffset + tryCount * TRY_ITEM_SIZE
        val handlerCount = payload.readUleb128(cursor)
        cursor = payload.leb128EndOffset(cursor)
        repeat(handlerCount) {
            val size = payload.readSleb128(cursor)
            cursor = payload.leb128EndOffset(cursor)
            repeat(abs(size)) {
                cursor = payload.leb128EndOffset(cursor)
                cursor = payload.rewriteUleb128PreservingWidth(cursor, replacementCodeUnits)
            }
            if (size <= 0) cursor = payload.rewriteUleb128PreservingWidth(cursor, replacementCodeUnits)
        }

        if (opaqueRanges.any { (codeOffset until cursor).overlaps(it) }) {
            throw PatchException("Cannot rewrite $description: its exception table overlaps an opaque payload block")
        }
    }
}

private fun ByteArray.leb128EndOffset(offset: Int): Int {
    var end = offset
    while (this[end].toInt() and 0x80 != 0) end++
    return end + 1
}

private fun ByteArray.readUleb128(offset: Int): Int {
    var value = 0
    var shift = 0
    var cursor = offset
    do {
        val byte = this[cursor++].toInt()
        value = value or ((byte and 0x7f) shl shift)
        shift += 7
    } while (byte and 0x80 != 0)
    return value
}

private fun ByteArray.readSleb128(offset: Int): Int {
    val value = readUleb128(offset)
    val bits = (leb128EndOffset(offset) - offset) * 7
    return if (bits < 32 && value and (1 shl (bits - 1)) != 0) value or (-1 shl bits) else value
}

private fun ByteArray.rewriteUleb128PreservingWidth(offset: Int, value: Int): Int {
    val end = leb128EndOffset(offset)
    val width = end - offset
    if (width < 5 && value ushr (width * 7) != 0) {
        throw PatchException("Relocated address $value does not fit the original $width-byte ULEB128")
    }
    for (index in offset until end) {
        val group = (value ushr ((index - offset) * 7)) and 0x7f
        this[index] = (if (index < end - 1) group or 0x80 else group).toByte()
    }
    return end
}
