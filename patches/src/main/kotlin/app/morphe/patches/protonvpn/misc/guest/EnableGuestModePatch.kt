/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.guest

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow

@Suppress("unused")
val enableGuestModePatch: Patch = patch(
    name = "Enable guest mode",
    description = "Adds Continue as guest to the welcome screen.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN_FDROID)

    execute {
        document("res/values/bools.xml").use { document ->
            document.getElementsByTagName("bool")
                .findElementByAttributeValueOrThrow("name", "core_feature_credential_less_enabled")
                .textContent = "true"
        }
    }
}
