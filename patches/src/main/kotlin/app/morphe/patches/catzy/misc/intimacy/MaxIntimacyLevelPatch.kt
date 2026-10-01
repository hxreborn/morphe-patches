/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy.misc.intimacy

import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patches.catzy.catzyPatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val maxIntimacyLevelPatch = catzyPatch(
    name = "Max intimacy level",
    description = "Raises pet intimacy to the highest level.",
    selection = PatchAvailability.DISABLED,
) {
    IntimacyPointsFingerprint.matchSingle().method.returnEarly(999_999)
}
