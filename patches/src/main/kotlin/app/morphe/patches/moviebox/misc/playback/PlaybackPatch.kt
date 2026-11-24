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
package app.morphe.patches.moviebox.misc.playback

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.ijiami.ijiamiPatch
import app.morphe.patches.shared.misc.ijiami.installAppHook
import app.morphe.util.inputStreamFromBundledResource

private const val APP_HOOK_CLASS = "hx.AppPatch"

private const val LIFE_STATUS_INTERCEPTOR = "Lcom/transsion/baselib/net/AppLifeStatusInterceptor;"
private const val TINKER_INTERNALS = "Lcom/tencent/tinker/loader/shareutil/ShareTinkerInternals;"
private const val PLAY_MODE = "Lkk/s;"

private const val PLAY_MODE_IS_STREAM = "b"

private val REGION_BLOCK_HANDLERS = listOf("i", "j", "k")
private const val BACKGROUND_REQUEST_FREEZE = "n"

private const val NO_SIM = ""

private val SIM_OPERATOR_READERS = listOf(
    "Lcom/transsion/api/gateway/utils/DeviceUtils;" to "getSimOperator",
    "Lcom/transsion/core/deviceinfo/DeviceInfo;" to "f",
    "Loh/b;" to "o",
)

internal val playbackPatch = patch {
    dependsOn(ijiamiPatch)

    execute {
        val hook = inputStreamFromBundledResource("moviebox", "dashhook.dex")
            ?: throw PatchException("Missing bundled resource: moviebox/dashhook.dex")
        installAppHook(hook.use { it.readBytes() }, APP_HOOK_CLASS)

        editIjiamiPayload {
            method(PLAY_MODE, PLAY_MODE_IS_STREAM).returnEarly(true)

            SIM_OPERATOR_READERS.forEach { (holder, name) -> method(holder, name).returnString(NO_SIM) }

            REGION_BLOCK_HANDLERS.forEach { method(LIFE_STATUS_INTERCEPTOR, it).returnEarly() }
            method(LIFE_STATUS_INTERCEPTOR, BACKGROUND_REQUEST_FREEZE).returnEarly(false)

            method(TINKER_INTERNALS, "isTinkerEnabled").returnEarly(false)
        }
    }
}
