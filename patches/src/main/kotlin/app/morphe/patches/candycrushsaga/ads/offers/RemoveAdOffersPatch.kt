/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.ads.offers

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.candycrushsaga.shared.serverresponse.SERVER_RESPONSES
import app.morphe.patches.candycrushsaga.shared.serverresponse.editServerResponsesPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceStub

@Suppress("unused")
val removeAdOffersPatch = bytecodePatch(
    name = "Remove ad offers",
    description = "Removes offers to watch an ad for extra moves, lives or boosters.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA, AppCompatibilities.CANDY_CRUSH_SODA_SAGA)
    dependsOn(editServerResponsesPatch)
    execute {
        mutableClassDefBy(SERVER_RESPONSES).replaceStub("removeAdOffers", 1, """
            const/4 v0, 0x1
            return v0
        """)
    }
}
