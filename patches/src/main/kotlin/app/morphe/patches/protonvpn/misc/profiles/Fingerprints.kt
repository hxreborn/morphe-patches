/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.profiles

import app.morphe.patches.protonvpn.misc.restrictions.FreeUserCheckFingerprint

internal object ProfileAvailabilityFingerprint : FreeUserCheckFingerprint(
    definingClass = "Lcom/protonvpn/android/profiles/ui/ProfilesViewModel;",
    name = "toItem",
)
