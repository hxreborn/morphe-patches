/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.more

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.p0Register
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addHiddenIdCheck
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private enum class MoreItem(val label: String, val resourceName: String) {
    BUSINESS_ACCOUNT("Business Account", "business_account"),
    DATING("Viber Dating", "dating"),
    ESIM("eSIM", "esim"),
    FREE_STICKERS("Get free stickers", "get_free_stickers"),
    PROMOTIONAL_BANNERS("promotional banners", "braze_banner"),
    RAKUTEN_ACCOUNT("Rakuten account", "rakuten_account"),
    VIBER_OUT("Viber Out", "viber_out"),
    VIBER_PLUS("Viber Plus", "viber_plus"),
    YOUR_NEWS("Your News", "ai_news_brief"),
}

@Suppress("unused")
val hideMoreItemsPatch: BytecodePatch = bytecodePatch(
    name = "Hide More screen items",
    description = "Hides Viber Dating, Your News, promotional banners and upsell entries from the More screen.",
) {
    compatibleWith(AppCompatibilities.VIBER)

    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    val hideItemOptions = MoreItem.entries.associateWith { item ->
        booleanOption(
            key = "hide" + item.name.split('_').joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) },
            default = true,
            title = "Hide ${item.label}",
            description = "Hides ${item.label} from the More screen.",
        )
    }

    execute {
        markPatchApplied("hideMoreItems", hideMoreItemsPatch)
        val hiddenIds = hideItemOptions.filterValues { it.value == true }.keys
            .map { resourceId(ResourceType.ID, it.resourceName) }
        if (hiddenIds.isEmpty()) return@execute

        val listItems = BuildMoreItemsFingerprint.matchSingle()
        val (createItem, _, _, _, _) = listItems.instructionMatches
        val itemType = createItem.instruction.getReference<MethodReference>()!!.returnType
        val settings = mutableClassDefBy(VIBER_SETTINGS_CLASS)
        settings.replaceStub("itemId", 1, """
            check-cast p0, $itemType
            iget p0, p0, ${descriptor(itemIdField(itemType))}
            return p0
        """)
        val hiddenCheck = mutableClassDefBy(listItems.classDef).addHiddenIdCheck("isHiddenItem", hiddenIds)
        settings.replaceStub("isHiddenMoreItem", 1, """
            invoke-static {p0}, $hiddenCheck
            move-result p0
            return p0
        """)
        listItems.method.apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructionsAtControlFlowLabel(index, """
                    invoke-static/range {v$register .. v$register}, $VIBER_SETTINGS_CLASS->filterMoreItems(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$register
                """)
            }
        }
        val attached = moreViewAttachedFingerprint(listItems.method).matchSingle()
        val (viewRead, repositoryRead, _, setItemsCall) = attached.instructionMatches
        val viewField = viewRead.instruction.getReference<FieldReference>()!!
        val repositoryField = repositoryRead.instruction.getReference<FieldReference>()!!
        val setItems = setItemsCall.instruction.getReference<MethodReference>()!!
        val resumed = moreResumeFingerprint(attached.classDef.type).matchSingle()
        val (superResume) = resumed.instructionMatches
        val presenterRegister = resumed.method.p0Register
        val viewRegister = resumed.method.implementation!!.registerCount
        val itemsRegister = viewRegister + 1
        val (method, parameterMoves) = resumed.method.addFreeRegisters(2)
        val refreshIndex = superResume.index + parameterMoves + 1
        method.addInstructionsWithLabels(refreshIndex, """
            iget-object v$viewRegister, v$presenterRegister, ${descriptor(viewField)}
            if-eqz v$viewRegister, :original
            check-cast v$viewRegister, ${setItems.definingClass}
            iget-object v$itemsRegister, v$presenterRegister, ${descriptor(repositoryField)}
            invoke-virtual/range {v$itemsRegister .. v$itemsRegister}, ${descriptor(listItems.method)}
            move-result-object v$itemsRegister
            invoke-interface/range {v$viewRegister .. v$itemsRegister}, ${descriptor(setItems)}
        """, ExternalLabel("original", method.getInstruction(refreshIndex)))
    }
}

private fun BytecodePatchContext.itemIdField(itemType: String): FieldReference {
    val builderType = itemConstructorFingerprint(itemType).matchSingle().method.parameterTypes.single().toString()
    val builderConstructor = itemBuilderConstructorFingerprint(builderType).matchSingle()
    val idParameterRegister = builderConstructor.method.implementation!!.registerCount - builderConstructor.method.parameterTypes.lastIndex
    val builderIdField = builderConstructor.instructionMatches
        .single { (it.instruction as TwoRegisterInstruction).registerA == idParameterRegister }
        .instruction.getReference<FieldReference>()!!
    val (_, itemIdWrite) = itemIdCopyFingerprint(itemType, builderIdField).matchSingle().instructionMatches
    return itemIdWrite.instruction.getReference<FieldReference>()!!
}
