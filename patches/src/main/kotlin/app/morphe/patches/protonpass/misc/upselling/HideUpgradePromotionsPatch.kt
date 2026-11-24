/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.upselling

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.protonUpgradePromotionRowsExtensionPatch
import app.morphe.patches.shared.misc.proton.UPSELLING_VISIBILITY_CLASS
import app.morphe.patches.shared.misc.proton.markUpgradePromotionsPatched
import app.morphe.patches.shared.misc.proton.markPatchMetadata
import app.morphe.patches.shared.misc.proton.returnVoidWhenUpsellingHidden
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField

private const val ORIGINAL_UPGRADE_AVAILABLE_FIELD = "originalUpgradeAvailable"

private val Match.originalUpgradeAvailableField
    get() = "${classDef.type}->$ORIGINAL_UPGRADE_AVAILABLE_FIELD:Z"

private fun Match.preserveAndResolveUpgradeAvailable() {
    val (upgradeAvailableWrite) = instructionMatches
    val put = upgradeAvailableWrite.getInstruction<TwoRegisterInstruction>()

    classDef.instanceFields.add(
        ImmutableField(
            classDef.type,
            ORIGINAL_UPGRADE_AVAILABLE_FIELD,
            "Z",
            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
            null,
            null,
            null,
        ).toMutable(),
    )
    method.addInstructions(
        upgradeAvailableWrite.index,
        """
            iput-boolean v${put.registerA}, v${put.registerB}, $originalUpgradeAvailableField
            invoke-static/range { v${put.registerA} .. v${put.registerA} }, $UPSELLING_VISIBILITY_CLASS->resolveUpgradeAvailable(Z)Z
            move-result v${put.registerA}
        """,
    )
}

private fun Match.readOriginalUpgradeAvailable() {
    val (upgradeAvailableRead) = instructionMatches
    val get = upgradeAvailableRead.getInstruction<TwoRegisterInstruction>()
    method.replaceInstruction(
        upgradeAvailableRead.index,
        "iget-boolean v${get.registerA}, v${get.registerB}, $originalUpgradeAvailableField",
    )
}

@Suppress("unused")
val hideUpgradePromotionsPatch: Patch = patch(
    name = "Hide upgrade promotions",
    description = "Hides the Upgrade buttons, upgrade prompts and the welcome offer after signing in. " +
        "Plan limits still apply.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS, AppCompatibilities.PROTON_PASS_FDROID)
    dependsOn(protonUpgradePromotionRowsExtensionPatch)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchMetadata("hideUpgradePromotions", hideUpgradePromotionsPatch, withDescription = true)
        markUpgradePromotionsPatched()

        UpgradeInfoConstructorFingerprint.match().preserveAndResolveUpgradeAvailable()
        PlanLimitReachedFingerprint.match().readOriginalUpgradeAvailable()
        UpgradeButtonFingerprint.method.returnVoidWhenUpsellingHidden()
        AttachmentsStateConstructorFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p5 }, $UPSELLING_VISIBILITY_CLASS->resolveUpgradeAvailable(Z)Z
                move-result p5
            """,
        )

        DarkWebMonitoringSectionFingerprint.method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $UPSELLING_VISIBILITY_CLASS->isHidden()Z
                    move-result v$register
                    if-eqz v$register, :shown
                    move-object/from16 v$register, p2
                    instance-of v$register, v$register, $DARK_WEB_MONITORING_CLASS${'$'}FreeNoDataBreaches;
                    if-nez v$register, :hidden
                    move-object/from16 v$register, p2
                    instance-of v$register, v$register, $DARK_WEB_MONITORING_CLASS${'$'}FreeLoading;
                    if-eqz v$register, :shown
                    :hidden
                    return-void
                """,
                ExternalLabel("shown", getInstruction(0)),
            )
        }

        SentinelUpsellRowFingerprint.match().run {
            val (_, counterRowCall) = instructionMatches
            val rowIndex = counterRowCall.index
            val hidden = method.getFreeRegisterProvider(rowIndex, 1).getFreeRegister()

            method.addInstructionsWithLabels(
                rowIndex,
                """
                    invoke-static { }, $UPSELLING_VISIBILITY_CLASS->isHidden()Z
                    move-result v$hidden
                    if-nez v$hidden, :skip_row
                """,
                ExternalLabel("skip_row", method.getInstruction(rowIndex + 1)),
            )
        }

        OnboardingRouteFingerprint.match().run {
            val (upsellRouteRead, onboardingRouteRead) = instructionMatches
            val onboardingRoute = onboardingRouteRead.getInstruction<ReferenceInstruction>().reference
            val firstRegister = upsellRouteRead.getInstruction<OneRegisterInstruction>().registerA
            val shownIndex = upsellRouteRead.index + 1
            val hidden = method.getFreeRegisterProvider(shownIndex, 1).getFreeRegister()

            method.addInstructionsWithLabels(
                shownIndex,
                """
                    invoke-static { }, $UPSELLING_VISIBILITY_CLASS->isHidden()Z
                    move-result v$hidden
                    if-eqz v$hidden, :shown
                    sget-object v$firstRegister, $onboardingRoute
                """,
                ExternalLabel("shown", method.getInstruction(shownIndex)),
            )
        }
    }
}
