/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val SWITCH_VIEW_CLASS = "${PROTON_EXTENSION_PACKAGE}MaterialSwitchView;"
private const val ON_CHECKED_CHANGE_PARAMETER = 1
private const val MODIFIER_PARAMETER = 2
private const val ENABLED_PARAMETER = 3
private const val ANDROID_VIEW_PARAMETERS = 5
private const val ANDROID_VIEW_PARAMETERS_WITH_DEFAULTS = 6

internal fun BytecodePatchContext.drawSwitchAsView(
    method: MutableMethod,
    composerParameter: Int,
    modifierCompanion: FieldReference?,
    androidViewFingerprint: (function: String, modifier: String, composer: String) -> Fingerprint,
    enabledParameter: Int = ENABLED_PARAMETER,
    colorsParameter: Int? = null,
) {
    val modifierDefaultBit = 1 shl MODIFIER_PARAMETER
    val enabledDefaultBit = 1 shl enabledParameter
    val function = method.parameterTypes[ON_CHECKED_CHANGE_PARAMETER].toString()
    val androidView = androidViewFingerprint(
        function,
        method.parameterTypes[MODIFIER_PARAMETER].toString(),
        method.parameterTypes[composerParameter].toString(),
    ).matchSingle().originalMethod
    val defaultMask = "p${method.parameterTypes.lastIndex}"
    val (lastArgument, defaultModifier) = when {
        androidView.parameterTypes.size == ANDROID_VIEW_PARAMETERS_WITH_DEFAULTS -> "v6" to """
            and-int/lit8 v6, $defaultMask, $modifierDefaultBit
            shr-int/lit8 v6, v6, 0x1
        """
        androidView.parameterTypes.size == ANDROID_VIEW_PARAMETERS && modifierCompanion != null -> "v5" to """
            and-int/lit8 v5, $defaultMask, $modifierDefaultBit
            if-eqz v5, :modifier
            sget-object v2, ${descriptor(modifierCompanion)}
            :modifier
        """
        else -> throw PatchException("Unexpected AndroidView parameters: ${descriptor(androidView)}")
    }
    val (primaryColorArgument, loadPrimaryColor) = colorsParameter?.let { parameter ->
        ", v5" to """
            const/4 v5, 0x0
            move-object/from16 v6, p$parameter
            if-eqz v6, :colors
            iget-wide v5, v6, ${method.parameterTypes[parameter]}->checkedThumbColor:J
            invoke-static { v5, v6 }, $SWITCH_VIEW_CLASS->composeColorArgb(J)I
            move-result v5
            :colors
        """
    } ?: ("" to "")

    mutableClassDefBy(SWITCH_VIEW_CLASS).replaceStub(
        "invokeCallback",
        3,
        """
            check-cast p0, $function
            invoke-static { p1 }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
            move-result-object v0
            invoke-interface { p0, v0 }, $function->invoke(Ljava/lang/Object;)Ljava/lang/Object;
            return-void
        """,
    )

    method.addInstructionsWithLabels(
        0,
        """
            invoke-static { }, $MATERIAL_SWITCHES_CLASS->isEnabled()Z
            move-result v0
            if-eqz v0, :original
            const-class v0, $function
            invoke-static { v0 }, $SWITCH_VIEW_CLASS->factory(Ljava/lang/Class;)Ljava/lang/Object;
            move-result-object v1
            check-cast v1, $function
            move/from16 v2, p0
            move-object/from16 v3, p$ON_CHECKED_CHANGE_PARAMETER
            move/from16 v4, p$enabledParameter
            and-int/lit8 v5, $defaultMask, $enabledDefaultBit
            if-eqz v5, :enabled
            const/4 v4, 0x1
            :enabled
            $loadPrimaryColor
            invoke-static { v0, v2, v3, v4$primaryColorArgument }, $SWITCH_VIEW_CLASS->update(Ljava/lang/Class;ZLjava/lang/Object;Z${if (colorsParameter != null) "I" else ""})Ljava/lang/Object;
            move-result-object v3
            check-cast v3, $function
            move-object/from16 v2, p$MODIFIER_PARAMETER
            $defaultModifier
            move-object/from16 v4, p$composerParameter
            const/4 v5, 0x0
            invoke-static/range { v1 .. $lastArgument }, ${descriptor(androidView)}
            return-void
        """,
        ExternalLabel("original", method.getInstruction(0)),
    )
}
