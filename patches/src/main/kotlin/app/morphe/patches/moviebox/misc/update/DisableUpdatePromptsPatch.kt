/*
 * Copyright (C) 2026 rushiranpise
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Additional terms under GPLv3 section 7:
 * - You must preserve reasonable legal notices and author attributions in this file.
 * - Modified versions must not misrepresent the origin of this file.
 *
 * Ported from rushiranpise/morphe-patches:
 * https://github.com/rushiranpise/morphe-patches/commit/636876589815a96ed23979b3a329f5b7826032af
 * Commit 636876589815a96ed23979b3a329f5b7826032af (2026-09-06),
 * patches/src/main/kotlin/app/template/patches/moviebox/phone/MovieBoxPhonePatch.kt
 */
package app.morphe.patches.moviebox.misc.update

import app.morphe.patcher.patch.patch
import app.morphe.patches.moviebox.misc.playback.playbackPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.requireArm64StandardInstall

private const val REMOTE_VERSION_INFO = "Lcom/transsion/version/update/RemoteVersionInfo;"

private const val NO_UPDATE_TYPE = ""

@Suppress("unused")
val disableUpdatePromptsPatch = patch(
    name = "Disable update prompts",
    description = "Removes update prompts and forced updates.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX)
    dependsOn(playbackPatch)
    availability(requireArm64StandardInstall)

    execute {
        editIjiamiPayload {
            listOf("getForceUpdate", "getHasUpdate").forEach {
                method(REMOTE_VERSION_INFO, it).returnEarly(false)
            }
            method(REMOTE_VERSION_INFO, "getNoticeConfig").returnNull()
            method(REMOTE_VERSION_INFO, "getUpdateType").returnString(NO_UPDATE_TYPE)
        }
    }
}
