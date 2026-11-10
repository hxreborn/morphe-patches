/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flights25

import app.morphe.patcher.patch.ApkArchitecture
import app.morphe.patcher.patch.InstallerType
import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patches.flights25.misc.premium.calendarImportPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class CalendarImportPatchTest {
    @Test
    fun `excludes calendar imports from unsupported architectures`() {
        val availability = assertNotNull(calendarImportPatch.availability)
        val expected = mapOf(
            ApkArchitecture.ARM64_V8A to PatchAvailability.ENABLED,
            ApkArchitecture.UNIVERSAL to PatchAvailability.ENABLED,
            ApkArchitecture.ARMEABI_V7A to PatchAvailability.UNAVAILABLE,
            ApkArchitecture.X86_64 to PatchAvailability.UNAVAILABLE,
            ApkArchitecture.X86 to PatchAvailability.UNAVAILABLE,
        )

        for (installer in InstallerType.entries) {
            val actual = ApkArchitecture.entries.associateWith { availability.resolve(installer, it) }
            assertEquals(expected, actual, installer.name)
        }
    }

    @Test
    fun `includes license correction when calendar import runs alone`() {
        assertEquals(setOf(removePairipProtectionPatch), calendarImportPatch.dependencies)
    }
}
