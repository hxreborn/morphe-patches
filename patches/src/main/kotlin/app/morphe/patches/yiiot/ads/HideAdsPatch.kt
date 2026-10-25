/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yiiot.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findInstructionIndicesReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val AD_CALLER_CLASSES = listOf(
    "Lcom/ants360/yicamera/util/AdUtil;",
    "Lcom/ants360/yicamera/util/NativeAdvertisingGoogleAdUtil;",
    "Lcom/ants360/yicamera/activity/splash/SplashActivity;",
    "Lcom/ants360/yicamera/fragment/MainPageFragment;",
)

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes splash, interstitial, banner and native ads. " +
        "Keeps the optional ad that unlocks an alarm video.",
) {
    compatibleWith(AppCompatibilities.YI_IOT)

    execute {
        val paidServiceCall = methodCall(
            SetNoAdPlanFingerprint.matchSingle().instructionMatches.last()
                .getInstruction<ReferenceInstruction>().reference as MethodReference,
        )

        AD_CALLER_CLASSES.forEach { classType ->
            var rewritten = 0
            mutableClassDefBy(classType).methods.forEach { method ->
                method.findInstructionIndicesReversed(paidServiceCall).forEach { callIndex ->
                    val moveResult = method.getInstruction<Instruction>(callIndex + 1)
                    if (moveResult.opcode != Opcode.MOVE_RESULT) return@forEach
                    val register = (moveResult as OneRegisterInstruction).registerA
                    method.addInstruction(callIndex + 2, "const/4 v$register, 0x1")
                    rewritten++
                }
            }
            if (rewritten == 0) throw PatchException("No paid service check in $classType")
        }

        LoadAppOpenAdFingerprint.matchSingle().let { match ->
            val userCheck = match.instructionMatches.first()
            val register = userCheck.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstruction(userCheck.index, "const/4 v$register, 0x0")
        }

        AdDialogFragmentOnCreateFingerprint.matchSingle().let { match ->
            match.method.addInstructions(
                match.instructionMatches.first().index + 1,
                """
                    invoke-virtual { p0 }, Landroid/app/Activity;->finish()V
                    return-void
                """,
            )
        }
    }
}
