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
package app.morphe.patches.moviebox.ads

import app.morphe.patcher.patch.patch
import app.morphe.patches.moviebox.misc.playback.playbackPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.requireArm64StandardInstall

private const val AD_SETTINGS = "Lqi/f;"
private const val AD_SCENE_CONFIG = "Lcom/transsion/ad/scene/a;"

private const val AD_SETTINGS_ADS_OFF = "c"
private const val AD_SCENE_TIMEOUT = "t"
private const val NO_AD_WAIT_SECONDS = 0

private val MINTEGRAL_LOADERS = listOf(
    "Lcom/hisavana/mintegral/executer/MintegralVideo;" to "initVideo",
    "Lcom/hisavana/mintegral/executer/MintegralBanner;" to "showBanner",
    "Lcom/hisavana/mintegral/executer/MintegralNative;" to "initNative",
    "Lcom/hisavana/mintegral/executer/MintegralInterstitial;" to "initInterstitial",
    "Lcom/hisavana/mintegral/executer/MintegralSplash;" to "onSplashStartLoad",
)

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes ads.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX)
    dependsOn(playbackPatch)
    availability(requireArm64StandardInstall)

    execute {
        editIjiamiPayload {
            method(AD_SETTINGS, AD_SETTINGS_ADS_OFF).returnEarly(true)
            method(AD_SCENE_CONFIG, AD_SCENE_TIMEOUT).returnEarly(NO_AD_WAIT_SECONDS)
            MINTEGRAL_LOADERS.forEach { (holder, name) -> method(holder, name).returnEarly() }
        }
    }
}
