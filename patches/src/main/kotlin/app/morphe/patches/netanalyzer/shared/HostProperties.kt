/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val TO_STRING_METHOD = "toString"
private const val TO_STRING_OPENING = '('
private const val TO_STRING_SEPARATOR = ", "
private const val TO_STRING_ASSIGNMENT = "="

internal fun ClassDef.propertyFields(): Map<String, String> {
    val toString = methods.singleOrNull { it.name == TO_STRING_METHOD && it.parameterTypes.isEmpty() }
        ?: throw PatchException("$type declares no $TO_STRING_METHOD")
    val fields = LinkedHashMap<String, String>()
    var property: String? = null
    toString.implementation!!.instructions.forEach { instruction ->
        instruction.toStringLabel()?.let { label ->
            property = label.substringAfterLast(TO_STRING_OPENING).substringAfterLast(TO_STRING_SEPARATOR)
                .removeSuffix(TO_STRING_ASSIGNMENT)
        }
        val field = instruction.getReference<FieldReference>()
        val label = property
        if (field != null && field.definingClass == type && label != null) {
            fields.putIfAbsent(label, field.name)
        }
    }
    return fields
}

private fun Instruction.toStringLabel(): String? {
    if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) return null
    val raw = getReference<StringReference>()!!.string
    return if (raw.endsWith(TO_STRING_ASSIGNMENT)) raw else decryptString(raw)
}
