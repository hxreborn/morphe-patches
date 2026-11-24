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
package app.morphe.patches.moviebox.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.moviebox.misc.playback.playbackPatch
import app.morphe.patches.moviebox.shared.FREE
import app.morphe.patches.moviebox.shared.MEMBER_PROVIDER
import app.morphe.patches.moviebox.shared.PREMIUM_PROVIDER
import app.morphe.patches.moviebox.shared.UNLIMITED
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.requireArm64StandardInstall

private const val PREMIUM_MARKER_ASSET = "assets/hx_unlock_premium"
private const val PREMIUM_MARKER_CONTENT = "unlocked"

private const val PREMIUM_MEMBER_TYPE = 2
private const val DAYS_LEFT = 9999
private const val NO_AD_UNLOCKS = 0
private const val PARALLEL_DOWNLOAD_TASKS = 5
private const val DOWNLOAD_RESOLUTIONS = 5

private const val MEMBER_CHECK_RESULT = "Lcom/transsion/memberapi/MemberCheckResult;"
private const val MEMBER_INFO = "Lcom/transsion/memberapi/MemberInfo;"
private const val MEMBER_BRIEF_INFO = "Lcom/transsion/member/bean/MemberBriefInfo;"
private const val PREMIUM_ACCESS = "Lcom/transsion/memberapi/PremiumV2CheckAccessDto;"
private const val MEMBER_RESOLUTION = "Lcom/transsion/baselib/db/member/MemberResolutionBean;"
private const val OBSERVE_LOGIN_ACTION = "Lcom/transsion/member/ObserveLoginAction;"
private const val MEMBER_LOGIN_HELPER = "Lgy/a;"
private const val BOTTOM_TAB_TYPE = "Lcom/transsion/home/enum/BottomTabType;"
private const val DOWNLOAD_RESOLUTION_ITEM = "Lcom/transsion/moviedetailapi/bean/DownloadResolutionItem;"

private val MEMBER_CHECK_FLAGS = listOf("isPassed", "getVipEnable", "getVipPayEnable")

private const val PREMIUM_TAB_VALUE = "BT_PREMIUM"
private const val PREMIUM_TAB_ENUM_NAME = "PREMIUM"

private const val PAY_MEMBER_ENABLED_FLAG = "f"
private const val SKIP_AD_FLAG = "g"
private const val MEMBER_ENABLED_FLAG = "A"
private const val PARALLEL_DOWNLOAD_TASK_COUNT = "E"
private const val SHOW_MEMBERSHIP_OFFER = "w"
private val MEMBER_FLAGS = listOf(PAY_MEMBER_ENABLED_FLAG, SKIP_AD_FLAG, MEMBER_ENABLED_FLAG)

private const val FREE_DOWNLOAD_COUNT = "g"
private const val MAX_RESOLUTION = "z"
private const val FREE_PLAY_SECONDS = "m"
private const val FREE_BLOCK_TIME = "y"
private const val HIGH_QUALITY_FREE_TIME = "r"
private const val DAILY_FREE_PLAY_COUNT = "D"
private const val FREE_INCENTIVIZED_AD_COUNT = "l"
private const val HIGH_QUALITY_INCENTIVIZED_AD_COUNT = "v"
private const val INCENTIVIZED_AD_COUNT = "s"
private const val RESOLUTIONS_PER_DOWNLOAD = "O"
private val FREE_PLAY_QUOTAS = listOf(
    FREE_PLAY_SECONDS,
    FREE_BLOCK_TIME,
    HIGH_QUALITY_FREE_TIME,
    DAILY_FREE_PLAY_COUNT,
)
private val AD_UNLOCK_COUNTS = listOf(
    FREE_INCENTIVIZED_AD_COUNT,
    HIGH_QUALITY_INCENTIVIZED_AD_COUNT,
    INCENTIVIZED_AD_COUNT,
)

private const val SHOW_MEMBER_LOGIN_DIALOG = "b"

private val REQUIRE_MEMBER_TYPE_HOLDERS = listOf(
    "Lcom/transsion/baselib/db/download/DownloadBean;",
    "Lcom/transsion/baselib/db/download/VipInfo;",
    "Lcom/transsion/moviedetailapi/DownloadItem;",
    "Lcom/transsion/shorttv/bean/DownloadItem;",
    "Lcom/transsion/shorttv_pugc/bean/DownloadItem;",
)

private val NEED_PAID_HOLDERS = listOf(
    "Lcom/transsion/shorttv/bean/ShortTVItem;",
    "Lcom/transsion/shorttv/bean/Subject;",
)

private val MEMBER_INFO_CALLERS = mapOf(
    (MEMBER_PROVIDER to "isActive") to listOf("c"),
    (PREMIUM_PROVIDER to "isActive") to listOf("c"),
    (PREMIUM_PROVIDER to "getVipLevel") to listOf("A", "i"),
    (PREMIUM_PROVIDER to "getDaysLeft") to listOf("o"),
)

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium, unlimited playback time, unlimited downloads, every resolution " +
        "and the Laboratory menu, and removes membership offers.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX)
    dependsOn(playbackPatch)
    availability(requireArm64StandardInstall)

    execute {
        resourceFile(PREMIUM_MARKER_ASSET, copy = false).writeText(PREMIUM_MARKER_CONTENT)

        editIjiamiPayload {
            fun memberInfoCallers(holder: String, callee: String) =
                MEMBER_INFO_CALLERS.getValue(holder to callee).map { method(holder, it) }

            MEMBER_CHECK_FLAGS.forEach { method(MEMBER_CHECK_RESULT, it).returnBoxed(true) }

            listOf(MEMBER_INFO, MEMBER_BRIEF_INFO).forEach {
                method(it, "isActive").returnEarly(true)
                method(it, "getMemberType").returnEarly(PREMIUM_MEMBER_TYPE)
            }

            MEMBER_FLAGS.forEach { method(MEMBER_PROVIDER, it).returnEarly(true) }
            memberInfoCallers(MEMBER_PROVIDER, "isActive").forEach { it.returnEarly(true) }
            method(MEMBER_PROVIDER, PARALLEL_DOWNLOAD_TASK_COUNT).returnEarly(PARALLEL_DOWNLOAD_TASKS)
            method(MEMBER_PROVIDER, SHOW_MEMBERSHIP_OFFER).returnEarly()
            methodWithString(BOTTOM_TAB_TYPE, PREMIUM_TAB_VALUE).replaceString(PREMIUM_TAB_VALUE, PREMIUM_TAB_ENUM_NAME)

            memberInfoCallers(PREMIUM_PROVIDER, "isActive").forEach { it.returnEarly(true) }
            memberInfoCallers(PREMIUM_PROVIDER, "getVipLevel").forEach { it.returnEarly(true) }
            method(PREMIUM_PROVIDER, FREE_DOWNLOAD_COUNT).returnEarly(UNLIMITED)
            method(PREMIUM_PROVIDER, MAX_RESOLUTION).returnEarly(UNLIMITED)
            FREE_PLAY_QUOTAS.forEach { method(PREMIUM_PROVIDER, it).returnEarly(UNLIMITED) }
            AD_UNLOCK_COUNTS.forEach { method(PREMIUM_PROVIDER, it).returnEarly(NO_AD_UNLOCKS) }
            method(PREMIUM_PROVIDER, RESOLUTIONS_PER_DOWNLOAD).returnEarly(DOWNLOAD_RESOLUTIONS)
            memberInfoCallers(PREMIUM_PROVIDER, "getDaysLeft").forEach { it.returnBoxed(DAYS_LEFT) }
            method(PREMIUM_ACCESS, "getHasAccess").returnBoxed(true)

            method(MEMBER_RESOLUTION, "isUnlock").returnBoxed(true)
            method(MEMBER_RESOLUTION, "getVipResolutionTip").returnBoxed(false)
            method(OBSERVE_LOGIN_ACTION, "onLogout").returnEarly()
            method(MEMBER_LOGIN_HELPER, SHOW_MEMBER_LOGIN_DIALOG).returnEarly(false)

            REQUIRE_MEMBER_TYPE_HOLDERS.forEach { method(it, "getRequireMemberType").returnNull() }
            method(DOWNLOAD_RESOLUTION_ITEM, "getRequireMemberType").returnEarly(FREE)
            NEED_PAID_HOLDERS.forEach { method(it, "getNeedPaid").returnEarly(FREE) }
        }
    }
}
