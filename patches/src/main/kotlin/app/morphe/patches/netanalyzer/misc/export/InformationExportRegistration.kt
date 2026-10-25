/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.export

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.netanalyzer.shared.propertyFields
import app.morphe.patches.netanalyzer.shared.registerAtLaunch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val INFORMATION_EXPORT_CLASS = "Lapp/hxreborn/extension/netanalyzer/InformationExport;"
private const val REGISTRATION_SEPARATOR = ";"
private const val LIST_SEPARATOR = ","
private const val CONNECTION_SLOT_FIELDS = 8
private const val FIRST_HOLDER_STORE = 1
private const val SECOND_HOLDER_STORE = 3
private val SIM_SLOTS = (1..4).map { "sim$it" }

private val CONNECTION_PROPERTIES = listOf(
    "name", "available", "address", "mask", "gateway", "dns", "addresses6", "gateway6", "dns6",
)
private val WIFI_PROPERTIES = listOf(
    "enabled", "supplicantState", "mac", "leaseTime", "ssid", "hidden", "bssid", "channel", "frequency",
    "wifiStandard", "linkSpeed", "rxLinkSpeed", "txLinkSpeed", "maxRxLinkSpeed", "maxTxLinkSpeed",
)
private val SIM_PROPERTIES = listOf(
    "available", "dataState", "dataActivity", "roaming", "simState", "simOperatorName", "simMccMnc",
    "simCountryIso", "operatorName", "mccMnc", "countryIso", "networkType", "phoneType",
)
private val PROXY_PROPERTIES = listOf("proxyHost", "proxyPort")

private fun ClassDef.fieldNames(properties: List<String>): String {
    val fields = propertyFields()
    return properties.joinToString(LIST_SEPARATOR) { property ->
        "$property=${fields[property] ?: throw PatchException("$type has no $property property")}"
    }
}

context(context: BytecodePatchContext)
private fun connectionsGetter(netState: String, connectionType: String): Method = with(context) {
    classDefBy(netState).methods.singleOrNull { method ->
        AccessFlags.STATIC.isSet(method.accessFlags) && method.parameterTypes.isEmpty() &&
            classDefByOrNull(method.returnType)?.instanceFields?.toList()?.let { fields ->
                fields.size == CONNECTION_SLOT_FIELDS && fields.all { it.type == connectionType }
            } == true
    } ?: throw PatchException("$netState has no getter for $CONNECTION_SLOT_FIELDS $connectionType slots")
}

context(context: BytecodePatchContext)
private fun Fingerprint.getter(): Method = with(context) { matchSingle().originalMethod }

context(context: BytecodePatchContext)
internal fun registerInformationExport() = with(context) {
    val connection = NetInfoToStringFingerprint.matchSingle().classDef
    val wifi = WifiNetInfoToStringFingerprint.matchSingle().classDef
    val sim = SimInfoToStringFingerprint.matchSingle().classDef
    val multiSim = MultiSimInfoToStringFingerprint.matchSingle().classDef
    val proxy = ProxySettingsToStringFingerprint.matchSingle().classDef
    val ipInfo = IpInfoToStringFingerprint.matchSingle().classDef

    val wifiGetter = informationGetterFingerprint(wifi.type).getter()
    val getters = listOf(
        connectionsGetter(wifiGetter.definingClass, connection.type),
        wifiGetter,
        informationGetterFingerprint(multiSim.type).getter(),
        proxyGetterFingerprint(proxy.type).getter(),
    )
    val netState = getters.map { it.definingClass }.distinct().singleOrNull()
        ?: throw PatchException("The network state getters are in ${getters.map { it.definingClass }.distinct()}, expected one class")

    val holder = externalIpHolderFingerprint(ipInfo.type).matchSingle()
    val holderStores = listOf(FIRST_HOLDER_STORE, SECOND_HOLDER_STORE).map { match ->
        holder.instructionMatches[match].instruction.getReference<FieldReference>()!!
    }
    if (holderStores.any { it.definingClass != holder.classDef.type }) {
        throw PatchException("${holder.classDef.type} stores its external IP holders in ${holderStores.map { it.definingClass }}, expected itself")
    }

    val registration = listOf(
        getters.joinToString(LIST_SEPARATOR) { it.name },
        holderStores.joinToString(LIST_SEPARATOR) { it.name },
        connection.fieldNames(CONNECTION_PROPERTIES),
        wifi.fieldNames(WIFI_PROPERTIES),
        sim.fieldNames(SIM_PROPERTIES),
        multiSim.fieldNames(SIM_SLOTS),
        proxy.fieldNames(PROXY_PROPERTIES),
    ).joinToString(REGISTRATION_SEPARATOR)

    registerAtLaunch(
        "$INFORMATION_EXPORT_CLASS->register(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)V",
        registration,
        listOf(netState, holder.classDef.type),
    )
}
