/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.post

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val customRetakeCountPatch = bytecodePatch(
    name = "Custom retake count",
    description = "Asks before each BeReal how many retakes your friends see on it.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded("${BEREAL_EXTENSION_PACKAGE}RetakeCount;")
        CreatePostRetakeCounterSetterFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, ${BEREAL_EXTENSION_PACKAGE}RetakeCount;->retakeCount(I)I
                move-result p1
            """,
        )

        val sendPost = SendPostDelegateFingerprint.matchSingle()
        val continuationType = sendPost.instructionMatches.first()
            .getInstruction<ReferenceInstruction>().getReference<TypeReference>()!!.type
        val construction = sendPostContinuationConstructionFingerprint(continuationType)
            .matchOrNull(sendPost.method)
            ?: throw PatchException("No $continuationType construction in ${sendPost.classDef.type}")
        sendPost.method.addInstruction(
            construction.instructionMatches.single().index + 1,
            "invoke-static/range { p4 .. p4 }, ${BEREAL_EXTENSION_PACKAGE}RetakeCount;->sendStarted(I)V",
        )
    }
}
