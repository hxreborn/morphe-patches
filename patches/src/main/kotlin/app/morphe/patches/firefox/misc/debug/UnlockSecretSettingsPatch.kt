/*
 * Copyright (C) 2026 heval99
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from heval99/Heval-Morphe-Patches:
 * https://github.com/heval99/Heval-Morphe-Patches/commit/28097b43dbc8de99bb516a44f84708ae6cbb0f64
 * Commit 28097b43dbc8de99bb516a44f84708ae6cbb0f64 (2026-10-25),
 * patches/src/main/kotlin/app/template/patches/firefox/misc/debug/UnlockSecretSettingsPatch.kt
 */
package app.morphe.patches.firefox.misc.debug

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.forceSettingsPreference
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.firefox.shared.whenEnabled
import app.morphe.patches.shared.compat.AppCompatibilities

private const val SECRET_SETTINGS = "$PATCH_SETTINGS->secretSettings()Z"

@Suppress("unused")
val unlockSecretSettingsPatch = bytecodePatch(
    name = "Unlock secret settings",
    description = "Shows the Secret Settings menu and enables custom add-on collections.",
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("secretSettings")

        forceSettingsPreference("pref_key_persistent_debug_menu", true, SECRET_SETTINGS)

        CustomExtensionCollectionFlagFingerprint.matchSingle().let { match ->
            val index = match.instructionMatches.single().index + 1
            match.method.whenEnabled(SECRET_SETTINGS, index) { (enabled) ->
                """
                    const/16 $enabled, 0x1
                    sput-boolean $enabled, $FEATURE_FLAGS->customExtensionCollectionFeature:Z
                """
            }
        }
    }
}
