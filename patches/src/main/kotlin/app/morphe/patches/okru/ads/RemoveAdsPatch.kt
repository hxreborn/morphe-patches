/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.okru.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private val DISABLED_AD_FLAG_KEYS = listOf(
    "vk.clips.feed.ad.enabled",
    "vk.clips.static.ads.enabled",
    "vk.clips.market.ads.enabled",
    "vk.clips.banner.carousel.enabled",
    "stream.vkclip.closeable_overlay.advertisement.enabled",
    "vkminiapps.ads.enabled",
)

private const val RETURN_EMPTY_LIST = """
    invoke-static { }, Ljava/util/Collections;->emptyList()Ljava/util/List;
    move-result-object v0
    return-object v0
"""

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes sponsored posts, video and clip ads, and ads in messages, notifications, " +
        "discussions, games, music and Dzen.",
) {
    compatibleWith(AppCompatibilities.OK)

    execute {
        BindReplaceableBannerFingerprint.matchSingle().method.returnEarly()
        BindBannerLinkFingerprint.matchSingle().method.returnEarly()
        ObtainInstreamAdControllerFingerprint.matchSingle().method.returnEarly(null)

        MyTargetIsLoadCalledFingerprint.matchSingle().method.returnEarly(true)
        MyTargetViewLoadFingerprint.matchSingle().method.returnEarly()

        LoadDiscussionBannerFingerprint.matchSingle().method.returnEarly()
        listOf(ZenLoadAdRequestFingerprint, ZenLoadAdRequestListFingerprint, GamesAdSourcesFingerprint).forEach { fingerprint ->
            val method = fingerprint.matchSingle().method
            val target = if (method.implementation!!.registerCount == 0) method.addFreeRegisters(1).first else method
            target.addInstructions(0, RETURN_EMPTY_LIST)
        }

        VideoCatalogLoadBannersFingerprint.matchSingle().let { match ->
            val (loadBannersRead) = match.instructionMatches
            val register = loadBannersRead.getInstruction<TwoRegisterInstruction>().registerA
            match.method.replaceInstruction(loadBannersRead.index, "const/4 v$register, 0x0")
        }

        DISABLED_AD_FLAG_KEYS.forEach { key ->
            adFlagFingerprint(key).matchSingle().method.returnEarly(false)
        }
    }
}
