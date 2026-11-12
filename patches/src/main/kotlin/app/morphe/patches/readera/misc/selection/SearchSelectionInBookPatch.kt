/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.selection

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.readera.shared.addSelectionAction
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.readera.shared.selectionToolbarPatch
import app.morphe.patches.readera.shared.stringResourceId
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode

private const val SELECTION_SEARCH = "Lapp/hxreborn/extension/readera/SelectionSearch;"
private const val TEXT_SEARCH_HINT = "Text search"

private var textSearchHint = -1L
private val textSearchHintResourcePatch = resourcePatch {
    execute {
        textSearchHint = stringResourceId(TEXT_SEARCH_HINT)
    }
}

@Suppress("unused")
val searchSelectionInBookPatch = bytecodePatch(
    name = "Search selection in book",
    description = "Searches the book for the selected text and lists every match. Found under More in the text selection menu.",
) {
    compatibleWith(AppCompatibilities.READERA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch, selectionToolbarPatch, textSearchHintResourcePatch)
    extendWith("extensions/extension.mpe")

    execute {
        registerReadEraPatch("Search selection in book", "Searches the book for the selected text.")
        val dialog = textSearchHintFingerprint(textSearchHint).matchAll(1..1).single().originalMethod.definingClass
        val dialogBase = classDefBy(dialog).superclass!!
        val open = textSearchOpenFingerprint(dialog).matchAll(1..1).single().originalMethod
        val submit = textSearchSubmitFingerprint(dialogBase).matchAll(1..1).single().originalMethod
        val activityType = open.parameterTypes.single().toString()

        mutableClassDefBy(SELECTION_SEARCH).apply {
            replaceStub("openTextSearch", 1, """
                check-cast p0, $activityType
                invoke-static {p0}, ${descriptor(open)}
                return-void
            """)
            replaceStub("submitTextSearch", 2, """
                check-cast p0, $dialogBase
                invoke-virtual {p0, p1}, ${descriptor(submit)}
                return-void
            """)
        }
        listOf(
            textSearchViewCreatedFingerprint(dialog).matchAll(1..1).single(),
            textSearchShowFingerprint(dialog).matchAll(1..1).single(),
        ).forEach { shown ->
            shown.method.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { returnIndex ->
                shown.method.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    "invoke-static {p0}, $SELECTION_SEARCH->onTextSearchShown(Ljava/lang/Object;)V",
                )
            }
        }
        addSelectionAction(SELECTION_SEARCH)
    }
}
