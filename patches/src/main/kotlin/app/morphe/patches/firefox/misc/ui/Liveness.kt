/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/Liveness.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.BuilderSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import java.util.BitSet

internal class Liveness(private val method: MutableMethod) {

    private val instructions = method.implementation!!.instructions.toList()
    private val registerCount = method.implementation!!.registerCount
    private val liveIn = Array(instructions.size + 1) { BitSet(registerCount) }
    private val handlers = handlerTargets()

    init {
        var changed = true
        while (changed) {
            changed = false
            for (i in instructions.indices.reversed()) {
                val result = BitSet(registerCount)
                successors(i).forEach { s -> if (s < liveIn.size) result.or(liveIn[s]) }
                val (defs, uses) = defsUses(instructions[i])
                defs.forEach { if (it in 0 until registerCount) result.clear(it) }
                handlers[i].forEach { h -> if (h < liveIn.size) result.or(liveIn[h]) }
                uses.forEach { if (it in 0 until registerCount) result.set(it) }
                if (result != liveIn[i]) {
                    liveIn[i] = result
                    changed = true
                }
            }
        }
    }

    fun isLive(index: Int, register: Int) = liveIn[index].get(register)

    fun free(index: Int, joins: List<Int> = emptyList(), exclude: Set<Int> = emptySet()): List<Int> =
        (0 until registerCount).filter { r ->
            r !in exclude && !liveIn[index].get(r) && joins.none { liveIn[it].get(r) }
        }

    fun takeLow(count: Int, index: Int, joins: List<Int> = emptyList(), exclude: Set<Int> = emptySet()): List<Int> {
        val picked = free(index, joins, exclude).filter { it < 16 }.take(count)
        if (picked.size < count) throw IllegalStateException("not enough free registers at $index")
        return picked
    }

    fun takeRun(length: Int, index: Int, joins: List<Int> = emptyList(), exclude: Set<Int> = emptySet()): Int {
        val free = free(index, joins, exclude).toSet()
        for (base in registerCount - length downTo 0) {
            if ((base until base + length).all { it in free }) return base
        }
        throw IllegalStateException("no free register run at $index")
    }

    fun redefinedBetween(register: Int, from: Int, until: Int): Boolean =
        (from until minOf(until, instructions.size)).any { defsUses(instructions[it]).first.contains(register) }

    fun copiesOf(source: Int, index: Int): List<Int> {
        val states = arrayOfNulls<BitSet>(instructions.size + 1)
        states[0] = BitSet(registerCount).also { it.set(source) }
        var changed = true

        fun merge(target: Int, state: BitSet) {
            if (target >= states.size) return
            val current = states[target]
            if (current == null) {
                states[target] = state.clone() as BitSet
                changed = true
                return
            }
            val next = current.clone() as BitSet
            next.and(state)
            if (next != current) {
                states[target] = next
                changed = true
            }
        }

        while (changed) {
            changed = false
            for (i in instructions.indices) {
                val before = states[i] ?: continue
                val instruction = instructions[i]
                val name = instruction.opcode.name
                val after = before.clone() as BitSet
                val isCopy = name.startsWith("move") && !name.startsWith("move-result") &&
                    name != "move-exception" && !name.contains("wide")
                val copied = isCopy && before.get((instruction as TwoRegisterInstruction).registerB)
                defsUses(instruction).first.forEach { if (it in 0 until registerCount) after.clear(it) }
                if (copied) after.set((instruction as TwoRegisterInstruction).registerA)
                successors(i).forEach { merge(it, after) }
                handlers[i].forEach { merge(it, before) }
            }
        }
        val state = states[index] ?: return emptyList()
        return (0 until registerCount).filter { state.get(it) }
    }

    private fun handlerTargets(): Array<MutableList<Int>> {
        val result = Array<MutableList<Int>>(instructions.size) { mutableListOf() }
        for (block in method.implementation!!.tryBlocks) {
            val handler = block.exceptionHandler.handler.location.index
            val end = minOf(block.end.location.index, instructions.size)
            for (i in block.start.location.index until end) result[i].add(handler)
        }
        return result
    }

    private fun successors(i: Int): List<Int> {
        val instruction = instructions[i]
        val name = instruction.opcode.name
        if (name.endsWith("-payload")) return emptyList()
        val result = ArrayList<Int>(2)
        if (name.endsWith("-switch")) {
            val payload = instructions[(instruction as BuilderOffsetInstruction).target.location.index]
            if (payload is BuilderSwitchPayload) {
                payload.switchElements.forEach { result.add(it.target.location.index) }
            }
            result.add(i + 1)
            return result
        }
        if (instruction is BuilderOffsetInstruction && name != "fill-array-data") {
            result.add(instruction.target.location.index)
        }
        val terminal = name.startsWith("return") || name == "throw" || name.startsWith("goto")
        if (!terminal) result.add(i + 1)
        return result
    }

    private fun defsUses(instruction: Instruction): Pair<List<Int>, List<Int>> {
        val name = instruction.opcode.name
        val defs = ArrayList<Int>()
        val uses = ArrayList<Int>()
        if (name.endsWith("-payload")) return defs to uses

        fun a() = (instruction as OneRegisterInstruction).registerA
        fun b() = (instruction as TwoRegisterInstruction).registerB
        fun c() = (instruction as ThreeRegisterInstruction).registerC
        fun pair(list: MutableList<Int>, register: Int, wide: Boolean) {
            list.add(register)
            if (wide) list.add(register + 1)
        }

        val wideName = name.contains("wide") || name.endsWith("-long") || name.endsWith("-double") ||
            name.contains("-long/") || name.contains("-double/")

        when {
            name == "nop" || name.startsWith("goto") -> Unit
            name.startsWith("move-result") -> pair(defs, a(), wideName)
            name == "move-exception" -> defs.add(a())
            name.startsWith("move") -> {
                pair(defs, a(), wideName)
                pair(uses, b(), wideName)
            }
            name == "return-void" -> Unit
            name.startsWith("return") -> pair(uses, a(), wideName)
            name.startsWith("const") -> pair(defs, a(), name.startsWith("const-wide"))
            name.startsWith("monitor") || name == "throw" || name == "fill-array-data" ||
                name.contains("switch") || name == "check-cast" -> uses.add(a())
            name == "instance-of" || name == "array-length" || name == "new-array" -> {
                defs.add(a())
                uses.add(b())
            }
            name == "new-instance" -> defs.add(a())
            name.startsWith("if-") -> {
                uses.add(a())
                if (!name.endsWith("z")) uses.add(b())
            }
            name.startsWith("cmp") -> {
                defs.add(a())
                pair(uses, b(), true)
                pair(uses, c(), true)
            }
            name.startsWith("aget") -> {
                pair(defs, a(), wideName)
                uses.add(b())
                uses.add(c())
            }
            name.startsWith("aput") -> {
                pair(uses, a(), wideName)
                uses.add(b())
                uses.add(c())
            }
            name.startsWith("iget") -> {
                pair(defs, a(), wideName)
                uses.add(b())
            }
            name.startsWith("iput") -> {
                pair(uses, a(), wideName)
                uses.add(b())
            }
            name.startsWith("sget") -> pair(defs, a(), wideName)
            name.startsWith("sput") -> pair(uses, a(), wideName)
            name.startsWith("invoke") || name.startsWith("filled-new-array") -> {
                if (instruction is RegisterRangeInstruction) {
                    for (r in instruction.startRegister until instruction.startRegister + instruction.registerCount) uses.add(r)
                } else if (instruction is FiveRegisterInstruction) {
                    val registers = listOf(
                        instruction.registerC, instruction.registerD, instruction.registerE,
                        instruction.registerF, instruction.registerG,
                    )
                    uses.addAll(registers.take(instruction.registerCount))
                }
            }
            name.contains("-to-") -> {
                val source = name.substringBefore("-to-")
                val target = name.substringAfter("-to-")
                pair(defs, a(), target == "long" || target == "double")
                pair(uses, b(), source == "long" || source == "double")
            }
            name.startsWith("neg-") || name.startsWith("not-") -> {
                pair(defs, a(), wideName)
                pair(uses, b(), wideName)
            }
            name.endsWith("/2addr") -> {
                pair(defs, a(), wideName)
                pair(uses, a(), wideName)
                pair(uses, b(), wideName)
            }
            name.endsWith("/lit8") || name.endsWith("/lit16") -> {
                defs.add(a())
                uses.add(b())
            }
            else -> {
                pair(defs, a(), wideName)
                pair(uses, b(), wideName)
                pair(uses, c(), wideName)
            }
        }
        return defs to uses
    }
}
