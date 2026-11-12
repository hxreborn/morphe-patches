/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.minipages

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Suppress("unused")
val pageThumbnailsPatch = bytecodePatch(
    name = "Page thumbnails",
    description = "Adds a button next to the page slider that shows the book's pages as thumbnails.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    execute {
        registerReadEraPatch("Page thumbnails", "Adds a button next to the page slider that shows pages as thumbnails.")
        val buttonId = HidePageThumbnailsButtonFingerprint.let {
            val (buttonIdLiteral, _, _, _, hideCall) = it.instructionMatches
            val id = it.method.getInstruction<WideLiteralInstruction>(buttonIdLiteral.index).wideLiteral
            it.method.removeInstruction(hideCall.index)
            id
        }

        hideButtonOnInflateFingerprint(buttonId).let {
            val (_, _, buttonResult, hideCall) = it.instructionMatches
            val buttonRegister = it.method.getInstruction<OneRegisterInstruction>(buttonResult.index).registerA
            val hideIndex = hideCall.index
            check(it.method.getInstruction<FiveRegisterInstruction>(hideIndex).registerC == buttonRegister) {
                "setVisibility target is not the page thumbnails button v$buttonRegister"
            }
            it.method.removeInstruction(hideIndex)
        }
    }
}
