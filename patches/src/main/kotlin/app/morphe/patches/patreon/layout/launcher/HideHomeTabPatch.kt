/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.launcher

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.patreon.misc.settings.PATREON_EXTENSION_PACKAGE
import app.morphe.patches.patreon.misc.settings.markFeaturePatched
import app.morphe.patches.patreon.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val HOME_TAB_CLASS = "${PATREON_EXTENSION_PACKAGE}HomeTab;"

@Suppress("unused")
val hideHomeTabPatch = bytecodePatch(
    name = "Hide Home tab",
    description = "Removes the Home tab and starts on Memberships. " +
        "Accounts without memberships keep the tab.",
) {
    compatibleWith(AppCompatibilities.PATREON)

    dependsOn(patchesSettingsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(HOME_TAB_CLASS)

        val membershipsTab = MembershipsTabToStringFingerprint.classDef
        val launcherTabs = launcherTabsFingerprint(membershipsTab.type)
        val (membershipsRead, _, homeRead, _, tabsResult) = launcherTabs.instructionMatches
        val (fallbackResult, fallbackCall) = launcherTabs.instructionMatches.asReversed()

        launcherTabs.method.apply {
            val membershipsRegister = getInstruction<OneRegisterInstruction>(membershipsRead.index).registerA
            val homeIndex = homeRead.index
            val homeRegister = getInstruction<OneRegisterInstruction>(homeIndex).registerA
            val homeTab = homeRead.instruction.getReference<FieldReference>()!!
            val tabsRegister = getInstruction<OneRegisterInstruction>(tabsResult.index).registerA
            val fallbackIndex = fallbackCall.index
            val fallbackRegister = getInstruction<OneRegisterInstruction>(fallbackResult.index).registerA

            addInstruction(fallbackResult.index + 1, "check-cast v$fallbackRegister, ${membershipsTab.interfaces.single()}")
            addInstruction(
                fallbackIndex + 1,
                "invoke-interface { v$tabsRegister, v$fallbackRegister }, Ljava/util/List;->get(I)Ljava/lang/Object;",
            )
            replaceInstruction(fallbackIndex, "const/4 v$fallbackRegister, 0x0")

            addInstructionsWithLabels(
                homeIndex + 1,
                """
                    if-eqz v$membershipsRegister, :keep_home
                    invoke-static { }, $HOME_TAB_CLASS->isHidden()Z
                    move-result v$homeRegister
                    if-eqz v$homeRegister, :show_home
                    const/4 v$homeRegister, 0x0
                    goto :keep_home
                    :show_home
                    sget-object v$homeRegister, ${descriptor(homeTab)}
                """,
                ExternalLabel("keep_home", getInstruction(homeIndex + 1)),
            )
        }
    }
}
