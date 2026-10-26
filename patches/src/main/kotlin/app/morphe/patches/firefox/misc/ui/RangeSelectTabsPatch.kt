/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/RangeSelectTabsPatch.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.instanceOf
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val RS_SELECT = "Lapp/hxreborn/extension/firefox/RangeSelect;"

private const val RS_POINTER_SCOPE = "Landroidx/compose/ui/input/pointer/PointerInputScope;"
private const val RS_DRAG_KT = "Landroidx/compose/foundation/gestures/DragGestureDetectorKt;"

private const val RS_FRAGMENT = "Lorg/mozilla/fenix/tabstray/ui/TabManagementFragment;"
private const val RS_TAB = "Lorg/mozilla/fenix/tabstray/data/TabsTrayItem\$Tab;"
private const val RS_GROUP_CLICKED = "Lorg/mozilla/fenix/tabstray/redux/action/TabGroupAction\$TabGroupClicked;"
private const val RS_LONG_CLICKED = "Lorg/mozilla/fenix/tabstray/redux/action/TabsTrayAction\$TabItemLongClicked;"

private const val RS_DETECT = "$RS_DRAG_KT->detectDragGesturesAfterLongPress(" +
    "$RS_POINTER_SCOPE$FUNCTION1$FUNCTION0$FUNCTION0$FUNCTION2$CONTINUATION)$OBJECT"

private object RangeLongClickFingerprint : Fingerprint(
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT),
    filters = listOf(
        methodCall(definingClass = RS_FRAGMENT, name = "getTabsTrayStore\$app"),
        newInstance(RS_LONG_CLICKED),
        methodCall(definingClass = RS_LONG_CLICKED, name = "<init>"),
    ),
)

private object RangeClickFingerprint : Fingerprint(
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT),
    filters = listOf(
        instanceOf(RS_TAB),
        methodCall(definingClass = FUNCTION1, name = "invoke"),
        newInstance(RS_GROUP_CLICKED),
    ),
)

private object InteractableGestureFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/fenix/tabstray/browser/compose/interactable/" +
        "InteractableListKt\$detectListPressAndDrag\$1;",
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(RS_POINTER_SCOPE, CONTINUATION),
    filters = listOf(
        fieldAccess(type = "Z", opcode = Opcode.IGET_BOOLEAN),
        opcode(Opcode.IF_EQZ, MatchAfterImmediately()),
        methodCall(definingClass = RS_DRAG_KT, name = "detectDragGesturesAfterLongPress"),
    ),
)

private object ReorderableGestureFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/fenix/tabstray/browser/compose/" +
        "ReorderableListKt\$detectListPressAndDrag\$1;",
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(RS_POINTER_SCOPE, CONTINUATION),
    filters = listOf(
        fieldAccess(type = "Z", opcode = Opcode.IGET_BOOLEAN),
        opcode(Opcode.IF_EQZ, MatchAfterImmediately()),
        methodCall(definingClass = RS_DRAG_KT, name = "detectDragGesturesAfterLongPress"),
    ),
)

private fun BytecodePatchContext.hookLongClick() {
    val method = RangeLongClickFingerprint.method
    val (storeCall, longClicked, longClickedInit) = RangeLongClickFingerprint.instructionMatches
    val fragment = (storeCall.instruction as FiveRegisterInstruction).registerC
    val item = (longClickedInit.instruction as FiveRegisterInstruction).registerD
    val scratch = (longClicked.instruction as OneRegisterInstruction).registerA

    method.applyEdits(
        insert(
            storeCall.index,
            """
                invoke-static {v$fragment, v$item}, $RS_SELECT->longPress(Ljava/lang/Object;Ljava/lang/Object;)Z
                move-result v$scratch
                if-eqz v$scratch, :hx_stock
                sget-object v$scratch, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
                return-object v$scratch
            """,
            mapOf("hx_stock" to { storeCall.index }),
        ),
    )
}

private fun BytecodePatchContext.hookClick() {
    val method = RangeClickFingerprint.method
    val tabCheck = RangeClickFingerprint.instructionMatches.first()
    val scratch = (tabCheck.instruction as TwoRegisterInstruction).registerA

    method.applyEdits(
        insert(
            tabCheck.index,
            """
                invoke-static {}, $RS_SELECT->consume()Z
                move-result v$scratch
                if-eqz v$scratch, :hx_click
                sget-object v$scratch, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
                return-object v$scratch
            """,
            mapOf("hx_click" to { tabCheck.index }),
        ),
    )
}

private fun BytecodePatchContext.hookGesture(fingerprint: Fingerprint) {
    val method = fingerprint.method
    val (_, gate) = fingerprint.instructionMatches
    val target = method.jumpTarget(gate.index)

    val unit = method.getInstruction(target)
    val back = method.getInstruction(target + 1)
    if (unit.opcode != Opcode.SGET_OBJECT || back.opcode != Opcode.RETURN_OBJECT) {
        throw PatchException("Unexpected select-mode branch target: ${unit.opcode}, ${back.opcode}")
    }

    val scopeParameter = parameterRegister(method, 0)
    val continuationParameter = parameterRegister(method, 1)
    val base = Liveness(method).takeRun(
        7,
        target,
        exclude = setOf(scopeParameter, continuationParameter),
    )
    if (base + 6 > 255) throw PatchException("Register out of range: v${base + 6}")

    val self = base
    val scope = base + 1
    val start = base + 2
    val end = base + 3
    val cancel = base + 4
    val drag = base + 5
    val continuation = base + 6

    method.applyEdits(
        insert(
            target,
            """
                invoke-static {}, $PATCH_SETTINGS->rangeSelect()Z
                move-result v$self
                if-eqz v$self, :hx_orig
                move-object/from16 v$self, p0
                invoke-static/range {v$self .. v$self}, $RS_SELECT->start(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$start
                check-cast v$start, $FUNCTION1
                invoke-static {}, $RS_SELECT->noop()Ljava/lang/Object;
                move-result-object v$end
                check-cast v$end, $FUNCTION0
                move-object/from16 v$cancel, v$end
                move-object/from16 v$drag, v$end
                check-cast v$drag, $FUNCTION2
                move-object/from16 v$scope, p1
                move-object/from16 v$continuation, p2
                invoke-static/range {v$scope .. v$continuation}, $RS_DETECT
                move-result-object v$scope
                return-object v$scope
            """,
            mapOf("hx_orig" to { target }),
        ),
    )
}

@Suppress("unused")
val rangeSelectTabsPatch = bytecodePatch(
    name = "Long press to select tab range",
    description = "Selects the tabs between two tabs when you long press the second one in selection mode. " +
        "Requires the List tab view.",
    default = true,
) {

    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("rangeSelect")


        hookLongClick()
        hookClick()
        hookGesture(InteractableGestureFingerprint)
        hookGesture(ReorderableGestureFingerprint)
    }
}
