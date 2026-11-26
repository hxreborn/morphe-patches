/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.magicposer.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.misc.extension.magicposerProtectionExtensionPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PROTECTION_CRASH_FILTER_CLASS = "Lapp/hxreborn/extension/magicposer/ProtectionCrashFilter;"

private const val UNBOX_MATCH = 1
private const val UNBOX_RESULT_MATCH = 2
private const val MAKE_TEXT_MATCH = 3
private const val MAKE_TEXT_RESULT_MATCH = 4
private const val SHOW_MATCH = 5
private const val REVIEW_AVAILABILITY_MATCH = 1

private val TOAST_CRASH_METHODS = listOf(
    "attachBaseContext" to listOf("Landroid/content/Context;"),
    "onCreate" to listOf("Landroid/os/Bundle;"),
    "onStart" to emptyList(),
    "onResume" to emptyList(),
)

internal val magicPoserProtectionPatch = patch {
    dependsOn(magicposerProtectionExtensionPatch)

    execute {
        MainActivityAttachBaseContextFingerprint.method.addInstruction(
            0,
            "invoke-static { }, $PROTECTION_CRASH_FILTER_CLASS->install()V",
        )
        var removed = 0
        while (true) {
            val comparison = tokenComparisonFingerprint().matchOrNull() ?: break
            comparison.method.removeInstruction(comparison.instructionMatches.last().index)
            removed++
        }
        if (removed == 0) throw PatchException("No integrity token comparison found")

        var crashBlocks = 0
        for ((name, parameters) in TOAST_CRASH_METHODS) {
            while (true) {
                val match = toastCrashFingerprint(name, parameters).matchOrNull() ?: break
                val matches = match.instructionMatches
                val unbox = matches[UNBOX_MATCH]
                val unboxResult = matches[UNBOX_RESULT_MATCH]
                val makeText = matches[MAKE_TEXT_MATCH]
                val makeTextResult = matches[MAKE_TEXT_RESULT_MATCH]
                val show = matches[SHOW_MATCH]
                val method = match.method
                val unboxedRegister = method.getInstruction<OneRegisterInstruction>(unboxResult.index).registerA
                method.removeInstruction(show.index)
                method.removeInstruction(makeTextResult.index)
                method.removeInstruction(makeText.index)
                method.removeInstruction(unboxResult.index)
                method.replaceInstruction(unbox.index, "const/16 v$unboxedRegister, 0x0")
                crashBlocks++
            }
        }
        if (crashBlocks == 0) throw PatchException("No integrity crash block found")

        val threadStart = ProtectionThreadStartFingerprint
        val startCall = threadStart.instructionMatches.first()
        val start = threadStart.method.getInstruction<FiveRegisterInstruction>(startCall.index)
        threadStart.method.replaceInstruction(
            startCall.index,
            "invoke-static { v${start.registerC}, v${start.registerD}, v${start.registerE} }, " +
                "$PROTECTION_CRASH_FILTER_CLASS->startProtectionThread" +
                "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)V",
        )
        val reviewFlow = ReviewFlowLaunchFingerprint
        val reviewAvailability = reviewFlow.instructionMatches[REVIEW_AVAILABILITY_MATCH]
        val reviewRegister = reviewFlow.method
            .getInstruction<OneRegisterInstruction>(reviewAvailability.index).registerA
        reviewFlow.method.replaceInstruction(reviewAvailability.index, "const/4 v$reviewRegister, 0x1")
    }
}
