/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.search.bulkactions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.outlook.misc.fix.signin.fixSignInPatch
import app.morphe.patches.outlook.misc.settings.OUTLOOK_PATCH_SETTINGS_CLASS
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.outlook.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
val selectMultipleSearchResultsPatch: Patch = patch(
    name = "Select multiple search results",
    description = "Long-press search results to select several, then mark, move or delete them together. " +
        "Mail tab of search only.",
) {
    dependsOn(outlookAppliedPatchesExtensionPatch)
    compatibleWith(AppCompatibilities.OUTLOOK)

    dependsOn(fixSignInPatch, patchesSettingsPatch)

    execute {
        markPatchApplied("selectMultipleSearchResults", selectMultipleSearchResultsPatch)
        val constructor = MailSearchResultsViewModelConstructorFingerprint.match()
        val (messageListActionBarResult, centralActionBarResult, searchListActionBarResult) =
            constructor.instructionMatches.filter { it.instruction.opcode == Opcode.MOVE_RESULT }

        constructor.method.apply {
            listOf(searchListActionBarResult, centralActionBarResult, messageListActionBarResult).forEach { featureCheckResult ->
                val featureRegister = featureCheckResult.getInstruction<OneRegisterInstruction>().registerA
                addInstructions(
                    featureCheckResult.index + 1,
                    """
                        invoke-static/range { v$featureRegister .. v$featureRegister }, $OUTLOOK_PATCH_SETTINGS_CLASS->searchActionBarEnabled(Z)Z
                        move-result v$featureRegister
                    """,
                )
            }
        }

        val listAction = SearchListActionFingerprint.match()
        val (viewModelCall, conversationSelectedCall, _, _, onTapCast) = listAction.instructionMatches
        val viewModelGetter = viewModelCall.getInstruction<ReferenceInstruction>().reference
        val onConversationSelected = conversationSelectedCall.getInstruction<ReferenceInstruction>().reference
        val selectionStateFlow = SelectionUiStateFingerprint.method.returnType

        listAction.method.apply {
            val registers = getFreeRegisterProvider(onTapCast.index, 2)
            val viewModelRegister = registers.getFreeRegister4Bit()
            val valueRegister = registers.getFreeRegister4Bit()
            addInstructionsWithLabels(
                onTapCast.index,
                """
                    invoke-static {}, $OUTLOOK_PATCH_SETTINGS_CLASS->selectMultipleSearchResults()Z
                    move-result v$valueRegister
                    if-eqz v$valueRegister, :open_conversation
                    invoke-virtual {p0}, $viewModelGetter
                    move-result-object v$viewModelRegister
                    invoke-virtual {v$viewModelRegister}, $SEARCH_CONVERSATIONS_VIEW_MODEL_CLASS->getSelectionUiState()$selectionStateFlow
                    move-result-object v$valueRegister
                    invoke-interface {v$valueRegister}, $selectionStateFlow->getValue()Ljava/lang/Object;
                    move-result-object v$valueRegister
                    instance-of v$valueRegister, v$valueRegister, $HAS_SELECTION_STATE_CLASS
                    if-eqz v$valueRegister, :open_conversation
                    move-object v$valueRegister, p1
                    check-cast v$valueRegister, $ON_TAP_CLASS
                    invoke-virtual {v$valueRegister}, $ON_TAP_CLASS->getConversationHeader()Lcom/microsoft/office/outlook/mail/ConversationHeader;
                    move-result-object v$valueRegister
                    invoke-virtual {v$viewModelRegister, v$valueRegister}, $onConversationSelected
                    return-void
                """,
                ExternalLabel("open_conversation", getInstruction(onTapCast.index)),
            )
        }
    }
}
