/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hypic.misc.upselling

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object HomeProButtonFingerprint : Fingerprint(
    returnType = "Landroid/view/View;",
    parameters = listOf("Landroidx/constraintlayout/widget/ConstraintLayout;"),
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "xt_layout_subscribe_entrance_home_page"),
    ),
)

private fun subscriptionsEnabledReadBefore(key: String) = listOf(
    methodCall(opcode = Opcode.INVOKE_STATIC, parameters = emptyList(), returnType = "Z"),
    opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    string(key),
)

internal object SettingsPlansRowFingerprint : Fingerprint(
    definingClass = "Lcom/xt/retouch/settings/SettingFragment;",
    name = "onCreateView",
    filters = subscriptionsEnabledReadBefore("plansAndBillingsLayout"),
)

internal object ProfileProEntranceFingerprint : Fingerprint(
    definingClass = "Lcom/xt/retouch/subscribe/impl/pay/SubscribeGlobalBridgeProcessor;",
    name = "getEditConfig",
    filters = subscriptionsEnabledReadBefore("vip_entrance_enable"),
)

internal object LynxAppFetchFingerprint : Fingerprint(
    definingClass = "Lcom/lm/components/lynx/bridge/handler/LynxCommonBridge;",
    name = "appFetch",
    returnType = "V",
)

internal object PromotionSlotRequestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "L", "Lorg/json/JSONObject;"),
    strings = listOf("resource_keys"),
)

internal object SubscribePageCreateFingerprint : Fingerprint(
    definingClass = "Lcom/xt/retouch/applauncher/module/commerce/HypicComposeSubscribeActivity;",
    name = "onCreate",
    filters = listOf(fieldAccess(type = "Ljava/lang/ref/WeakReference;", opcode = Opcode.SPUT_OBJECT)),
)
