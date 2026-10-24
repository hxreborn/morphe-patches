/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal const val LOADING_INDICATOR_CLASS = "${PROTON_EXTENSION_PACKAGE}LoadingIndicator;"
internal const val SHOW_LOADING_INDICATOR = "$LOADING_INDICATOR_CLASS->show(Ljava/lang/Object;ZF)V"
private const val VIEW_INTEROP_PACKAGE = "Landroidx/compose/ui/viewinterop/"
private const val ANDROID_VIEW_FACTORY_PARAMETER = 0
private const val ANDROID_VIEW_UPDATE_PARAMETER = 2
private const val ANDROID_VIEW_COMPOSER_PARAMETER = 3
private const val ANDROID_VIEW_PARAMETERS = 5
private const val ANDROID_VIEW_PARAMETERS_WITH_DEFAULTS = 6
private const val ANDROID_VIEW_MODIFIER_DEFAULT_MASK = 0x2
private const val SHOW_REGISTERS = 10

internal fun androidViewFingerprint(modifier: String, composer: String, parameterCount: Int) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", modifier, "L", composer) + List(parameterCount - ANDROID_VIEW_COMPOSER_PARAMETER - 1) { "I" },
    custom = { method, classDef ->
        classDef.type.startsWith(VIEW_INTEROP_PACKAGE) &&
            method.parameterTypes[ANDROID_VIEW_FACTORY_PARAMETER] == method.parameterTypes[ANDROID_VIEW_UPDATE_PARAMETER]
    },
)

internal fun BytecodePatchContext.bindLoadingIndicatorView(androidView: Method, modifierCompanion: FieldReference?) {
    val functionType = androidView.parameterTypes.first().toString()
    val composerType = androidView.parameterTypes[ANDROID_VIEW_COMPOSER_PARAMETER].toString()
    val (modifierArgument, defaultMaskArgument, lastArgument) = when {
        androidView.parameterTypes.size == ANDROID_VIEW_PARAMETERS_WITH_DEFAULTS ->
            Triple("const/4 v2, 0x0", "const/4 v6, $ANDROID_VIEW_MODIFIER_DEFAULT_MASK", "v6")
        androidView.parameterTypes.size == ANDROID_VIEW_PARAMETERS && modifierCompanion != null ->
            Triple("sget-object v2, ${descriptor(modifierCompanion)}", "", "v5")
        else -> throw PatchException("Unexpected AndroidView parameters: ${descriptor(androidView)}")
    }

    mutableClassDefBy(LOADING_INDICATOR_CLASS).replaceStub(
        "show",
        SHOW_REGISTERS,
        """
            const-class v0, $functionType
            invoke-static { v0 }, $LOADING_INDICATOR_CLASS->factory(Ljava/lang/Class;)Ljava/lang/Object;
            move-result-object v1
            check-cast v1, $functionType
            $modifierArgument
            invoke-static { v0, p1, p2 }, $LOADING_INDICATOR_CLASS->update(Ljava/lang/Class;ZF)Ljava/lang/Object;
            move-result-object v3
            check-cast v3, $functionType
            move-object v4, p0
            check-cast v4, $composerType
            const/4 v5, 0x0
            $defaultMaskArgument
            invoke-static/range { v1 .. $lastArgument }, ${descriptor(androidView)}
            return-void
        """,
    )
}
