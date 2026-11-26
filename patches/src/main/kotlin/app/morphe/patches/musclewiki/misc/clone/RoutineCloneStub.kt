/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.clone

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.arm64.Arm64
import app.morphe.patches.shared.misc.dart.appendNativeCode
import app.morphe.patches.shared.writeInt
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val INSTRUCTION_BYTES = 4
private const val CONDITIONAL_BRANCH_SHIFT = 5
private const val CONDITIONAL_BRANCH_BITS = 19
private const val UNCONDITIONAL_BRANCH_BITS = 26
private const val FIELD_SHIFT = 12
private const val FIELD_MASK = 0x1ff

internal fun dartField(instruction: Int, offset: Int): Int {
    require(offset in -256..255)
    return (instruction and (FIELD_MASK shl FIELD_SHIFT).inv()) or ((offset and FIELD_MASK) shl FIELD_SHIFT)
}

internal class RoutineCloneStub {
    private val code = mutableListOf<Int>()
    private val labels = mutableMapOf<String, Int>()
    private val localBranches = mutableListOf<LocalBranch>()
    private val calls = mutableListOf<ExternalBranch>()

    fun emit(vararg instructions: Int) {
        code.addAll(instructions.toList())
    }

    fun emit(instructions: List<Int>) {
        code.addAll(instructions)
    }

    fun label(name: String) {
        if (labels.put(name, code.size) != null) throw PatchException("Duplicate native label $name")
    }

    fun branch(label: String, instruction: Int, conditional: Boolean) {
        localBranches += LocalBranch(code.size, label, instruction, conditional)
        code += 0
    }

    fun call(target: Int) {
        calls += ExternalBranch(code.size, target, true)
        code += 0
    }

    fun jump(target: Int) {
        calls += ExternalBranch(code.size, target, false)
        code += 0
    }

    fun appendTo(bytes: ByteArray): Int {
        localBranches.forEach { branch ->
            val target = labels[branch.label] ?: throw PatchException("Missing native label ${branch.label}")
            val distance = target - branch.index
            val bits = if (branch.conditional) CONDITIONAL_BRANCH_BITS else UNCONDITIONAL_BRANCH_BITS
            if (distance !in -(1 shl (bits - 1)) until (1 shl (bits - 1))) {
                throw PatchException("Native branch is out of range")
            }
            val immediate = distance and ((1 shl bits) - 1)
            code[branch.index] = branch.instruction or
                (if (branch.conditional) immediate shl CONDITIONAL_BRANCH_SHIFT else immediate)
        }
        val body = ByteBuffer.allocate(code.size * INSTRUCTION_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        code.forEach(body::putInt)
        val start = bytes.appendNativeCode(body.array())
        calls.forEach { branch ->
            val address = start + branch.index * INSTRUCTION_BYTES
            val distance = branch.target - address
            if (distance % INSTRUCTION_BYTES != 0 ||
                distance / INSTRUCTION_BYTES !in -(1 shl (UNCONDITIONAL_BRANCH_BITS - 1)) until
                (1 shl (UNCONDITIONAL_BRANCH_BITS - 1))) throw PatchException("Native call is out of range")
            bytes.writeInt(address, if (branch.link) Arm64.branchLink(distance / INSTRUCTION_BYTES)
                else Arm64.branch(distance / INSTRUCTION_BYTES))
        }
        return start
    }

    private data class LocalBranch(val index: Int, val label: String, val instruction: Int, val conditional: Boolean)
    private data class ExternalBranch(val index: Int, val target: Int, val link: Boolean)
}
