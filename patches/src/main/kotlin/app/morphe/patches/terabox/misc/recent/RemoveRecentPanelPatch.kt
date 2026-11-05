/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.recent

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/terabox/RecentPanel;"

private const val DESCRIPTION = "Removes Recent, Starred and Offline from Home."

@Suppress("unused")
val removeRecentPanelPatch = bytecodePatch(
    name = "Remove recent panel",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("removeRecentPanel", DESCRIPTION)
        val fragment = HomeCardFragmentFingerprint.matchSingle().classDef
        homeViewCreatedFingerprint(fragment.type).matchSingle().method.apply {
            addInstruction(
                0,
                "invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS_DESCRIPTOR->updateRecentPanel(Landroid/view/View;)V",
            )
        }
        homeResumeFingerprint(fragment.type).matchSingle().method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructions(
                0,
                """
                    invoke-virtual { p0 }, Landroidx/fragment/app/Fragment;->getView()Landroid/view/View;
                    move-result-object v$register
                    invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS_DESCRIPTOR->updateRecentPanel(Landroid/view/View;)V
                """,
            )
        }
    }
}
