/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private fun MutableMethod.returnNull() = addInstructions(
    0,
    """
        const/4 v0, 0x0
        return-object v0
    """,
)

private val removeAdsExtensionPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
}

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes ads and sponsored BeReals from both feeds and the " +
        "full-screen ad at launch, and stops the ad networks from loading.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(removePairipProtectionPatch, removeAdsExtensionPatch, removeAdNetworkProvidersPatch)

    execute {
        setExtensionIsPatchIncluded("${BEREAL_EXTENSION_PACKAGE}SponsoredPosts;")

        val adRequestType = AdRequestIdToStringFingerprint.matchSingle().classDef.type
        val adSelector = adSelectorFingerprint(adRequestType).matchSingle()
        adSelector.method.returnNull()

        val placementType = AdPlacementEnumFingerprint.matchSingle().classDef.type
        adProviderSetupFingerprint(adSelector.classDef.type, placementType).matchSingle().method.returnNull()

        val nativeAdLoader = NativeAdLoaderFingerprint.matchSingle().classDef.type
        nativeAdBatchLoadFingerprint(nativeAdLoader).matchSingle().method.addInstructions(0, "return-object p0")

        SpotlightAdLoadFingerprint.matchAll(1..2).forEach { loader ->
            loader.method.returnNull()
            spotlightAdEntryFingerprint(loader.classDef.type).matchSingle().method.returnNull()
        }

        AdsSetupFingerprint.matchSingle().method.returnNull()
        AdnInitializeFingerprint.matchSingle().method.returnNull()

        FeedAddMessageFingerprint.matchAll(1..2).forEach {
            feedAddHasAdFingerprint(it.classDef.type).matchSingle().method.returnEarly(false)
        }

        FeedPostEntryMessageFingerprint.matchAll(1..2).forEach { entry ->
            val loop = feedPostEntryLoopFingerprint(entry.classDef.type).matchSingle()
            val iteratorCall = loop.instructionMatches.first()
            val items = iteratorCall.getInstruction<FiveRegisterInstruction>().registerC
            loop.method.addInstructions(
                iteratorCall.index,
                """
                    invoke-static/range { v$items .. v$items }, ${BEREAL_EXTENSION_PACKAGE}SponsoredPosts;->withoutSponsored(Ljava/lang/Iterable;)Ljava/lang/Iterable;
                    move-result-object v$items
                """,
            )
        }

        val experiment = ServerFeedAdsExperimentFingerprint.matchSingle()
        val resultType = experiment.instructionMatches.first().getInstruction<ReferenceInstruction>()
            .getReference<TypeReference>()!!.type
        experiment.method.addInstructions(
            0,
            """
                new-instance p0, $resultType
                const/16 p1, 0x0
                invoke-direct/range { p0 .. p1 }, $resultType-><init>(Z)V
                return-object p0
            """,
        )
    }
}
