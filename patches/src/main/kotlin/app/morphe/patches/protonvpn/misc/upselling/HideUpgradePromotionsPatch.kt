/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.upselling

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patches.protonvpn.misc.anchors.ServerGroupsMainScreenStateToStringFingerprint
import app.morphe.patches.protonvpn.misc.restrictions.filterConstructorProperty
import app.morphe.patches.protonvpn.misc.anchors.findPropertyField
import app.morphe.patches.protonvpn.misc.anchors.setExtensionMember
import app.morphe.patches.protonvpn.misc.anchors.unitInstanceField
import app.morphe.patches.protonvpn.misc.restrictions.filterReturnValue
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.UPSELLING_VISIBILITY_CLASS
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.smali.toJavaClassName
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PROMOTIONS_CLASS = "Lapp/hxreborn/extension/protonvpn/Promotions;"
private const val IS_HIDDEN = "$UPSELLING_VISIBILITY_CLASS->isHidden()Z"

@Suppress("unused")
val hideUpgradePromotionsPatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Hides settings that need a paid plan, upgrade banners, the Discover VPN Plus carousel and special offers.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(UPSELLING_VISIBILITY_CLASS)

        val plusBadgeId = resourceId(ResourceType.DRAWABLE, "vpn_plus_badge")

        settingRowWithIconFingerprint().matchSingle().method.apply {
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

        val settingViewState = SettingsValueItemFingerprint.matchSingle().originalMethod.parameterTypes.first().toString()
        val settingViewStateClass = classDefBy(settingViewState)
        val isRestrictedGetter = settingViewStateClass.methods.singleOrNull {
            it.returnType == "Z" && it.parameterTypes.isEmpty() && !AccessFlags.STATIC.isSet(it.accessFlags)
        }
        val readIsRestricted = if (isRestrictedGetter != null) {
            """
                invoke-virtual/range { p0 .. p0 }, $settingViewState->${isRestrictedGetter.name}()Z
                move-result v0
            """
        } else {
            val field = settingViewStateClass.instanceFields.single { it.type == "Z" }
            """
                move-object/from16 v0, p0
                iget-boolean v0, v0, $settingViewState->${field.name}:Z
            """
        }
        SettingsValueItemFingerprint.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    $readIsRestricted
                    if-eqz v0, :shown
                    invoke-static { }, $IS_HIDDEN
                    move-result v0
                    if-eqz v0, :shown
                    return-void
                """,
                ExternalLabel("shown", getInstruction(0)),
            )
        }

        setExtensionMember("apiNotificationType", findPropertyField(ApiNotificationToStringFingerprint, "type").name)
        val inlinedActiveNotifications = inlinedActiveNotificationsFingerprint().matchOrNull()
        if (inlinedActiveNotifications != null) {
            inlinedActiveNotifications.run {
                val sorted = instructionMatches.last()
                val register = sorted.getInstruction<OneRegisterInstruction>().registerA
                method.addInstructions(
                    sorted.index + 1,
                    """
                        invoke-static/range { v$register .. v$register }, $PROMOTIONS_CLASS->withoutPromoNotifications(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$register
                    """,
                )
            }
        } else {
            activeNotificationsFingerprint().matchSingle().method.filterReturnValue(
                "$PROMOTIONS_CLASS->withoutPromoNotifications(Ljava/util/List;)Ljava/util/List;",
            )
        }

        setExtensionMember("serverGroupBannerClass", ServerGroupBannerToStringFingerprint.originalClassDef.type.toJavaClassName())
        filterConstructorProperty(
            ServerGroupsMainScreenStateToStringFingerprint,
            "items",
            "$PROMOTIONS_CLASS->withoutUpgradeBanners(Ljava/util/List;)Ljava/util/List;",
        )

        (FreeConnectionsUpsellBannerFingerprint.matchOrNull() ?: InlinedFreeConnectionsUpsellBannerFingerprint.matchSingle()).run {
            val bannerRoot = instructionMatches.last()
            val register = bannerRoot.getInstruction<OneRegisterInstruction>().registerA
            method.addInstruction(
                bannerRoot.index + 1,
                "invoke-static { v$register }, $PROMOTIONS_CLASS->hideUpgradeView(Landroid/view/View;)V",
            )
        }

        upgradeCarouselFingerprint().matchSingle().run {
            val freeUserResult = instructionMatches[1]
            val register = freeUserResult.getInstruction<OneRegisterInstruction>().registerA
            method.addInstructions(
                freeUserResult.index + 1,
                """
                    invoke-static { v$register }, $PROMOTIONS_CLASS->showsUpsell(Z)Z
                    move-result v$register
                """,
            )
        }

        filterConstructorProperty(
            AccountSettingsViewStateToStringFingerprint,
            "upgradeToPlusBanner",
            "$PROMOTIONS_CLASS->showsUpsell(Z)Z",
        )

        UpgradeDialogCreateFingerprint.matchSingle().run {
            val superOnCreate = instructionMatches.first().index
            val register = method.getFreeRegisterProvider(superOnCreate + 1, 1).getFreeRegister()
            method.addInstructionsWithLabels(
                superOnCreate + 1,
                """
                    invoke-static/range { p0 .. p0 }, $PROMOTIONS_CLASS->hidesOnboardingUpgrade(Landroid/app/Activity;)Z
                    move-result v$register
                    if-eqz v$register, :shown
                    invoke-virtual/range { p0 .. p0 }, Landroid/app/Activity;->finish()V
                    return-void
                """,
                ExternalLabel("shown", method.getInstruction(superOnCreate + 1)),
            )
        }

        if (classDefByOrNull(UPGRADE_ONBOARDING_ACTIVITY) == null) return@execute

        val onboardingLauncher = launchOnboardingFingerprint().matchOrNull()
        if (onboardingLauncher != null) {
            onboardingLauncher.method.apply {
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
        } else {
            onboardingLaunchCoroutineFingerprint().matchSingle().run {
                val launcher = instructionMatches.first()
                val register = launcher.getInstruction<OneRegisterInstruction>().registerA
                method.addInstructionsWithLabels(
                    launcher.index,
                    """
                        invoke-static { }, $IS_HIDDEN
                        move-result v$register
                        if-eqz v$register, :shown
                        sget-object v$register, $unitInstanceField
                        return-object v$register
                    """,
                    ExternalLabel("shown", method.getInstruction(launcher.index)),
                )
            }
        }
    }
}
