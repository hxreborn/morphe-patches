/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.fof

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}fof/FriendsOfFriendsTab;"

@Suppress("unused")
val removeFriendsOfFriendsTabPatch = bytecodePatch(
    name = "Remove Friends of Friends tab",
    description = "Removes the Friends of Friends tab from the home screen.",
    default = false,
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val homeTabType = HomeTabToStringFingerprint.matchSingle().classDef.type
        val match = homeTabListFingerprint(homeTabType).matchSingle()
        val addIndex = match.instructionMatches.last().index
        val add = match.method.getInstruction<FiveRegisterInstruction>(addIndex)
        match.method.replaceInstruction(
            addIndex,
            "invoke-static { v${add.registerC}, v${add.registerD} }, " +
                "$EXTENSION_CLASS->addTab(Ljava/util/List;Ljava/lang/Object;)Z",
        )
    }
}
