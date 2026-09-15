/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.protonmail.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.returnEarly
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val MENU_CLASS = "Lapp/hxreborn/extension/protonmail/PatchesMenu;"
private const val SETTINGS_ROW_NAME = "hxreborn patches"

private const val SETTINGS_ITEM_NAME_PARAMETER = 1
private const val SETTINGS_ITEM_ICON_PARAMETER = 3
private const val SETTINGS_ITEM_BADGE_PARAMETER = 6
private const val SETTINGS_ITEM_TEMPLATE_INDEX = 1

private val MethodReference.smaliDescriptor
    get() = "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"

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

private fun MutableMethod.literalWrittenTo(register: Int, before: Int): Long {
    for (index in before - 1 downTo 0) {
        val instruction = getInstruction(index)
        if (instruction !is OneRegisterInstruction || instruction.registerA != register) continue
        if (instruction !is WideLiteralInstruction) {
            throw PatchException(
                "Expected a literal write to settings item register v$register at instruction " +
                    "$index, found ${instruction.opcode}",
            )
        }
        return instruction.wideLiteral
    }
    throw PatchException(
        "No write to settings item register v$register before instruction $before",
    )
}

private const val SETTINGS_ACTIVITY_CLASS =
    "app.hxreborn.extension.protonmail.PatchesSettingsActivity"

private val patchesSettingsActivityPatch = resourcePatch {
    finalize {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", SETTINGS_ACTIVITY_CLASS)
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:theme", "@style/ProtonTheme.Mail")
            application.appendChild(activity)
        }
    }
}

private const val APPLIED_PATCHES_CLASS = "Lapp/hxreborn/extension/protonmail/AppliedPatches;"

internal fun BytecodePatchContext.markPatchApplied(methodName: String) =
    mutableClassDefBy(APPLIED_PATCHES_CLASS).methods
        .single { it.name == methodName }
        .returnEarly(true)

internal fun appliedPatchMarkerPatch(methodName: String) = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute { markPatchApplied(methodName) }
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(spoofSignaturePatch, resourceMappingPatch, patchesSettingsActivityPatch)

    execute {
        val bundleVersion = PatchesSettingsVersion::class.java
            .getResourceAsStream("/protonmail-bundle-version.txt")?.bufferedReader()?.use {
                it.readText().trim()
            } ?: throw PatchException("Patch bundle version resource is unavailable")
        mutableClassDefBy(MENU_CLASS).methods.single { it.name == "bundleVersion" }
            .returnEarly(bundleVersion)

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
                    invoke-static/range { v${register(0)} .. v${register(0)} }, $MENU_CLASS->settingsRowAction(Ljava/lang/Class;)Ljava/lang/Object;
                    move-result-object v${register(onClickParameter)}
                    check-cast v${register(onClickParameter)}, $onClickType
                    const v${register(0)}, 0x0
                    const-string v${register(SETTINGS_ITEM_NAME_PARAMETER)}, "$SETTINGS_ROW_NAME"
                    const v${register(SETTINGS_ITEM_ICON_PARAMETER)}, $iconId
                    ${if (hasBadge) "const v${register(SETTINGS_ITEM_BADGE_PARAMETER)}, 0x0" else ""}
                    const v${last - 1}, 0x0
                    const v$last, $defaultArgumentsMask
                    invoke-static/range { v$first .. v$last }, ${reference.smaliDescriptor}
                    invoke-static { v${dividerCall.registerC}, v${dividerCall.registerD} }, ${dividerReference.smaliDescriptor}
                """,
            )
        }
    }
}

private object PatchesSettingsVersion
