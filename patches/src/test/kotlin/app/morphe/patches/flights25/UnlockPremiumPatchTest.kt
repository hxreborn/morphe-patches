/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flights25

import app.morphe.patches.flights25.misc.premium.unlockPremiumPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class UnlockPremiumPatchTest {
    @Test
    fun `keeps premium independent of native patches`() {
        assertEquals(setOf(removePairipProtectionPatch), unlockPremiumPatch.dependencies)
    }

    @Test
    fun `offers premium on every architecture`() {
        assertNull(unlockPremiumPatch.availability)
    }
}
