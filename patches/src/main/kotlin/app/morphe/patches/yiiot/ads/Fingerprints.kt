/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.yiiot.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val PLAN_LOOKUP_WINDOW = 3
private const val PAID_SERVICE_CHECK_WINDOW = 2

internal object SetNoAdPlanFingerprint : Fingerprint(
    definingClass = "Lcom/xiaoyi/profile/UserFragment;",
    name = "setNoAdPlan",
    returnType = "V",
    filters = listOf(
        methodCall(name = "getIndicatorView"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = emptyList(), location = MatchAfterWithin(PLAN_LOOKUP_WINDOW)),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = emptyList(),
            returnType = "Z",
            location = MatchAfterWithin(PAID_SERVICE_CHECK_WINDOW),
        ),
    ),
)

internal object LoadAppOpenAdFingerprint : Fingerprint(
    definingClass = "Lcom/ants360/yicamera/util/GoogleAdManager;",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Z"),
    strings = listOf("KEY_SPLASH_BANNER_SCREEN_DATA"),
    filters = listOf(opcode(Opcode.IF_EQZ)),
)

internal object AdDialogFragmentOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/ants360/yicamera/fragment/AdDialogFragment;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(opcode(Opcode.INVOKE_SUPER)),
)

internal object TopBannerListFingerprint : Fingerprint(
    definingClass = "Lcom/xiaoyi/cloud/newCloud/bean/BannerDetailInfo;",
    name = "getTopBanner",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)

internal object BottomBannerListFingerprint : Fingerprint(
    definingClass = "Lcom/xiaoyi/cloud/newCloud/bean/BannerDetailInfo;",
    name = "getBottomBanner",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)

internal object HomePromoPopupFingerprint : Fingerprint(
    definingClass = "Lcom/ants360/yicamera/db/AdsManager;",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Lcom/xiaoyi/callspi/app/BaseActivity;"),
    filters = listOf(string("ADS_LAST_SHOW_PIC")),
)

internal object NebulaPageListenersFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "L", "L"),
    strings = listOf("bannerListener", "inAppMessageListener"),
)

internal object HighlightAdPlaceholderRowsFingerprint : Fingerprint(
    definingClass = "Lcom/ants360/yicamera/viewmodel/DeviceListViewModel;",
    name = "getHighlightAdFakeData",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)

private const val DEVICE_LIST_FRAGMENT = "Lcom/ants360/yicamera/fragment/DeviceListFragment;"
private const val SHOPPING_OBSERVER_CALL_WINDOW = 2

internal object PremiumCloudRowFingerprint : Fingerprint(
    definingClass = DEVICE_LIST_FRAGMENT,
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        string("ignore cloud discover"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Ljava/lang/String;"),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
        anyInstruction(
            opcode(Opcode.GOTO),
            opcode(Opcode.GOTO_16),
            opcode(Opcode.GOTO_32),
            location = MatchAfterImmediately(),
        ),
        fieldAccess(
            definingClass = DEVICE_LIST_FRAGMENT,
            name = "deviceRecyclerItemList",
            type = "Ljava/util/List;",
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object ShoppingProductsObserverFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/List;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            definingClass = DEVICE_LIST_FRAGMENT,
            parameters = listOf(DEVICE_LIST_FRAGMENT),
            returnType = "Ljava/util/List;",
        ),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            name = "clear",
            location = MatchAfterWithin(SHOPPING_OBSERVER_CALL_WINDOW),
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            definingClass = DEVICE_LIST_FRAGMENT,
            parameters = listOf(DEVICE_LIST_FRAGMENT),
            returnType = "Ljava/util/List;",
            location = MatchAfterWithin(SHOPPING_OBSERVER_CALL_WINDOW),
        ),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            name = "addAll",
            location = MatchAfterWithin(SHOPPING_OBSERVER_CALL_WINDOW),
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            definingClass = DEVICE_LIST_FRAGMENT,
            parameters = listOf(DEVICE_LIST_FRAGMENT),
            returnType = "V",
            location = MatchAfterWithin(SHOPPING_OBSERVER_CALL_WINDOW),
        ),
        opcode(Opcode.RETURN_VOID, MatchAfterImmediately()),
    ),
)
