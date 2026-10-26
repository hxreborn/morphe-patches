/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.jiagu

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.DEX_FILE_SIZE_OFFSET
import app.morphe.patches.shared.misc.smali.assembleDex
import app.morphe.patches.shared.writeInt
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.nio.ByteBuffer

private const val STUB_CLASS = "Lcom/stub/StubApp;"
private const val CONTEXT_FIELD = "d"
private const val DEBUG_INFO_KEPT_CLASS = "Lcom/tianyu/util/Configuration;"
private const val MAX_LAYOUT_ATTEMPTS = 16

private val dexOpcodes = Opcodes.forDexVersion(35)

internal fun JiaguDex.withLoader(loaderSmali: String): ByteArray {
    val stock = DexBackedDexFile(dexOpcodes, ByteBuffer.wrap(stub.apply { writeInt(DEX_FILE_SIZE_OFFSET, size) }))
    val loader = loaderMethod(loaderSmali)

    var padding = 0
    repeat(MAX_LAYOUT_ATTEMPTS) {
        val built = stock.writeWithLoader(loader, padding)
        if (built.size == stubSize) return withStub(built)

        padding += stubSize - built.size
        if (padding < 0) {
            throw PatchException("The loader does not fit the Jiagu stub: ${built.size} > $stubSize bytes")
        }
    }
    throw PatchException("The Jiagu stub size did not converge to $stubSize bytes")
}

private fun loaderMethod(smali: String): Method {
    val source = ".class public $STUB_CLASS\n.super Ljava/lang/Object;\n\n$smali\n"
    val method = DexBackedDexFile(dexOpcodes, ByteBuffer.wrap(assembleDex(listOf(source)))).classes.single().methods.singleOrNull()
        ?: throw PatchException("The loader must define exactly one method")

    if (!AccessFlags.STATIC.isSet(method.accessFlags) || method.parameters.isNotEmpty() || method.returnType != "V") {
        throw PatchException("The loader must be a static method with no parameters returning void")
    }
    return ImmutableMethod.of(method)
}

private fun DexFile.writeWithLoader(loader: Method, padding: Int): ByteArray {
    val pool = DexPool(dexOpcodes)
    classes.forEach { classDef ->
        val methods = classDef.methods.map { method ->
            when {
                classDef.type == DEBUG_INFO_KEPT_CLASS -> method
                classDef.type == STUB_CLASS && method.name == "attachBaseContext" ->
                    method.withoutDebugInfo().callingAfterContextStore(loader)
                else -> method.withoutDebugInfo()
            }
        }
        val loaders = if (classDef.type == STUB_CLASS) listOf(loader.paddedBy(padding)) else emptyList()

        pool.internClass(
            ImmutableClassDef(
                classDef.type,
                classDef.accessFlags,
                classDef.superclass,
                classDef.interfaces,
                null,
                classDef.annotations,
                classDef.fields.map(ImmutableField::of),
                methods + loaders,
            ),
        )
    }
    return MemoryDataStore().also(pool::writeTo).data
}

private fun Method.withImplementation(
    implementation: MethodImplementation?,
    parameters: List<ImmutableMethodParameter> = this.parameters.map(ImmutableMethodParameter::of),
) = ImmutableMethod(definingClass, name, parameters, returnType, accessFlags, annotations, hiddenApiRestrictions, implementation)

private fun Method.withoutDebugInfo() = withImplementation(
    implementation?.let { ImmutableMethodImplementation(it.registerCount, it.instructions, it.tryBlocks, emptyList()) },
    parameters.map { ImmutableMethodParameter(it.type, it.annotations, null) },
)

private fun Instruction.isContextStore() =
    opcode == Opcode.SPUT_OBJECT &&
        ((this as ReferenceInstruction).reference as FieldReference).let {
            it.definingClass == STUB_CLASS && it.name == CONTEXT_FIELD
        }

private fun Method.callingAfterContextStore(target: Method): Method {
    val code = MutableMethodImplementation(implementation!!)
    val stores = code.instructions.indices.filter { code.instructions[it].isContextStore() }
    val store = stores.singleOrNull()
        ?: throw PatchException("Expected one context store in $STUB_CLASS->$name, found ${stores.size}")

    code.addInstruction(store + 1, BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, target))
    return withImplementation(code)
}

private fun Method.paddedBy(length: Int): Method {
    val code = MutableMethodImplementation(implementation!!)
    val paddingRegister = code.registerCount

    code.addInstruction(
        0,
        BuilderInstruction21c(Opcode.CONST_STRING, paddingRegister, ImmutableStringReference("A".repeat(length))),
    )
    return withImplementation(
        object : MethodImplementation by code {
            override fun getRegisterCount() = paddingRegister + 1
        },
    )
}
