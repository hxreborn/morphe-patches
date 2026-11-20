/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import java.io.File
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal class PatchBytecode(classDirectories: List<File>) {
    private class MethodInfo {
        val strings = mutableSetOf<String>()
        val callees = mutableSetOf<String>()
        val classReferences = mutableSetOf<String>()
    }

    private val methods = mutableMapOf<String, MethodInfo>()

    private val methodsByClass = mutableMapOf<String, MutableList<String>>()

    init {
        classDirectories.forEach { directory ->
            directory.walkTopDown().filter { it.extension == "class" }.forEach { parse(it.readBytes()) }
        }
    }

    private fun parse(bytes: ByteArray) {
        val reader = ClassReader(bytes)
        val owner = reader.className
        reader.accept(
            object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?,
                ): MethodVisitor {
                    val key = "$owner.$name$descriptor"
                    val info = MethodInfo().also { methods[key] = it }
                    methodsByClass.getOrPut(owner) { mutableListOf() } += key
                    return object : MethodVisitor(Opcodes.ASM9) {
                        private fun constant(value: Any?) {
                            when (value) {
                                is String -> info.strings += value
                                is Handle -> info.callees += "${value.owner}.${value.name}${value.desc}"
                                is ConstantDynamic -> repeat(value.bootstrapMethodArgumentCount) {
                                    constant(value.getBootstrapMethodArgument(it))
                                }
                            }
                        }

                        override fun visitLdcInsn(value: Any?) = constant(value)

                        override fun visitMethodInsn(
                            opcode: Int,
                            owner: String,
                            name: String,
                            descriptor: String,
                            isInterface: Boolean,
                        ) {
                            info.callees += "$owner.$name$descriptor"
                            info.classReferences += owner
                        }

                        override fun visitInvokeDynamicInsn(
                            name: String?,
                            descriptor: String?,
                            bootstrapMethodHandle: Handle?,
                            vararg bootstrapMethodArguments: Any?,
                        ) = bootstrapMethodArguments.forEach(::constant)

                        override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                            info.classReferences += owner
                        }

                        override fun visitTypeInsn(opcode: Int, type: String) {
                            info.classReferences += type
                        }
                    }
                }
            },
            0,
        )
    }

    fun stringsReachedBy(className: String, field: String): Set<String> = reached(className, field).first

    fun qualifiedArguments(className: String, field: String, isPrefix: (String) -> Boolean): Map<String, Set<String>> {
        val keys = reached(className, field).second.filter(methods::containsKey)
        val qualifiers = keys.filter { key -> methods.getValue(key).strings.any(isPrefix) && key.substringAfter('(').contains("Ljava/lang/String;") }
            .toSet()
        val result = mutableMapOf<String, MutableSet<String>>()
        for (key in keys) {
            val method = methods.getValue(key)
            val called = method.callees.filter { it in qualifiers }
            if (called.isEmpty()) continue
            for (qualifier in called) {
                for (prefix in methods.getValue(qualifier).strings.filter(isPrefix)) {
                    result.getOrPut(prefix) { mutableSetOf() } += method.strings
                }
            }
        }
        return result
    }

    private fun reached(className: String, field: String): Pair<Set<String>, Set<String>> {
        val owner = className.replace('.', '/')
        val roots = mutableSetOf<String>()
        methodsByClass[owner].orEmpty().forEach { key ->
            val name = key.removePrefix("$owner.")
            if (name.startsWith("$field\$lambda") || name.startsWith("$field$")) roots += key
        }
        methodsByClass.forEach { (type, keys) -> if (type.startsWith("$owner$$field$")) roots += keys }
        val strings = mutableSetOf<String>()
        if (roots.isEmpty()) {
            methods["$owner.<clinit>()V"]?.let { clinit ->
                strings += clinit.strings
                roots += clinit.callees.filter(methods::containsKey)
            }
        }
        val seen = mutableSetOf<String>()
        val queue = ArrayDeque(roots)
        while (queue.isNotEmpty()) {
            val key = queue.removeFirst()
            if (!seen.add(key)) continue
            val method = methods[key] ?: continue
            strings += method.strings
            method.callees.filter(methods::containsKey).forEach(queue::add)
            for (reference in method.classReferences) {
                val initializer = "$reference.<clinit>()V"
                if (seen.add(initializer)) methods[initializer]?.let { strings += it.strings }
                methodsByClass[reference].orEmpty().filter { it.startsWith("$reference.<init>") }.forEach(queue::add)
            }
        }
        return strings to seen
    }
}