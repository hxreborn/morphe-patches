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
package app.morphe.patches.moviebox.misc.allinone

import app.morphe.patcher.patch.ApkArchitecture
import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.ijiami.freeDexEntry
import app.morphe.patches.shared.misc.ijiami.ijiamiPatch
import app.morphe.util.inputStreamFromBundledResource

private const val PREMIUM_MEMBER_TYPE = 2
private const val PARALLEL_DOWNLOAD_TASKS = 5
private const val DAYS_LEFT = 9999
private const val DOWNLOAD_RESOLUTIONS = 5
private const val FREE = 0
private const val NO_AD_UNLOCKS = 0
private const val UNLIMITED = Int.MAX_VALUE

private const val MEMBER_CHECK_RESULT = "Lcom/transsion/memberapi/MemberCheckResult;"
private const val MEMBER_INFO = "Lcom/transsion/memberapi/MemberInfo;"
private const val MEMBER_BRIEF_INFO = "Lcom/transsion/member/bean/MemberBriefInfo;"
private const val MEMBER_PROVIDER = "Lcom/transsion/member/MemberProvider;"
private const val PREMIUM_PROVIDER = "Lcom/transsion/member/premium/PremiumProvider;"
private const val PREMIUM_ACCESS = "Lcom/transsion/memberapi/PremiumV2CheckAccessDto;"
private const val MEMBER_RESOLUTION = "Lcom/transsion/baselib/db/member/MemberResolutionBean;"
private const val OBSERVE_LOGIN_ACTION = "Lcom/transsion/member/ObserveLoginAction;"
private const val DOWNLOAD_RESOLUTION_ITEM = "Lcom/transsion/moviedetailapi/bean/DownloadResolutionItem;"
private const val REMOTE_VERSION_INFO = "Lcom/transsion/version/update/RemoteVersionInfo;"
private const val LIFE_STATUS_INTERCEPTOR = "Lcom/transsion/baselib/net/AppLifeStatusInterceptor;"
private const val PLAY_MODE = "Lkk/t;"
private const val AD_SETTINGS = "Lqi/f;"
private const val AD_SCENE_CONFIG = "Lcom/transsion/ad/scene/a;"

private val MEMBER_CHECK_FLAGS = listOf("isPassed", "getVipEnable", "getVipPayEnable")

private val MEMBER_FLAG_KEYS = listOf("kv_is_pay_enable_member", "kv_is_skip_ad", "kv_is_enable_member")
private const val PARALLEL_DOWNLOAD_KEY = "kv_parallel_download_task_num"
private const val UPSELL_DIALOG_LOG = "checkMemberShipShow data:"

private val PREMIUM_QUOTA_KEYS = listOf("free_download_count", "max_resolution")
private val FREE_PLAY_QUOTA_KEYS = listOf(
    "preview_seconds",
    "free_hd_preview_count",
    "free_play_seconds",
    "free_block_time",
    "highquality_free_time",
    "daily_free_play_count",
)
private val AD_UNLOCK_COUNT_KEYS = listOf(
    "free_incentivized_ad_count",
    "highquality_incentivized_ad_count",
    "incentivized_ad_count",
)
private const val RESOLUTIONS_PER_DOWNLOAD_KEY = "per_download_resource_count"

private const val BOOLEAN = "Z"
private const val BOXED_INTEGER = "Ljava/lang/Integer;"

private const val PLAY_MODE_IS_STREAM = "b"

private const val AD_SETTINGS_ADS_OFF = "c"
private const val AD_SCENE_TIMEOUT = "t"

private val REGION_BLOCK_HANDLERS = listOf("i", "j", "k")
private const val BACKGROUND_REQUEST_FREEZE = "n"

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

private const val NO_SIM = ""

private val SIM_OPERATOR_READERS = listOf(
    "Lcom/transsion/api/gateway/utils/DeviceUtils;" to "getSimOperator",
    "Lcom/transsion/core/deviceinfo/DeviceInfo;" to "f",
    "Loh/b;" to "o",
)

private val MINTEGRAL_LOADERS = listOf(
    "Lcom/hisavana/mintegral/executer/MintegralVideo;" to "initVideo",
    "Lcom/hisavana/mintegral/executer/MintegralBanner;" to "showBanner",
    "Lcom/hisavana/mintegral/executer/MintegralNative;" to "initNative",
    "Lcom/hisavana/mintegral/executer/MintegralInterstitial;" to "initInterstitial",
    "Lcom/hisavana/mintegral/executer/MintegralSplash;" to "onSplashStartLoad",
)

@Suppress("unused")
val allInOnePatch = resourcePatch(
    name = "All-In-One",
    description = "Enables video playback and downloads, removes ads and upsell prompts, " +
        "bypasses the region block, and unlocks the hidden Laboratory menu. " +
        "Requires Android 10 or later.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX)
    dependsOn(ijiamiPatch)

    availability { _, architecture ->
        when (architecture) {
            ApkArchitecture.ARM64_V8A, ApkArchitecture.UNIVERSAL -> PatchAvailability.ENABLED
            else -> PatchAvailability.UNAVAILABLE
        }
    }

    execute {
        val hook = inputStreamFromBundledResource("moviebox", "dashhook.dex")
            ?: throw PatchException("Missing bundled resource: moviebox/dashhook.dex")
        get(freeDexEntry(), copy = false).writeBytes(hook.use { it.readBytes() })

        editIjiamiPayload {
            MEMBER_CHECK_FLAGS.forEach { method(MEMBER_CHECK_RESULT, it).returnBoxed(true) }

            listOf(MEMBER_INFO, MEMBER_BRIEF_INFO).forEach {
                method(it, "isActive").returnEarly(true)
                method(it, "getMemberType").returnEarly(PREMIUM_MEMBER_TYPE)
            }

            MEMBER_FLAG_KEYS.forEach { methodWithString(MEMBER_PROVIDER, it).returnEarly(true) }
            methodsCalling(MEMBER_PROVIDER, MEMBER_INFO, "isActive", BOOLEAN).returnEarly(true)
            methodWithString(MEMBER_PROVIDER, PARALLEL_DOWNLOAD_KEY).returnEarly(PARALLEL_DOWNLOAD_TASKS)
            methodWithString(MEMBER_PROVIDER, UPSELL_DIALOG_LOG).returnEarly()

            methodsCalling(PREMIUM_PROVIDER, MEMBER_INFO, "isActive", BOOLEAN).returnEarly(true)
            methodsCalling(PREMIUM_PROVIDER, MEMBER_INFO, "getVipLevel", BOOLEAN).returnEarly(true)
            PREMIUM_QUOTA_KEYS.forEach { methodWithString(PREMIUM_PROVIDER, it).returnEarly(UNLIMITED) }
            FREE_PLAY_QUOTA_KEYS.forEach { methodWithStringOrNull(PREMIUM_PROVIDER, it)?.returnEarly(UNLIMITED) }
            AD_UNLOCK_COUNT_KEYS.forEach { methodWithStringOrNull(PREMIUM_PROVIDER, it)?.returnEarly(NO_AD_UNLOCKS) }
            methodWithString(PREMIUM_PROVIDER, RESOLUTIONS_PER_DOWNLOAD_KEY).returnEarly(DOWNLOAD_RESOLUTIONS)
            methodsCalling(PREMIUM_PROVIDER, MEMBER_INFO, "getDaysLeft", BOXED_INTEGER).returnBoxed(DAYS_LEFT)
            method(PREMIUM_ACCESS, "getHasAccess").returnBoxed(true)

            method(MEMBER_RESOLUTION, "isUnlock").returnBoxed(true)
            method(MEMBER_RESOLUTION, "getVipResolutionTip").returnBoxed(false)
            method(OBSERVE_LOGIN_ACTION, "onLogout").returnEarly()

            REQUIRE_MEMBER_TYPE_HOLDERS.forEach { method(it, "getRequireMemberType").returnNull() }
            method(DOWNLOAD_RESOLUTION_ITEM, "getRequireMemberType").returnEarly(FREE)
            NEED_PAID_HOLDERS.forEach { method(it, "getNeedPaid").returnEarly(FREE) }

            method(PLAY_MODE, PLAY_MODE_IS_STREAM).returnEarly(true)

            method(AD_SETTINGS, AD_SETTINGS_ADS_OFF).returnEarly(true)
            method(AD_SCENE_CONFIG, AD_SCENE_TIMEOUT).returnEarly(0)
            MINTEGRAL_LOADERS.forEach { (holder, name) -> method(holder, name).returnEarly() }

            SIM_OPERATOR_READERS.forEach { (holder, name) -> method(holder, name).returnString(NO_SIM) }

            REGION_BLOCK_HANDLERS.forEach { method(LIFE_STATUS_INTERCEPTOR, it).returnEarly() }
            method(LIFE_STATUS_INTERCEPTOR, BACKGROUND_REQUEST_FREEZE).returnEarly(false)

            listOf("getForceUpdate", "getHasUpdate").forEach {
                method(REMOTE_VERSION_INFO, it).returnEarly(false)
            }
        }
    }
}
