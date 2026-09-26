/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.upselling

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.protonvpn.misc.restrictions.filterReturnValue
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.UPSELLING_VISIBILITY_CLASS
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PROMOTIONS_CLASS = "Lapp/hxreborn/extension/protonvpn/Promotions;"
private const val IS_HIDDEN = "$UPSELLING_VISIBILITY_CLASS->isHidden()Z"

@Suppress("unused")
val hideUpgradePromotionsPatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Hides settings that need a paid plan, upgrade banners, the Discover VPN Plus carousel and special offers.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch, resourceMappingPatch)

    execute {
        markFeaturePatched(UPSELLING_VISIBILITY_CLASS)

        val plusBadgeId = getResourceId(ResourceType.DRAWABLE, "vpn_plus_badge")
            ?: throw PatchException("Missing drawable: vpn_plus_badge")

        SettingRowWithIconFingerprint.matchSingle().method.apply {
            addInstructionsWithLabels(
                0,
                """
                    if-eqz p4, :shown
                    invoke-virtual/range { p4 .. p4 }, Ljava/lang/Integer;->intValue()I
                    move-result v0
                    const v1, $plusBadgeId
                    if-ne v0, v1, :shown
                    invoke-static { }, $IS_HIDDEN
                    move-result v0
                    if-eqz v0, :shown
                    return-void
                """,
                ExternalLabel("shown", getInstruction(0)),
            )
        }

        SettingsValueItemFingerprint.matchSingle().method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-virtual/range { p0 .. p0 }, Lcom/protonvpn/android/redesign/settings/ui/SettingsViewModel${'$'}SettingViewState;->isRestricted()Z
                    move-result v0
                    if-eqz v0, :shown
                    invoke-static { }, $IS_HIDDEN
                    move-result v0
                    if-eqz v0, :shown
                    return-void
                """,
                ExternalLabel("shown", getInstruction(0)),
            )
        }

        ActiveNotificationsFingerprint.matchSingle().method.filterReturnValue(
            "$PROMOTIONS_CLASS->withoutPromoNotifications(Ljava/util/List;)Ljava/util/List;",
        )

        ServerGroupsMainScreenStateFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static/range { p2 .. p2 }, $PROMOTIONS_CLASS->withoutUpgradeBanners(Ljava/util/List;)Ljava/util/List;
                move-result-object p2
            """,
        )

        FreeConnectionsInfoFingerprint.matchSingle().method.apply {
            val returnIndex = instructions.lastIndex
            if (getInstruction(returnIndex).opcode != Opcode.RETURN_VOID) {
                throw PatchException("setupViews does not end in return-void")
            }
            replaceInstruction(returnIndex, "nop")
            addInstructions(
                returnIndex + 1,
                """
                    iget-object v0, p0, Lcom/protonvpn/android/databinding/FreeConnectionsInfoBinding;->upsellBanner:Lcom/protonvpn/android/databinding/ItemFreeUpsellBinding;
                    invoke-virtual { v0 }, Lcom/protonvpn/android/databinding/ItemFreeUpsellBinding;->getRoot()Landroidx/constraintlayout/widget/ConstraintLayout;
                    move-result-object v0
                    invoke-static { v0 }, $PROMOTIONS_CLASS->hideUpgradeView(Landroid/view/View;)V
                    return-void
                """,
            )
        }

        UpgradeCarouselFingerprint.matchSingle().run {
            val freeUserResult = instructionMatches[1]
            val register = freeUserResult.getInstruction<OneRegisterInstruction>().registerA
            method.addInstructions(
                freeUserResult.index + 1,
                """
                    invoke-static { v$register }, $PROMOTIONS_CLASS->showsUpgradeCarousel(Z)Z
                    move-result v$register
                """,
            )
        }

        LaunchOnboardingFingerprint.matchSingle().method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $IS_HIDDEN
                    move-result v0
                    if-eqz v0, :shown
                    return-void
                """,
                ExternalLabel("shown", getInstruction(0)),
            )
        }
    }
}
