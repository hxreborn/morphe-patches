/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.nextdoor.ads

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.nextdoor.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val BYTE_REGISTER_LIMIT = 256
private const val NIBBLE_REGISTER_LIMIT = 16

private fun Match.match(filter: InstructionFilter) = instructionMatches.single { it.filter === filter }

@Suppress("unused")
val removeSponsoredPostsPatch = patch(
    name = "Remove sponsored posts",
    description = "Removes sponsored posts from the feed and For Sale ads from listings and search results.",
) {
    compatibleWith(AppCompatibilities.NEXTDOOR)

    dependsOn(spoofSignaturePatch)

    execute {
        val match = AdsDataItemConverterFingerprint.match()
        fun indexOf(filter: InstructionFilter) = match.instructionMatches.single { it.filter === filter }.index

        match.method.apply {
            val returnWithoutRows =
                getInstruction<BuilderOffsetInstruction>(indexOf(missingAdBranch)).target.location.instruction!!
            listOf(adTypeBranch, adPlaceholderTypeBranch).map(::indexOf).sortedDescending().forEach { branchIndex ->
                addInstructionsWithLabels(
                    branchIndex + 1,
                    "goto :return_without_rows",
                    ExternalLabel("return_without_rows", returnWithoutRows),
                )
            }
        }

        val gridRollup = ClassifiedGridRollupFingerprint.match()
        val adWrapperMatch = gridRollup.match(gridAdWrapperConstructor)
        val adWrapper = adWrapperMatch.getInstruction<FiveRegisterInstruction>()
        val adWrapperTail = gridRollup.match(gridAdWrapperTail).getInstruction<TwoRegisterInstruction>()
        val tileConstructorMatch = gridRollup.match(gridTileConstructor)
        val tileConstructor = tileConstructorMatch.getInstruction<RegisterRangeInstruction>()
        val tileParameterTypes = tileConstructorMatch.methodReference.parameterTypes
        val tileMove = gridRollup.match(gridTileMove).getInstruction<TwoRegisterInstruction>()
        val tileNullCheckMatch = gridRollup.match(gridTileNullCheck)
        val tileNullCheck = tileNullCheckMatch.getInstruction<OneRegisterInstruction>()
        val tileNullCheckTarget = gridRollup.method.getInstruction<BuilderOffsetInstruction>(tileNullCheckMatch.index)
        val tileAdd = gridRollup.match(gridTileAdd)
        val tileRegister = tileConstructor.startRegister
        val adRegister = tileRegister + tileConstructor.registerCount - 1
        check(
            tileConstructor.registerCount == tileParameterTypes.size + 1 &&
                tileParameterTypes.last() == adWrapperMatch.methodReference.definingClass &&
                ROLLUP_CARD_TYPE_CLASS in tileParameterTypes,
        ) { "The tile constructor does not take the rollup card type and the ad wrapper" }
        check(adWrapperTail.registerA == adRegister && adWrapperTail.registerB == adWrapper.registerC) {
            "The last tile constructor argument is not the ad wrapper"
        }
        check(
            tileMove.registerB == tileRegister &&
                tileNullCheck.registerA == tileMove.registerA &&
                tileAdd.getInstruction<FiveRegisterInstruction>().registerD == tileMove.registerA &&
                tileNullCheckTarget.target.location.index > tileAdd.index,
        ) { "The constructed tile does not reach the null-checked list add" }
        check(maxOf(tileRegister, adRegister) < BYTE_REGISTER_LIMIT) { "Tile registers exceed 8-bit encodings" }
        gridRollup.method.addInstructionsWithLabels(
            tileConstructorMatch.index + 1,
            """
                if-eqz v$adRegister, :keep_tile
                const/16 v$tileRegister, 0x0
            """,
            ExternalLabel("keep_tile", gridRollup.method.getInstruction(tileConstructorMatch.index + 1)),
        )

        val itemConverter = SearchResultItemConverterFingerprint.match()
        val itemTypeRead = itemConverter.match(searchItemTypeRead).getInstruction<TwoRegisterInstruction>()
        val switchMapMatch = itemConverter.match(searchItemTypeSwitchMap)
        val switchMap = switchMapMatch.getInstruction<OneRegisterInstruction>()
        val itemTypeOrdinal = itemConverter.match(searchItemTypeOrdinal).getInstruction<FiveRegisterInstruction>()
        val ordinalResult = itemConverter.match(searchItemTypeOrdinalResult).getInstruction<OneRegisterInstruction>()
        val switchIndex = itemConverter.match(searchItemTypeSwitchIndex).getInstruction<ThreeRegisterInstruction>()
        val switchOnIndex = itemConverter.match(searchItemTypeSwitch).getInstruction<OneRegisterInstruction>()
        val itemTypeRegister = itemTypeRead.registerA
        val scratchRegister = switchMap.registerA
        check(
            itemTypeOrdinal.registerC == itemTypeRegister && ordinalResult.registerA == switchIndex.registerC &&
                switchIndex.registerB == scratchRegister && switchOnIndex.registerA == switchIndex.registerA,
        ) { "The item type read does not feed the switch map lookup" }
        check(scratchRegister != itemTypeRegister) { "The switch map register overlaps the item type" }
        check(itemTypeRegister < NIBBLE_REGISTER_LIMIT && scratchRegister < NIBBLE_REGISTER_LIMIT) {
            "Item type registers exceed 4-bit encodings"
        }
        val itemTypeFields = classDefBy(SEARCH_RESULT_ITEM_TYPE_CLASS).fields.filter { it.name in CLASSIFIED_AD_ITEM_TYPES }
        check(
            itemTypeFields.size == CLASSIFIED_AD_ITEM_TYPES.size && itemTypeFields.all {
                it.type == SEARCH_RESULT_ITEM_TYPE_CLASS && AccessFlags.STATIC.isSet(it.accessFlags) &&
                    AccessFlags.ENUM.isSet(it.accessFlags)
            },
        ) { "Classified ad item types are not static enum fields" }
        val adItemChecks = CLASSIFIED_AD_ITEM_TYPES.joinToString("\n") { itemType ->
            """
                sget-object v$scratchRegister, $SEARCH_RESULT_ITEM_TYPE_CLASS->$itemType:$SEARCH_RESULT_ITEM_TYPE_CLASS
                if-eq v$itemTypeRegister, v$scratchRegister, :drop_ad_item
            """
        }
        itemConverter.method.addInstructionsWithLabels(
            switchMapMatch.index,
            """
                $adItemChecks
                goto :keep_item
                :drop_ad_item
                const/16 v$scratchRegister, 0x0
                return-object v$scratchRegister
            """,
            ExternalLabel("keep_item", itemConverter.method.getInstruction(switchMapMatch.index)),
        )
    }
}
