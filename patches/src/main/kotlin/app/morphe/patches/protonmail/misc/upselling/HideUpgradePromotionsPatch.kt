/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.upselling

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.protonmail.misc.banner.answerAutoDeleteBannerCheck
import app.morphe.patches.protonmail.misc.banner.autoDeleteBannerStatePatch
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.protonUpgradePromotionRowsExtensionPatch
import app.morphe.patches.shared.misc.proton.UPSELLING_VISIBILITY_CLASS
import app.morphe.patches.shared.misc.proton.markUpgradePromotionsPatched
import app.morphe.patches.shared.misc.proton.markPatchMetadata
import app.morphe.patches.shared.misc.proton.returnVoidWhenUpsellingHidden
import app.morphe.util.findInstructionIndicesReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import org.w3c.dom.Element

private val unhandledSidebarUpsellingPatch = resourcePatch {
    execute {
        val declared = document(get("res/values/public.xml").inputStream()).use { document ->
            val resources = document.documentElement.childNodes

            (0 until resources.length)
                .mapNotNull { resources.item(it) as? Element }
                .filter { it.getAttribute("type") == ResourceType.STRING.value }
                .map { it.getAttribute("name") }
                .filter { it.startsWith(SIDEBAR_UPSELLING_LABEL_PREFIX) }
        }

        val unhandled = declared - SIDEBAR_UPSELLING_LABELS.toSet()

        if (unhandled.isNotEmpty()) {
            throw PatchException("Unhandled sidebar upselling rows: ${unhandled.joinToString()}")
        }
    }
}

@Suppress("unused")
val hideUpgradePromotionsPatch: BytecodePatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Hides the top-bar upgrade button, promotional sidebar rows and the auto-delete " +
        "upgrade banner in Trash and Spam, and skips the seasonal sale screens shown at launch. " +
        "Keeps the Empty trash and Empty spam buttons.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(protonUpgradePromotionRowsExtensionPatch)
    dependsOn(
        patchesSettingsPatch,
        unhandledSidebarUpsellingPatch,
        autoDeleteBannerStatePatch,
    )

    execute {
        markPatchMetadata("hideUpgradePromotions", hideUpgradePromotionsPatch, withDescription = true)
        markUpgradePromotionsPatched()
        answerAutoDeleteBannerCheck("isUpsellHidden", "$UPSELLING_VISIBILITY_CLASS->isHidden()Z")

        val rows = sidebarUpsellingRowFingerprints.mapNotNull { it.methodOrNull }

        if (rows.isEmpty()) throw PatchException("No sidebar upselling rows matched")

        rows.forEach { it.returnVoidWhenUpsellingHidden() }

        UpgradeUpsellingButtonFingerprint.matchAll(UPGRADE_BUTTON_COUNT..UPGRADE_BUTTON_COUNT).forEach {
            it.method.returnVoidWhenUpsellingHidden()
        }

        val promotionModalShowStates = PromotionModalShowStateFingerprint
            .matchAll(PROMOTION_MODAL_COUNT..PROMOTION_MODAL_COUNT)
            .map { it.classDef.type }
            .toSet()

        with(HomeInterstitialPriorityFingerprint.method) {
            val showChecks = findInstructionIndicesReversed {
                opcode == Opcode.INSTANCE_OF && getReference<TypeReference>()?.type in promotionModalShowStates
            }

            if (showChecks.size != PROMOTION_MODAL_COUNT) {
                throw PatchException(
                    "Promotion modal check count mismatch: expected $PROMOTION_MODAL_COUNT, got ${showChecks.size}",
                )
            }

            showChecks.forEach { index ->
                val isShown = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructionsWithLabels(
                    index + 1,
                    """
                        if-eqz v$isShown, :checked
                        invoke-static { }, $UPSELLING_VISIBILITY_CLASS->isHidden()Z
                        move-result v$isShown
                        xor-int/lit8 v$isShown, v$isShown, 0x1
                    """,
                    ExternalLabel("checked", getInstruction(index + 1)),
                )
            }
        }
    }
}
