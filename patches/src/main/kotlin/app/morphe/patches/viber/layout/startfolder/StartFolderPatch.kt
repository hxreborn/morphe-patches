/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.startfolder

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.p0Register
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addFieldReader
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.viber.layout.unfiled.FolderEntityFingerprint
import app.morphe.patches.viber.layout.unfiled.FolderSelectionFingerprint
import app.morphe.patches.viber.layout.unfiled.FolderTabEntityFingerprint
import app.morphe.patches.viber.layout.unfiled.folderConstructorFingerprint
import app.morphe.patches.viber.layout.unfiled.folderTabConstructorFingerprint
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/StartFolder;"
private const val FOLDER_NAME_PARAMETER_INDEX = 1
private const val FOLDER_TYPE_PARAMETER_INDEX = 2

@Suppress("unused")
val startFolderPatch: BytecodePatch = bytecodePatch(
    name = "Start folder",
    description = "Opens Chats in the selected folder.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        val folder = FolderEntityFingerprint.matchSingle().classDef
        val tab = FolderTabEntityFingerprint.matchSingle().classDef
        val tabConstructor = folderTabConstructorFingerprint(tab.type, folder.type).matchSingle()
        val (idRead) = tabConstructor.instructionMatches
        val readId = descriptor(idRead.instruction.getReference<MethodReference>()!!)
        val constructor = folderConstructorFingerprint(folder.type).matchSingle().method
        fun fieldWrittenFromParameter(parameterIndex: Int, type: String): FieldReference {
            val register = constructor.p0Register + 1 + parameterIndex
            return constructor.implementation!!.instructions.single {
                (it.opcode == Opcode.IPUT_OBJECT || it.opcode == Opcode.IPUT) &&
                    (it as TwoRegisterInstruction).registerA == register &&
                    it.registerB == constructor.p0Register &&
                    it.getReference<FieldReference>()!!.type == type
            }.getReference<FieldReference>()!!
        }
        val folderClass = mutableClassDefBy(folder)
        val readName = folderClass.addFieldReader(fieldWrittenFromParameter(FOLDER_NAME_PARAMETER_INDEX, "Ljava/lang/String;"))
        val readType = folderClass.addFieldReader(fieldWrittenFromParameter(FOLDER_TYPE_PARAMETER_INDEX, "I"))
        val tabs = startFolderTabsFingerprint(folder.type, tab.type).matchSingle()
        val (entityCall, _, pendingRead, indexLookup) = tabs.instructionMatches
        val readEntity = when (val entity = entityCall.instruction.getReference<Reference>()!!) {
            is MethodReference -> descriptor(entity)
            is FieldReference -> mutableClassDefBy(tab).addFieldReader(entity)
            else -> throw PatchException("Folder tab entity access has reference $entity")
        }
        val extension = mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR)
        extension.replaceStub("folderId", 1, """
            check-cast p0, ${tab.type}
            invoke-virtual {p0}, $readEntity
            move-result-object p0
            invoke-virtual {p0}, $readId
            move-result-object p0
            return-object p0
        """)
        extension.replaceStub("folderName", 1, """
            check-cast p0, ${tab.type}
            invoke-virtual {p0}, $readEntity
            move-result-object p0
            invoke-virtual {p0}, $readName
            move-result-object p0
            return-object p0
        """)
        extension.replaceStub("folderType", 1, """
            check-cast p0, ${tab.type}
            invoke-virtual {p0}, $readEntity
            move-result-object p0
            invoke-virtual {p0}, $readType
            move-result p0
            return p0
        """)
        extension.replaceResourceIdStubs(
            ResourceType.STRING,
            mapOf(
                "allTitleId" to "folders_tabs_all_tab_title",
                "unreadTitleId" to "unread_folder_name",
                "aiAssistantsTitleId" to "ai_assistants_folder_name",
            ),
        )
        val lookup = indexLookup.instruction as FiveRegisterInstruction
        val pending = pendingRead.instruction.getReference<FieldReference>()!!
        val owner = (pendingRead.instruction as TwoRegisterInstruction).registerB
        tabs.method.addInstructionsAtControlFlowLabel(indexLookup.index, """
            invoke-static {v${lookup.registerC}, v${lookup.registerD}, p0}, $EXTENSION_CLASS_DESCRIPTOR->initialIndex(Ljava/util/List;ILjava/lang/Object;)I
            move-result v${lookup.registerD}
            iput v${lookup.registerD}, v$owner, ${descriptor(pending)}
        """)
        val controller = FolderSelectionFingerprint.matchSingle().classDef.type
        resetFolderFingerprint(controller, folder.type).matchSingle().apply {
            val (_, position) = instructionMatches
            method.addFilterCallAfter(position.index, "$EXTENSION_CLASS_DESCRIPTOR->resetIndex(I)I")
        }
        markPatchApplied("startFolder", startFolderPatch)
    }
}
