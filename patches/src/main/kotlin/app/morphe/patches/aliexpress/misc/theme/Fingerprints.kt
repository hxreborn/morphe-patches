/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val BASE_ACTIVITY = "Lcom/aliexpress/framework/AlgBaseActivity;"
internal const val SET_DEFAULT_NIGHT_MODE = "Landroidx/appcompat/app/AppCompatDelegate;->setDefaultNightMode(I)V"

internal object ActivityConfigurationFingerprint : Fingerprint(
    definingClass = BASE_ACTIVITY,
    returnType = "V",
    parameters = listOf("Landroid/content/res/Configuration;"),
    filters = listOf(
        fieldAccess(
            definingClass = "Landroid/content/res/Configuration;",
            name = "uiMode",
            type = "I",
            opcode = Opcode.IPUT,
        ),
    ),
)

internal object DefaultNightModeFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(smali = SET_DEFAULT_NIGHT_MODE),
    ),
)

internal object DarkModeEnabledFingerprint : Fingerprint(
    classFingerprint = DefaultNightModeFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            definingClass = "this",
            returnType = "Z",
            parameters = emptyList(),
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
        methodCall(
            definingClass = "this",
            returnType = "Z",
            parameters = emptyList(),
            opcode = Opcode.INVOKE_STATIC,
        ),
    ),
)

internal object NativeDarkSwitchFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("is_local_dark_mode_open", "is_local_dark_mode_changed"),
)

internal object NavigationBarFillerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/view/ViewGroup;"),
    filters = listOf(
        newInstance("Landroid/view/View;"),
        methodCall(smali = "Landroid/view/View;->setBackgroundColor(I)V"),
    ),
)

internal object TemplateColorFingerprint : Fingerprint(
    definingClass = "Lcom/ahe/android/hybridengine/widget/AHEWidgetNode;",
    name = "tryFetchDarkModeColor",
    returnType = "I",
    parameters = listOf("Ljava/lang/String;", "I", "I"),
)

internal object WebPageAttachFingerprint : Fingerprint(
    definingClass = "Lcom/aliexpress/adc/adapter/webview/AdcUCWebview;",
    name = "onAttachedToWindow",
    returnType = "V",
    parameters = emptyList(),
)

internal object WebPageMessageFingerprint : Fingerprint(
    definingClass = "Lcom/aliexpress/adc/adapter/webview/AdcUCWebview;",
    name = "handleMessage",
    returnType = "Z",
    parameters = listOf("Landroid/os/Message;"),
)

internal object BackdropImageFingerprint : Fingerprint(
    definingClass = "Lcom/aliexpress/android/aehome/ahe/ahenode/HomeAHEAEImageViewWidgetNode;",
    name = "onRenderView",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/view/View;"),
)

internal object SearchBackdropImageFingerprint : Fingerprint(
    definingClass = "Lcom/aliexpress/android/aesearch/common/ahe/widget/imageview/SearchImageWidgetNode;",
    name = "onRenderView",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/view/View;"),
)

internal object QuantityControlFingerprint : Fingerprint(
    definingClass = "Lcom/aliexpress/component/ahe/view/widgets/ShopCounterView;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I"),
    filters = listOf(opcode(Opcode.RETURN_VOID)),
)

internal object DarkModeServerFlagFingerprint : Fingerprint(
    classFingerprint = DefaultNightModeFingerprint,
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("global_enable_dark_mode_v2"),
)
