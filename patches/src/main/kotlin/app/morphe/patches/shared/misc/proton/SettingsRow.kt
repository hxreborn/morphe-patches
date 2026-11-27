/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val COMPOSER_TYPE = "Landroidx/compose/runtime/Composer;"
private const val SETTINGS_ROW_METHOD = "patchesSettingsRow"
private const val SETTINGS_ROW_ON_CLICK = "$PATCHES_MENU_CLASS->settingsRowOnClick(Ljava/lang/Class;)Ljava/lang/Object;"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val MODIFIER_DEFAULT_BIT = 1L

internal class SettingsRowIcon(val parameter: Int, val resourceId: Long)

internal class SettingsRowSummary(val parameter: Int, val textCall: String)

internal class SettingsRowSpec(
    val title: String = SETTINGS_ROW_TITLE,
    val onClickCall: String = SETTINGS_ROW_ON_CLICK,
    val methodName: String = SETTINGS_ROW_METHOD,
    val summary: SettingsRowSummary? = null,
    val defaultModifier: Boolean = false,
)

private fun MutableMethod.summaryConstructorType(callIndex: Int, summaryType: String): String {
    val constructorCall = (0 until callIndex).lastOrNull { index ->
        val instruction = getInstruction(index)
        val reference = instruction.getReference<MethodReference>()
        instruction.opcode == Opcode.INVOKE_DIRECT &&
            reference?.name == "<init>" &&
            reference.parameterTypes.map(CharSequence::toString) == listOf(STRING_TYPE)
    } ?: throw PatchException("No summary constructor before instruction $callIndex in $this")
    val type = getInstruction(constructorCall).getReference<MethodReference>()!!.definingClass
    val summaryClass = classDefBy(type)
    if (summaryClass.superclass != summaryType && summaryType !in summaryClass.interfaces) {
        throw PatchException("$type is not a $summaryType")
    }
    return type
}

internal fun MutableClass.addSettingsRowMethod(
    templateMethod: MutableMethod,
    templateCallIndex: Int,
    titleParameter: Int,
    onClickParameter: Int,
    icon: SettingsRowIcon? = null,
    composerParameter: Int? = null,
    spec: SettingsRowSpec = SettingsRowSpec(),
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
    val templateMask = templateMethod.literalWrittenTo(
        templateCall.startRegister + templateCall.registerCount - 1,
        templateCallIndex,
    )
    val defaultArgumentsMask = if (spec.defaultModifier) templateMask or MODIFIER_DEFAULT_BIT else templateMask
    val assignedParameters = setOfNotNull(
        titleParameter,
        onClickParameter,
        composerParameter,
        defaultArgumentsParameter,
        icon?.parameter,
        spec.summary?.parameter,
    )
    val summarySmali = spec.summary?.let { summary ->
        val summaryType = templateMethod.summaryConstructorType(templateCallIndex, parameterTypes[summary.parameter])
        val text = (parameterTypes.indices - assignedParameters).first()
        """
            invoke-static {}, ${summary.textCall}
            move-result-object v$text
            new-instance v${summary.parameter}, $summaryType
            invoke-direct { v${summary.parameter}, v$text }, $summaryType-><init>(Ljava/lang/String;)V
        """
    }.orEmpty()

    val method = addMethod(
        spec.methodName,
        listOf(composerType),
        "V",
        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
        parameterTypes.size + 1,
        """
            $summarySmali
            ${(parameterTypes.indices - assignedParameters).joinToString("\n") { "const v$it, 0x0" }}
            ${icon?.let { "const v${it.parameter}, ${it.resourceId}" } ?: ""}
            const-string v$titleParameter, "${spec.title}"
            const-class v$onClickParameter, $onClickType
            invoke-static/range { v$onClickParameter .. v$onClickParameter }, ${spec.onClickCall}
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
