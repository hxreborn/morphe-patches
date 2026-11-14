/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.layout.popups

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.candycrushsaga.shared.serverresponse.SERVER_RESPONSES
import app.morphe.patches.candycrushsaga.shared.serverresponse.editServerResponsesPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceStub

@Suppress("unused")
val removeOfferPopupsPatch = bytecodePatch(
    name = "Remove offer pop-ups",
    description = "Removes promotional offer pop-ups.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA)
    dependsOn(editServerResponsesPatch)
    execute {
        mutableClassDefBy(SERVER_RESPONSES).replaceStub("removeOffers", 1, """
            const/4 v0, 0x1
            return v0
        """)
    }
}
