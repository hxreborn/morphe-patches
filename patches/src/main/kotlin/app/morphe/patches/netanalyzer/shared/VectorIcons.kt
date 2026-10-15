/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val REFERENCE_ICON = "leak_add"
private const val ICON_METHOD = "hxIcon"
private const val ICON_FIELD = "hxIconCache"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val ARRAY_LIST_TYPE = "Ljava/util/ArrayList;"
private const val TO_STRING = "toString"
private const val CONSTRUCTOR = "<init>"
private const val FLOAT_TYPE = "F"
private const val WIDE_TYPE = "J"
private const val BUILDER_FIRST_ARGUMENT = 1
private const val WIDE_REGISTERS = 2
private const val ICON_REGISTERS = 12

private val VECTOR_BUILDER_PARAMETERS = listOf(STRING_TYPE, "F", "F", "F", "F", "J", "I", "Z", "I")

private val NODE_PREFIXES = mapOf(
    'M' to "MoveTo(x=",
    'm' to "RelativeMoveTo(dx=",
    'L' to "LineTo(x=",
    'l' to "RelativeLineTo(dx=",
    'H' to "HorizontalTo(x=",
    'h' to "RelativeHorizontalTo(dx=",
    'V' to "VerticalTo(y=",
    'v' to "RelativeVerticalTo(dy=",
    'Q' to "QuadTo(x1=",
    'q' to "RelativeQuadTo(dx1=",
    'T' to "ReflectiveQuadTo(x=",
    't' to "RelativeReflectiveQuadTo(dx=",
)

private const val CLOSE = 'Z'

private class VectorApi(
    val builderInit: MethodReference,
    val builderRegisters: Int,
    val builderArguments: List<Pair<Int, String>>,
    val brushInit: MethodReference,
    val black: FieldReference,
    val addPath: MethodReference,
    val build: MethodReference,
    val close: FieldReference,
    val nodes: Map<Char, MethodReference>,
)

private var vectorApi: VectorApi? = null
private val iconMethods = HashMap<String, Method>()

internal val vectorIconsPatch = bytecodePatch {
    execute {
        vectorApi = null
        iconMethods.clear()
    }
}

context(context: BytecodePatchContext)
internal fun imageVectorType(): String = vectorApi().build.returnType

context(context: BytecodePatchContext)
internal fun iconMethod(classDef: MutableClass, name: String, pathData: String): Method =
    iconMethods.getOrPut("${classDef.type}$name") {
        val api = vectorApi()
        val index = iconMethods.size
        val cache = ImmutableField(
            classDef.type,
            "$ICON_FIELD$index",
            api.build.returnType,
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            null,
            null,
            null,
        ).toMutable()
        classDef.fields.add(cache)
        val cacheReference = "${classDef.type}->${cache.name}:${cache.type}"
        val smali = buildString {
            appendLine("sget-object v0, $cacheReference")
            appendLine("if-nez v0, :done")
            appendLine("new-instance v0, ${api.builderInit.definingClass}")
            appendLine("const-string v1, \"$name\"")
            api.builderArguments.forEach { (register, literal) -> appendLine(literal.format(register)) }
            appendLine("invoke-direct/range { v0 .. v${api.builderRegisters - 1} }, ${api.builderInit.descriptor()}")
            appendLine("new-instance v1, $ARRAY_LIST_TYPE")
            appendLine("invoke-direct { v1 }, $ARRAY_LIST_TYPE-><init>()V")
            pathCommands(pathData).forEach { (command, values) ->
                if (command == CLOSE) {
                    appendLine("sget-object v2, ${api.close.definingClass}->${api.close.name}:${api.close.type}")
                } else {
                    val node = api.nodes.getValue(command)
                    appendLine("new-instance v2, ${node.definingClass}")
                    values.forEachIndexed { value, number ->
                        appendLine("const v${value + 3}, ${number.toRawBits()}")
                    }
                    appendLine("invoke-direct/range { v2 .. v${values.size + 2} }, ${node.descriptor()}")
                }
                appendLine("invoke-virtual { v1, v2 }, $ARRAY_LIST_TYPE->add(Ljava/lang/Object;)Z")
            }
            appendLine("new-instance v2, ${api.brushInit.definingClass}")
            appendLine("sget-wide v3, ${api.black.definingClass}->${api.black.name}:${api.black.type}")
            appendLine("invoke-direct { v2, v3, v4 }, ${api.brushInit.descriptor()}")
            appendLine("invoke-static { v0, v1, v2 }, ${api.addPath.descriptor()}")
            appendLine("invoke-virtual { v0 }, ${api.build.descriptor()}")
            appendLine("move-result-object v0")
            appendLine("sput-object v0, $cacheReference")
            appendLine(":done")
            appendLine("return-object v0")
        }
        ImmutableMethod(
            classDef.type,
            "$ICON_METHOD$index",
            emptyList(),
            api.build.returnType,
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(ICON_REGISTERS),
        ).toMutable().apply { addInstructionsWithLabels(0, smali) }.also { classDef.methods.add(it) }
    }

context(context: BytecodePatchContext)
private fun vectorApi(): VectorApi = vectorApi ?: with(context) {
    val reference = Fingerprint(
        filters = listOf(
            decryptedString(REFERENCE_ICON),
            methodCall(name = CONSTRUCTOR, parameters = VECTOR_BUILDER_PARAMETERS, opcode = Opcode.INVOKE_DIRECT_RANGE),
        ),
    ).matchSingle()
    val instructions = reference.method.instructions.toList()
    val initIndex = reference.instructionMatches.last().index
    val builderInit = instructions[initIndex].getReference<MethodReference>()!!
    val builderType = builderInit.definingClass
    val range = instructions[initIndex] as RegisterRangeInstruction
    var offset = BUILDER_FIRST_ARGUMENT
    val builderArguments = builderInit.parameterTypes.map { it.toString() }.mapNotNull { type ->
        val register = offset
        offset += if (type == WIDE_TYPE) WIDE_REGISTERS else 1
        if (type == STRING_TYPE) null else register to literalFor(instructions.subList(0, initIndex), range.startRegister + register, type)
    }
    val after = instructions.drop(initIndex)
    fun call(predicate: (MethodReference) -> Boolean) = after.firstNotNullOfOrNull { instruction ->
        instruction.getReference<MethodReference>()?.takeIf(predicate)
    } ?: throw PatchException("$REFERENCE_ICON builder in ${reference.method} lacks an expected call")
    val addPath = call { it.definingClass == builderType && it.parameterTypes.size == 3 && it.returnType == "V" }
    val brushType = addPath.parameterTypes.last().toString()
    val brushInit = call { it.definingClass == brushType && it.name == CONSTRUCTOR }
    val black = after.first { it.opcode == Opcode.SGET_WIDE }.getReference<FieldReference>()!!
    val build = call { it.definingClass == builderType && it.parameterTypes.isEmpty() && it.returnType.startsWith("L") }
    val pathBuilderType = after.first {
        it.opcode == Opcode.NEW_INSTANCE && it.getReference<TypeReference>()!!.type !in setOf(brushType, builderType)
    }.getReference<TypeReference>()!!.type
    val closeMethod = call { it.definingClass == pathBuilderType && it.parameterTypes.isEmpty() && it.returnType == "V" }
    val close = classDefBy(pathBuilderType).methods.single {
        it.name == closeMethod.name && it.parameterTypes.isEmpty() && it.returnType == "V"
    }.implementation!!.instructions.first { it.opcode == Opcode.SGET_OBJECT }.getReference<FieldReference>()!!
    val nodes = NODE_PREFIXES.mapValues { (_, prefix) ->
        val node = Fingerprint(name = TO_STRING, returnType = STRING_TYPE, filters = listOf(string(prefix))).matchSingle()
        classDefBy(node.originalMethod.definingClass).methods.single { method ->
            method.name == CONSTRUCTOR && method.parameterTypes.isNotEmpty() &&
                method.parameterTypes.all { it == FLOAT_TYPE }
        }
    }
    VectorApi(builderInit, range.registerCount, builderArguments, brushInit, black, addPath, build, close, nodes)
        .also { vectorApi = it }
}

private fun literalFor(before: List<Instruction>, register: Int, type: String): String {
    val write = before.last { it is OneRegisterInstruction && it.registerA == register && it.opcode.setsRegister() }
    return when {
        write is WideLiteralInstruction && type == WIDE_TYPE -> "const-wide v%d, ${write.wideLiteral}"
        write is NarrowLiteralInstruction -> "const v%d, ${write.narrowLiteral}"
        write is TwoRegisterInstruction && !write.opcode.setsWideRegister() ->
            literalFor(before.subList(0, before.indexOf(write)), write.registerB, type)
        else -> throw PatchException("Vector builder argument v$register is not a literal: ${write.opcode}")
    }
}

private fun pathCommands(pathData: String): List<Pair<Char, List<Float>>> {
    val tokens = Regex("[A-Za-z]|-?\\d*\\.?\\d+").findAll(pathData).map { it.value }.toList()
    val commands = mutableListOf<Pair<Char, List<Float>>>()
    var index = 0
    var command = ' '
    while (index < tokens.size) {
        if (tokens[index][0].isLetter()) {
            command = tokens[index++][0]
        }
        if (command.uppercaseChar() == CLOSE) {
            commands += command.uppercaseChar() to emptyList()
            continue
        }
        val arity = ARITY[command.uppercaseChar()] ?: throw PatchException("Unsupported path command $command")
        commands += command to tokens.subList(index, index + arity).map(String::toFloat)
        index += arity
    }
    return commands
}

private val ARITY = mapOf('M' to 2, 'L' to 2, 'H' to 1, 'V' to 1, 'Q' to 4, 'T' to 2)
