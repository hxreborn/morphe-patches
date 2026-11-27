/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.spikestats.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val BANNER_AD_LAYOUT_ID = "linear_layout_banner_ad"

private fun MutableMethod.removeBannerAndReturn() {
    addInstructions(
        0,
        """
            const v0, ${resourceId(ResourceType.ID, BANNER_AD_LAYOUT_ID)}
            invoke-virtual {p0, v0}, Landroidx/appcompat/app/AppCompatActivity;->findViewById(I)Landroid/view/View;
            move-result-object v0
            const/16 v1, 0x8
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
            return-void
        """.trimIndent(),
    )
}

@Suppress("unused")
val removeBannerAdsPatch = patch(
    name = "Remove banner ads",
    description = "Removes banner ads from match summaries and details.",
) {
    compatibleWith(AppCompatibilities.SPIKE_STATS)

    execute {
        MatchSummariesBannerFingerprint.matchAll(1..2).forEach { match ->
            match.method.removeBannerAndReturn()
        }
        MatchDetailsBannerFingerprint.method.removeBannerAndReturn()

        val inlineAdMatch = MatchDetailsInlineAdFingerprint.match()
        val (_, inlineAdEnabledResult, _) = inlineAdMatch.instructionMatches
        val inlineAdEnabledRegister = inlineAdEnabledResult.getInstruction<OneRegisterInstruction>().registerA
        inlineAdMatch.method.replaceInstruction(inlineAdEnabledResult.index, "const/16 v$inlineAdEnabledRegister, 0x0")

        listOf("res/layout/activity_match_summaries.xml", "res/layout/activity_match_details.xml").forEach { layoutPath ->
            document(layoutPath).use { layout ->
                layout.getElementsByTagName("*")
                    .findElementByAttributeValueOrThrow("android:id", "@id/$BANNER_AD_LAYOUT_ID")
                    .setAttribute("android:visibility", "gone")
            }
        }
    }
}
