/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.promos

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.instructionMatch
import app.morphe.patches.bereal.misc.settings.returnEarlyWhen
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}promos/Promotions;"

private fun MutableMethod.filterBoolean(index: Int, register: String, filter: String) = addInstructions(
    index,
    """
        invoke-static/range { $register .. $register }, $EXTENSION_CLASS->$filter(Z)Z
        move-result $register
    """,
)

@Suppress("unused")
val removePromotionsPatch = bytecodePatch(
    name = "Remove promotions",
    description = "Removes Recap cards, the \"How's BeReal?\" review prompt, the " +
        "profile completion card, the notifications banner and the music tooltip.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val yearRecapType = YearRecapWidgetToStringFingerprint.matchSingle().classDef.type
        yearRecapWidgetConstructorFingerprint(yearRecapType).matchSingle().method.filterBoolean(0, "p1", "shown")

        val monthlyRecapType = MonthlyRecapWidgetToStringFingerprint.matchSingle().classDef.type
        val monthlyRecap = monthlyRecapDismissedFingerprint(monthlyRecapType).matchSingle()
        val dismissedIndex = monthlyRecap.instructionMatch(Opcode.MOVE_RESULT).index
        val dismissed = monthlyRecap.method.getInstruction<OneRegisterInstruction>(dismissedIndex).registerA
        monthlyRecap.method.filterBoolean(dismissedIndex + 1, "v$dismissed", "dismissed")

        val reviewSheetType = ReviewSheetToStringFingerprint.matchSingle().classDef.type
        val reviewPrompt = reviewPromptEnabledFingerprint(reviewSheetType).matchSingle()
        val enabledIndex = reviewPrompt.instructionMatch(Opcode.MOVE_RESULT).index
        val enabled = reviewPrompt.method.getInstruction<OneRegisterInstruction>(enabledIndex).registerA
        reviewPrompt.method.filterBoolean(enabledIndex + 1, "v$enabled", "shown")

        ProfileCompletionBannerFingerprint.matchSingle().method.returnEarlyWhen("$EXTENSION_CLASS->removed()Z")
        NotificationPermissionBannerFingerprint.matchSingle().method.returnEarlyWhen("$EXTENSION_CLASS->removed()Z")

        MusicButtonFingerprint.matchSingle().method.filterBoolean(0, "p1", "shown")
    }
}
