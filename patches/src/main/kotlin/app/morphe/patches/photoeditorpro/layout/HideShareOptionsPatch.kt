/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.EXTENSION_PACKAGE
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val RESULT_SCREEN_CLASS = "$EXTENSION_PACKAGE/ResultScreen;"

@Suppress("unused")
val hideShareOptionsPatch = bytecodePatch(
    name = "Hide share options",
    description = "Adds an option to hide the share buttons on the save screen " +
        "and center the saved photo.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        SHARE_ROW_FINGERPRINTS.forEach { fingerprint ->
            val match = fingerprint.matchSingle()
            val setAdapterCall = match.instructionMatches.single()
            val shareRowRegister = setAdapterCall.getInstruction<FiveRegisterInstruction>().registerC

            match.method.addInstruction(
                setAdapterCall.index + 1,
                "invoke-static { v$shareRowRegister }, " +
                    "$RESULT_SCREEN_CLASS->hideShareRowAndCenterPreview(Landroid/view/View;)V",
            )
        }
        markPatchInstalled("pep_hide_share")
    }
}
