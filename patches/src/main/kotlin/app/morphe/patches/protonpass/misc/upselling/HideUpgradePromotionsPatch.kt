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
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.returnVoidWhenUpsellingHidden
import app.morphe.patches.shared.misc.proton.UPSELLING_VISIBILITY_CLASS
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField

private val Match.firstIndex
    get() = instructionMatches.first().index

private val Match.firstRegister
    get() = instructionMatches.first().getInstruction<OneRegisterInstruction>().registerA

private const val ORIGINAL_UPGRADE_AVAILABLE_FIELD = "originalUpgradeAvailable"

private val Match.originalUpgradeAvailableField
    get() = "${classDef.type}->$ORIGINAL_UPGRADE_AVAILABLE_FIELD:Z"

private fun Match.preserveAndResolveUpgradeAvailable() {
    val put = instructionMatches.first().getInstruction<TwoRegisterInstruction>()

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
        firstIndex,
        """
            iput-boolean v${put.registerA}, v${put.registerB}, $originalUpgradeAvailableField
            invoke-static/range { v${put.registerA} .. v${put.registerA} }, $UPSELLING_VISIBILITY_CLASS->resolveUpgradeAvailable(Z)Z
            move-result v${put.registerA}
        """,
    )
}

private fun Match.readOriginalUpgradeAvailable() {
    val get = instructionMatches.first().getInstruction<TwoRegisterInstruction>()
    method.replaceInstruction(
        firstIndex,
        "iget-boolean v${get.registerA}, v${get.registerB}, $originalUpgradeAvailableField",
    )
}

@Suppress("unused")
val hideUpgradePromotionsPatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Hides the Upgrade buttons, upgrade prompts and the welcome offer after signing in. " +
        "Plan limits still apply.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch)

    execute {
        markFeaturePatched(UPSELLING_VISIBILITY_CLASS)

        UpgradeInfoConstructorFingerprint.matchSingle().preserveAndResolveUpgradeAvailable()
        PlanLimitReachedFingerprint.matchSingle().readOriginalUpgradeAvailable()
        UpgradeButtonFingerprint.matchSingle().method.returnVoidWhenUpsellingHidden()
        AttachmentsStateConstructorFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p5 }, $UPSELLING_VISIBILITY_CLASS->resolveUpgradeAvailable(Z)Z
                move-result p5
            """,
        )

        DarkWebMonitoringSectionFingerprint.matchSingle().method.apply {
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

        SentinelUpsellRowFingerprint.matchSingle().run {
            val rowIndex = instructionMatches.last().index
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

        OnboardingRouteFingerprint.matchSingle().run {
            val onboardingRoute = instructionMatches.last().getInstruction<ReferenceInstruction>().reference
            val shownIndex = firstIndex + 1
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
