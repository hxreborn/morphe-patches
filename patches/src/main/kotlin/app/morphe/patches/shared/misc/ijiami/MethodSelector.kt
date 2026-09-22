/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException

private const val CODE_INSNS_SIZE_OFFSET = 12
private const val CODE_HEADER_SIZE = 16

private const val OP_NOP = 0x00
private const val OP_CONST_STRING = 0x1a
private const val OP_CONST_STRING_JUMBO = 0x1b
private val INVOKE_OPCODES = (0x6e..0x72) + (0x74..0x78)

private const val PACKED_SWITCH_PAYLOAD = 1
private const val SPARSE_SWITCH_PAYLOAD = 2
private const val FILL_ARRAY_DATA_PAYLOAD = 3

private const val INSTRUCTION_CODE_UNITS =
    "1123123123111111111232235223211221223331123332222222222222222211" +
        "1111222222222222222222222222222222222222222222333331333331111111" +
        "1111111111111111222222222222222222222222222222221111111111111111" +
        "1111111111111111222222222222222222211111111111111111111111443322"

internal sealed interface MethodSelector {
    fun matches(dex: PayloadDex, methodIndex: Int, codeOffset: Int): Boolean

    val criterion: String
}

internal class LoadsString(private val value: String) : MethodSelector {
    override val criterion = "loads \"$value\""

    override fun matches(dex: PayloadDex, methodIndex: Int, codeOffset: Int): Boolean {
        val string = dex.stringIndexOf(value) ?: return false
        return dex.walkCode(methodIndex, codeOffset) { opcode, operand ->
            (opcode == OP_CONST_STRING || opcode == OP_CONST_STRING_JUMBO) && operand == string
        }
    }
}

internal class CallsMethod(
    private val classDescriptor: String,
    private val name: String,
    private val returnType: String,
) : MethodSelector {
    override val criterion = "calls $classDescriptor->$name and returns $returnType"

    override fun matches(dex: PayloadDex, methodIndex: Int, codeOffset: Int): Boolean {
        if (dex.returnTypeOf(methodIndex) != returnType) return false
        val callees = dex.methodIndicesOf(classDescriptor, name)
        if (callees.isEmpty()) return false
        return dex.walkCode(methodIndex, codeOffset) { opcode, operand ->
            opcode in INVOKE_OPCODES && operand in callees
        }
    }
}

private fun PayloadDex.walkCode(methodIndex: Int, codeOffset: Int, hit: (Int, Int) -> Boolean): Boolean {
    val codeUnits = int(codeOffset + CODE_INSNS_SIZE_OFFSET)
    val instructions = codeOffset + CODE_HEADER_SIZE
    var cursor = 0

    while (cursor < codeUnits) {
        val at = instructions + cursor * 2
        val unit = short(at)
        val opcode = unit and 0xFF
        val width = if (opcode == OP_NOP) {
            payloadCodeUnits(unit ushr 8, at)
        } else {
            INSTRUCTION_CODE_UNITS[opcode].digitToInt()
        }

        if (width <= 0) {
            throw PatchException(
                "Payload ident ${unit ushr 8} at code unit $cursor of ${nameOf(methodIndex)} " +
                    "in payload dex at $start has width $width",
            )
        }
        if (cursor + width > codeUnits) {
            throw PatchException(
                "Instruction at code unit $cursor of ${nameOf(methodIndex)} in payload dex at $start " +
                    "ends at ${cursor + width}, past the method's $codeUnits code units",
            )
        }

        if (opcode == OP_CONST_STRING_JUMBO) {
            if (hit(opcode, int(at + 2))) return true
        } else if (opcode != OP_NOP && hit(opcode, short(at + 2))) {
            return true
        }

        cursor += width
    }

    return false
}

private fun PayloadDex.payloadCodeUnits(identifier: Int, offset: Int) = when (identifier) {
    PACKED_SWITCH_PAYLOAD -> 4 + short(offset + 2) * 2
    SPARSE_SWITCH_PAYLOAD -> 2 + short(offset + 2) * 4
    FILL_ARRAY_DATA_PAYLOAD -> 4 + (int(offset + 4) * short(offset + 2) + 1) / 2
    else -> 1
}
