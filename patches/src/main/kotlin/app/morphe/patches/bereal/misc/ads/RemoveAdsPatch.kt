/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.markFeaturePatched
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.smali.addFieldReader
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val SPONSORED_FIELD = "isSponsored_"

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
val removeAdsPatch: BytecodePatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes the full-screen ad at launch and sponsored BeReals in both feeds. " +
        "Stops the ad networks from loading.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(removePairipProtectionPatch, removeAdsExtensionPatch, removeAdNetworkProvidersPatch)

    execute {
        markFeaturePatched("${BEREAL_EXTENSION_PACKAGE}SponsoredPosts;", removeAdsPatch, withDescription = false)

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

        val feedPostEntries = FeedPostEntryMessageFingerprint.matchAll(1..2)
        feedPostEntries.forEach { entry ->
            val loop = feedPostEntryLoopFingerprint(entry.classDef.type).matchSingle()
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

        val experiment = ServerFeedAdsExperimentFingerprint.matchSingle()
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

private fun BytecodePatchContext.replaceSponsoredStub(entries: List<ClassDef>) {
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
