/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flipaclip.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val STATE_BRANCH_OFFSET = 2
private const val NO_AD_BRANCH_WINDOW = 8

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes fullscreen ads when opening projects.",
) {
    compatibleWith(AppCompatibilities.FLIPACLIP)
    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)
    availability(requireArm64)

    execute {
        val showInterstitial = ShowInterstitialFingerprint.matchSingle()
        showInterstitial.method.apply {
            val instructions = implementation!!.instructions
            val branchIndices = instructions.windowed(NO_AD_BRANCH_WINDOW).withIndex().mapNotNull { (start, window) ->
                val candidates = window.iterator()
                val stateRead = candidates.next()
                val activeState = candidates.next()
                val branch = candidates.next()
                val normalize = candidates.next()
                val noAd = candidates.next()
                val initializeNoAd = candidates.next()
                val complete = candidates.next()
                val exit = candidates.next()
                val stateField = stateRead.getReference<FieldReference>() ?: return@mapNotNull null
                val activeField = activeState.getReference<FieldReference>() ?: return@mapNotNull null
                val stateRegisters = stateRead as? TwoRegisterInstruction ?: return@mapNotNull null
                val activeRegister = activeState as? OneRegisterInstruction ?: return@mapNotNull null
                val branchRegisters = branch as? TwoRegisterInstruction ?: return@mapNotNull null
                val branchTarget = branch as? BuilderOffsetInstruction ?: return@mapNotNull null
                val noAdRegister = noAd as? OneRegisterInstruction ?: return@mapNotNull null
                val noAdType = noAd.getReference<TypeReference>()?.type ?: return@mapNotNull null
                val noAdConstructor = initializeNoAd as? Instruction35c ?: return@mapNotNull null
                val noAdCompletion = complete as? Instruction35c ?: return@mapNotNull null
                val constructor = initializeNoAd.getReference<MethodReference>() ?: return@mapNotNull null
                val callback = complete.getReference<MethodReference>() ?: return@mapNotNull null
                val normalizedArgument = normalize.getReference<MethodReference>() ?: return@mapNotNull null
                val adBoxRead = instructions.subList(0, start).any { prior ->
                    val priorRegisters = prior as? TwoRegisterInstruction
                    val priorField = prior.getReference<FieldReference>()
                    prior.opcode == Opcode.IGET_OBJECT && priorRegisters?.registerA == stateRegisters.registerB &&
                        priorField?.definingClass == showInterstitial.classDef.type && priorField?.type == stateField.definingClass
                }
                val callbackReceiverConstructed = instructions.subList(0, start).any { prior ->
                    prior.opcode == Opcode.NEW_INSTANCE &&
                        (prior as? OneRegisterInstruction)?.registerA == noAdCompletion.registerC &&
                        prior.getReference<TypeReference>()?.type == callback.definingClass
                }
                if (stateRead.opcode != Opcode.IGET_OBJECT || activeState.opcode != Opcode.SGET_OBJECT ||
                    branch.opcode != Opcode.IF_EQ || stateField.type != activeField.type ||
                    activeField.definingClass != activeField.type || !adBoxRead || !callbackReceiverConstructed ||
                    branchRegisters.registerA != stateRegisters.registerA || branchRegisters.registerB != activeRegister.registerA ||
                    branchTarget.target.location.index != start + NO_AD_BRANCH_WINDOW ||
                    normalize.opcode != Opcode.INVOKE_STATIC_RANGE ||
                    normalizedArgument.definingClass != "Ljava/util/Objects;" || normalizedArgument.name != "toString" ||
                    normalizedArgument.parameterTypes != listOf("Ljava/lang/Object;") ||
                    normalizedArgument.returnType != "Ljava/lang/String;" ||
                    noAd.opcode != Opcode.NEW_INSTANCE || initializeNoAd.opcode != Opcode.INVOKE_DIRECT ||
                    noAdConstructor.registerCount != 1 || noAdConstructor.registerC != noAdRegister.registerA ||
                    constructor.name != "<init>" || constructor.definingClass != "Ljava/lang/Object;" ||
                    classDefBy(noAdType).superclass != callback.parameterTypes.singleOrNull()?.toString() ||
                    complete.opcode != Opcode.INVOKE_VIRTUAL || noAdCompletion.registerCount != 2 ||
                    noAdCompletion.registerD != noAdRegister.registerA || callback.parameterTypes.size != 1 ||
                    callback.returnType != "V" || exit.opcode != Opcode.RETURN_VOID
                ) return@mapNotNull null
                start + STATE_BRANCH_OFFSET
            }
            if (branchIndices.size != 1) throw PatchException("Expected one AdBox interstitial state branch, found ${branchIndices.size}")
            replaceInstruction(branchIndices.single(), "nop")
        }
    }
}
