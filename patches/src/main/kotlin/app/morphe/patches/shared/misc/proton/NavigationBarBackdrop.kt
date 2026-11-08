/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val NAVIGATION_BAR_BACKDROP_CLASS = "${PROTON_EXTENSION_PACKAGE}NavigationBarBackdrop;"
private const val NATIVE_CANVAS = "Landroid/graphics/Canvas;"
private const val DRAW_NODE_KIND = 4

internal fun BytecodePatchContext.installNavigationBarBackdrop(modifierType: String) {
    val drawMatch = BackdropDrawContentFingerprint.matchSingle()
    val draw = drawMatch.method
    val scopeType = draw.definingClass
    val (scopeRead, contextRead, canvasCall) = drawMatch.instructionMatches
    val scopeField = scopeRead.instruction.getReference<FieldReference>()!!
    val contextField = contextRead.instruction.getReference<FieldReference>()!!
    val contextType = contextField.type
    val canvasGetter = canvasCall.instruction.getReference<MethodReference>()!!
    check(contextField.definingClass == scopeField.type && canvasGetter.definingClass == contextType)
    val canvasClass = backdropCanvasFingerprint(canvasGetter.returnType).matchSingle().originalClassDef
    val canvasField = canvasClass.fields.single { it.type == NATIVE_CANVAS }
    check(!AccessFlags.FINAL.isSet(canvasField.accessFlags))
    val sizeGetter = classDefBy(scopeType).methods.flatMap { method ->
        method.implementation?.instructions?.mapNotNull { it.getReference<MethodReference>() }?.toList().orEmpty()
    }.distinctBy(::descriptor).single {
        it.definingClass == contextType && it.parameterTypes.isEmpty() && it.returnType == "J"
    }
    listOf(scopeField, contextField, canvasField).forEach { field ->
        mutableClassDefBy(field.definingClass).fields.single { it.name == field.name }.apply {
            accessFlags = accessFlags and (AccessFlags.PRIVATE.value or AccessFlags.PROTECTED.value).inv() or AccessFlags.PUBLIC.value
        }
    }
    listOf(scopeType, scopeField.type, contextType, canvasClass.type).forEach { type ->
        mutableClassDefBy(type).apply { accessFlags = accessFlags or AccessFlags.PUBLIC.value }
    }
    val extension = mutableClassDefBy(NAVIGATION_BAR_BACKDROP_CLASS)
    val canvasPath = """
        check-cast p0, $scopeType
        iget-object v0, p0, ${descriptor(scopeField)}
        iget-object v0, v0, ${descriptor(contextField)}
        invoke-virtual { v0 }, ${descriptor(canvasGetter)}
        move-result-object v0
        check-cast v0, ${canvasClass.type}
    """
    extension.replaceStub("canvas", 2, """
        $canvasPath
        iget-object v0, v0, ${descriptor(canvasField)}
        return-object v0
    """)
    extension.replaceStub("replaceCanvas", 4, """
        $canvasPath
        iget-object v1, v0, ${descriptor(canvasField)}
        iput-object p1, v0, ${descriptor(canvasField)}
        return-object v1
    """)
    extension.replaceStub("drawContent", 1, """
        check-cast p0, $scopeType
        invoke-virtual { p0 }, ${descriptor(draw)}
        return-void
    """)
    extension.replaceStub("size", 3, """
        check-cast p0, $scopeType
        iget-object v0, p0, ${descriptor(scopeField)}
        iget-object v0, v0, ${descriptor(contextField)}
        invoke-virtual { v0 }, ${descriptor(sizeGetter)}
        move-result-wide v0
        return-wide v0
    """)
    val coordinates = BackdropCoordinatesFingerprint.matchSingle().originalClassDef
    val position = coordinates.methods.single { method ->
        method.returnType == "J" && method.parameterTypes.size == 2 && method.parameterTypes[1] == "J" &&
            method.parameterTypes[0] != coordinates.type &&
            method.implementation?.instructions?.any { it.opcode == Opcode.INSTANCE_OF } == true
    }
    mutableClassDefBy(coordinates.type).apply { accessFlags = accessFlags or AccessFlags.PUBLIC.value }
    val coordinateType = position.parameterTypes.first().toString()
    val attached = classDefBy(coordinateType).methods.single {
        it.parameterTypes.isEmpty() && it.returnType == "Z"
    }
    extension.replaceStub("isAttached", 1, """
        if-eqz p0, :detached
        check-cast p0, $coordinateType
        invoke-interface { p0 }, ${descriptor(attached)}
        move-result p0
        return p0
        :detached
        const/4 p0, 0x0
        return p0
    """)
    extension.replaceStub("relativePosition", 4, """
        check-cast p0, ${coordinates.type}
        check-cast p1, $coordinateType
        const-wide/16 v0, 0x0
        invoke-virtual { p0, p1, v0, v1 }, ${descriptor(position)}
        move-result-wide v0
        return-wide v0
    """)
    val coordinator = draw.instructions.mapNotNull { it.getReference<MethodReference>() }.distinctBy(::descriptor).single {
        it.returnType == coordinates.type && it.parameterTypes.size == 2 && it.parameterTypes[1] == "I"
    }
    val drawNodeField = classDefBy(scopeType).fields.single { field ->
        field.type != scopeField.type && draw.instructions.any { it.getReference<FieldReference>() == field }
    }
    mutableClassDefBy(scopeType).fields.single { it.name == drawNodeField.name }.apply {
        accessFlags = accessFlags and (AccessFlags.PRIVATE.value or AccessFlags.PROTECTED.value).inv() or AccessFlags.PUBLIC.value
    }
    extension.replaceStub("coordinates", 3, """
        check-cast p0, $scopeType
        iget-object v0, p0, ${descriptor(drawNodeField)}
        const/4 v1, $DRAW_NODE_KIND
        invoke-static { v0, v1 }, ${descriptor(coordinator)}
        move-result-object v0
        return-object v0
    """)
    val callback = backdropCallbackFingerprint(scopeType).matchSingle()
    val (invocation) = callback.instructionMatches
    val host = invocation.instruction.getReference<MethodReference>()!!
    check(callback.method.instructions.elementAtOrNull(invocation.index + 1)?.opcode !in setOf(Opcode.MOVE_RESULT_OBJECT, Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_WIDE))
    val target = mutableClassDefBy("${NAVIGATION_BAR_BACKDROP_CLASS.dropLast(1)}\$DrawCallback;")
    target.interfaces.add(host.definingClass)
    target.addMethod(host.name, listOf("Ljava/lang/Object;"), "Ljava/lang/Object;", AccessFlags.PUBLIC.value, 2, """
        invoke-virtual { p0, p1 }, ${target.type}->accept(Ljava/lang/Object;)V
        const/4 p1, 0x0
        return-object p1
    """)
    val element = backdropElementFingerprint(callback.method.definingClass).matchSingle().originalClassDef
    val constructor = element.methods.single { it.name == "<init>" && it.parameterTypes == listOf(host.definingClass) }
    mutableClassDefBy(element.type).apply { accessFlags = accessFlags or AccessFlags.PUBLIC.value }
    val then = classDefBy(modifierType).methods.single { it.parameterTypes == listOf(modifierType) && it.returnType == modifierType }
    extension.replaceStub("appendModifiers", 3, """
        check-cast p0, $modifierType
        check-cast p1, ${host.definingClass}
        new-instance v0, ${element.type}
        invoke-direct { v0, p1 }, ${descriptor(constructor)}
        invoke-interface { p0, v0 }, ${descriptor(then)}
        move-result-object p0
        return-object p0
    """)
}
