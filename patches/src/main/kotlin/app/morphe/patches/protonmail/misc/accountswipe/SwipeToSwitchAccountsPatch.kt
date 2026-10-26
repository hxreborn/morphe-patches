/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.accountswipe

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.protonmail.shared.uniffiCallsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/accountswipe/AccountAvatarSwipe;"

@Suppress("unused")
val swipeToSwitchAccountsPatch = bytecodePatch(
    name = "Swipe to switch accounts",
    description = "Switches between signed-in accounts by swiping up or down on the account avatar.",
) {
    dependsOn(patchesSettingsPatch, uniffiCallsPatch)
    compatibleWith(AppCompatibilities.PROTON_MAIL)

    extendWith("extensions/extension.mpe")

    execute {
        markPatchApplied("swipeToSwitchAccounts")

        val onTouchEventField = PointerInteropFilterTouchFingerprint.matchSingle()
            .instructionMatches[2].getInstruction<ReferenceInstruction>().getReference<FieldReference>()!!
        val interopFilterType = onTouchEventField.definingClass
        val function1Type = onTouchEventField.type
        val modifierType = classDefBy(classDefBy(interopFilterType).interfaces.single()).interfaces.single()
        val modifierThen = classDefBy(modifierType).methods.single {
            it.parameterTypes == listOf(modifierType) && it.returnType == modifierType
        }

        val extension = mutableClassDefBy(EXTENSION_CLASS)
        extension.interfaces.add(function1Type)

        val avatarMatch = AccountAvatarFingerprint.matchSingle()
        avatarMatch.method.apply {
            val buttonCallIndex = indexOfFirstInstructionOrThrow(avatarMatch.instructionMatches.single().index) {
                (opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) &&
                    (getReference<MethodReference>()?.parameterTypes?.getOrNull(1) == modifierType)
            }
            val buttonCall = getInstruction<ReferenceInstruction>(buttonCallIndex)
            val onClickType = buttonCall.getReference<MethodReference>()!!.parameterTypes.first().toString()
            val (onClickRegister, modifierRegister) = when (buttonCall) {
                is RegisterRangeInstruction -> buttonCall.startRegister to buttonCall.startRegister + 1
                is FiveRegisterInstruction -> buttonCall.registerC to buttonCall.registerD
                else -> throw PatchException("Unexpected account avatar button call: ${buttonCall.opcode}")
            }
            if (onClickRegister > 15 || modifierRegister > 15) {
                throw PatchException("Account avatar button arguments exceed invoke encoding limit")
            }

            val avatarSizeField = getInstruction<ReferenceInstruction>(
                indexOfFirstInstructionReversedOrThrow(buttonCallIndex) {
                    opcode == Opcode.SGET && getReference<FieldReference>()?.type == "F"
                },
            ).getReference<FieldReference>()!!

            extension.replaceStub(
                "withSwipe",
                4,
                """
                    new-instance v0, $EXTENSION_CLASS
                    sget v1, ${descriptor(avatarSizeField)}
                    invoke-direct { v0, p1, v1 }, $EXTENSION_CLASS-><init>(Ljava/lang/Object;F)V
                    new-instance p1, $interopFilterType
                    invoke-direct { p1 }, $interopFilterType-><init>()V
                    iput-object v0, p1, $interopFilterType->${onTouchEventField.name}:$function1Type
                    check-cast p0, $modifierType
                    invoke-interface { p0, p1 }, $modifierType->${modifierThen.name}($modifierType)$modifierType
                    move-result-object p0
                    return-object p0
                """,
            )

            addInstructions(
                buttonCallIndex,
                """
                    invoke-static { v$modifierRegister, v$onClickRegister }, $EXTENSION_CLASS->withSwipe(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$modifierRegister
                    check-cast v$modifierRegister, $modifierType
                """,
            )

            extension.replaceStub(
                "invokeOnClick",
                1,
                """
                    check-cast p0, $onClickType
                    invoke-interface { p0 }, $onClickType->invoke()Ljava/lang/Object;
                    return-void
                """,
            )
        }

        WatchAccountsFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->captureMailSession(Ljava/lang/Object;)V",
        )
    }
}
