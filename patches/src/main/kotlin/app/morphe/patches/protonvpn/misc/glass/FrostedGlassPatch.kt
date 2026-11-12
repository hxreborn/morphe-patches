/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.glass

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.CORE_BRAND_COLORS
import app.morphe.patches.shared.misc.proton.BackdropDrawContentFingerprint
import app.morphe.patches.shared.misc.proton.BackdropCoordinatesFingerprint
import app.morphe.patches.shared.misc.proton.RoundedShapeFingerprint
import app.morphe.patches.shared.misc.proton.installNavigationBarBackdrop
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val MAP_MODIFIER_PARAMETER = 1

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/protonvpn/FrostedGlass;"

@Suppress("unused")
val frostedGlassPatch = bytecodePatch(
    name = "Frosted glass",
    description = "Blurs the map behind home screen panels and sheets. Requires Android 12 or later.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        val map = HomeMapFingerprint.matchSingle().method
        val modifier = map.parameterTypes.first().toString()
        installNavigationBarBackdrop(modifier, EXTENSION_CLASS_DESCRIPTOR)
        val mapView = mapViewFingerprint(modifier).matchSingle()
        val (viewCall) = mapView.instructionMatches
        val view = viewCall.instruction.getReference<MethodReference>()!!
        val viewRegisters = view.parameterTypes.size
        val bridge = mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).addStaticMethod(
            "mapView", view.parameterTypes.map(CharSequence::toString), "V", viewRegisters,
            """
                invoke-static { p$MAP_MODIFIER_PARAMETER }, $EXTENSION_CLASS_DESCRIPTOR->source(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object p$MAP_MODIFIER_PARAMETER
                check-cast p$MAP_MODIFIER_PARAMETER, $modifier
                invoke-static/range { p0 .. p${viewRegisters - 1} }, ${descriptor(view)}
                return-void
            """,
        )
        val arguments = when (val call = viewCall.instruction) {
            is RegisterRangeInstruction -> "invoke-static/range { v${call.startRegister} .. v${call.startRegister + call.registerCount - 1} }"
            is FiveRegisterInstruction -> {
                val registers = listOf(call.registerC, call.registerD, call.registerE, call.registerF, call.registerG)
                "invoke-static { ${registers.take(call.registerCount).joinToString { "v$it" }} }"
            }
            else -> error("Expected static map view call")
        }
        mapView.method.replaceInstruction(viewCall.index, "$arguments, $bridge")
        val rounded = RoundedShapeFingerprint.matchSingle().originalClassDef
        val shape = rounded.interfaces.singleOrNull() ?: classDefBy(rounded.superclass!!).interfaces.single()
        val element = backgroundElementFingerprint(shape).matchSingle().originalClassDef
        val node = element.methods.flatMap { method ->
            method.implementation?.instructions?.filter { it.opcode == Opcode.NEW_INSTANCE }?.mapNotNull {
                it.getReference<TypeReference>()?.type
            }?.toList().orEmpty()
        }.distinct().single { type -> classDefBy(type).fields.any { it.type == shape } }
        val content = BackdropDrawContentFingerprint.matchSingle().method
        val draw = backgroundDrawFingerprint(node, content).matchSingle()
        val drawScope = draw.method.parameterTypes.single().toString()
        val extension = mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR)
        val fieldMatch = backgroundFieldsFingerprint(element.type, node, shape).matchSingle()
        val (_, colorWrite, _, shapeWrite) = fieldMatch.instructionMatches
        val color = colorWrite.instruction.getReference<FieldReference>()!!
        val shapeField = shapeWrite.instruction.getReference<FieldReference>()!!
        mutableClassDefBy(rounded.type).apply { accessFlags = accessFlags or AccessFlags.PUBLIC.value }
        mutableClassDefBy(node).apply {
            accessFlags = accessFlags or AccessFlags.PUBLIC.value
            fields.filter { it.name == color.name || it.name == shapeField.name }.forEach {
                it.accessFlags = it.accessFlags and AccessFlags.PRIVATE.value.inv() or AccessFlags.PUBLIC.value
            }
        }
        val brandColors = CORE_BRAND_COLORS.mapIndexed { index, color -> "const v$index, ${color.toInt()}" }.joinToString("\n")
        extension.replaceStub("brandColors", CORE_BRAND_COLORS.size, """
            $brandColors
            filled-new-array/range { v0 .. v${CORE_BRAND_COLORS.lastIndex} }, [I
            move-result-object v0
            return-object v0
        """)
        extension.replaceStub("color", 3, """
            check-cast p0, $node
            iget-wide v0, p0, ${descriptor(color)}
            return-wide v0
        """)
        extension.replaceStub("setColor", 3, """
            check-cast p0, $node
            iput-wide p1, p0, ${descriptor(color)}
            return-void
        """)
        extension.replaceStub("isRounded", 1, """
            check-cast p0, $node
            iget-object p0, p0, ${descriptor(shapeField)}
            instance-of p0, p0, ${rounded.type}
            return p0
        """)
        extension.replaceStub("drawBackgroundMask", 2, """
            check-cast p0, $node
            check-cast p1, $drawScope
            invoke-virtual { p0, p1 }, ${descriptor(draw.method)}
            return-void
        """)
        val coordinator = BackdropCoordinatesFingerprint.matchSingle().originalClassDef.type
        val screenMatch = screenPositionFingerprint(coordinator).matchSingle()
        val (_, layoutRead, ownerCall, _) = screenMatch.instructionMatches
        val layoutField = layoutRead.instruction.getReference<FieldReference>()!!
        val owner = ownerCall.instruction.getReference<MethodReference>()!!
        val screenPosition = screenMatch.method
        extension.replaceStub("view", 1, """
            check-cast p0, $coordinator
            iget-object p0, p0, ${descriptor(layoutField)}
            invoke-static { p0 }, ${descriptor(owner)}
            move-result-object p0
            check-cast p0, Landroid/view/View;
            return-object p0
        """)
        extension.replaceStub("screenPosition", 3, """
            check-cast p0, $coordinator
            const-wide/16 v0, 0x0
            invoke-virtual { p0, v0, v1 }, ${descriptor(screenPosition)}
            move-result-wide v0
            return-wide v0
        """)
        val gradient = MapFadeGradientFingerprint.matchSingle().originalClassDef.type
        val fade = mapFadeFingerprint(gradient).matchSingle()
        val (heightRead, densityCall, _, _, unitRead) = fade.instructionMatches
        val height = heightRead.instruction.getReference<FieldReference>()!!
        val density = densityCall.instruction.getReference<MethodReference>()!!
        val unit = unitRead.instruction.getReference<FieldReference>()!!
        extension.replaceStub("fadeHeight", 3, """
            check-cast p0, ${fade.method.definingClass}
            iget v0, p0, ${descriptor(height)}
            check-cast p1, ${density.definingClass}
            invoke-interface { p1, v0 }, ${descriptor(density)}
            move-result v0
            return v0
        """)
        extension.replaceStub("drawFade", 2, """
            check-cast p0, ${fade.method.definingClass}
            invoke-virtual { p0, p1 }, ${descriptor(fade.method)}
            return-void
        """)
        check(fade.method.implementation!!.registerCount > fade.method.parameterTypes.size + 1)
        fade.method.addInstructions(0, """
            invoke-static/range { p0 .. p1 }, $EXTENSION_CLASS_DESCRIPTOR->fade(Ljava/lang/Object;Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :original
            sget-object v0, ${descriptor(unit)}
            return-object v0
            :original
            nop
        """)
        content.addInstructions(0, """
            invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isDrawingMask()Z
            move-result v0
            if-eqz v0, :original
            return-void
            :original
            nop
        """)
        draw.method.addInstructions(0, """
            invoke-static { p0, p1 }, $EXTENSION_CLASS_DESCRIPTOR->draw(Ljava/lang/Object;Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :original
            return-void
            :original
            nop
        """)
    }
}
