/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.promotions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

private const val MAIN_ACTIVITY = "Lcom/dubox/drive/ui/MainActivity;"
private const val SERVER_RESPONSE = "Lcom/dubox/drive/home/homecard/server/response/"

internal object VipStatusSectionFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string(
            "com.dubox.drive.mine.cmm.ui.aboutme.VipStatusSection (VipStatusSection.kt:",
            StringComparisonType.STARTS_WITH,
        ),
    ),
)

internal object CreditsUnlockDividerFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string(
            "com.dubox.drive.mine.cmm.ui.aboutme.CreditsUnlockDivider (VipStatusSection.kt:",
            StringComparisonType.STARTS_WITH,
        ),
    ),
)

internal object OperationCardFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string(
            "com.dubox.drive.mine.cmm.ui.aboutme.OperationCard (OperationCard.kt:",
            StringComparisonType.STARTS_WITH,
        ),
    ),
)

internal object HomeCardVisibilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;", "I"),
    strings = listOf("cardId"),
    filters = listOf(methodCall(definingClass = "Lcom/dubox/drive/ads/AdManager;")),
)

internal object ActivityPopupFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("${SERVER_RESPONSE}FloatWindowData;", "Landroidx/fragment/app/FragmentActivity;"),
)

internal object DiscountPopupFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    returnType = "V",
    parameters = listOf("${SERVER_RESPONSE}PopupResponse;"),
)

internal object CouponPopupFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "showCouponDialog",
    returnType = "V",
    parameters = emptyList(),
)

internal object VipGuidePopupFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Landroidx/fragment/app/FragmentActivity;",
        "Lcom/dubox/drive/vip/domain/job/server/response/MainVipPopupResponse;",
    ),
)

internal object NewUserGiftPopupFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroidx/fragment/app/FragmentManager;"),
    strings = listOf("is_first_launch_space_manager"),
)

internal object VideoPlayerUpsellFingerprint : Fingerprint(
    definingClass = "Lcom/dubox/drive/ui/preview/video/pageC/VideoPlayerCViewModel;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroidx/lifecycle/LifecycleOwner;"),
    filters = listOf(methodCall(definingClass = "Lcom/dubox/drive/ads/AdManager;")),
)
