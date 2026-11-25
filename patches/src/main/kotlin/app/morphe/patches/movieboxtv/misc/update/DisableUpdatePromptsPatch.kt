/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.update

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

private const val REMOTE_VERSION_INFO = "Lcom/transsion/version/update/RemoteVersionInfo;"

private object ForceUpdateFingerprint : Fingerprint(
    definingClass = REMOTE_VERSION_INFO,
    name = "getForceUpdate",
    returnType = "Z",
    parameters = listOf(),
)

private object HasUpdateFingerprint : Fingerprint(
    definingClass = REMOTE_VERSION_INFO,
    name = "getHasUpdate",
    returnType = "Z",
    parameters = listOf(),
)

@Suppress("unused")
val disableUpdatePromptsPatch = patch(
    name = "Disable update prompts",
    description = "Removes update prompts and forced updates.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)

    execute {
        listOf(ForceUpdateFingerprint, HasUpdateFingerprint).forEach { it.method.returnEarly(false) }
    }
}
