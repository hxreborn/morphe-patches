/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object RoundedShapeFingerprint : Fingerprint(strings = listOf("RoundedCornerShape(topStart = "))
internal object DpCornerSizeFingerprint : Fingerprint(strings = listOf("CornerSize(size = ", ".dp)"))
internal object GraphicsLayerElementFingerprint : Fingerprint(strings = listOf("GraphicsLayerElement(scaleX="))

internal fun paddingElementFingerprint(modifierElementType: String) = Fingerprint(
    name = "<init>",
    parameters = listOf("F", "F", "F", "F"),
    strings = listOf("Padding must be non-negative"),
    custom = { _, classDef -> classDef.superclass == modifierElementType },
)

internal fun sizeElementFingerprint(modifierElementType: String) = Fingerprint(
    name = "<init>",
    parameters = listOf("F", "F", "F", "F", "Z"),
    custom = { _, classDef -> classDef.superclass == modifierElementType },
)

internal object BackdropDrawContentFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(parameters = emptyList(), returnType = "L", location = MatchAfterImmediately()),
    ),
    parameters = emptyList(),
    returnType = "V",
    custom = { method, classDef ->
        classDef.methods.any { it.parameterTypes.take(4) == listOf("J", "J", "J", "J") } &&
            method.implementation?.instructions?.firstOrNull()?.opcode == Opcode.IGET_OBJECT
    },
)

internal object BackdropCoordinatesFingerprint : Fingerprint(strings = listOf("layouts are not part of the same hierarchy"))

internal fun backdropCallbackFingerprint(argumentType: String) = Fingerprint(
    parameters = listOf(argumentType),
    returnType = "V",
    filters = listOf(methodCall(parameters = listOf("Ljava/lang/Object;"), returnType = "Ljava/lang/Object;")),
    custom = { method, _ ->
        method.implementation?.instructions?.count {
            it.getReference<MethodReference>() != null
        } == 1
    },
)

internal fun backdropElementFingerprint(nodeType: String) = Fingerprint(
    parameters = emptyList(),
    filters = listOf(newInstance(nodeType)),
)

internal fun backdropCanvasFingerprint(canvasType: String) = Fingerprint(
    name = "<init>",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.interfaces.contains(canvasType) && classDef.fields.count { it.type == "Landroid/graphics/Canvas;" } == 1
    },
)
