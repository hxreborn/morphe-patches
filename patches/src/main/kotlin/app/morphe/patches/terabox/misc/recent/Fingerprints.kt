/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.recent

import app.morphe.patcher.Fingerprint

internal object HomeCardFragmentFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("home_shortcut_tab_edit_click"),
)

internal fun homeViewCreatedFingerprint(fragment: String) = Fingerprint(
    definingClass = fragment,
    name = "onViewCreated",
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
)

internal fun homeResumeFingerprint(fragment: String) = Fingerprint(
    definingClass = fragment,
    name = "onResume",
    returnType = "V",
    parameters = emptyList(),
)
