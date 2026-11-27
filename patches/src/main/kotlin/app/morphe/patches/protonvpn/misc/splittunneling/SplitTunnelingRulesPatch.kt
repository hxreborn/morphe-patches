/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.splittunneling

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getReference
import app.morphe.patches.protonvpn.misc.anchors.resourceFieldsPatch
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.protonSplitTunnelingRulesExtensionPatch
import app.morphe.patches.shared.misc.proton.SettingsRowIcon
import app.morphe.patches.shared.misc.proton.SettingsRowSpec
import app.morphe.patches.shared.misc.proton.SettingsRowSummary
import app.morphe.patches.shared.misc.proton.VPN_SETTINGS_CLASS
import app.morphe.patches.shared.misc.proton.addSettingsRowMethod
import app.morphe.patches.shared.misc.proton.forwardStub
import app.morphe.patches.shared.misc.proton.markPatchApplied
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private const val SPLIT_TUNNELING_RULES = "Lapp/hxreborn/extension/protonvpn/SplitTunnelingRules;"
private const val SPLIT_TUNNELING_RULE_ROWS = "Lapp/hxreborn/extension/protonvpn/SplitTunnelingRuleRows;"
private const val WITH_MATCHING_APPS =
    "${SPLIT_TUNNELING_RULES}->withMatchingApps(Ljava/util/List;)Ljava/util/List;"
private val APP_LIST_GETTERS = setOf(INCLUDED_APPS_GETTER, EXCLUDED_APPS_GETTER)
private const val RULES_ROW_TITLE = "Rules"
private const val RULES_ROW_CLICK = "${SPLIT_TUNNELING_RULE_ROWS}->rulesRowOnClick(Ljava/lang/Class;)Ljava/lang/Object;"
private const val RULES_ROW_SUMMARY = "${SPLIT_TUNNELING_RULE_ROWS}->rulesRowSummary()Ljava/lang/String;"
private const val APPS_SUMMARY = "${SPLIT_TUNNELING_RULE_ROWS}->appsSummary(Ljava/lang/String;)Ljava/lang/String;"
private const val RULES_ROW_METHOD = "splitTunnelingRulesRow"
private const val ICON_PARAMETER = 1
private const val TITLE_PARAMETER = 2
private const val SUMMARY_PARAMETER = 3
private const val ON_CLICK_PARAMETER = 8
private const val COMPOSER_PARAMETER = 9

private val logger = Logger.getLogger("app.morphe.patches.protonvpn.misc.splittunneling")

private fun addRulesRowAndSummary() {
    val match = IncludedAppsRowFingerprint.match()
    val (summaryCall, includedAppsIcon, rowCall) = match.instructionMatches
    val rowMethod = match.classDef.addSettingsRowMethod(
        match.method,
        rowCall.index,
        TITLE_PARAMETER,
        ON_CLICK_PARAMETER,
        SettingsRowIcon(ICON_PARAMETER, resourceId(ResourceType.DRAWABLE, "ic_proton_sliders")),
        COMPOSER_PARAMETER,
        SettingsRowSpec(
            title = RULES_ROW_TITLE,
            onClickCall = RULES_ROW_CLICK,
            methodName = RULES_ROW_METHOD,
            summary = SettingsRowSummary(SUMMARY_PARAMETER, RULES_ROW_SUMMARY),
            defaultModifier = true,
        ),
    )

    val row = match.method.getInstruction<RegisterRangeInstruction>(rowCall.index)
    val composer = row.startRegister + COMPOSER_PARAMETER
    match.method.addInstructions(rowCall.index + 1, "invoke-static/range { v$composer .. v$composer }, $rowMethod")

    val summary = match.method.getInstruction<OneRegisterInstruction>(summaryCall.index + 1).registerA
    match.method.addInstructions(
        summaryCall.index + 2,
        """
            invoke-static/range { v$summary .. v$summary }, $APPS_SUMMARY
            move-result-object v$summary
        """,
    )
}

@Suppress("unused")
val splitTunnelingRulesPatch: Patch = patch(
    name = "Split tunneling rules",
    description = "Adds category and keyword rules that include or exclude matching apps in split tunneling, " +
        "new installs included. Reconnect to apply a change.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN, AppCompatibilities.PROTON_VPN_FDROID)
    dependsOn(
        patchesSettingsPatch,
        unlockSplitTunnelingPatch,
        resourceFieldsPatch,
        protonSplitTunnelingRulesExtensionPatch,
    )

    execute {
        markPatchApplied(VPN_SETTINGS_CLASS, "splitTunnelingRules", splitTunnelingRulesPatch)
        forwardStub(VPN_SETTINGS_CLASS, "addSplitTunnelingRuleRows", SPLIT_TUNNELING_RULE_ROWS)

        try {
            addRulesRowAndSummary()
        } catch (ex: PatchException) {
            logger.warning("Skipped the rules row on the split tunneling screen: ${ex.message}")
        }

        AppsSplitTunnelingFingerprint.method.apply {
            val reads = implementation!!.instructions.withIndex().filter { (_, instruction) ->
                instruction.getReference<MethodReference>()?.name in APP_LIST_GETTERS
            }.map { it.index }

            reads.asReversed().forEach { read ->
                if (getInstruction(read + 1).opcode != Opcode.MOVE_RESULT_OBJECT) {
                    throw PatchException("No result move after the read at $read in $this")
                }
                val list = getInstruction<OneRegisterInstruction>(read + 1).registerA
                addInstructions(
                    read + 2,
                    """
                        invoke-static/range { v$list .. v$list }, $WITH_MATCHING_APPS
                        move-result-object v$list
                    """,
                )
            }
        }
    }
}
