/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc

import app.morphe.patcher.patch.ApkArchitecture
import app.morphe.patcher.patch.InstallerType
import app.morphe.patcher.patch.PatchAvailability
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AvailabilityTest {
    @Test
    fun `requires a root mount patch on an arm64 mount install`() {
        assertEquals(PatchAvailability.REQUIRED, requireArm64RootMount.resolve(InstallerType.MOUNT, ApkArchitecture.ARM64_V8A))
    }

    @Test
    fun `requires a root mount patch on a universal mount install`() {
        assertEquals(PatchAvailability.REQUIRED, requireArm64RootMount.resolve(InstallerType.MOUNT, ApkArchitecture.UNIVERSAL))
    }

    @Test
    fun `excludes a root mount patch from a 32-bit mount install`() {
        assertEquals(PatchAvailability.UNAVAILABLE, requireArm64RootMount.resolve(InstallerType.MOUNT, ApkArchitecture.ARMEABI_V7A))
    }

    @Test
    fun `excludes a root mount patch from a standard install`() {
        assertEquals(PatchAvailability.UNAVAILABLE, requireArm64RootMount.resolve(InstallerType.STANDARD, ApkArchitecture.ARM64_V8A))
    }

    @Test
    fun `excludes a root mount patch from a Shizuku install`() {
        assertEquals(PatchAvailability.UNAVAILABLE, requireArm64RootMount.resolve(InstallerType.SHIZUKU, ApkArchitecture.ARM64_V8A))
    }

    @Test
    fun `enables an arm patch on an armeabi-v7a install`() {
        assertEquals(PatchAvailability.ENABLED, requireArm.resolve(InstallerType.STANDARD, ApkArchitecture.ARMEABI_V7A))
    }

    @Test
    fun `excludes an arm patch from an x86_64 install`() {
        assertEquals(PatchAvailability.UNAVAILABLE, requireArm.resolve(InstallerType.STANDARD, ApkArchitecture.X86_64))
    }
}
