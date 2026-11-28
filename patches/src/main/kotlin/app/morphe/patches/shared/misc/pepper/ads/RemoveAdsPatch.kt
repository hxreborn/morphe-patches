/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pepper.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.p0Register
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ads.failAdMobLoadsPatch
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.smali.parameterRegister
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val APPLICATION_CLASS = "Lcom/pepper/apps/android/app/PepperApplication;"
private const val DETAIL_PRIMARY_AD_PARAMETER_INDEX = 17
private const val DETAIL_SECONDARY_AD_PARAMETER_INDEX = 18
private const val SOURCE_LIST_MOVE_WINDOW = 4
private const val BYTE_REGISTER_LIMIT = 256

private const val KOTLIN_UNIT = "Lkotlin/Unit;"

private fun returnUnitEarly(method: MutableMethod) {
    val returnRegister = method.p0Register
    check(returnRegister < BYTE_REGISTER_LIMIT) {
        "Unit return register out of range: expected 0..${BYTE_REGISTER_LIMIT - 1}, got $returnRegister"
    }
    val unitInstance = classDefBy(KOTLIN_UNIT).staticFields.single { it.type == KOTLIN_UNIT }.name
    method.addInstructions(
        0,
        """
            sget-object v$returnRegister, $KOTLIN_UNIT->$unitInstance:$KOTLIN_UNIT
            return-object v$returnRegister
        """,
    )
}

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes ad placements and sponsored deals.",
) {
    compatibleWith(AppCompatibilities.CHOLLOMETRO, AppCompatibilities.PEPPER)

    dependsOn(failAdMobLoadsPatch, removePairipProtectionPatch, sharedSignatureExtensionPatch)

    execute {
        if (packageMetadata.packageName == AppCompatibilities.CHOLLOMETRO.packageName) {
            spoofSignature(APPLICATION_CLASS)
        }

        val adInitializerType = GoogleMobileAdsInitFingerprint.match().classDef.type
        GoogleMobileAdsInitFingerprint.method.returnEarly()
        returnUnitEarly(aatKitInitFingerprint(adInitializerType).match().method)
        returnUnitEarly(adMediationInitFingerprint(adInitializerType).match().method)
        returnUnitEarly(SponsoredThreadAdLoadFingerprint.match().method)
        OpenWrapBannerLoadFingerprint.method.returnEarly(null)
        AatKitBannerLoadFingerprint.method.returnEarly(null)
        AatKitBannerPrefetchFingerprint.method.returnEarly()

        val sponsoredItemType = ExplorationSponsoredItemFingerprint.match().classDef.type
        val adItemType = ExplorationAdItemFingerprint.match().classDef.type
        val mapperMatch = explorationItemMapperFingerprint(sponsoredItemType, adItemType).match()
        val (sponsoredTypeCheckMatch, adTypeCheckMatch, listAddMatch) = mapperMatch.instructionMatches
        val mapperMethod = mapperMatch.method
        val sponsoredTypeCheck =
            mapperMethod.getInstruction<TwoRegisterInstruction>(sponsoredTypeCheckMatch.index)
        val adTypeCheck = mapperMethod.getInstruction<TwoRegisterInstruction>(adTypeCheckMatch.index)
        val listAddCall = mapperMethod.getInstruction<FiveRegisterInstruction>(listAddMatch.index)
        val sourceListMoveIndex = (sponsoredTypeCheckMatch.index - 1 downTo
            sponsoredTypeCheckMatch.index - SOURCE_LIST_MOVE_WINDOW).first {
            mapperMethod.getInstruction(it).opcode == Opcode.MOVE_OBJECT_FROM16
        }
        val sourceListMove = mapperMethod.getInstruction<TwoRegisterInstruction>(sourceListMoveIndex)
        val sourceListRegister = sourceListMove.registerA
        val destinationListRegister = listAddCall.registerC
        check(
            sponsoredTypeCheck.registerB == adTypeCheck.registerB &&
                sponsoredTypeCheck.registerB == listAddCall.registerD,
        ) {
            "Exploration item register mismatch: sponsored check v${sponsoredTypeCheck.registerB}, " +
                "ad check v${adTypeCheck.registerB}, list add v${listAddCall.registerD}"
        }
        check(sourceListRegister != destinationListRegister) {
            "Exploration list register overlap: source and destination both use v$sourceListRegister"
        }
        val continueAfterListAdd = mapperMethod.getInstruction(listAddMatch.index + 1)
        listOf("ad" to adTypeCheckMatch, "sponsored" to sponsoredTypeCheckMatch).forEach {
            (itemKind, typeCheckMatch) ->
            val typeCheck = mapperMethod.getInstruction<TwoRegisterInstruction>(typeCheckMatch.index)
            val keepItemInstruction = mapperMethod.getInstruction(typeCheckMatch.index + 1)
            mapperMethod.addInstructionsWithLabels(
                typeCheckMatch.index + 1,
                """
                    if-eqz v${typeCheck.registerA}, :keep_$itemKind
                    move-object/from16 v$destinationListRegister, v$sourceListRegister
                    goto :continue_without_$itemKind
                """,
                ExternalLabel("keep_$itemKind", keepItemInstruction),
                ExternalLabel("continue_without_$itemKind", continueAfterListAdd),
            )
        }

        val adItemTypes = listOf(
            InterscrollerAdItemFingerprint.match().classDef.type,
            LargeAdItemFingerprint.match().classDef.type,
            MediumAdItemFingerprint.match().classDef.type,
            SmallAdItemFingerprint.match().classDef.type,
        )
        val adItemFactoryMethod = adItemFactoryFingerprint(adItemTypes).match().method
        val detailAdSlotsMethod = detailAdSlotsFingerprint(adItemFactoryMethod).match().method
        val adPlacementType = adItemFactoryMethod.parameterTypes.first()
        val primaryDetailAdSlotType =
            detailAdSlotsMethod.parameterTypes[DETAIL_PRIMARY_AD_PARAMETER_INDEX].toString()
        val secondaryDetailAdSlotType =
            detailAdSlotsMethod.parameterTypes[DETAIL_SECONDARY_AD_PARAMETER_INDEX].toString()
        check(secondaryDetailAdSlotType == primaryDetailAdSlotType) {
            "Detail ad slot type mismatch: expected $primaryDetailAdSlotType, got $secondaryDetailAdSlotType"
        }
        val detailAdSlotSuperclass = classDefBy(primaryDetailAdSlotType).superclass
        check(detailAdSlotSuperclass == adPlacementType) {
            "Detail ad slot superclass mismatch: expected $adPlacementType, got $detailAdSlotSuperclass"
        }
        detailAdSlotsMethod.addInstructions(
            0,
            """
                const/16 ${detailAdSlotsMethod.parameterRegister(DETAIL_PRIMARY_AD_PARAMETER_INDEX)}, 0x0
                const/16 ${detailAdSlotsMethod.parameterRegister(DETAIL_SECONDARY_AD_PARAMETER_INDEX)}, 0x0
            """,
        )
    }
}
