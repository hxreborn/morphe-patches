/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.export

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patches.netanalyzer.shared.composableLambda
import app.morphe.patches.netanalyzer.shared.decryptedString
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val STRING_TYPE = "Ljava/lang/String;"
private const val BOOLEAN_TYPE = "Z"
private const val ARRAY_LIST_TYPE = "Ljava/util/ArrayList;"
private const val DOUBLE_TYPE = "D"
private const val DNS_TYPE_MNEMONIC = "NSAP-PTR"
private const val WIFI_HOTSPOT_TO_STRING = "WifiHotspot(protocolList="
private const val STATIC_INITIALIZER = "<clinit>"
private const val NET_INFO_TO_STRING = "NetInfo(type="
private const val WIFI_NET_INFO_TO_STRING = "WifiNetInfo(tethering="
private const val SIM_INFO_TO_STRING = "SimInfo(available="
private const val MULTI_SIM_INFO_TO_STRING = "MultiSimInfo(sim1="
private const val PROXY_SETTINGS_TO_STRING = "ProxySettings(proxyHost="
private const val IP_INFO_TO_STRING = "IpInfo(address="

internal object NetInfoToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = STRING_TYPE,
    parameters = listOf(),
    filters = listOf(decryptedString(NET_INFO_TO_STRING)),
)

internal object WifiNetInfoToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = STRING_TYPE,
    parameters = listOf(),
    filters = listOf(decryptedString(WIFI_NET_INFO_TO_STRING)),
)

internal object SimInfoToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = STRING_TYPE,
    parameters = listOf(),
    filters = listOf(decryptedString(SIM_INFO_TO_STRING)),
)

internal object MultiSimInfoToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = STRING_TYPE,
    parameters = listOf(),
    filters = listOf(decryptedString(MULTI_SIM_INFO_TO_STRING)),
)

internal object ProxySettingsToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = STRING_TYPE,
    parameters = listOf(),
    filters = listOf(decryptedString(PROXY_SETTINGS_TO_STRING)),
)

internal object IpInfoToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = STRING_TYPE,
    parameters = listOf(),
    filters = listOf(decryptedString(IP_INFO_TO_STRING)),
)

internal fun informationGetterFingerprint(returnType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = returnType,
    parameters = listOf(),
)

internal fun proxyGetterFingerprint(proxyType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = proxyType,
    parameters = listOf(BOOLEAN_TYPE),
)

internal fun externalIpHolderFingerprint(ipInfoType: String) = Fingerprint(
    name = STATIC_INITIALIZER,
    filters = listOf(
        newInstance(ipInfoType),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
        newInstance(ipInfoType),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
    ),
)

internal object PortScanStartFingerprint : Fingerprint(
    filters = listOf(
        methodCall(parameters = listOf(STRING_TYPE, ARRAY_LIST_TYPE, DOUBLE_TYPE, BOOLEAN_TYPE), returnType = "V"),
    ),
)

internal object DnsTypeMnemonicsFingerprint : Fingerprint(
    strings = listOf(DNS_TYPE_MNEMONIC),
)

internal object WifiHotspotToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = STRING_TYPE,
    parameters = listOf(),
    filters = listOf(decryptedString(WIFI_HOTSPOT_TO_STRING)),
)

internal fun hotspotConstructionFingerprint(hotspotType: String) = Fingerprint(
    filters = listOf(newInstance(hotspotType)),
)

private const val WHOIS_SCREEN_SOURCE = "ToolsWhoisScreen.kt"

internal object WhoisToolbarFingerprint : Fingerprint(
    filters = listOf(composableLambda(WHOIS_SCREEN_SOURCE)),
)
