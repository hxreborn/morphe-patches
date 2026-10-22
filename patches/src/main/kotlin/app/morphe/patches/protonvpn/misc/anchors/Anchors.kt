/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.anchors

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal open class ToStringFingerprint(prefix: String) : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf(string(prefix, StringComparisonType.STARTS_WITH)),
)

internal object KotlinUnitToStringFingerprint : ToStringFingerprint("kotlin.Unit")

internal val BytecodePatchContext.unitInstanceField
    get() = KotlinUnitToStringFingerprint.originalClassDef.let { unit ->
        "${unit.type}->${unit.staticFields.single { it.type == unit.type }.name}:${unit.type}"
    }

internal object ServerGroupsMainScreenStateToStringFingerprint :
    ToStringFingerprint("ServerGroupsMainScreenState(selectedFilter=")

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

private fun Instruction.reference() = (this as? ReferenceInstruction)?.reference

internal fun BytecodePatchContext.findPropertyField(toString: Fingerprint, property: String): FieldReference {
    toString.matchSingle()
    val instructions = toString.originalMethod.implementation!!.instructions.toList()
    val labels = instructions.mapNotNull { (it.reference() as? StringReference)?.string }.filter { it.endsWith("=") }
    val labelIndex = labels.indexOfFirst { label ->
        label.endsWith("$property=") &&
            label.dropLast(property.length + 1).let { it.endsWith("(") || it.endsWith(", ") }
    }
    if (labelIndex < 0) throw PatchException("No $property in ${toString.originalMethod}")
    val fields = instructions
        .filter { it.opcode.name.startsWith("iget") }
        .mapNotNull { it.reference() as? FieldReference }
        .filter { it.definingClass == toString.originalClassDef.type }
        .distinct()
    if (fields.size != labels.size) {
        throw PatchException("${toString.originalMethod} reads ${fields.size} fields for ${labels.size} labels")
    }
    return fields[labelIndex]
}

private fun BytecodePatchContext.propertyGetters(toString: Fingerprint, field: FieldReference) =
    toString.originalClassDef.methods.filter { method ->
        method.parameterTypes.isEmpty() && !AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.implementation?.instructions?.toList()?.let { body ->
                body.size == 2 && body[0].reference() == field
            } == true
    }

internal fun BytecodePatchContext.propertyGetterCall(toString: Fingerprint, property: String): InstructionFilter {
    val field = findPropertyField(toString, property)
    val getters = propertyGetters(toString, field)
    if (getters.isEmpty()) throw PatchException("No getter for $field")
    return anyInstruction(*getters.map { methodCall(it) }.toTypedArray())
}

internal fun BytecodePatchContext.propertyRead(toString: Fingerprint, property: String): InstructionFilter {
    val field = findPropertyField(toString, property)
    val read = fieldAccess(field, listOf(Opcode.IGET, Opcode.IGET_OBJECT, Opcode.IGET_BOOLEAN))
    val getters = propertyGetters(toString, field)
    return anyInstruction(read, *getters.map { methodCall(it) }.toTypedArray())
}
