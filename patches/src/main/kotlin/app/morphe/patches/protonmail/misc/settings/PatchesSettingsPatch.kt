/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.protonmail.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.misc.proton.PATCHES_MENU_CLASS
import app.morphe.patches.shared.misc.proton.SETTINGS_ROW_TITLE
import app.morphe.patches.shared.misc.proton.injectAppCompatDefaultNightMode
import app.morphe.patches.shared.misc.proton.injectBundleVersion
import app.morphe.patches.shared.misc.proton.literalWrittenTo
import app.morphe.patches.shared.misc.proton.patchesSettingsActivityPatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SETTINGS_ITEM_NAME_PARAMETER = 1
private const val SETTINGS_ITEM_ICON_PARAMETER = 3
private const val SETTINGS_ITEM_BADGE_PARAMETER = 6
private const val SETTINGS_ITEM_TEMPLATE_INDEX = 1

private fun MethodReference.registerOffsetOf(parameter: Int) =
    parameterTypes.take(parameter).sumOf { if (it == "J" || it == "D") 2 else 1 }

private fun MutableMethod.settingsItemCalls() = instructions.withIndex().filter { (_, instruction) ->
    instruction.opcode == Opcode.INVOKE_STATIC_RANGE &&
        instruction.getReference<MethodReference>()?.let {
            it.returnType == "V" &&
                it.parameterTypes.size in 10..11 &&
                it.parameterTypes[SETTINGS_ITEM_NAME_PARAMETER] == "Ljava/lang/String;" &&
                it.parameterTypes.takeLast(2).all { parameter -> parameter == "I" }
        } == true
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(
        spoofSignaturePatch,
        resourceMappingPatch,
        patchesSettingsActivityPatch("@style/ProtonTheme.Mail"),
    )

    execute {
        injectBundleVersion()
        injectAppCompatDefaultNightMode()

        val iconId = getResourceId(ResourceType.DRAWABLE, "ic_proton_wrench")
            ?: throw PatchException("Missing settings icon: ic_proton_wrench")

        with(MainSettingsItemsFingerprint.matchSingle().method) {
            val (callIndex, settingsItemCall) =
                settingsItemCalls().getOrNull(SETTINGS_ITEM_TEMPLATE_INDEX)
                    ?: throw PatchException(
                        "Missing settings item template at index $SETTINGS_ITEM_TEMPLATE_INDEX",
                    )

            val reference = settingsItemCall.getReference<MethodReference>()!!
            val first = (settingsItemCall as RegisterRangeInstruction).startRegister
            val last = first + settingsItemCall.registerCount - 1

            fun register(parameter: Int) = first + reference.registerOffsetOf(parameter)

            val hasBadge = reference.parameterTypes.size == 11
            val onClickParameter = reference.parameterTypes.size - 4
            if (hasBadge && literalWrittenTo(register(SETTINGS_ITEM_BADGE_PARAMETER), callIndex) != 0L) {
                throw PatchException(
                    "Settings item template at instruction $callIndex has a nonzero badge " +
                        "parameter",
                )
            }

            val defaultArgumentsMask = literalWrittenTo(last, callIndex)
            val onClickType = reference.parameterTypes[onClickParameter].toString()

            val dividerReference = getInstruction<FiveRegisterInstruction>(
                indexOfFirstInstructionOrThrow(callIndex, Opcode.INVOKE_STATIC),
            ).getReference<MethodReference>()!!

            val firstItemIndex = settingsItemCalls().first().index
            val anchorIndex = indexOfFirstInstructionOrThrow(firstItemIndex) {
                opcode == Opcode.INVOKE_STATIC &&
                    getReference<MethodReference>() == dividerReference
            }
            val dividerCall = getInstruction<FiveRegisterInstruction>(anchorIndex)

            addInstructions(
                anchorIndex + 1,
                """
                    const-class v${register(0)}, $onClickType
                    invoke-static/range { v${register(0)} .. v${register(0)} }, $PATCHES_MENU_CLASS->settingsRowOnClick(Ljava/lang/Class;)Ljava/lang/Object;
                    move-result-object v${register(onClickParameter)}
                    check-cast v${register(onClickParameter)}, $onClickType
                    const v${register(0)}, 0x0
                    const-string v${register(SETTINGS_ITEM_NAME_PARAMETER)}, "$SETTINGS_ROW_TITLE"
                    const v${register(SETTINGS_ITEM_ICON_PARAMETER)}, $iconId
                    ${if (hasBadge) "const v${register(SETTINGS_ITEM_BADGE_PARAMETER)}, 0x0" else ""}
                    const v${last - 1}, 0x0
                    const v$last, $defaultArgumentsMask
                    invoke-static/range { v$first .. v$last }, ${DexFormatter.INSTANCE.getMethodDescriptor(reference)}
                    invoke-static { v${dividerCall.registerC}, v${dividerCall.registerD} }, ${DexFormatter.INSTANCE.getMethodDescriptor(dividerReference)}
                """,
            )
        }
    }
}
