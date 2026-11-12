/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SELECTION_TOOLBAR = "Lapp/hxreborn/extension/readera/SelectionToolbar;"
private const val SCROLL_VIEW = "Landroid/widget/ScrollView;"
private const val ATTACH_REGISTERS = 3

internal val selectionToolbarPatch = bytecodePatch {
    execute {
        val clipboard = ClipboardCopyFingerprint.matchAll(1..1).single().originalMethod
        val copy = selectionCopyFingerprint(clipboard).matchAll(1..1).single()
        val (hasSelectionCall, textCall, hideCall, activityRead, clearCall) = copy.instructionMatches
        val hasSelection = hasSelectionCall.instruction.getReference<MethodReference>()!!
        val text = textCall.instruction.getReference<MethodReference>()!!
        val hide = hideCall.instruction.getReference<MethodReference>()!!
        val activity = activityRead.instruction.getReference<FieldReference>()!!
        val clear = clearCall.instruction.getReference<MethodReference>()!!
        val toolbarType = copy.originalMethod.definingClass
        check(hasSelection.definingClass == toolbarType && text.definingClass == toolbarType && hide.definingClass == toolbarType) {
            "Selection toolbar types disagree: $toolbarType, ${descriptor(hasSelection)}, ${descriptor(text)}, ${descriptor(hide)}"
        }

        val positionHelper = SelectionPositionFingerprint.matchAll(1..1).single().originalMethod
        val (documentType, selectionType) = positionHelper.parameterTypes.map { it.toString() }
        val documentAccessor = documentAccessorFingerprint(documentType).matchAll(1..1).single().originalMethod
        val (stateRead) = selectionTextFingerprint(text).matchAll(1..1).single().instructionMatches
        val state = stateRead.instruction.getReference<FieldReference>()!!
        val selection = classDefBy(state.type).fields.single { it.type == selectionType }

        mutableClassDefBy(toolbarType).apply {
            interfaces.add(SELECTION_TOOLBAR)
            addMethod("selectionText", listOf("Z"), "Ljava/lang/String;", AccessFlags.PUBLIC.value, 3, """
                invoke-direct {p0}, ${descriptor(hasSelection)}
                move-result v0
                if-eqz v0, :missing
                invoke-direct {p0, p1}, ${descriptor(text)}
                move-result-object v0
                if-eqz v0, :missing
                return-object v0
                :missing
                const-string v0, ""
                return-object v0
            """)
            addMethod("selectionPosition", emptyList(), "Ljava/lang/Object;", AccessFlags.PUBLIC.value, 3, """
                invoke-direct {p0}, ${descriptor(hasSelection)}
                move-result v0
                if-eqz v0, :missing
                iget-object v0, p0, ${descriptor(activity)}
                invoke-virtual {v0}, ${descriptor(documentAccessor)}
                move-result-object v0
                if-eqz v0, :missing
                iget-object v1, p0, ${descriptor(state)}
                iget-object v1, v1, ${descriptor(selection)}
                if-eqz v1, :missing
                invoke-static {v0, v1}, ${descriptor(positionHelper)}
                move-result-object v0
                return-object v0
                :missing
                const/4 v0, 0x0
                return-object v0
            """)
            addMethod("closeSelection", emptyList(), "V", AccessFlags.PUBLIC.value, 2, """
                invoke-virtual {p0}, ${descriptor(hide)}
                iget-object v0, p0, ${descriptor(activity)}
                invoke-virtual {v0}, ${descriptor(clear)}
                return-void
            """)
            addMethod("selectionActivity", emptyList(), "Landroid/app/Activity;", AccessFlags.PUBLIC.value, 2, """
                iget-object v0, p0, ${descriptor(activity)}
                return-object v0
            """)
        }
    }
}

internal fun BytecodePatchContext.addSelectionAction(actionClass: String) {
    val menus = SelectionMenusFingerprint.matchAll(1..1).single()
    val (_, selectionMenuWrite, _, quoteMenuWrite) = menus.instructionMatches
    val selectionMenu = selectionMenuWrite.instruction.getReference<FieldReference>()!!
    val quoteMenu = quoteMenuWrite.instruction.getReference<FieldReference>()!!
    menus.method.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { returnIndex ->
        val registers = menus.method.getFreeRegisterProvider(returnIndex, ATTACH_REGISTERS)
        val toolbar = registers.getFreeRegister4Bit()
        val selectionList = registers.getFreeRegister4Bit()
        val quoteList = registers.getFreeRegister4Bit()
        menus.method.addInstructionsAtControlFlowLabel(returnIndex, """
            move-object/from16 v$toolbar, p0
            iget-object v$selectionList, v$toolbar, ${descriptor(selectionMenu)}
            iget-object v$quoteList, v$toolbar, ${descriptor(quoteMenu)}
            invoke-static {v$toolbar, v$selectionList, v$quoteList}, $actionClass->attach($SELECTION_TOOLBAR$SCROLL_VIEW$SCROLL_VIEW)V
        """)
    }
}
