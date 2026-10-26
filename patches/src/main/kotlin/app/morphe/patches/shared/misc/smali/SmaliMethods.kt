/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.smali

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import java.nio.ByteBuffer

internal fun descriptor(method: MethodReference): String = DexFormatter.INSTANCE.getMethodDescriptor(method)

internal fun descriptor(field: FieldReference): String = DexFormatter.INSTANCE.getFieldDescriptor(field)

internal fun String.toJavaClassName() = removePrefix("L").removeSuffix(";").replace('/', '.')

internal fun Method.parameterRegister(index: Int): String {
    val receiver = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
    val preceding = parameterTypes.take(index).sumOf { if (it == "J" || it == "D") 2 else 1 }
    return "p${receiver + preceding}"
}

private fun MutableClass.compileMethod(
    name: String,
    parameters: List<ImmutableMethodParameter>,
    returnType: String,
    accessFlags: Int,
    registers: Int,
    smali: String,
): MutableMethod {
    val hasTryBlocks = ".catch" in smali
    val implementation = if (hasTryBlocks) {
        assembleImplementation(name, parameters, returnType, accessFlags, registers, smali)
    } else {
        MutableMethodImplementation(registers)
    }
    return ImmutableMethod(type, name, parameters, returnType, accessFlags, null, null, implementation)
        .toMutable().apply { if (!hasTryBlocks) addInstructionsWithLabels(0, smali) }
}

private fun MutableClass.assembleImplementation(
    name: String,
    parameters: List<ImmutableMethodParameter>,
    returnType: String,
    accessFlags: Int,
    registers: Int,
    smali: String,
): MutableMethodImplementation {
    val signature = "$name(${parameters.joinToString("") { it.type }})$returnType"
    val source = """
        .class public $type
        .super Ljava/lang/Object;
        .method ${AccessFlags.formatAccessFlagsForMethod(accessFlags)} $signature
            .registers $registers
            $smali
        .end method
    """.trimIndent()
    val method = DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(assembleDex(listOf(source))))
        .classes.single().methods.single()
    return MutableMethodImplementation(checkNotNull(method.implementation) { "$type->$signature has no body" })
}

internal fun MutableClass.addMethod(
    name: String,
    parameters: List<String>,
    returnType: String,
    accessFlags: Int,
    registers: Int,
    smali: String,
): MutableMethod = compileMethod(
    name,
    parameters.map { ImmutableMethodParameter(it, null, null) },
    returnType,
    accessFlags,
    registers,
    smali,
).also(methods::add)

internal fun MutableClass.addStaticMethod(
    name: String,
    parameters: List<String>,
    returnType: String,
    registers: Int,
    smali: String,
): String = descriptor(
    addMethod(name, parameters, returnType, AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, registers, smali),
)

internal fun MutableClass.replaceStub(name: String, registers: Int, smali: String) {
    val stub = methods.single { it.name == name }
    methods.remove(stub)
    addStaticMethod(name, stub.parameterTypes.map(CharSequence::toString), stub.returnType, registers, smali)
}

internal fun MutableClass.replaceMethod(name: String, registers: Int, smali: String) {
    val original = methods.single { it.name == name }
    methods.remove(original)
    methods.add(
        compileMethod(
            name,
            original.parameters.map { ImmutableMethodParameter.of(it) },
            original.returnType,
            original.accessFlags,
            registers,
            smali,
        ),
    )
}

internal fun MutableClass.addDefaultConstructor() {
    if (methods.any { it.name == "<init>" && it.parameterTypes.isEmpty() }) return
    methods.add(
        compileMethod(
            "<init>",
            emptyList(),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.CONSTRUCTOR.value,
            1,
            "invoke-direct {p0}, Ljava/lang/Object;-><init>()V\nreturn-void",
        ),
    )
}
