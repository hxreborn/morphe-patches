/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.navigationbar

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patches.shared.misc.proton.FLOATING_NAVIGATION_BAR_CLASS
import app.morphe.patches.shared.misc.proton.NAVIGATION_BAR_BACKDROP_CLASS
import app.morphe.patches.shared.misc.proton.literalWrittenTo
import app.morphe.patches.shared.misc.proton.captureScaffoldPageColor
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField

private const val PADDING_VALUES = "Landroidx/compose/foundation/layout/PaddingValues;"
private const val AUTO_HIDE = "Lapp/hxreborn/extension/protonpass/NavigationBarAutoHide;"
private const val DEFAULT_LIST_PADDING = 0x4
private const val DEFAULT_SCROLL_SIDE_PADDING = 0x7
private const val LIST_PADDING_STABILITY = 0x380
private const val CONTENT_BOTTOM_ARGUMENT = 4
private const val LIST_CONTENT_PADDING_ARGUMENT = 2
private const val SCAFFOLD_BACKGROUND_PARAMETER = 13
private const val SCAFFOLD_BACKGROUND_DEFAULT = 0x2000

internal fun BytecodePatchContext.installFloatingContentPadding(content: Match) {
    captureScaffoldPageColor(
        MainScaffoldFingerprint.matchSingle(), SCAFFOLD_BACKGROUND_PARAMETER, SCAFFOLD_BACKGROUND_DEFAULT,
        "getBackground-0d7_KjU",
    )
    val bottomCall = content.instructionMatches.first().instruction.getReference<MethodReference>()!!
    val paddingCall = content.instructionMatches.single { it.filter === contentPaddingCall }.instruction.getReference<MethodReference>()!!
    val extension = mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS)
    val scaffold = ImmutableField(extension.type, "passScaffoldPadding", PADDING_VALUES, AccessFlags.PRIVATE.value or AccessFlags.STATIC.value, null, null, null)
    extension.fields.add(scaffold.toMutable())
    val capture = extension.addStaticMethod("passBottomPadding", listOf(PADDING_VALUES), "F", 1, """
        sput-object p0, ${descriptor(scaffold)}
        invoke-interface { p0 }, ${descriptor(bottomCall)}
        move-result p0
        return p0
    """)
    val clearance = extension.addStaticMethod("passContentClearance", emptyList(), "F", 2, """
        invoke-static {}, $FLOATING_NAVIGATION_BAR_CLASS->isEnabled()Z
        move-result v0
        if-eqz v0, :empty
        sget-object v0, ${descriptor(scaffold)}
        if-eqz v0, :empty
        invoke-interface { v0 }, ${descriptor(bottomCall)}
        move-result v0
        invoke-static {}, $AUTO_HIDE->barOffsetDp()F
        move-result v1
        sub-float/2addr v0, v1
        const/4 v1, 0x0
        invoke-static { v0, v1 }, Ljava/lang/Math;->max(FF)F
        move-result v0
        return v0
        :empty
        const/4 v0, 0x0
        return v0
    """)
    val paddingParameters = paddingCall.parameterTypes.map(CharSequence::toString)
    check(paddingParameters == listOf(COMPOSE_MODIFIER, "F", "F", "F", "F", "I"))
    val overlay = extension.addStaticMethod("passContentPadding", paddingParameters, COMPOSE_MODIFIER, paddingParameters.size + 1, """
        invoke-static {}, $FLOATING_NAVIGATION_BAR_CLASS->isEnabled()Z
        move-result v0
        if-eqz v0, :original
        const/4 p$CONTENT_BOTTOM_ARGUMENT, 0x0
        :original
        invoke-static/range { p0 .. p${paddingParameters.lastIndex} }, ${descriptor(paddingCall)}
        move-result-object p0
        invoke-static/range { p0 .. p0 }, $NAVIGATION_BAR_BACKDROP_CLASS->source(Ljava/lang/Object;)Ljava/lang/Object;
        move-result-object p0
        check-cast p0, $COMPOSE_MODIFIER
        return-object p0
    """)
    val bottomIndex = content.method.instructions.indexOfFirst { it.getReference<MethodReference>() == bottomCall }
    val bottom = content.method.instructions.elementAt(bottomIndex)
    content.method.replaceInstruction(bottomIndex, staticInvocation(bottom, capture))
    val paddingIndex = content.method.instructions.indexOfFirst { it.getReference<MethodReference>() == paddingCall }
    val padding = content.method.instructions.elementAt(paddingIndex)
    content.method.replaceInstruction(paddingIndex, staticInvocation(padding, overlay))
    val append = extension.addStaticMethod("passScrollClearance", listOf(COMPOSE_MODIFIER), COMPOSE_MODIFIER, 7, """
        invoke-static {}, $FLOATING_NAVIGATION_BAR_CLASS->isEnabled()Z
        move-result v0
        if-eqz v0, :original
        const/4 v1, 0x0
        const/4 v2, 0x0
        const/4 v3, 0x0
        invoke-static {}, $clearance
        move-result v4
        const/4 v5, $DEFAULT_SCROLL_SIDE_PADDING
        move-object v0, p0
        invoke-static/range { v0 .. v5 }, ${descriptor(paddingCall)}
        move-result-object p0
        :original
        return-object p0
    """)
    listOf("profile_option_account", "security_center_home_section_password_health", "home_empty_vault_create_login").forEach { resource ->
        val scroll = scrollContentFingerprint(resource).matchSingle()
        val (_, result) = scroll.instructionMatches
        val register = result.getInstruction<OneRegisterInstruction>().registerA
        scroll.method.addInstructions(result.index + 1, """
            invoke-static/range { v$register .. v$register }, $append
            move-result-object v$register
        """)
    }
    val items = HomeItemsContentFingerprint.matchAll(0..1).singleOrNull() ?: HomeItemsWithPaddingImplFingerprint.matchSingle()
    val lists = listOfNotNull(items, HomePagedItemsContentFingerprint.matchAll(0..1).singleOrNull())
    val native = items.instructionMatches.last().instruction.getReference<MethodReference>()!!
    val parameters = native.parameterTypes.map(CharSequence::toString)
    check(parameters[LIST_CONTENT_PADDING_ARGUMENT] in listOf(PADDING_VALUES, "Landroidx/compose/foundation/layout/PaddingValuesImpl;") && parameters.takeLast(2) == listOf("I", "I"))
    val count = parameters.size
    val bridge = extension.addStaticMethod("passItemsContent", parameters, "V", count + 5, """
        invoke-static {}, $FLOATING_NAVIGATION_BAR_CLASS->isEnabled()Z
        move-result v0
        if-eqz v0, :original
        const/4 v0, 0x0
        const/4 v1, 0x0
        const/4 v2, 0x0
        invoke-static {}, $clearance
        move-result v3
        new-instance v4, Landroidx/compose/foundation/layout/PaddingValuesImpl;
        invoke-direct { v4, v0, v1, v2, v3 }, Landroidx/compose/foundation/layout/PaddingValuesImpl;-><init>(FFFF)V
        move-object/from16 p$LIST_CONTENT_PADDING_ARGUMENT, v4
        move/from16 v0, p${count - 1}
        and-int/lit8 v0, v0, ${DEFAULT_LIST_PADDING.inv()}
        move/16 p${count - 1}, v0
        move/from16 v0, p${count - 2}
        and-int/lit16 v0, v0, ${LIST_PADDING_STABILITY.inv()}
        move/16 p${count - 2}, v0
        :original
        invoke-static/range { p0 .. p${count - 1} }, ${descriptor(native)}
        return-void
    """)
    lists.forEach { match ->
        val call = match.instructionMatches.last()
        check(call.instruction.getReference<MethodReference>() == native) { "Expected shared item list layout" }
        val defaultRegister = (call.instruction as RegisterRangeInstruction).let { it.startRegister + it.registerCount - 1 }
        check(match.method.literalWrittenTo(defaultRegister, call.index) and DEFAULT_LIST_PADDING.toLong() != 0L) { "Expected default item list padding" }
        match.method.replaceInstruction(call.index, staticInvocation(call.instruction, bridge))
    }
}

private fun staticInvocation(call: Instruction, method: String): String = when (call) {
    is RegisterRangeInstruction -> "invoke-static/range { v${call.startRegister} .. v${call.startRegister + call.registerCount - 1} }, $method"
    is FiveRegisterInstruction -> "invoke-static { ${listOf(call.registerC, call.registerD, call.registerE, call.registerF, call.registerG).take(call.registerCount).joinToString { "v$it" }} }, $method"
    else -> error("Expected native invocation")
}
