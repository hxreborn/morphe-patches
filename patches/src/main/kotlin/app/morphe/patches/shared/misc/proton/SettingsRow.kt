/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val COMPOSER_TYPE = "Landroidx/compose/runtime/Composer;"
private const val SETTINGS_ROW_METHOD = "patchesSettingsRow"

internal class SettingsRowIcon(val parameter: Int, val resourceId: Long)

internal fun MutableClass.addSettingsRowMethod(
    templateMethod: MutableMethod,
    templateCallIndex: Int,
    titleParameter: Int,
    onClickParameter: Int,
    icon: SettingsRowIcon? = null,
    composerParameter: Int? = null,
): String {
    val templateCall = templateMethod.getInstruction<RegisterRangeInstruction>(templateCallIndex)
    val templateRow = templateCall.getReference<MethodReference>()!!
    val parameterTypes = templateRow.parameterTypes.map(CharSequence::toString)
    if (parameterTypes.any { it == "J" || it == "D" }) {
        throw PatchException("Wide parameters are unsupported in ${descriptor(templateRow)}")
    }
    if (parameterTypes[titleParameter] != "Ljava/lang/String;") {
        throw PatchException(
            "Expected String title at parameter $titleParameter in ${descriptor(templateRow)}",
        )
    }
    val onClickType = parameterTypes[onClickParameter]
    val composerParameter = composerParameter ?: parameterTypes.indexOf(COMPOSER_TYPE)
    if (composerParameter < 0) {
        throw PatchException("No Composer parameter in ${descriptor(templateRow)}")
    }
    val composerType = parameterTypes[composerParameter]

    val defaultArgumentsParameter = parameterTypes.lastIndex
    val defaultArgumentsMask = templateMethod.literalWrittenTo(
        templateCall.startRegister + templateCall.registerCount - 1,
        templateCallIndex,
    )
    val assignedParameters = setOfNotNull(
        titleParameter,
        onClickParameter,
        composerParameter,
        defaultArgumentsParameter,
        icon?.parameter,
    )

    val method = addMethod(
        SETTINGS_ROW_METHOD,
        listOf(composerType),
        "V",
        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
        parameterTypes.size + 1,
        """
            ${(parameterTypes.indices - assignedParameters).joinToString("\n") { "const v$it, 0x0" }}
            ${icon?.let { "const v${it.parameter}, ${it.resourceId}" } ?: ""}
            const-string v$titleParameter, "$SETTINGS_ROW_TITLE"
            const-class v$onClickParameter, $onClickType
            invoke-static/range { v$onClickParameter .. v$onClickParameter }, $PATCHES_MENU_CLASS->settingsRowOnClick(Ljava/lang/Class;)Ljava/lang/Object;
            move-result-object v$onClickParameter
            check-cast v$onClickParameter, $onClickType
            move-object/from16 v$composerParameter, p0
            const v$defaultArgumentsParameter, $defaultArgumentsMask
            invoke-static/range { v0 .. v$defaultArgumentsParameter }, ${descriptor(templateRow)}
            return-void
        """,
    )

    return descriptor(method)
}
