/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val DEFAULT_HOME_BACKGROUND_COLOR = "#E4E4E4"
private const val HOME_BACKGROUND_TINT_PREFERENCE = "home_background_tint_color"
private const val OPAQUE_BLACK_ARGB = -0x1000000
private const val OPAQUE_WHITE_ARGB = -0x1
private const val TITLE_VIEW_CLASS = "Lcom/realme/iot/common/widgets/TitleView;"

internal object HeadsetSubScreenBackgroundFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
    name = "init",
    definingClass = "Lcom/realme/iot/headset/activity/device/HeadsetDeviceSubScreenActivity;",
    filters = listOf(
        literal(OPAQUE_WHITE_ARGB),
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "setBackgroundColor",
            location = InstructionLocation.MatchAfterImmediately(),
        ),
    ),
)

internal object HeadsetSubScreenTitleFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
    name = "initToolbar",
    definingClass = "Lcom/realme/iot/headset/activity/device/HeadsetDeviceSubScreenActivity;",
    filters = listOf(
        methodCall(definingClass = TITLE_VIEW_CLASS, name = "setCenterText"),
    ),
)

internal object HeadsetVirtualDeviceTitleFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    definingClass = "Lcom/realme/iot/headset/activity/device/HeadsetVirtualDeviceActivity;",
    filters = listOf(
        methodCall(
            definingClass = TITLE_VIEW_CLASS,
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "V",
        ),
    ),
)

internal object SpatialAudioPlayButtonTextColorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z"),
    definingClass = "Lcom/realme/iot/headset/activity/soundeffect/SpatialAudioActivity;",
    filters = listOf(
        literal(OPAQUE_BLACK_ARGB),
        methodCall(
            definingClass = "Landroid/widget/TextView;",
            name = "setTextColor",
            location = InstructionLocation.MatchAfterImmediately(),
        ),
    ),
)

internal object ApplicationOnCreateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
    name = "onCreate",
    definingClass = "Lcom/realme/link/LinkApplication;",
)

internal object SetDefaultNightModeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("I"),
    definingClass = "Landroidx/appcompat/app/AppCompatDelegate;",
    name = "setDefaultNightMode",
)

internal object SetAppearanceLightStatusBarsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Z"),
    definingClass = "Landroidx/core/view/WindowInsetsControllerCompat;",
    name = "setAppearanceLightStatusBars",
)

internal object SetAppearanceLightNavigationBarsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Z"),
    definingClass = "Landroidx/core/view/WindowInsetsControllerCompat;",
    name = "setAppearanceLightNavigationBars",
)

internal object CommonTitleBarBackIconFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf(),
    definingClass = "Lcom/realme/iot/common/mvp/CommonTitleBarHelper;",
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/realme/iot/common/R\$mipmap;",
            name = "cancel",
            type = "I",
            opcode = Opcode.SGET,
        ),
    ),
)

internal object HomeHeaderTintFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    definingClass = "Lcom/realme/link/home/MainFragment;",
    filters = listOf(
        string(HOME_BACKGROUND_TINT_PREFERENCE),
        literal(OPAQUE_BLACK_ARGB),
    ),
)

internal object WebViewSettingsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(),
    name = "initData",
    filters = listOf(
        methodCall(definingClass = "Landroid/webkit/WebView;", name = "getSettings"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = InstructionLocation.MatchAfterImmediately()),
    ),
)

internal object DefaultHomeBackgroundColorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Integer;",
    parameters = listOf(),
    name = "invoke",
    filters = listOf(
        string(DEFAULT_HOME_BACKGROUND_COLOR),
        methodCall(definingClass = "Landroid/graphics/Color;", name = "parseColor"),
    ),
)
