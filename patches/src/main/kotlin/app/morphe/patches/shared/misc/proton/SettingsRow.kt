/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

internal const val COMPOSER_TYPE = "Landroidx/compose/runtime/Composer;"
private const val ON_CLICK_TYPE = "Lkotlin/jvm/functions/Function0;"
private const val SETTINGS_ROW_METHOD = "patchesSettingsRow"

internal fun MutableClass.addSettingsRowMethod(
    templateMethod: MutableMethod,
    templateCallIndex: Int,
    titleParameter: Int,
    onClickParameter: Int,
): String {
    val templateCall = templateMethod.getInstruction<RegisterRangeInstruction>(templateCallIndex)
    val templateRow = templateCall.getReference<MethodReference>()!!
    val parameterTypes = templateRow.parameterTypes.map(CharSequence::toString)
    if (parameterTypes.any { it == "J" || it == "D" }) {
        throw PatchException("Wide parameters are unsupported in ${DexFormatter.INSTANCE.getMethodDescriptor(templateRow)}")
    }
    if (parameterTypes[titleParameter] != "Ljava/lang/String;" || parameterTypes[onClickParameter] != ON_CLICK_TYPE) {
        throw PatchException(
            "Expected String title at parameter $titleParameter and $ON_CLICK_TYPE at parameter $onClickParameter in " +
                DexFormatter.INSTANCE.getMethodDescriptor(templateRow),
        )
    }
    val composerParameter = parameterTypes.indexOf(COMPOSER_TYPE)
    if (composerParameter < 0) {
        throw PatchException("No Composer parameter in ${DexFormatter.INSTANCE.getMethodDescriptor(templateRow)}")
    }

    val defaultArgumentsParameter = parameterTypes.lastIndex
    val defaultArgumentsMask = templateMethod.literalWrittenTo(
        templateCall.startRegister + templateCall.registerCount - 1,
        templateCallIndex,
    )
    val assignedParameters = setOf(
        titleParameter,
        onClickParameter,
        composerParameter,
        defaultArgumentsParameter,
    )

    val method = ImmutableMethod(
        type,
        SETTINGS_ROW_METHOD,
        listOf(ImmutableMethodParameter(COMPOSER_TYPE, null, null)),
        "V",
        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
        null,
        null,
        MutableMethodImplementation(parameterTypes.size + 1),
    ).toMutable().apply {
        addInstructions(
            0,
            """
                ${(parameterTypes.indices - assignedParameters).joinToString("\n") { "const v$it, 0x0" }}
                const-string v$titleParameter, "$SETTINGS_ROW_TITLE"
                const-class v$onClickParameter, $ON_CLICK_TYPE
                invoke-static/range { v$onClickParameter .. v$onClickParameter }, $PATCHES_MENU_CLASS->settingsRowOnClick(Ljava/lang/Class;)Ljava/lang/Object;
                move-result-object v$onClickParameter
                check-cast v$onClickParameter, $ON_CLICK_TYPE
                move-object/from16 v$composerParameter, p0
                const v$defaultArgumentsParameter, $defaultArgumentsMask
                invoke-static/range { v0 .. v$defaultArgumentsParameter }, ${DexFormatter.INSTANCE.getMethodDescriptor(templateRow)}
                return-void
            """,
        )
    }
    methods.add(method)

    return DexFormatter.INSTANCE.getMethodDescriptor(method)
}
