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
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val SWITCH_VIEW_CLASS = "${PROTON_EXTENSION_PACKAGE}MaterialSwitchView;"
private const val ON_CHECKED_CHANGE_PARAMETER = 1
private const val MODIFIER_PARAMETER = 2
private const val ENABLED_PARAMETER = 3
private const val MODIFIER_DEFAULT_BIT = 0x4
private const val ENABLED_DEFAULT_BIT = 0x8
private const val ANDROID_VIEW_PARAMETERS = 5
private const val ANDROID_VIEW_PARAMETERS_WITH_DEFAULTS = 6

internal fun BytecodePatchContext.drawSwitchAsView(
    method: MutableMethod,
    composerParameter: Int,
    modifierCompanion: FieldReference?,
    androidViewFingerprint: (function: String, modifier: String, composer: String) -> Fingerprint,
) {
    val function = method.parameterTypes[ON_CHECKED_CHANGE_PARAMETER].toString()
    val androidView = androidViewFingerprint(
        function,
        method.parameterTypes[MODIFIER_PARAMETER].toString(),
        method.parameterTypes[composerParameter].toString(),
    ).matchSingle().originalMethod
    val defaultMask = "p${method.parameterTypes.lastIndex}"
    val (lastArgument, defaultModifier) = when {
        androidView.parameterTypes.size == ANDROID_VIEW_PARAMETERS_WITH_DEFAULTS -> "v6" to """
            and-int/lit8 v6, $defaultMask, $MODIFIER_DEFAULT_BIT
            shr-int/lit8 v6, v6, 0x1
        """
        androidView.parameterTypes.size == ANDROID_VIEW_PARAMETERS && modifierCompanion != null -> "v5" to """
            and-int/lit8 v5, $defaultMask, $MODIFIER_DEFAULT_BIT
            if-eqz v5, :modifier
            sget-object v2, ${descriptor(modifierCompanion)}
            :modifier
        """
        else -> throw PatchException("Unexpected AndroidView parameters: ${descriptor(androidView)}")
    }

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
            move/from16 v4, p$ENABLED_PARAMETER
            and-int/lit8 v5, $defaultMask, $ENABLED_DEFAULT_BIT
            if-eqz v5, :enabled
            const/4 v4, 0x1
            :enabled
            invoke-static { v0, v2, v3, v4 }, $SWITCH_VIEW_CLASS->update(Ljava/lang/Class;ZLjava/lang/Object;Z)Ljava/lang/Object;
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
