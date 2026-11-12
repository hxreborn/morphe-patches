/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.selection

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.shared.ReadAloudFromCurrentPositionFingerprint
import app.morphe.patches.readera.shared.SelectionPositionFingerprint
import app.morphe.patches.readera.shared.addSelectionAction
import app.morphe.patches.readera.shared.chapterReaderPatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.readera.shared.selectionToolbarPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SELECTION_READ_ALOUD = "Lapp/hxreborn/extension/readera/SelectionReadAloud;"

@Suppress("unused")
val readAloudFromSelectionPatch = bytecodePatch(
    name = "Read aloud from selection",
    description = "Starts Read aloud from the selected text. Found under More in the text selection menu.",
) {
    compatibleWith(AppCompatibilities.READERA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch, chapterReaderPatch, selectionToolbarPatch)
    extendWith("extensions/extension.mpe")

    execute {
        registerReadEraPatch("Read aloud from selection", "Starts Read aloud from the selected text.")
        val (_, _, _, copyCall) = ReadAloudFromCurrentPositionFingerprint.matchAll(1..1).single().instructionMatches
        val readingPosition = copyCall.instruction.getReference<MethodReference>()!!.definingClass
        val selectionPosition = SelectionPositionFingerprint.matchAll(1..1).single().originalMethod
        check(selectionPosition.returnType == readingPosition) {
            "Selection position types disagree: ${descriptor(selectionPosition)}, $readingPosition"
        }
        addSelectionAction(SELECTION_READ_ALOUD)
    }
}
