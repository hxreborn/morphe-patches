/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.anchors

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal open class ToStringFingerprint(prefix: String) : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf(prefix),
)

internal object KotlinUnitToStringFingerprint : ToStringFingerprint("kotlin.Unit")

internal val BytecodePatchContext.unitInstanceField
    get() = KotlinUnitToStringFingerprint.originalClassDef.let { unit ->
        "${unit.type}->${unit.staticFields.single { it.type == unit.type }.name}:${unit.type}"
    }

internal object ServerGroupsMainScreenStateToStringFingerprint :
    ToStringFingerprint("ServerGroupsMainScreenState(selectedFilter=")

internal object ServerGroupsMainScreenStateFingerprint : Fingerprint(
    classFingerprint = ServerGroupsMainScreenStateToStringFingerprint,
    name = "<init>",
    parameters = listOf("L", "Ljava/util/List;", "Ljava/util/List;"),
)

internal object VpnUserToStringFingerprint : ToStringFingerprint("VpnUser(userId=")

internal object VpnUserIsFreeUserFingerprint : Fingerprint(
    classFingerprint = VpnUserToStringFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Integer;", name = "intValue"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
    ),
)

internal val BytecodePatchContext.vpnUserType
    get() = VpnUserToStringFingerprint.originalClassDef.type

internal fun BytecodePatchContext.isFreeUserCall(
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = methodCall(VpnUserIsFreeUserFingerprint.originalMethod, location)

private const val EXTENSION_MEMBERS_CLASS = "Lapp/hxreborn/extension/protonvpn/Members;"

internal fun BytecodePatchContext.setExtensionMember(member: String, value: String) =
    mutableClassDefBy(EXTENSION_MEMBERS_CLASS).methods.single { it.name == member }.returnEarly(value)

internal fun String.toJavaClassName() = substring(1, length - 1).replace('/', '.')

private fun Instruction.reference() = (this as? ReferenceInstruction)?.reference

internal fun BytecodePatchContext.findPropertyGetter(toString: Fingerprint, property: String): Method {
    toString.matchSingle()
    val instructions = toString.originalMethod.implementation!!.instructions.toList()
    val labelIndex = instructions.indexOfFirst { instruction ->
        val label = (instruction.reference() as? StringReference)?.string ?: return@indexOfFirst false
        label.endsWith("$property=") &&
            label.dropLast(property.length + 1).let { it.endsWith("(") || it.endsWith(", ") }
    }
    if (labelIndex < 0) throw PatchException("No $property in ${toString.originalMethod}")

    val valueAppend = instructions.drop(labelIndex + 1)
        .filter { (it.reference() as? MethodReference)?.name == "append" }
        .getOrNull(1) as? FiveRegisterInstruction
        ?: throw PatchException("No appended $property in ${toString.originalMethod}")
    val field = instructions.take(instructions.indexOf(valueAppend as Instruction)).lastOrNull {
        it is TwoRegisterInstruction && it.registerA == valueAppend.registerD && it.reference() is FieldReference
    }?.reference() as? FieldReference
        ?: throw PatchException("No field read for $property in ${toString.originalMethod}")

    return toString.originalClassDef.methods.singleOrNull { method ->
        method.parameterTypes.isEmpty() && !AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.implementation?.instructions?.toList()?.let { body ->
                body.size == 2 && body[0].reference() == field
            } == true
    } ?: throw PatchException("No getter for $field")
}
