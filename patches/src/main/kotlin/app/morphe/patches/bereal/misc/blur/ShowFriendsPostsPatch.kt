/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.blur

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.markFeaturePatched
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}blur/FriendsPosts;"

@Suppress("unused")
val showFriendsPostsPatch: BytecodePatch = bytecodePatch(
    name = "Show friends' posts before posting",
    description = "Shows friends' BeReals in My Friends and Friends of Friends before you post your own.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        markFeaturePatched(EXTENSION_CLASS, showFriendsPostsPatch)

        val placementType = FeedPlacementEnumFingerprint.matchSingle().classDef.type
        val flowType = postVisibilityFlowConstructorFingerprint(placementType).matchSingle().classDef.type
        val flow = postVisibilityFlowFingerprint(flowType).matchSingle()

        val resultIndex = flow.instructionMatches.last().index
        val register = flow.method.getInstruction<OneRegisterInstruction>(resultIndex).registerA
        flow.method.addInstructions(
            resultIndex + 1,
            """
                invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->blurred(Z)Z
                move-result v$register
            """,
        )
    }
}
