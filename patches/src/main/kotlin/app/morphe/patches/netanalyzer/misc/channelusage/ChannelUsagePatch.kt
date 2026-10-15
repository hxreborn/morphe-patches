/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.channelusage

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.EXTENSION_PACKAGE
import app.morphe.patches.netanalyzer.shared.FEATURES_CLASS
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.WIFI_PAGER_MENU
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.composableFingerprint
import app.morphe.patches.netanalyzer.shared.composableParameters
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.netanalyzer.shared.requireNibbleRegisters
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val HAS_USAGE_PAGE = "hasUsagePage"
private const val SHOW_USAGE_PAGE = 1
private const val CHANNEL_USAGE_ACTIVITY = "ChannelUsageActivity"

@Suppress("unused")
val channelUsagePatch = bytecodePatch(
    name = "Wi-Fi channel usage",
    description = "Adds a chart of how busy each Wi-Fi channel is, stacking the networks that share it.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(patchesMenuPatch)

    execute {
        addMenuEntry(MenuSection.NETWORK, "Channel Usage", CHANNEL_USAGE_ACTIVITY)

        val menu = composableFingerprint(WIFI_PAGER_MENU).matchSingle()
        val parameters = menu.composableParameters()
        val usagePageIndex = parameters.indexOf(HAS_USAGE_PAGE)
        if (usagePageIndex < 0) throw PatchException("$WIFI_PAGER_MENU takes $parameters, expected $HAS_USAGE_PAGE")
        val method = menu.method
        if (method.parameterTypes.take(usagePageIndex).any { it.toString() == "J" || it.toString() == "D" }) {
            throw PatchException("$WIFI_PAGER_MENU has a wide parameter before $HAS_USAGE_PAGE")
        }
        val usagePageRegister = method.implementation!!.registerCount - MethodUtil.getParameterRegisterCount(method) + usagePageIndex
        method.addInstruction(0, "const/16 v$usagePageRegister, $SHOW_USAGE_PAGE")

        val usageItem = ChannelUsageMenuItemFingerprint.matchSingle()
        val itemCall = usageItem.instructionMatches.last()
        val onClickRegister = itemCall.getInstruction<FiveRegisterInstruction>().registerE
        val onClickType = usageItem.method.instructions.take(itemCall.index).last {
            it.opcode == Opcode.NEW_INSTANCE && (it as OneRegisterInstruction).registerA == onClickRegister
        }.getReference<TypeReference>()!!.type

        val selection = pageSelectionFingerprint(onClickType).matchSingle()
        val (pageLiteral, selectPage, savePage) = selection.instructionMatches
        val register = pageLiteral.getInstruction<OneRegisterInstruction>().registerA
        requireNibbleRegisters(register)
        selection.method.replaceInstruction(
            pageLiteral.index,
            "const-string v$register, \"$EXTENSION_PACKAGE.$CHANNEL_USAGE_ACTIVITY\"",
        )
        selection.method.replaceInstruction(selectPage.index, "invoke-static { v$register }, $FEATURES_CLASS->open(Ljava/lang/String;)V")
        selection.method.replaceInstruction(savePage.index, "nop")
    }
}
