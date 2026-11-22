/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.gaps

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patches.shared.misc.extension.viberRemoveListGapsExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/ListGaps;"

@Suppress("unused")
val removeListGapsPatch: Patch = patch(
    name = "Remove list gaps",
    description = "Removes list gaps and extra scrolling.",
) {
    dependsOn(viberRemoveListGapsExtensionPatch)
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("removeListGaps", removeListGapsPatch)
        ChatFooterHeightFingerprint.match().apply {
            val (_, _, height, _) = instructionMatches
            method.addFilterCallAfter(height.index, "$EXTENSION_CLASS_DESCRIPTOR->chatFooterHeight(I)I")
        }
        listOf(ChatListBindingFingerprint, MoreListBindingFingerprint).forEach { fingerprint ->
            fingerprint.match().apply {
                val (_, _, list) = instructionMatches
                val register = (list.instruction as OneRegisterInstruction).registerA
                method.addInstructions(list.index + 1, """
                    invoke-static/range {v$register .. v$register}, $EXTENSION_CLASS_DESCRIPTOR->observeList(Landroid/view/View;)V
                """)
            }
        }
    }
}
