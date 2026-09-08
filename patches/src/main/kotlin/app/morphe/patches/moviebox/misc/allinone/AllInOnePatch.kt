/*
 * SPDX-FileCopyrightText: 2026 rushiranpise
 * SPDX-FileCopyrightText: 2026 hxreborn
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

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.returnEarly

private const val PREMIUM_MEMBER_TYPE = 2
private const val PARALLEL_DOWNLOAD_TASKS = 5
private const val DAYS_LEFT = 9999
private const val EXPIRY_DATE = "2099-12-31"
private const val AD_SDK_COUNTRY = "90101"

private val REQUIRE_MEMBER_TYPE_HOLDERS = listOf(
    "Lcom/transsion/baselib/db/download/DownloadBean;",
    "Lcom/transsion/moviedetailapi/DownloadItem;",
    "Lcom/transsion/shorttv/bean/DownloadItem;",
    "Lcom/transsion/shorttv_pugc/bean/DownloadItem;",
)

private val MINTEGRAL_LOADERS = listOf(
    "Lcom/hisavana/mintegral/executer/MintegralVideo;" to "initVideo",
    "Lcom/hisavana/mintegral/executer/MintegralBanner;" to "showBanner",
    "Lcom/hisavana/mintegral/executer/MintegralNative;" to "initNative",
    "Lcom/hisavana/mintegral/executer/MintegralInterstitial;" to "initInterstitial",
    "Lcom/hisavana/mintegral/executer/MintegralSplash;" to "onSplashStartLoad",
)

context(context: BytecodePatchContext)
private fun classOf(type: String) = context.mutableClassDefByOrNull(type)
    ?: throw PatchException("$type not found")

private fun MutableClass.method(
    name: String,
    returnType: String,
    vararg parameters: String,
): MutableMethod = methods.firstOrNull {
    it.name == name && it.returnType == returnType &&
        it.parameterTypes.map(CharSequence::toString) == parameters.toList()
} ?: throw PatchException("$type->$name(${parameters.joinToString()})$returnType not found")

private fun MutableMethod.returnBoxedBoolean(value: Boolean) = addInstructions(
    0,
    """
        sget-object v0, Ljava/lang/Boolean;->${if (value) "TRUE" else "FALSE"}:Ljava/lang/Boolean;
        return-object v0
    """,
)

private fun MutableMethod.returnBoxedInt(value: Int) = addInstructions(
    0,
    """
        const/16 v0, $value
        invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
        move-result-object v0
        return-object v0
    """,
)

@Suppress("unused")
val allInOnePatch = bytecodePatch(
    name = "All-In-One",
    description = "Unlocks premium, removes ads and upsell prompts, and bypasses the region block. " +
        "The home feed still carries the app's own update banner.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX)

    execute {
        classOf("Lcom/transsion/memberapi/MemberCheckResult;").apply {
            listOf("isPassed", "getVipEnable", "getVipPayEnable").forEach {
                method(it, "Ljava/lang/Boolean;").returnBoxedBoolean(true)
            }
        }

        classOf("Lcom/transsion/memberapi/MemberInfo;").apply {
            method("isActive", "Z").returnEarly(true)
            method("getMemberType", "I").returnEarly(PREMIUM_MEMBER_TYPE)
            method("getDaysLeft", "Ljava/lang/Integer;").returnBoxedInt(DAYS_LEFT)
            method("getExpiryDate", "Ljava/lang/String;").returnEarly(EXPIRY_DATE)
            method("getNextRenewDate", "Ljava/lang/String;").returnEarly(EXPIRY_DATE)
        }

        classOf("Lcom/transsion/member/bean/MemberBriefInfo;").apply {
            method("isActive", "Z").returnEarly(true)
            method("getMemberType", "I").returnEarly(PREMIUM_MEMBER_TYPE)
            method("getExpiryDate", "Ljava/lang/String;").returnEarly(EXPIRY_DATE)
        }

        classOf("Lcom/transsion/member/MemberProvider;").apply {
            method("l", "Z").returnEarly(true)
            method("d", "Z").returnEarly(true)
            method("e", "Z").returnEarly(true)
            method("z", "Z").returnEarly(true)
            method("C", "I").returnEarly(PARALLEL_DOWNLOAD_TASKS)
            method("w", "V", "F").returnEarly()
        }

        classOf("Lcom/transsion/member/ObserveLoginAction;")
            .method("onLogout", "V").returnEarly()

        classOf("Lcom/transsion/baselib/db/member/MemberResolutionBean;").apply {
            method("isUnlock", "Ljava/lang/Boolean;").returnBoxedBoolean(true)
            method("getVipResolutionTip", "Ljava/lang/Boolean;").returnBoxedBoolean(false)
        }

        classOf("Lcom/transsion/baselib/db/member/MemberResolutionDao\$DefaultImpls;")
            .method(
                "b",
                "Ljava/lang/Object;",
                "Lcom/transsion/baselib/db/member/MemberResolutionDao;",
                "Ljava/lang/String;",
                "I",
                "I",
                "Z",
                "Lkotlin/coroutines/Continuation;",
            ).addInstructions(
                0,
                """
                    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
                    return-object v0
                """,
            )

        REQUIRE_MEMBER_TYPE_HOLDERS.forEach {
            classOf(it).method("getRequireMemberType", "Ljava/lang/Integer;").returnBoxedInt(0)
        }

        classOf("Lcom/transsion/moviedetailapi/bean/DownloadResolutionItem;")
            .method("getRequireMemberType", "I").returnEarly(0)

        classOf("Lcom/transsion/baselib/net/AppLifeStatusInterceptor;").apply {
            method("i", "Z", "Lokhttp3/Interceptor\$Chain;").returnEarly(false)
            method("g", "V").returnEarly()
        }

        classOf("Lcom/transsion/subroom/activity/NotAvailableActivity;")
            .method("initView", "V", "Landroid/os/Bundle;").addInstructions(
                0,
                """
                    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                    return-void
                """,
            )

        classOf("Lcom/transsion/version/update/RemoteVersionInfo;").apply {
            method("getForceUpdate", "Z").returnEarly(false)
            method("getHasUpdate", "Z").returnEarly(false)
        }

        classOf("Lcom/transsion/ad/strategy/NationalInformationManager;")
            .method("d", "Ljava/lang/String;").returnEarly(AD_SDK_COUNTRY)

        classOf("Lcom/transsion/ad/scene/SceneInterceptManager;")
            .method(
                "a",
                "Ljava/lang/Object;",
                "Ljava/lang/String;",
                "Lkotlin/coroutines/Continuation;",
            ).addInstructions(
                0,
                """
                    new-instance v0, Lkotlin/Pair;
                    sget-object v1, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                    const-string v2, "no ads"
                    invoke-direct {v0, v1, v2}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                    return-object v0
                """,
            )

        classOf("Lcom/transsion/ad/scene/a;")
            .method("s", "I", "Ljava/lang/String;").returnEarly(0)

        MINTEGRAL_LOADERS.forEach { (type, name) ->
            classOf(type).method(name, "V").returnEarly()
        }
    }
}
