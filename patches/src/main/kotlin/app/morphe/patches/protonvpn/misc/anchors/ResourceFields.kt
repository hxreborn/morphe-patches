/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.anchors

import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.value.IntEncodedValue

private const val APP_RESOURCE_PACKAGE = 0x7f

private var resourceIdsByField: Map<String, Long> = emptyMap()

private fun FieldReference.descriptor() = "$definingClass->$name"

internal val resourceFieldsPatch = bytecodePatch {
    execute {
        val resourceIds = HashMap<String, Long>()
        classDefForEach { classDef ->
            classDef.staticFields.forEach { field ->
                val value = (field.initialValue as? IntEncodedValue)?.value ?: return@forEach
                if (value ushr 24 == APP_RESOURCE_PACKAGE) resourceIds[field.descriptor()] = value.toLong()
            }
        }
        resourceIdsByField = resourceIds
    }
}

internal class ResourceFieldFilter(
    type: ResourceType,
    name: String,
    location: InstructionLocation,
) : OpcodesFilter(null as List<Opcode>?, location) {
    private val resolvedId by lazy { resourceId(type, name) }

    override fun matches(enclosingMethod: Method, instruction: Instruction): Boolean {
        if (!super.matches(enclosingMethod, instruction)) return false
        if (instruction is WideLiteralInstruction) return instruction.wideLiteral == resolvedId
        if (instruction.opcode != Opcode.SGET) return false
        val field = (instruction as ReferenceInstruction).reference as FieldReference
        return resourceIdsByField[field.descriptor()] == resolvedId
    }
}

internal fun resourceField(
    type: ResourceType,
    name: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
) = ResourceFieldFilter(type, name, location)
