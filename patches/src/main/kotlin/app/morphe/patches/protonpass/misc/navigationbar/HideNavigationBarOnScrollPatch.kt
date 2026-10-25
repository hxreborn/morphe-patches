/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.navigationbar

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.literalWrittenTo
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val NAVIGATION_BAR_CLASS = "Lapp/hxreborn/extension/protonpass/NavigationBarAutoHide;"
private const val SCROLL_HANDLER_CLASS = "Lapp/hxreborn/extension/protonpass/NavigationBarAutoHide\$ScrollHandler;"
private const val HIDDEN_OFFSET_CLASS = "Lapp/hxreborn/extension/protonpass/NavigationBarAutoHide\$HiddenOffset;"
private const val COMPOSER = "Landroidx/compose/runtime/Composer;"
private const val FIRST_PARAMETER_STABILITY_BITS = 0b1110L
private const val LAST_FIVE_REGISTER_INVOKE_REGISTER = 15
private const val PADDING_BOTTOM_ARGUMENT = 4

@Suppress("unused")
val hideNavigationBarOnScrollPatch = bytecodePatch(
    name = "Hide navigation bar on scroll",
    description = "Hides the bottom navigation bar on scroll down and shows it on scroll up.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(NAVIGATION_BAR_CLASS)
        requireAppMethods(NAVIGATION_BAR_CLASS, SCROLL_HANDLER_CLASS, HIDDEN_OFFSET_CLASS)
        VelocityConstructorFingerprint.matchSingle()

        val nestedScroll = descriptor(NestedScrollModifierFingerprint.matchSingle().originalMethod)
        val offset = descriptor(OffsetModifierFingerprint.matchSingle().originalMethod)

        HomeBottomBarFingerprint.matchSingle().let { match ->
            match.method.offsetModifierArgument(
                match.instructionMatches.last().index,
                offset,
                "$NAVIGATION_BAR_CLASS->barOffsetDp()F",
            )
        }
        AppSnackbarHostFingerprint.matchSingle().let { match ->
            match.method.offsetModifierArgument(
                match.instructionMatches.last().index,
                offset,
                "$NAVIGATION_BAR_CLASS->snackbarOffsetDp()F",
            )
        }
        AppContentPaddingFingerprint.matchSingle().let { match ->
            match.method.hookContentPadding(match, nestedScroll)
        }
        LazyListApplyMeasureResultFingerprint.matchSingle().method.reportMeasuredList()
    }
}

private fun BytecodePatchContext.requireAppMethods(vararg extensionClasses: String) {
    val missing = extensionClasses
        .flatMap { classDefBy(it).methods }
        .mapNotNull { it.implementation }
        .flatMap { it.instructions }
        .mapNotNull { it.getReference<MethodReference>() }
        .filter { it.definingClass.startsWith("Landroidx/") && !declares(it.definingClass, it) }
        .map { descriptor(it) }
        .distinct()
    if (missing.isNotEmpty()) {
        throw PatchException("The app does not declare ${missing.joinToString()}")
    }
}

private fun BytecodePatchContext.declares(type: String, method: MethodReference): Boolean {
    val classDef = classDefByOrNull(type) ?: return false
    return classDef.methods.any { MethodUtil.methodSignaturesMatch(it, method) } ||
        (listOfNotNull(classDef.superclass) + classDef.interfaces).any { declares(it, method) }
}

private fun MutableMethod.hookContentPadding(match: Match, nestedScroll: String) {
    fun indexOf(filter: InstructionFilter) = match.instructionMatches.single { it.filter === filter }.index
    fun resultRegister(filter: InstructionFilter) = getInstruction<OneRegisterInstruction>(indexOf(filter)).registerA

    val paddingCallIndex = indexOf(contentPaddingCall)
    val paddingResultIndex = indexOf(contentPaddingResult)
    val sizeAnimationResultIndex = indexOf(contentSizeAnimationResult)
    val padded = resultRegister(contentPaddingResult)
    val sizeAnimated = resultRegister(contentSizeAnimationResult)

    val paddingCall = getInstruction(paddingCallIndex)
    val paddingParameters = paddingCall.getReference<MethodReference>()!!.parameterTypes.map(CharSequence::toString)
    if (paddingParameters != listOf(COMPOSE_MODIFIER, "F", "F", "F", "F", "I")) {
        throw PatchException("Unexpected content padding parameters $paddingParameters")
    }
    val bottom = argumentRegisters(paddingCall)[PADDING_BOTTOM_ARGUMENT]

    val touchedBeforeSizeAnimation = (paddingResultIndex + 1..sizeAnimationResultIndex)
        .flatMap { registersOf(getInstruction(it)) }
    val (scrolling, connection, dispatcher) = lowFreeRegisters(
        paddingResultIndex + 1,
        3,
        *(touchedBeforeSizeAnimation + padded + sizeAnimated).toIntArray(),
    )

    addInstructions(
        sizeAnimationResultIndex + 1,
        """
            move-object/16 v$connection, v$sizeAnimated
            invoke-static { v$scrolling, v$connection }, $NAVIGATION_BAR_CLASS->contentModifier($COMPOSE_MODIFIER$COMPOSE_MODIFIER)$COMPOSE_MODIFIER
            move-result-object v$sizeAnimated
        """,
    )
    addInstructions(
        paddingResultIndex + 1,
        """
            invoke-static { }, $NAVIGATION_BAR_CLASS->scrollConnection()$NESTED_SCROLL_CONNECTION
            move-result-object v$connection
            const/4 v$dispatcher, 0x0
            move-object/16 v$scrolling, v$padded
            invoke-static { v$scrolling, v$connection, v$dispatcher }, $nestedScroll
            move-result-object v$scrolling
            move-object/16 v$padded, v$scrolling
        """,
    )
    addInstructions(
        paddingCallIndex,
        """
            invoke-static/range { v$bottom .. v$bottom }, $NAVIGATION_BAR_CLASS->contentBottomPadding(F)F
            move-result v$bottom
        """,
    )
}

private fun MutableMethod.reportMeasuredList() {
    val list = implementation!!.registerCount - 1 - parameterTypes.sumOf { if (it == "J" || it == "D") 2L else 1L }.toInt()
    val overwritesList = instructions.any {
        it is OneRegisterInstruction && it.opcode.setsRegister() &&
            (it.registerA == list || (it.opcode.setsWideRegister() && it.registerA + 1 == list))
    }
    if (overwritesList) {
        throw PatchException("${descriptor(this)} overwrites its receiver register")
    }
    instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }.reversed().forEach {
        replaceInstruction(it, "invoke-static/range { v$list .. v$list }, $NAVIGATION_BAR_CLASS->onListMeasured($LAZY_LIST_STATE)V")
        addInstruction(it + 1, "return-void")
    }
}

private fun MutableMethod.offsetModifierArgument(callIndex: Int, offset: String, distanceGetter: String) {
    val call = getInstruction(callIndex)
    val arguments = argumentRegisters(call)
    val modifier = arguments.first()
    val changed = changedArgumentRegister(call, arguments)
    val ownsChanged = arguments.count { it == changed } == 1
    if (!ownsChanged && literalWrittenTo(changed, callIndex) and FIRST_PARAMETER_STABILITY_BITS != 0L) {
        throw PatchException("Shared \$changed argument marks the modifier stable at $callIndex in ${descriptor(this)}")
    }
    val (moved, zero, distance) = lowFreeRegisters(callIndex, 3, *arguments.toIntArray())

    addInstructions(
        callIndex + 1,
        "move-object/16 v$modifier, v$zero\n" + if (ownsChanged) "move/16 v$changed, v$distance" else "",
    )
    addInstructions(
        callIndex,
        """
            invoke-static { }, $distanceGetter
            move-result v$distance
            const/4 v$zero, 0x0
            move-object/16 v$moved, v$modifier
            invoke-static { v$moved, v$zero, v$distance }, $offset
            move-result-object v$moved
            move-object/16 v$zero, v$modifier
            move-object/16 v$modifier, v$moved
        """ + if (ownsChanged) {
            """
                move/16 v$distance, v$changed
                and-int/lit8 v$changed, v$changed, ${FIRST_PARAMETER_STABILITY_BITS.inv()}
            """
        } else {
            ""
        },
    )
}

private fun changedArgumentRegister(call: Instruction, arguments: List<Int>): Int {
    val parameters = call.getReference<MethodReference>()!!.parameterTypes.map(CharSequence::toString)
    val composer = parameters.indexOf(COMPOSER)
    if (composer < 0 || composer == parameters.lastIndex) {
        throw PatchException("Expected a composable call, found ${descriptor(call.getReference<MethodReference>()!!)}")
    }
    return arguments[parameters.take(composer + 1).sumOf { if (it == "J" || it == "D") 2L else 1L }.toInt()]
}

private fun registersOf(instruction: Instruction): List<Int> = when (instruction) {
    is RegisterRangeInstruction, is FiveRegisterInstruction -> argumentRegisters(instruction)
    is TwoRegisterInstruction -> listOf(instruction.registerA, instruction.registerB)
    is OneRegisterInstruction -> listOf(instruction.registerA)
    else -> emptyList()
}

private fun argumentRegisters(call: Instruction): List<Int> = when (call) {
    is RegisterRangeInstruction -> List(call.registerCount) { call.startRegister + it }
    is FiveRegisterInstruction -> listOf(call.registerC, call.registerD, call.registerE, call.registerF, call.registerG)
        .take(call.registerCount)
    else -> throw PatchException("Expected an invoke, found ${call.opcode}")
}

private fun MutableMethod.lowFreeRegisters(index: Int, count: Int, vararg excluded: Int): List<Int> {
    val provider = getFreeRegisterProvider(index, count, *excluded)
    val registers = generateSequence { runCatching { provider.getFreeRegister() }.getOrNull() }
        .filter { it <= LAST_FIVE_REGISTER_INVOKE_REGISTER }
        .take(count)
        .toList()
    if (registers.size < count) {
        throw PatchException("Found ${registers.size} of $count free registers below v16 at index $index in ${descriptor(this)}")
    }
    return registers
}
