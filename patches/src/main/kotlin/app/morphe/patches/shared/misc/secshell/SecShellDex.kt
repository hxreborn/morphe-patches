/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.secshell

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.InlineSmaliCompiler
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.util.MethodUtil
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.nio.ByteBuffer

private const val VERSION_OFFSET = 4
private const val VERSION_LENGTH = 3

internal class SecShellDex(bytes: ByteArray) {
    private val opcodes = Opcodes.forDexVersion(String(bytes, VERSION_OFFSET, VERSION_LENGTH).toInt())

    private val dexFile = DexBackedDexFile(opcodes, ByteBuffer.wrap(bytes))

    private val editedClasses = LinkedHashMap<String, MutableClass>()

    private val addedClasses = mutableListOf<ClassDef>()

    val isEdited get() = editedClasses.isNotEmpty() || addedClasses.isNotEmpty()

    fun hasClass(type: String) = dexFile.classes.any { it.type == type }

    fun hasString(string: String) = string in dexFile.stringSection

    fun methodsUsingString(string: String, firstParameter: String? = null): List<MutableMethod> =
        dexFile.classes.flatMap { classDef ->
            classDef.methods.filter { method ->
                (firstParameter == null || method.parameterTypes.firstOrNull() == firstParameter) &&
                    method.implementation?.instructions?.any { it.usesString(string) } == true
            }
        }.map { found ->
            editedClass(found.definingClass).methods.single { MethodUtil.methodSignaturesMatch(it, found) }
        }

    fun instanceFields(classType: String) = classDef(classType).instanceFields.toList()

    fun referencesInMethodUsing(string: String): List<Reference> {
        val bodies = dexFile.classes.flatMap { it.methods }.mapNotNull { it.implementation }
            .filter { body -> body.instructions.any { it.usesString(string) } }
        val body = bodies.singleOrNull()
            ?: throw PatchException("Expected one method using \"$string\", found ${bodies.size}")
        return body.instructions.filterIsInstance<ReferenceInstruction>().map { it.reference }
    }

    fun method(classType: String, name: String): MutableMethod {
        val matches = methods(classType, name)
        return matches.singleOrNull()
            ?: throw PatchException("Expected one method $name in $classType, found ${matches.size}")
    }

    fun methods(classType: String) = editedClass(classType).methods.toList()

    fun methods(classType: String, name: String) = editedClass(classType).methods.filter { it.name == name }

    fun replaceBody(classType: String, name: String, registers: Int, smali: String) {
        val original = method(classType, name)
        editedClass(classType).methods.apply {
            remove(original)
            add(original.withBody(registers, smali).toMutable())
        }
    }

    fun addClass(classDef: ClassDef) {
        if (hasClass(classDef.type)) throw PatchException("Payload dex already defines ${classDef.type}")
        addedClasses += classDef
    }

    fun toBytes(): ByteArray {
        val pool = DexPool(opcodes)
        dexFile.classes.forEach { pool.internClass(editedClasses[it.type] ?: it) }
        addedClasses.forEach(pool::internClass)
        val store = MemoryDataStore()
        pool.writeTo(store)
        return store.data.copyOf(store.size)
    }

    private fun Instruction.usesString(string: String) =
        getReference<StringReference>()?.string == string

    private fun classDef(type: String) =
        dexFile.classes.firstOrNull { it.type == type } ?: throw PatchException("Payload dex has no class $type")

    private fun editedClass(type: String) = editedClasses.getOrPut(type) { MutableClass(classDef(type)) }

    private fun Method.withBody(registers: Int, smali: String): Method {
        val implementation = MutableMethodImplementation(registers)
        InlineSmaliCompiler.compile(
            smali,
            parameterTypes.joinToString(""),
            registers,
            AccessFlags.STATIC.isSet(accessFlags),
        ).forEach(implementation::addInstruction)

        return ImmutableMethod(
            definingClass,
            name,
            parameters,
            returnType,
            accessFlags,
            annotations,
            hiddenApiRestrictions,
            implementation,
        )
    }
}
