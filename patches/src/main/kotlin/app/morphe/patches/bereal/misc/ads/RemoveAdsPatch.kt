/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.bereal.misc.base.beRealBasePatch
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.markFeaturePatched
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addFieldReader
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import app.morphe.patches.shared.misc.extension.berealSponsoredPostsExtensionPatch

private const val SPONSORED_FIELD = "isSponsored_"

private fun MutableMethod.returnNull() = addInstructions(
    0,
    """
        const/4 v0, 0x0
        return-object v0
    """,
)

@Suppress("unused")
val removeAdsPatch: Patch = patch(
    name = "Remove ads",
    description = "Removes the full-screen ad at launch and sponsored BeReals in both feeds. " +
        "Stops the ad networks from loading.",
) {
    dependsOn(berealSponsoredPostsExtensionPatch)
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(
        beRealBasePatch,
        removeAdNetworkProvidersPatch,
    )

    execute {
        markFeaturePatched("${BEREAL_EXTENSION_PACKAGE}SponsoredPosts;", removeAdsPatch)

        val adRequestType = AdRequestIdToStringFingerprint.match().classDef.type
        val adSelector = adSelectorFingerprint(adRequestType).match()
        adSelector.method.returnNull()

        val placementType = AdPlacementEnumFingerprint.match().classDef.type
        adProviderSetupFingerprint(adSelector.classDef.type, placementType).method.returnNull()

        val nativeAdLoader = NativeAdLoaderFingerprint.match().classDef.type
        nativeAdBatchLoadFingerprint(nativeAdLoader).method.addInstructions(0, "return-object p0")

        SpotlightAdLoadFingerprint.matchAll(1..2).forEach { loader ->
            loader.method.returnNull()
            spotlightAdEntryFingerprint(loader.classDef.type).method.returnNull()
        }

        AdsSetupFingerprint.method.returnNull()
        AdnInitializeFingerprint.method.returnNull()

        FeedAddMessageFingerprint.matchAll(1..2).forEach {
            feedAddHasAdFingerprint(it.classDef.type).method.returnEarly(false)
        }

        val feedPostEntries = FeedPostEntryMessageFingerprint.matchAll(1..2)
        feedPostEntries.forEach { entry ->
            val loop = feedPostEntryLoopFingerprint(entry.classDef.type).match()
            val (iteratorCall, _) = loop.instructionMatches
            val items = iteratorCall.getInstruction<FiveRegisterInstruction>().registerC
            loop.method.addInstructions(
                iteratorCall.index,
                """
                    invoke-static/range { v$items .. v$items }, ${BEREAL_EXTENSION_PACKAGE}SponsoredPosts;->withoutSponsored(Ljava/lang/Iterable;)Ljava/lang/Iterable;
                    move-result-object v$items
                """,
            )
        }

        replaceSponsoredStub(feedPostEntries.map { it.classDef })

        val experiment = ServerFeedAdsExperimentFingerprint.match()
        val (resultInstance, _, _) = experiment.instructionMatches
        val resultType = resultInstance.getInstruction<ReferenceInstruction>().getReference<TypeReference>()!!.type
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

private fun replaceSponsoredStub(entries: List<ClassDef>) {
    val checks = entries.mapIndexed { index, entry ->
        val readSponsored = mutableClassDefBy(entry.type)
            .addFieldReader(entry.fields.single { it.name == SPONSORED_FIELD })
        """
            instance-of v0, p0, ${entry.type}
            if-eqz v0, :other$index
            check-cast p0, ${entry.type}
            invoke-virtual { p0 }, $readSponsored
            move-result v0
            return v0
            :other$index
        """.trimIndent()
    }
    mutableClassDefBy("${BEREAL_EXTENSION_PACKAGE}SponsoredPosts;").replaceStub(
        "isSponsored",
        2,
        checks.joinToString("\n") + "\nconst/4 v0, 0x0\nreturn v0",
    )
}
