/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val LOADING_INDICATOR_CLASS = "${PROTON_EXTENSION_PACKAGE}LoadingIndicator;"
internal const val SHOW_LOADING_INDICATOR = "$LOADING_INDICATOR_CLASS->show(Ljava/lang/Object;ZF)V"
private const val SHOW_LOADING_PROGRESS = "$LOADING_INDICATOR_CLASS->showProgress(Ljava/lang/Object;F)V"
private const val CONTAINER_SIZE = "$LOADING_INDICATOR_CLASS->containerSize(F)F"
private const val SIZE_MODIFIERS_CLASS = "Landroidx/compose/foundation/layout/SizeKt;"
private const val VIEW_INTEROP_PACKAGE = "Landroidx/compose/ui/viewinterop/"
private const val COMPOSE_COMPOSER = "Landroidx/compose/runtime/Composer;"
private const val COMPOSE_MODIFIER = "Landroidx/compose/ui/Modifier;"
private const val MODIFIER_COMPANION = "Landroidx/compose/ui/Modifier\$Companion;"
private const val ANDROID_VIEW_FACTORY_PARAMETER = 0
private const val ANDROID_VIEW_UPDATE_PARAMETER = 2
private const val ANDROID_VIEW_COMPOSER_PARAMETER = 3
private const val ANDROID_VIEW_PARAMETERS = 5
private const val ANDROID_VIEW_PARAMETERS_WITH_DEFAULTS = 6
private const val ANDROID_VIEW_MODIFIER_DEFAULT_MASK = 0x2
private const val SHOW_REGISTERS = 10
private const val INDICATOR_WRAPPER_LOCALS = 3
private const val PULL_STATE_PARAMETER = "p0"

internal fun androidViewFingerprint(modifier: String, composer: String, parameterCount: Int) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", modifier, "L", composer) + List(parameterCount - ANDROID_VIEW_COMPOSER_PARAMETER - 1) { "I" },
    custom = { method, classDef ->
        classDef.type.startsWith(VIEW_INTEROP_PACKAGE) &&
            method.parameterTypes[ANDROID_VIEW_FACTORY_PARAMETER] == method.parameterTypes[ANDROID_VIEW_UPDATE_PARAMETER]
    },
)

internal fun pullRefreshContentFingerprint(progressIndicatorClass: String, pullRefreshClass: String) = Fingerprint(
    filters = listOf(
        fieldAccess(type = MODIFIER_COMPANION, opcode = Opcode.SGET_OBJECT),
        methodCall(definingClass = progressIndicatorClass, returnType = "V"),
        methodCall(definingClass = pullRefreshClass, returnType = "V"),
    ),
)

internal fun sizeModifierCall(location: InstructionLocation) = methodCall(
    definingClass = SIZE_MODIFIERS_CLASS,
    parameters = listOf(COMPOSE_MODIFIER, "F"),
    returnType = COMPOSE_MODIFIER,
    location = location,
)

internal fun resizeLoadingIndicatorContainer(method: MutableMethod, sizeCall: Match.InstructionMatch) {
    val sizeInvoke = sizeCall.instruction as? FiveRegisterInstruction
        ?: throw PatchException("Expected a size modifier invoke at index ${sizeCall.index} in ${descriptor(method)}")
    val sizeRegister = sizeInvoke.registerD
    method.addInstructions(
        sizeCall.index,
        """
            invoke-static/range { v$sizeRegister .. v$sizeRegister }, $CONTAINER_SIZE
            move-result v$sizeRegister
        """,
    )
}

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

internal fun BytecodePatchContext.drawPullRefreshAsLoadingIndicator(
    contentFingerprint: Fingerprint,
    androidViewParameterCount: Int,
    loadPullProgress: (stateType: String, state: String, progress: String, scratch: String) -> String,
) {
    markFeaturePatched(LOADING_INDICATOR_CLASS)

    val match = contentFingerprint.matchSingle()
    val content = match.method
    val androidView = androidViewFingerprint(COMPOSE_MODIFIER, COMPOSE_COMPOSER, androidViewParameterCount)
        .matchSingle().originalMethod
    val modifierCompanion = match.instructionMatches.first().instruction.getReference<FieldReference>()
    bindLoadingIndicatorView(androidView, modifierCompanion)

    redirectToLoadingIndicator(content, match.instructionMatches[1].index, "loadingIndicatorRefreshing") { _ ->
        """
            const/4 v1, 0x1
            const/4 v2, 0x0
            invoke-static { v0, v1, v2 }, $SHOW_LOADING_INDICATOR
        """
    }
    redirectToLoadingIndicator(content, match.instructionMatches.last().index, "loadingIndicatorPulling") { target ->
        """
            ${loadPullProgress(target.parameterTypes.first().toString(), PULL_STATE_PARAMETER, "v1", "v2")}
            invoke-static { v0, v1 }, $SHOW_LOADING_PROGRESS
        """
    }
}

private fun BytecodePatchContext.redirectToLoadingIndicator(
    caller: MutableMethod,
    index: Int,
    wrapperName: String,
    showIndicator: (target: MethodReference) -> String,
) {
    val call = caller.getInstruction(index)
    val target = call.getReference<MethodReference>()!!
    val parameters = target.parameterTypes.map(CharSequence::toString)
    val parameterRegisters = parameters.sumOf(::registerWidth)
    val composerParameter = parameters.indexOf(COMPOSE_COMPOSER)
    if (composerParameter < 0) throw PatchException("No Composer parameter in ${descriptor(target)}")

    val composerRegister = parameters.take(composerParameter).sumOf(::registerWidth)
    val wrapper = mutableClassDefBy(target.definingClass).addStaticMethod(
        wrapperName,
        parameters,
        "V",
        INDICATOR_WRAPPER_LOCALS + parameterRegisters,
        """
            invoke-static { }, $LOADING_INDICATOR_CLASS->isEnabled()Z
            move-result v0
            if-eqz v0, :original
            move-object/from16 v0, p$composerRegister
            ${showIndicator(target)}
            return-void
            :original
            invoke-static/range { p0 .. p${parameterRegisters - 1} }, ${descriptor(target)}
            return-void
        """,
    )

    val arguments = when (call) {
        is RegisterRangeInstruction -> "/range { v${call.startRegister} .. v${call.startRegister + call.registerCount - 1} }"
        is FiveRegisterInstruction -> listOf(call.registerC, call.registerD, call.registerE, call.registerF, call.registerG)
            .take(call.registerCount).joinToString(prefix = " { ", postfix = " }") { "v$it" }
        else -> throw PatchException("Expected an invoke instruction at index $index in ${descriptor(caller)}")
    }
    caller.replaceInstruction(index, "invoke-static$arguments, $wrapper")
}

private fun registerWidth(type: String) = if (type == "J" || type == "D") 2 else 1
