/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val FLOATING_NAVIGATION_BAR_CLASS = "${PROTON_EXTENSION_PACKAGE}FloatingNavigationBar;"
private const val FLOATING_MODIFIER_REGISTERS = 19
private const val HORIZONTAL_MARGIN_DP = 22f
private const val BOTTOM_MARGIN_DP = 22f
private const val CORNER_RADIUS_DP = 28f
private const val BAR_HEIGHT_DP = 56f
private const val UNSPECIFIED_SIZE = Float.NaN
private const val UNIT_SCALE = 1f
private const val CENTER_TRANSFORM_ORIGIN = 0x3f0000003f000000L
private const val PACKED_BLACK = -0x100000000000000L
private const val SHAPE_CORNER_COUNT = 4

internal fun BytecodePatchContext.injectFloatingNavigationBar(method: MutableMethod, callIndex: Int, fixedHeight: Boolean = false) {
    val call = method.getInstruction<RegisterRangeInstruction>(callIndex)
    val reference = call.getReference<MethodReference>()!!
    val modifierType = reference.parameterTypes.first().toString()
    if (reference.parameterTypes[1].toString() != "J") {
        throw PatchException("Expected navigation background after modifier in ${descriptor(reference)}")
    }
    installFloatingModifier(modifierType, fixedHeight)
    installNavigationBarBackdrop(modifierType)
    if (call.opcode != Opcode.INVOKE_STATIC_RANGE) {
        throw PatchException("Expected static navigation call in ${descriptor(method)}")
    }
    val parameters = reference.parameterTypes.map(CharSequence::toString)
    val composerType = parameters.dropLastWhile { it == "I" }.last()
    val composerRegister = parameters.take(parameters.indexOf(composerType)).sumOf { if (it == "J" || it == "D") 2 else 1 }
    val parameterRegisters = parameters.sumOf { if (it == "J" || it == "D") 2 else 1 }
    val bridge = mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS).addStaticMethod(
        reference.name,
        parameters,
        "V",
        parameterRegisters + 2,
        """
            move-object/from16 v0, p0
            move-object/from16 v1, p$composerRegister
            invoke-static { v0, v1 }, $FLOATING_NAVIGATION_BAR_CLASS->modifier(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object p0
            check-cast p0, $modifierType
            invoke-static/range { p1 .. p2 }, $FLOATING_NAVIGATION_BAR_CLASS->background(J)J
            move-result-wide p1
            invoke-static/range { p0 .. p${parameterRegisters - 1} }, ${descriptor(reference)}
            return-void
        """,
    )
    method.replaceInstruction(
        callIndex,
        "invoke-static/range { v${call.startRegister} .. v${call.startRegister + call.registerCount - 1} }, $bridge",
    )
}

private fun BytecodePatchContext.installFloatingModifier(modifierType: String, fixedHeight: Boolean) {
    val shape = RoundedShapeFingerprint.matchSingle().originalClassDef
    val corner = DpCornerSizeFingerprint.matchSingle().originalClassDef
    val layer = GraphicsLayerElementFingerprint.matchSingle().originalClassDef
    val padding = paddingElementFingerprint(layer.superclass!!).matchSingle().originalClassDef
    val cornerConstructor = corner.methods.single { it.name == "<init>" && it.parameterTypes == listOf("F") }
    val cornerType = corner.interfaces.single()
    val shapeConstructor = (shape.methods + classDefByOrNull(shape.superclass!!)?.methods?.toList().orEmpty()).firstOrNull {
        it.name == "<init>" && it.parameterTypes == List(SHAPE_CORNER_COUNT) { cornerType }
    } ?: throw PatchException("No four-corner constructor for ${shape.type}")
    val layerConstructor = layer.methods.single { it.name == "<init>" }
    val shapeType = shape.interfaces.singleOrNull() ?: classDefBy(shape.superclass!!).interfaces.single()
    val layerParameters = listOf("F", "F", "F", "F", "F", "J", shapeType, "Z", "J", "J")
    if (layerConstructor.parameterTypes.map(CharSequence::toString) != layerParameters) {
        throw PatchException("Unexpected graphics layer constructor ${descriptor(layerConstructor)}")
    }
    val paddingConstructor = padding.methods.single { it.name == "<init>" }
    val then = classDefBy(modifierType).methods.single {
        it.parameterTypes == listOf(modifierType) && it.returnType == modifierType
    }
    listOf(shape.type, corner.type, layer.type, padding.type).forEach { type ->
        mutableClassDefBy(type).accessFlags = mutableClassDefBy(type).accessFlags or AccessFlags.PUBLIC.value
    }
    val heightModifier = if (fixedHeight) {
        val size = sizeElementFingerprint(layer.superclass!!).matchSingle().originalClassDef
        val constructor = size.methods.single { it.name == "<init>" && it.parameterTypes.last() == "Z" }
        mutableClassDefBy(size.type).accessFlags = mutableClassDefBy(size.type).accessFlags or AccessFlags.PUBLIC.value
        """
            new-instance v0, ${size.type}
            const v1, ${UNSPECIFIED_SIZE.toRawBits()}
            const v2, ${BAR_HEIGHT_DP.toRawBits()}
            move v3, v1
            move v4, v2
            const/4 v5, 0x1
            invoke-direct/range { v0 .. v5 }, ${descriptor(constructor)}
            move-object/from16 v1, v17
            invoke-interface { v1, v0 }, ${descriptor(then)}
            move-result-object v17
        """
    } else ""
    mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS).replaceStub(
        "floatingModifier",
        FLOATING_MODIFIER_REGISTERS,
        """
            move-object/from16 v17, p0
            check-cast v17, $modifierType
            new-instance v0, ${padding.type}
            const v1, ${HORIZONTAL_MARGIN_DP.toRawBits()}
            const/4 v2, 0x0
            move v3, v1
            const v4, ${BOTTOM_MARGIN_DP.toRawBits()}
            invoke-direct { v0, v1, v2, v3, v4 }, ${descriptor(paddingConstructor)}
            move-object/from16 v1, v17
            invoke-interface { v1, v0 }, ${descriptor(then)}
            move-result-object v17
            $heightModifier
            new-instance v0, ${corner.type}
            const v1, ${CORNER_RADIUS_DP.toRawBits()}
            invoke-direct { v0, v1 }, ${descriptor(cornerConstructor)}
            new-instance v8, ${shape.type}
            invoke-direct { v8, v0, v0, v0, v0 }, ${descriptor(shapeConstructor)}
            new-instance v0, ${layer.type}
            const v1, ${UNIT_SCALE.toRawBits()}
            move v2, v1
            move v3, v1
            const/4 v4, 0x0
            const/4 v5, 0x0
            const-wide v6, ${CENTER_TRANSFORM_ORIGIN}L
            const/4 v9, 0x1
            const-wide v10, ${PACKED_BLACK}L
            move-wide v12, v10
            invoke-direct/range { v0 .. v13 }, ${descriptor(layerConstructor)}
            move-object/from16 v1, v17
            invoke-interface { v1, v0 }, ${descriptor(then)}
            move-result-object v0
            return-object v0
        """,
    )
}

internal fun BytecodePatchContext.installMeasuredBottomInsets(modifierType: String) {
    val layer = GraphicsLayerElementFingerprint.matchSingle().originalClassDef
    val padding = paddingElementFingerprint(layer.superclass!!).matchSingle().originalClassDef
    val constructor = padding.methods.single { it.name == "<init>" }
    val then = classDefBy(modifierType).methods.single {
        it.parameterTypes == listOf(modifierType) && it.returnType == modifierType
    }
    val extension = mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS)
    val insetField = ImmutableField(
        FLOATING_NAVIGATION_BAR_CLASS, "bottomInsetDp", "F",
        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value, null, null, null,
    )
    extension.fields.add(insetField.toMutable())
    extension.replaceStub(
        "itemBottomPadding",
        2,
        """
            invoke-static {}, $FLOATING_NAVIGATION_BAR_CLASS->isEnabled()Z
            move-result v0
            if-eqz v0, :original
            sput p0, ${descriptor(insetField)}
            const/4 p0, 0x0
            :original
            return p0
        """,
    )
    extension.replaceStub(
        "outerInsets",
        8,
        """
            check-cast p0, $modifierType
            new-instance v0, ${padding.type}
            const/4 v1, 0x0
            const/4 v2, 0x0
            const/4 v3, 0x0
            sget v4, ${descriptor(insetField)}
            invoke-direct { v0, v1, v2, v3, v4 }, ${descriptor(constructor)}
            invoke-interface { p0, v0 }, ${descriptor(then)}
            move-result-object p0
            return-object p0
        """,
    )
}
