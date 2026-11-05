/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.more

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.shared.addHiddenIdCheck
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
val hideMoreItemsPatch = bytecodePatch(
    name = "Hide More screen items",
    description = "Hides Viber Dating, Your News, promotional banners and upsell entries from the More screen.",
) {
    compatibleWith(AppCompatibilities.VIBER)

    dependsOn(spoofSignaturePatch)

    val hideItemOptions = MoreItem.entries.associateWith { item ->
        booleanOption(
            key = "hide" + item.name.split('_').joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) },
            default = true,
            title = "Hide ${item.label}",
            description = "Hides ${item.label} from the More screen.",
        )
    }

    execute {
        val hiddenIds = hideItemOptions.filterValues { it.value == true }.keys
            .map { resourceId(ResourceType.ID, it.resourceName) }
        if (hiddenIds.isEmpty()) return@execute

        val listItems = BuildMoreItemsFingerprint.matchSingle()
        val (createItem, itemResult, addItem, nextItem) = listItems.instructionMatches
        val itemType = createItem.instruction.getReference<MethodReference>()!!.returnType
        val itemRegister = (itemResult.instruction as OneRegisterInstruction).registerA
        val isHiddenItem = mutableClassDefBy(listItems.classDef).addHiddenIdCheck("isHiddenItem", hiddenIds)

        val idRegister = listItems.method.implementation!!.registerCount
        val (method, parameterMoves) = listItems.method.addFreeRegisters(1)
        method.addInstructionsWithLabels(
            addItem.index + parameterMoves,
            """
                iget v$idRegister, v$itemRegister, ${descriptor(itemIdField(itemType))}
                invoke-static { v$idRegister }, $isHiddenItem
                move-result v$idRegister
                if-nez v$idRegister, :next
            """,
            ExternalLabel("next", method.getInstruction(nextItem.index + parameterMoves)),
        )
    }
}

private fun BytecodePatchContext.itemIdField(itemType: String): FieldReference {
    val builderType = itemConstructorFingerprint(itemType).matchSingle().method.parameterTypes.single().toString()
    val builderConstructor = itemBuilderConstructorFingerprint(builderType).matchSingle()
    val idParameterRegister = builderConstructor.method.implementation!!.registerCount - 2
    val builderIdField = builderConstructor.instructionMatches
        .single { (it.instruction as TwoRegisterInstruction).registerA == idParameterRegister }
        .instruction.getReference<FieldReference>()!!
    val (_, itemIdWrite) = itemIdCopyFingerprint(itemType, builderIdField).matchSingle().instructionMatches
    return itemIdWrite.instruction.getReference<FieldReference>()!!
}
