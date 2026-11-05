/*
 * Copyright (C) 2026 Quantro100
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from Quantro100/Morphe-patches:
 * https://github.com/Quantro100/Morphe-patches/commit/f384d233384d2bc3d63d9f5011507f774d586b6f
 * Commit f384d233384d2bc3d63d9f5011507f774d586b6f (2026-06-02),
 * patches/src/main/kotlin/app/morphe/patches/aliexpress/updates/DisableForcedUpdatesPatch.kt
 */
package app.morphe.patches.aliexpress.misc.updates

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.aliexpress.misc.settings.markPatchApplied
import app.morphe.patches.aliexpress.misc.settings.returnWhenEnabled
import app.morphe.patches.aliexpress.misc.signin.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val PATCH_SETTINGS_CLASS = "Lapp/hxreborn/extension/aliexpress/PatchSettings;"

@Suppress("unused")
val disableUpdatePromptsPatch: BytecodePatch = bytecodePatch(
    name = "Disable update prompts",
    description = "Removes automatic update dialogs. The Version row in Settings still checks manually.",
) {
    compatibleWith(AppCompatibilities.ALIEXPRESS)
    dependsOn(spoofSignaturePatch)

    execute {
        markPatchApplied("disableUpdatePrompts", disableUpdatePromptsPatch)
        UpdateDialogFingerprint.matchSingle().method.returnWhenEnabled("$PATCH_SETTINGS_CLASS->disableUpdatePrompts()Z")
    }
}
