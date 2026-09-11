/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException

private const val REGISTERS_SIZE_OFFSET = 0
private const val OUTS_SIZE_OFFSET = 4
private const val INSTRUCTIONS_SIZE_OFFSET = 12
private const val INSTRUCTIONS_OFFSET = 16

private const val OP_SGET_OBJECT = 0x62
private const val OP_INVOKE_STATIC = 0x71
private const val OP_MOVE_RESULT_OBJECT = 0x0c
private const val OP_RETURN_VOID = 0x0e
private const val OP_RETURN = 0x0f
private const val OP_RETURN_OBJECT = 0x11
private const val OP_CONST_4 = 0x12
private const val OP_CONST_16 = 0x13
private const val OP_CONST = 0x14

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

    fun seal() = dexes.forEach { dex ->
        val range = dex.start until dex.start + dex.size
        if (opaqueRanges.none { range.overlaps(it) }) dex.reseal()
    }

    private fun bodiesOf(classDescriptor: String, name: String): List<MethodBody> {
        val bodies = dexes.flatMap { it.bodiesOf(classDescriptor, name) }

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

    fun returnDigits(value: Int) = bodies.forEach { it.returnDigits(value) }

    fun returnBoxed(value: Boolean) = bodies.forEach { it.returnBoxed(value) }

    fun returnBoxed(value: Int) = bodies.forEach { it.returnBoxed(value) }
}

internal class MethodBody(
    private val dex: PayloadDex,
    private val payload: ByteArray,
    private val codeOffset: Int,
    private val returnType: String,
    private val description: String,
) {
    private val registers = payload.readShort(codeOffset + REGISTERS_SIZE_OFFSET)
    private val outgoing = payload.readShort(codeOffset + OUTS_SIZE_OFFSET)
    private val instructionsSize = payload.readInt(codeOffset + INSTRUCTIONS_SIZE_OFFSET)
    private val instructions = codeOffset + INSTRUCTIONS_OFFSET

    val range = codeOffset until instructions + instructionsSize * 2

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

    fun returnDigits(value: Int) {
        requireReturnType(STRING)
        requireRegister()
        requireOutgoing()

        val method = dex.methodIndexOf(STRING, VALUE_OF, VALUE_OF_INT_SHORTY)
            ?: throw PatchException("Missing method reference $STRING.$VALUE_OF in dex for $description")
        requireIndex(method, "$STRING.$VALUE_OF")

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
        if (instructionsSize * 2 < code.size) {
            throw PatchException(
                "Replacement for $description requires ${code.size} bytes, available: ${instructionsSize * 2}",
            )
        }
        code.copyInto(payload, instructions)
    }
}
