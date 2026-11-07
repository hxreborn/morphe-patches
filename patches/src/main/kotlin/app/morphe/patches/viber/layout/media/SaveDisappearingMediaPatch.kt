/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.media

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied

@Suppress("unused")
val saveDisappearingMediaPatch: BytecodePatch = bytecodePatch(
    name = "Save disappearing media",
    description = "Shows Save to gallery for disappearing photos and videos.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("saveDisappearingMedia", saveDisappearingMediaPatch)
        val menuStateType = MediaMenuStateFingerprint.matchSingle().classDef.type
        mediaMenuStateBuilderFingerprint(menuStateType).matchSingle().apply {
            val (_, _, saveDisappearingCheck) = instructionMatches
            method.addFilterCallAfter(saveDisappearingCheck.index + 1, "$VIBER_SETTINGS_CLASS->blocksMediaSave(Z)Z")
        }
    }
}
