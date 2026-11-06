/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.unfiled

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.p0Register
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/UnfiledChats;"
private const val PREDEFINED_FOLDER_TYPE = 1
private const val READ_ONLY_FOLDER_FLAGS = 7

@Suppress("unused")
val unfiledChatsPatch: BytecodePatch = bytecodePatch(
    name = "Unfiled chats",
    description = "Adds a folder tab for chats outside your custom folders.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        val folder = FolderEntityFingerprint.matchSingle().classDef
        val tab = FolderTabEntityFingerprint.matchSingle().classDef
        val folderConstructor = folderConstructorFingerprint(folder.type).matchSingle()
        val tabConstructor = folderTabConstructorFingerprint(tab.type, folder.type).matchSingle()
        val (idRead) = tabConstructor.instructionMatches
        val folderId = idRead.instruction.getReference<MethodReference>()!!
        val extension = mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR)
        extension.replaceResourceIdStubs(
            ResourceType.ID,
            mapOf(
                "addChatsButtonId" to "folders_empty_add_chats_button",
                "emptyTitleId" to "folders_empty_title",
                "emptySubtitleId" to "folders_empty_subtitle",
            ),
        )
        extension.replaceStub("createTab", 9, """
            new-instance v0, ${folder.type}
            const-string v1, "hx_unfiled_chats"
            const-string v2, "Unfiled"
            const/4 v3, $PREDEFINED_FOLDER_TYPE
            const/4 v4, -0x1
            const-wide/16 v5, $READ_ONLY_FOLDER_FLAGS
            const-string v7, ""
            invoke-direct/range {v0 .. v7}, ${descriptor(folderConstructor.method)}
            new-instance v8, ${tab.type}
            const/4 v1, 0x0
            invoke-direct {v8, v0, v1}, ${descriptor(tabConstructor.method)}
            return-object v8
        """)
        extension.replaceStub("folderId", 1, """
            check-cast p0, ${folder.type}
            invoke-virtual {p0}, ${descriptor(folderId)}
            move-result-object p0
            return-object p0
        """)
        val tabs = FolderTabsFingerprint.matchSingle()
        val (_, emptyCheck) = tabs.instructionMatches
        val tabsRegister = (emptyCheck.instruction as FiveRegisterInstruction).registerC
        tabs.method.replaceInstruction(
            emptyCheck.index,
            "invoke-static {v$tabsRegister}, $EXTENSION_CLASS_DESCRIPTOR->addTabAndIsEmpty(Ljava/util/List;)Z",
        )

        FolderSelectionFingerprint.matchSingle().method.addInstruction(
            0, "invoke-static/range {p1 .. p1}, $EXTENSION_CLASS_DESCRIPTOR->selectFolder(Ljava/lang/Object;)V",
        )
        val (selectedIdRead, _) = UnreadFolderFingerprint.matchSingle().instructionMatches
        val selectedIdField = selectedIdRead.instruction.getReference<FieldReference>()!!
        val loadParams = loadParamsFingerprint(selectedIdField).matchSingle()
        val (folderRead, _, saveParams) = loadParams.instructionMatches
        val originalFolderRegister = (folderRead.instruction as OneRegisterInstruction).registerA
        val queryParamsRegister = (saveParams.instruction as FiveRegisterInstruction).registerD
        val savedFolderRegister = loadParams.method.implementation!!.registerCount
        val savedParamsRegister = savedFolderRegister + 1
        val (paramsMethod, parameterMoves) = loadParams.method.addFreeRegisters(2)
        paramsMethod.addInstructionsAtControlFlowLabel(saveParams.index + parameterMoves, """
            move-object/from16 v$savedParamsRegister, v$queryParamsRegister
            invoke-static/range {v$savedFolderRegister .. v$savedParamsRegister}, $EXTENSION_CLASS_DESCRIPTOR->rememberQuery(Ljava/lang/String;Ljava/lang/Object;)V
        """)
        paramsMethod.addInstructions(folderRead.index + parameterMoves + 1, """
            move-object/from16 v$savedFolderRegister, v$originalFolderRegister
            invoke-static/range {v$originalFolderRegister .. v$originalFolderRegister}, $EXTENSION_CLASS_DESCRIPTOR->queryFolder(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$originalFolderRegister
        """)

        val folderQuery = FolderQueryFingerprint.matchSingle().method
        val dispatcher = if (folderQuery.parameterTypes.isEmpty()) {
            folderQueryDispatcherFingerprint(folderQuery).matchSingle().method
        } else {
            folderQuery
        }
        check(AccessFlags.STATIC.isSet(dispatcher.accessFlags)) { "Folder query dispatcher ${dispatcher.name} is not static" }
        check(dispatcher.parameterTypes.size == 2) {
            "Folder query dispatcher ${dispatcher.name} takes ${dispatcher.parameterTypes}, expected two parameters"
        }
        val selectionRegister = dispatcher.implementation!!.registerCount + 1
        val (queryMethod, _) = dispatcher.addFreeRegisters(2)
        val queryRegister = queryMethod.p0Register
        queryMethod.findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
            val returned = queryMethod.getInstruction<OneRegisterInstruction>(index).registerA
            queryMethod.addInstructionsAtControlFlowLabel(index, """
                move-object/from16 v$selectionRegister, v$returned
                invoke-static/range {v$queryRegister .. v$selectionRegister}, $EXTENSION_CLASS_DESCRIPTOR->filterSelection(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;
                move-result-object v$returned
            """)
        }
        val emptyView = EmptyFolderViewFingerprint.matchSingle().method
        val viewRegister = emptyView.implementation!!.registerCount
        val (expanded, _) = emptyView.addFreeRegisters(1)
        val receiver = expanded.p0Register
        expanded.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            expanded.addInstructionsAtControlFlowLabel(index, """
                invoke-virtual/range {v$receiver .. v$receiver}, Landroidx/fragment/app/Fragment;->getView()Landroid/view/View;
                move-result-object v$viewRegister
                invoke-static/range {v$viewRegister .. v$viewRegister}, $EXTENSION_CLASS_DESCRIPTOR->updateEmptyView(Landroid/view/View;)V
            """)
        }
        markPatchApplied("unfiledChats", unfiledChatsPatch)
    }
}
