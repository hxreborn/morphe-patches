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
private const val DETAIL_PRIMARY_AD_PARAMETER = 17
private const val DETAIL_SECONDARY_AD_PARAMETER = 18
private const val CURRENT_LIST_MOVE_WINDOW = 4
private const val BYTE_REGISTER_LIMIT = 256

private const val KOTLIN_UNIT = "Lkotlin/Unit;"

private fun returnUnitEarly(method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod) {
    val returnRegister = method.p0Register
    check(returnRegister < BYTE_REGISTER_LIMIT) { "Unit return register exceeds the 8-bit encoding" }
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

        val initializerType = GoogleMobileAdsInitFingerprint.match().classDef.type
        GoogleMobileAdsInitFingerprint.method.returnEarly()
        returnUnitEarly(aatKitInitFingerprint(initializerType).match().method)
        returnUnitEarly(adMediationInitFingerprint(initializerType).match().method)
        returnUnitEarly(SponsoredThreadAdLoadFingerprint.match().method)
        OpenWrapBannerLoadFingerprint.method.returnEarly(null)
        AatKitBannerLoadFingerprint.method.returnEarly(null)
        AatKitBannerPrefetchFingerprint.method.returnEarly()

        val sponsoredItemType = ExplorationSponsoredItemFingerprint.match().classDef.type
        val adItemType = ExplorationAdItemFingerprint.match().classDef.type
        val mapper = explorationItemMapperFingerprint(sponsoredItemType, adItemType).match()
        val (sponsoredCheckMatch, adCheckMatch, listAddMatch) = mapper.instructionMatches
        val method = mapper.method
        val sponsoredCheck = method.getInstruction<TwoRegisterInstruction>(sponsoredCheckMatch.index)
        val adCheck = method.getInstruction<TwoRegisterInstruction>(adCheckMatch.index)
        val listAdd = method.getInstruction<FiveRegisterInstruction>(listAddMatch.index)
        val currentListMoveIndex = (sponsoredCheckMatch.index - 1 downTo
            sponsoredCheckMatch.index - CURRENT_LIST_MOVE_WINDOW).first {
            method.getInstruction(it).opcode == Opcode.MOVE_OBJECT_FROM16
        }
        val currentListMove = method.getInstruction<TwoRegisterInstruction>(currentListMoveIndex)
        val currentListRegister = currentListMove.registerA
        val finalListRegister = listAdd.registerC
        check(sponsoredCheck.registerB == adCheck.registerB && sponsoredCheck.registerB == listAdd.registerD) {
            "Exploration item checks do not feed the list add"
        }
        check(currentListRegister != finalListRegister) { "Current and final list registers overlap" }
        val continueWithoutItem = method.getInstruction(listAddMatch.index + 1)
        listOf("ad" to adCheckMatch, "sponsored" to sponsoredCheckMatch).forEach { (name, match) ->
            val check = method.getInstruction<TwoRegisterInstruction>(match.index)
            val keepItem = method.getInstruction(match.index + 1)
            method.addInstructionsWithLabels(
                match.index + 1,
                """
                    if-eqz v${check.registerA}, :keep_$name
                    move-object/from16 v$finalListRegister, v$currentListRegister
                    goto :continue_without_$name
                """,
                ExternalLabel("keep_$name", keepItem),
                ExternalLabel("continue_without_$name", continueWithoutItem),
            )
        }

        val adItemTypes = listOf(
            InterscrollerAdItemFingerprint.match().classDef.type,
            LargeAdItemFingerprint.match().classDef.type,
            MediumAdItemFingerprint.match().classDef.type,
            SmallAdItemFingerprint.match().classDef.type,
        )
        val adItemFactory = adItemFactoryFingerprint(adItemTypes).match().method
        val detailAds = detailAdSlotsFingerprint(adItemFactory).match().method
        val adPlacementType = adItemFactory.parameterTypes.first()
        val detailAdSlotType = detailAds.parameterTypes[DETAIL_PRIMARY_AD_PARAMETER].toString()
        check(
            detailAds.parameterTypes[DETAIL_SECONDARY_AD_PARAMETER] == detailAdSlotType &&
                classDefBy(detailAdSlotType).superclass == adPlacementType,
        ) { "Detail ad slot parameters do not use the ad placement type" }
        detailAds.addInstructions(
            0,
            """
                const/16 ${detailAds.parameterRegister(DETAIL_PRIMARY_AD_PARAMETER)}, 0x0
                const/16 ${detailAds.parameterRegister(DETAIL_SECONDARY_AD_PARAMETER)}, 0x0
            """,
        )
    }
}
