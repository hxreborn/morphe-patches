/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.export

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.netanalyzer.shared.LAN_SCAN_MENU
import app.morphe.patches.netanalyzer.shared.TOOLS_PING_MENU
import app.morphe.patches.netanalyzer.shared.WIFI_PAGER_MENU
import app.morphe.patches.netanalyzer.shared.addExportMenu
import app.morphe.patches.netanalyzer.shared.afterTextButtonSlot
import app.morphe.patches.netanalyzer.shared.composableFingerprint
import app.morphe.patches.netanalyzer.shared.composableParameters
import app.morphe.patches.netanalyzer.shared.emptyGroupSlot
import app.morphe.patches.netanalyzer.shared.emptyGroupSlotOrNull
import app.morphe.patches.netanalyzer.shared.lanScanObserverPatch
import app.morphe.patches.netanalyzer.shared.propertyFields
import app.morphe.patches.netanalyzer.shared.hostToolsPatch
import app.morphe.patches.netanalyzer.shared.toolbarActionsPatch
import app.morphe.patches.netanalyzer.shared.registerAtLaunch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.toJavaClassName
import app.morphe.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val TOOLS_TRACEROUTE_MENU = "ToolsTracerouteMenu"
private const val TOOLS_PORTSCAN_MENU = "ToolsPortscanMenu"
private const val TOOLS_DNS_MENU = "ToolsDnsMenu"
private const val INFO_SCREEN_MENU = "InfoScreenMenu"
private const val TOOLS_WHOIS_SCREEN = "ToolsWhoisScreen"
private const val WHOIS_ITEM = "item"
private const val WHOIS_ITEM_ADDRESS = "address"
private const val WHOIS_ITEM_USE_RDAP = "useRdap"
private const val WHOIS_STATE_PAIRS = 3
private const val WHOIS_EXPORT_CLASS = "Lapp/hxreborn/extension/netanalyzer/WhoisExport;"
private const val PORT_SCAN_EXPORT_CLASS = "Lapp/hxreborn/extension/netanalyzer/PortScanExport;"
private const val DNS_EXPORT_CLASS = "Lapp/hxreborn/extension/netanalyzer/DnsExport;"
private const val WIFI_EXPORT_CLASS = "Lapp/hxreborn/extension/netanalyzer/WifiExport;"
private val DNS_MENU_PARAMETERS = listOf("response", "showAllSections", "hostname")
private val DNS_MENU_PARAMETER_TYPES = listOf("L", "Z", "Ljava/lang/String;")
private val DNS_RECORD_PROPERTIES = listOf("type", "name", "ttl", "rdata", "priority")
private const val ARRAY_PREFIX = "["
private const val LIST_SEPARATOR = ","
private const val REGISTRATION_SEPARATOR = ";"

@Suppress("unused")
val exportResultsPatch = bytecodePatch(
    name = "Export results",
    description = "Adds an export menu to Ping, Traceroute, Port Scan, DNS, Whois, LAN Scan, Wi-Fi Signal and " +
        "Information, with plain text and CSV output.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(toolbarActionsPatch, hostToolsPatch, lanScanObserverPatch)

    execute {
        addExportMenu(afterTextButtonSlot(TOOLS_PING_MENU), "ping")
        addExportMenu(afterTextButtonSlot(TOOLS_TRACEROUTE_MENU), "traceroute")
        addExportMenu(afterTextButtonSlot(TOOLS_PORTSCAN_MENU), "portscan")
        addExportMenu(afterTextButtonSlot(LAN_SCAN_MENU), "lan")
        addExportMenu(emptyGroupSlot(TOOLS_DNS_MENU), "dns")
        addExportMenu(emptyGroupSlot(WIFI_PAGER_MENU), "wifi")
        addExportMenu(emptyGroupSlot(INFO_SCREEN_MENU), "information")
        addExportMenu(whoisToolbarSlot(), "whois")
        recordPortScanStarts()
        observeDnsResponses()
        registerWifiScanHolders()
        registerInformationExport()
        observeWhoisResults()
    }
}

context(context: BytecodePatchContext)
private fun recordPortScanStarts() = with(context) {
    val starts = PortScanStartFingerprint.matchAll()
        .map { it.instructionMatches.single().instruction.getReference<MethodReference>()!! }
        .distinctBy { it.toString() }
    val start = starts.singleOrNull() ?: throw PatchException("The port scan start calls $starts, expected one method")
    val method = mutableClassDefBy(start.definingClass).findMutableMethodOf(start)
    val registers = method.getFreeRegisterProvider(0, 2)
    val toolRegister = registers.getFreeRegister4Bit()
    val hostRegister = registers.getFreeRegister4Bit()
    method.addInstructions(
        0,
        """
            const-class v$toolRegister, ${start.definingClass}
            move-object/from16 v$hostRegister, p0
            invoke-static { v$toolRegister, v$hostRegister }, $PORT_SCAN_EXPORT_CLASS->recordStart(Ljava/lang/Class;Ljava/lang/String;)V
        """,
    )
}

context(context: BytecodePatchContext)
private fun observeDnsResponses() = with(context) {
    val menu = composableFingerprint(TOOLS_DNS_MENU).matchSingle()
    val parameters = menu.composableParameters()
    val method = menu.method
    val types = method.parameterTypes.take(DNS_MENU_PARAMETERS.size).map { it.toString() }
    if (parameters.take(DNS_MENU_PARAMETERS.size) != DNS_MENU_PARAMETERS ||
        types.zip(DNS_MENU_PARAMETER_TYPES).any { (type, expected) -> !type.startsWith(expected) }
    ) {
        throw PatchException("$TOOLS_DNS_MENU takes $parameters $types, expected $DNS_MENU_PARAMETERS")
    }
    method.addInstructions(
        0,
        "invoke-static/range { p0 .. p2 }, $DNS_EXPORT_CLASS->observe(Ljava/lang/Object;ZLjava/lang/String;)V",
    )

    val responseType = types.first()
    val sectionFields = classDefBy(responseType).fields.filter { it.type.startsWith(ARRAY_PREFIX + "L") }
    val recordType = sectionFields.firstOrNull()?.type?.removePrefix(ARRAY_PREFIX)
        ?: throw PatchException("$responseType holds no record arrays")
    val recordFields = classDefBy(recordType).propertyFields()
    val recordRegistration = DNS_RECORD_PROPERTIES.joinToString(LIST_SEPARATOR) { property ->
        "$property=${recordFields[property] ?: throw PatchException("$recordType has no $property property")}"
    }
    val mnemonics = DnsTypeMnemonicsFingerprint.matchSingle().originalMethod.definingClass
    registerAtLaunch(
        "$DNS_EXPORT_CLASS->register(Ljava/lang/Class;Ljava/lang/String;)V",
        sectionFields.filter { it.type == ARRAY_PREFIX + recordType }.joinToString(LIST_SEPARATOR) { it.name } +
            REGISTRATION_SEPARATOR + recordRegistration,
        listOf(mnemonics),
    )
}

context(context: BytecodePatchContext)
private fun registerWifiScanHolders() {
    val hotspotType = WifiHotspotToStringFingerprint.matchSingle().originalMethod.definingClass
    val holders = hotspotConstructionFingerprint(hotspotType).matchAll()
        .map { it.originalMethod.definingClass }.distinct()
    registerAtLaunch(
        "$WIFI_EXPORT_CLASS->register(Ljava/lang/String;)V",
        hotspotType.toJavaClassName() + REGISTRATION_SEPARATOR + holders.joinToString(LIST_SEPARATOR) { it.toJavaClassName() },
    )
}

context(context: BytecodePatchContext)
private fun whoisToolbarSlot() = WhoisToolbarFingerprint.matchAll().firstNotNullOfOrNull(::emptyGroupSlotOrNull)
    ?: throw PatchException("No $TOOLS_WHOIS_SCREEN toolbar lambda has an empty composer group")

context(context: BytecodePatchContext)
private fun observeWhoisResults() = with(context) {
    val screen = composableFingerprint(TOOLS_WHOIS_SCREEN).matchSingle()
    val itemIndex = screen.composableParameters().indexOf(WHOIS_ITEM)
    if (itemIndex < 0) throw PatchException("$TOOLS_WHOIS_SCREEN takes no $WHOIS_ITEM parameter")
    val method = screen.method
    val itemType = method.parameterTypes[itemIndex].toString()
    val itemFields = classDefBy(itemType).propertyFields()
    val useRdap = itemFields[WHOIS_ITEM_USE_RDAP] ?: throw PatchException("$itemType has no $WHOIS_ITEM_USE_RDAP property")
    val address = itemFields[WHOIS_ITEM_ADDRESS] ?: throw PatchException("$itemType has no $WHOIS_ITEM_ADDRESS property")
    val instructions = method.instructions.toList()
    val useRdapRead = instructions.indexOfFirst {
        it.opcode == Opcode.IGET_BOOLEAN && it.getReference<FieldReference>()!!.name == useRdap
    }
    if (useRdapRead < 0) throw PatchException("$TOOLS_WHOIS_SCREEN never reads $WHOIS_ITEM_USE_RDAP")
    val states = instructions.drop(useRdapRead).filter { it.opcode == Opcode.SGET_OBJECT }
        .map { it.getReference<FieldReference>()!! }.filter { it.type != it.definingClass }.take(WHOIS_STATE_PAIRS * 2)
    val (rdapStates, whoisStates) = states.withIndex().partition { it.index % 2 == 0 }
    if (states.size != WHOIS_STATE_PAIRS * 2 || rdapStates.map { it.value.definingClass }.distinct().size != 1 ||
        whoisStates.map { it.value.definingClass }.distinct().size != 1
    ) {
        throw PatchException("$TOOLS_WHOIS_SCREEN selects $states, expected $WHOIS_STATE_PAIRS RDAP/whois state pairs")
    }
    val rdapResult = rdapStates.last().value
    val whoisResult = whoisStates.last().value
    val registration = listOf(
        whoisResult.definingClass.toJavaClassName(), whoisResult.name, rdapResult.definingClass.toJavaClassName(), rdapResult.name,
        address, useRdap,
    ).joinToString(REGISTRATION_SEPARATOR)
    val itemRegister = method.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
    val firstParameter = method.implementation!!.registerCount - MethodUtil.getParameterRegisterCount(method)
    method.addInstructions(
        0,
        """
            move-object/from16 v$itemRegister, v${firstParameter + itemIndex}
            invoke-static { v$itemRegister }, $WHOIS_EXPORT_CLASS->observe(Ljava/lang/Object;)V
        """,
    )
    registerAtLaunch("$WHOIS_EXPORT_CLASS->register(Ljava/lang/String;)V", registration)
}
