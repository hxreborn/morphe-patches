/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.perplexity.misc.upselling

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.perplexity.misc.returnUnitEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val hideUpgradePromotionsPatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Hides the subscription offer after signing in, the Upgrade plan banner, home screen " +
        "promotions and upgrade cards in answers. Plan limits still apply.",
) {
    compatibleWith(AppCompatibilities.PERPLEXITY)

    execute {
        val userDataType = SignInFunnelUserDataToStringFingerprint.matchSingle().classDef.type
        skipPaywallAfterSignInFingerprint(userDataType).matchSingle().method.returnEarly(true)

        returnUnitEarly(HomeUpsellFetchFingerprint.matchSingle().method)

        val upsellTypeEnum = UpsellTypeEnumFingerprint.matchSingle().classDef.type
        ThreadUpsellMapperFingerprint.matchSingle().method.apply {
            val remoteUpsellType = parameterTypes.single().toString()
            val upsellTypeField = classDefBy(remoteUpsellType).fields.single { it.type == upsellTypeEnum }
            val registers = getFreeRegisterProvider(0, 2)
            val typeRegister = registers.getFreeRegister4Bit()
            val compareRegister = registers.getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    iget-object v$typeRegister, p0, $upsellTypeField
                    if-eqz v$typeRegister, :keep
                    invoke-virtual {v$typeRegister}, Ljava/lang/Enum;->name()Ljava/lang/String;
                    move-result-object v$typeRegister
                    const-string v$compareRegister, "UPGRADE_TO_"
                    invoke-virtual {v$typeRegister, v$compareRegister}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
                    move-result v$compareRegister
                    if-nez v$compareRegister, :drop
                    const-string v$compareRegister, "OPEN_SUBSCRIPTION_PROMO"
                    invoke-virtual {v$typeRegister, v$compareRegister}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v$compareRegister
                    if-eqz v$compareRegister, :keep
                    :drop
                    const/4 v$typeRegister, 0x0
                    return-object v$typeRegister
                """,
                ExternalLabel("keep", getInstruction(0)),
            )
        }

        if (UpgradePlanBannerToStringFingerprint.matchOrNull() != null) {
            UpgradePlanBannerFlagFingerprint.matchSingle().apply {
                val index = instructionMatches.last().index
                val bannerFlag = method.getInstruction<ReferenceInstruction>(index).reference
                val homeStateRegister = method.getInstruction<TwoRegisterInstruction>(index).registerB
                val falseRegister = method.getFreeRegisterProvider(index + 1, 1).getFreeRegister4Bit()

                method.addInstructions(
                    index + 1,
                    """
                        const/4 v$falseRegister, 0x0
                        iput-boolean v$falseRegister, v$homeStateRegister, $bannerFlag
                    """,
                )
            }
        }
    }
}
