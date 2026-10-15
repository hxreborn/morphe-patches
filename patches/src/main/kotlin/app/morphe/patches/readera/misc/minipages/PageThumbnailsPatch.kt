/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.minipages

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Suppress("unused")
val pageThumbnailsPatch = bytecodePatch(
    name = "Page thumbnails",
    description = "Adds the page thumbnails button to the reader for browsing all pages of a book.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    execute {
        val buttonId = HidePageThumbnailsButtonFingerprint.let {
            val id = it.method.getInstruction<WideLiteralInstruction>(it.instructionMatches.first().index).wideLiteral
            it.method.removeInstruction(it.instructionMatches.last().index)
            id
        }

        hideButtonOnInflateFingerprint(buttonId).let {
            val buttonRegister = it.method.getInstruction<OneRegisterInstruction>(it.instructionMatches[2].index).registerA
            val hideIndex = it.instructionMatches.last().index
            check(it.method.getInstruction<FiveRegisterInstruction>(hideIndex).registerC == buttonRegister) {
                "setVisibility target is not the page thumbnails button v$buttonRegister"
            }
            it.method.removeInstruction(hideIndex)
        }
    }
}
