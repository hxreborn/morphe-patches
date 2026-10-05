/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.nags

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.returnEarly

@Suppress("unused")
val removeNagsPatch = bytecodePatch(
    name = "Remove nags",
    description = "Removes the rate this app dialog, the promotional dialogs shown on startup " +
        "and the Premium button in the toolbar.",
) {
    compatibleWith(AppCompatibilities.READERA)

    execute {
        StartupNagDispatchFingerprint.matchAll(1..1)

        StartupNagDispatchFingerprint.instructionMatches.forEach {
            it.getMethodCalled().returnEarly()
        }

        val promoDispatcher = StartupNagDispatchFingerprint.instructionMatches.last().getMethodCalled()
        ToolbarPromoModeFingerprint.matchAll(classDefBy(promoDispatcher.definingClass), 1..1)
        ToolbarPromoModeFingerprint.method.returnEarly(FRESH_INSTALL_TOOLBAR_MODE)
    }
}
