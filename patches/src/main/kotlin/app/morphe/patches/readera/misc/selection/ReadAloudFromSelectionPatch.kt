/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.selection

import app.morphe.patcher.patch.patch
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
import app.morphe.patches.shared.misc.extension.readeraReadAloudFromSelectionExtensionPatch
import app.morphe.patches.shared.misc.extension.readeraSelectionMenuExtensionPatch

private const val SELECTION_READ_ALOUD = "Lapp/hxreborn/extension/readera/selection/SelectionReadAloud;"

@Suppress("unused")
val readAloudFromSelectionPatch = patch(
    name = "Read aloud from selection",
    description = "Starts Read aloud from the selected text. Found under More in the text selection menu.",
) {
    dependsOn(readeraReadAloudFromSelectionExtensionPatch, readeraSelectionMenuExtensionPatch)
    compatibleWith(AppCompatibilities.READERA)
    availability(requireArm64)
    dependsOn(
        patchesSettingsPatch,
        disableSignatureChecksPatch,
        chapterReaderPatch,
        selectionToolbarPatch,
    )

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
