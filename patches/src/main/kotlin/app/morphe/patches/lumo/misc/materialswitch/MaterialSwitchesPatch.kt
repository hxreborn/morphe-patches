/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.materialswitch

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.lumo.misc.PageFinishedFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.generated.descriptor
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val WEB_SWITCH_CLASS = "Lapp/hxreborn/extension/proton/WebMaterialSwitch;"
private const val SWITCH_VIEW_CLASS = "Lapp/hxreborn/extension/proton/MaterialSwitchView;"
private const val MODIFIER_PARAMETER = 1
private const val COMPOSER_PARAMETER = 4

private fun BytecodePatchContext.drawNativeSwitchesAsViews() {
    val switch = MaterialSwitchFingerprint.matchSingle().method
    val modifier = switch.parameterTypes[MODIFIER_PARAMETER].toString()
    val androidView = androidViewFingerprint(modifier, switch.parameterTypes[COMPOSER_PARAMETER].toString())
        .matchSingle().originalMethod
    val function = androidView.parameterTypes.first().toString()
    val modifierCompanion = switch.instructions.firstNotNullOf { instruction ->
        ((instruction as? ReferenceInstruction)?.reference as? FieldReference)?.takeIf { field ->
            instruction.opcode == Opcode.SGET_OBJECT && field.type == field.definingClass &&
                modifier in classDefBy(field.type).interfaces
        }
    }

    switch.addInstructions(
        0,
        """
            const-class v0, $function
            invoke-static { v0 }, $SWITCH_VIEW_CLASS->factory(Ljava/lang/Class;)Ljava/lang/Object;
            move-result-object v1
            check-cast v1, $function
            move/from16 v2, p0
            const/4 v3, 0x0
            const/4 v4, 0x1
            invoke-static { v0, v2, v3, v4 }, $SWITCH_VIEW_CLASS->update(Ljava/lang/Class;ZLjava/lang/Object;Z)Ljava/lang/Object;
            move-result-object v3
            check-cast v3, $function
            sget-object v2, ${descriptor(modifierCompanion)}
            move-object/from16 v4, p$COMPOSER_PARAMETER
            const/4 v5, 0x0
            const/4 v6, 0x0
            invoke-static/range { v1 .. v6 }, ${descriptor(androidView)}
            return-void
        """,
    )
}

@Suppress("unused")
val materialSwitchesPatch = bytecodePatch(
    name = "Material 3 switches",
    description = "Shows switches in the Material 3 style with check and close icons.",
) {
    compatibleWith(AppCompatibilities.LUMO)
    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)
    availability(requireArm64)
    extendWith("extensions/extension.mpe")

    execute {
        PageFinishedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $WEB_SWITCH_CLASS->restyle(Landroid/webkit/WebView;)V",
        )
        drawNativeSwitchesAsViews()
    }
}
