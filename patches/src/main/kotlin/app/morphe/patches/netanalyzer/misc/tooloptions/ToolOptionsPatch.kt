/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.tooloptions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.netanalyzer.shared.FEATURES_CLASS
import app.morphe.patches.netanalyzer.shared.MaterialIcons
import app.morphe.patches.netanalyzer.shared.MenuSection
import app.morphe.patches.netanalyzer.shared.ToolItemNormaliserFingerprint
import app.morphe.patches.netanalyzer.shared.addMenuEntry
import app.morphe.patches.netanalyzer.shared.addToolbarIcon
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.netanalyzer.shared.propertyFields
import app.morphe.patches.netanalyzer.shared.requireNibbleRegisters
import app.morphe.patches.netanalyzer.shared.textButtonSlot
import app.morphe.patches.netanalyzer.shared.toolbarActionsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val TOOL_OPTION_PROPERTIES = listOf(
    "pingDelay", "pingProtocol", "pingTcpPort", "maxDelay", "minDelay", "tracerouteUdp", "payloadSize",
    "dontFragment", "audible", "portscanDelay", "whoisServer", "rdapServer", "dnssec", "norec", "showAllSections",
)

@Suppress("unused")
val toolOptionsPatch = bytecodePatch(
    name = "Tool options",
    description = "Adds ping protocol, TCP ping, probe delay, payload size, don't fragment and audible ping, " +
        "traceroute and port scan delays, whois and RDAP servers, and DNS flags.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(toolbarActionsPatch, patchesMenuPatch)

    execute {
        addToolbarIcon(textButtonSlot("ToolsQueryMenu"), "settings", MaterialIcons.SETTINGS, "Options", "ToolOptionsActivity")
        addMenuEntry(MenuSection.SETTINGS, "Tool Options", "ToolOptionsActivity")

        val normaliser = ToolItemNormaliserFingerprint.matchSingle()
        val itemType = normaliser.originalMethod.returnType
        val fields = classDefBy(itemType).propertyFields()
        val fieldNames = TOOL_OPTION_PROPERTIES.joinToString(",") { property ->
            "$property=${fields[property] ?: throw PatchException("$itemType has no $property property")}"
        }

        val returnIndex = normaliser.instructionMatches.last().index
        val itemRegister = normaliser.method.getInstruction<OneRegisterInstruction>(returnIndex).registerA
        val fieldNamesRegister = normaliser.method.getFreeRegisterProvider(returnIndex, 1, itemRegister)
            .getFreeRegister4Bit()
        requireNibbleRegisters(itemRegister)
        normaliser.method.addInstructions(
            returnIndex,
            """
                const-string v$fieldNamesRegister, "$fieldNames"
                invoke-static { v$itemRegister, v$fieldNamesRegister }, $FEATURES_CLASS->applyToolOptions(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
                move-result-object v$itemRegister
                check-cast v$itemRegister, $itemType
            """,
        )
    }
}
