/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yiiot.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findInstructionIndicesReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction30t
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val AD_CALLER_CLASSES = listOf(
    "Lcom/ants360/yicamera/util/AdUtil;",
    "Lcom/ants360/yicamera/util/NativeAdvertisingGoogleAdUtil;",
    "Lcom/ants360/yicamera/activity/splash/SplashActivity;",
    "Lcom/ants360/yicamera/fragment/MainPageFragment;",
)

private const val RETURN_EMPTY_LIST = """
    new-instance v0, Ljava/util/ArrayList;
    invoke-direct { v0 }, Ljava/util/ArrayList;-><init>()V
    return-object v0
"""

@Suppress("unused")
val hideAdsPatch = patch(
    name = "Hide ads",
    description = "Removes splash, interstitial, banner and native ads. " +
        "Hides promotional cards and popups on Home and Highlights. " +
        "Keeps the optional ad that unlocks an alarm video.",
) {
    compatibleWith(AppCompatibilities.YI_IOT)

    execute {
        val (_, _, checkPaidService) = SetNoAdPlanFingerprint.match().instructionMatches
        val paidServiceCall = methodCall(checkPaidService.methodReference)

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

        LoadAppOpenAdFingerprint.match().let { match ->
            val (userCheck) = match.instructionMatches
            val register = userCheck.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstruction(userCheck.index, "const/4 v$register, 0x0")
        }

        AdDialogFragmentOnCreateFingerprint.match().let { match ->
            val (superOnCreate) = match.instructionMatches
            match.method.addInstructions(
                superOnCreate.index + 1,
                """
                    invoke-virtual { p0 }, Landroid/app/Activity;->finish()V
                    return-void
                """,
            )
        }

        listOf(TopBannerListFingerprint, BottomBannerListFingerprint).forEach { fingerprint ->
            fingerprint.method.addInstructions(0, RETURN_EMPTY_LIST)
        }

        HomePromoPopupFingerprint.method.addInstruction(0, "return-void")

        NebulaPageListenersFingerprint.method.addInstruction(0, "return-void")

        HighlightAdPlaceholderRowsFingerprint.method.addInstructions(0, RETURN_EMPTY_LIST)

        with(PremiumCloudRowFingerprint.match()) {
            val (_, _, skipRowJump, rowStart) = instructionMatches
            val afterPremiumCloudRow = method.getInstruction<BuilderOffsetInstruction>(skipRowJump.index).target
            method.replaceInstruction(rowStart.index, BuilderInstruction30t(Opcode.GOTO_32, afterPremiumCloudRow))
        }

        ShoppingProductsObserverFingerprint.method.addInstruction(0, "return-void")
    }
}
