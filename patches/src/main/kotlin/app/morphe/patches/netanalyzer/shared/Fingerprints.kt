/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val HEADER_ROW = "Header"
internal const val CLICKABLE_ROW = "ClickableRow"
internal const val PING_RESULT_ROW = "PingResultRow"
internal const val PING_RESULT_ROW_PROBE = "probe"

internal object HeaderRowFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable(HEADER_ROW)),
)

internal object ClickableRowFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable(CLICKABLE_ROW)),
)

internal object TamperDialogFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable("Dialog", "TamperDetector.kt")),
)

internal object MainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lnet/techet/netanalyzerlite/an/activity/MainActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

private const val STRING_TYPE = "Ljava/lang/String;"
private const val INT_TYPE = "I"
private const val BOOLEAN_TYPE = "Z"
private const val OBJECT_TYPE_PREFIX = "L"

internal val PING_START_PARAMETERS = listOf(
    STRING_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE, OBJECT_TYPE_PREFIX, INT_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE,
)
internal val TRACEROUTE_START_PARAMETERS = listOf(STRING_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE)

internal object ToolStartFingerprint : Fingerprint(
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = TRACEROUTE_START_PARAMETERS, returnType = "V"),
        methodCall(opcode = Opcode.INVOKE_STATIC_RANGE, parameters = PING_START_PARAMETERS, returnType = "V"),
    ),
)

internal object PingResultRowFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable(PING_RESULT_ROW)),
)

private const val MAX_PAYLOAD_BYTES = 1500L
private const val DEFAULT_TCP_PING_PORT = 443L

internal object ToolItemNormaliserFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        literal(MAX_PAYLOAD_BYTES),
        literal(DEFAULT_TCP_PING_PORT),
        opcode(Opcode.RETURN_OBJECT),
    ),
    custom = { method, _ ->
        method.parameterTypes.size == 1 && method.returnType == method.parameterTypes.first().toString()
    },
)

internal const val LAN_SCAN_MENU = "LanScanMenu"

internal object LanScanMenuFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable(LAN_SCAN_MENU)),
)

private const val NETWORK_SCAN_NAME_SUFFIX = " Network"

internal fun networkScanItemFingerprint(scanItemType: String) = Fingerprint(
    returnType = scanItemType,
    parameters = listOf(),
    filters = listOf(decryptedString(NETWORK_SCAN_NAME_SUFFIX)),
)

private const val MASK_TYPE = "I"

internal object ClickableModifierFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(OBJECT_TYPE_PREFIX, BOOLEAN_TYPE, STRING_TYPE, OBJECT_TYPE_PREFIX, MASK_TYPE),
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE),
        opcode(Opcode.INVOKE_DIRECT_RANGE),
        opcode(Opcode.INVOKE_INTERFACE),
    ),
    custom = { method, _ -> method.returnType == method.parameterTypes.first().toString() },
)

internal const val HEADER_WITH_BUTTON = "HeaderWithButton"
internal const val TOOLS_PING_MENU = "ToolsPingMenu"
internal const val WIFI_PAGER_MENU = "WifiPagerMenu"
private const val FLOAT_TYPE = "F"

internal fun composableFingerprint(composableName: String) = Fingerprint(
    returnType = "V",
    filters = listOf(composable(composableName)),
)

internal object HeaderWithButtonPaddingFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        composable(HEADER_WITH_BUTTON),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(OBJECT_TYPE_PREFIX, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE),
        ),
    ),
)

private const val CUSTOM_COLORS_PALETTE_TO_STRING = "CustomColorsPalette(raterBgColor="

internal object CustomColorsPaletteToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = STRING_TYPE,
    parameters = listOf(),
    filters = listOf(decryptedString(CUSTOM_COLORS_PALETTE_TO_STRING)),
)
