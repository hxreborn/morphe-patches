/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.dictionary

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.DRAWER_DICTIONARY_ITEM
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.showDrawerItem
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val dictionarySectionPatch = bytecodePatch(
    name = "Dictionary section",
    description = "Adds the Dictionary section with all saved words to the main menu.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    execute {
        showDrawerItem(DRAWER_DICTIONARY_ITEM)
    }
}
