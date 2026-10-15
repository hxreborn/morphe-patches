/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

@SuppressWarnings("unused")
public final class InformationExport implements ExportSource {

    private static final String SUBJECT = "Information";

    private static final String REGISTRATION_SEPARATOR = ";";

    private static final String LIST_SEPARATOR = ",";

    private static final String NAME_SEPARATOR = "=";

    private static final int GETTER_CONNECTIONS = 0;

    private static final int GETTER_WIFI = 1;

    private static final int GETTER_SIM = 2;

    private static final int GETTER_PROXY = 3;

    private static final int PART_GETTERS = 0;

    private static final int PART_EXTERNAL_FIELDS = 1;

    private static final int PART_CONNECTION = 2;

    private static final int PART_WIFI = 3;

    private static final int PART_SIM = 4;

    private static final int PART_MULTI_SIM = 5;

    private static final int PART_PROXY = 6;

    private static final int REGISTRATION_PARTS = 7;

    private static final int ACTIVE_CONNECTION_FIELD = 7;

    private static final int[] CONNECTION_FIELD_ORDER = { 2, 3, 4, 0, 5, 1 };

    private static final int SIM_COUNT = 4;

    private static final int MCC_LENGTH = 3;

    private static final int MCC_MNC_MIN_LENGTH = 4;

    private static final int LAST_API_WITHOUT_WIFI_STANDARD = Build.VERSION_CODES.Q;

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int SECONDS_PER_HOUR = 3600;

    private static final int SECONDS_PER_DAY = 86400;

    private static final String NOT_AVAILABLE = "N/A";

    private static final String YES = "Yes";

    private static final String NO = "No";

    private static final String LIST_JOIN = ", ";

    private static final String EXTERNAL_IP = "External IP";

    private static final String EXTERNAL_IPV6 = "External IPv6";

    private static final String HOSTNAME_SUFFIX = " Hostname";

    private static final String AS_NUMBER_SUFFIX = " AS Number";

    private static final String AS_NAME_SUFFIX = " AS Name";

    private static final String STATE_VALUE_METHOD = "getValue";

    private static final String[] DATA_STATES = { "Disconnected", "Connecting", "Connected", "Suspended" };

    private static final String[] DATA_ACTIVITIES = { "None", "In", "Out", "InOut", "Dormant" };

    private static final String[] SIM_STATES = { "Unknown", "Absent", "PIN Required", "PUK Required", "Network Locked",
            "Ready" };

    private static final String[] NETWORK_TYPES = { "Unknown", "GPRS", "EDGE", "UMTS", "CDMA", "EVDO_0", "EVDO_A",
            "1xRTT", "HSDPA", "HSUPA", "HSPA", "IDEN", "EVDO_B", "LTE", "EHRPD", "HSPAP", "GSM", "TD_SCDMA", "IWLAN",
            "LTE_CA", "5G" };

    private static final String[] PHONE_TYPES = { "None", "GSM", "CDMA", "SIP" };

    private static final Map<String, String> SUPPLICANT_STATES = new HashMap<>();

    static {
        SUPPLICANT_STATES.put("ASSOCIATED", "Associated");
        SUPPLICANT_STATES.put("ASSOCIATING", "Associating");
        SUPPLICANT_STATES.put("AUTHENTICATING", "Authenticating");
        SUPPLICANT_STATES.put("COMPLETED", "Completed");
        SUPPLICANT_STATES.put("DISCONNECTED", "Disconnected");
        SUPPLICANT_STATES.put("DORMANT", "Dormant");
        SUPPLICANT_STATES.put("FOUR_WAY_HANDSHAKE", "Four Way Handshake");
        SUPPLICANT_STATES.put("GROUP_HANDSHAKE", "Group Handshake");
        SUPPLICANT_STATES.put("INACTIVE", "Inactive");
        SUPPLICANT_STATES.put("INTERFACE_DISABLED", "Disabled");
        SUPPLICANT_STATES.put("INVALID", "Invalid");
        SUPPLICANT_STATES.put("SCANNING", "Scanning");
        SUPPLICANT_STATES.put("UNINITIALIZED", "Uninitialized");
    }

    private static final Map<String, String> CONNECTION_FIELDS = new HashMap<>();

    private static final Map<String, String> WIFI_FIELDS = new HashMap<>();

    private static final Map<String, String> SIM_FIELDS = new HashMap<>();

    private static final Map<String, String> MULTI_SIM_FIELDS = new HashMap<>();

    private static final Map<String, String> PROXY_FIELDS = new HashMap<>();

    private static Class<?> netState;

    private static Class<?> externalState;

    private static String[] getters;

    private static String[] externalFields;

    InformationExport() {
    }

    public static void register(Class<?> netStateClass, Class<?> externalStateClass, String registration) {
        final String[] parts = registration.split(REGISTRATION_SEPARATOR, REGISTRATION_PARTS);
        netState = netStateClass;
        externalState = externalStateClass;
        getters = parts[PART_GETTERS].split(LIST_SEPARATOR);
        externalFields = parts[PART_EXTERNAL_FIELDS].split(LIST_SEPARATOR);
        parse(parts[PART_CONNECTION], CONNECTION_FIELDS);
        parse(parts[PART_WIFI], WIFI_FIELDS);
        parse(parts[PART_SIM], SIM_FIELDS);
        parse(parts[PART_MULTI_SIM], MULTI_SIM_FIELDS);
        parse(parts[PART_PROXY], PROXY_FIELDS);
    }

    @Override
    public String subject(Context context) {
        return SUBJECT;
    }

    @Override
    public String text(Context context) {
        if (netState == null) {
            throw new IllegalStateException("The information export is not registered");
        }
        try {
            final ExportOutline outline = new ExportOutline();
            final Object connections = invoke(getters[GETTER_CONNECTIONS]);
            final List<Field> slots = instanceFields(connections);
            outline.title("Active Connection");
            final Object active = slots.get(ACTIVE_CONNECTION_FIELD).get(connections);
            outline.line("Connection Type", string(active, CONNECTION_FIELDS, "name"));
            externalIp(outline, false);
            externalIp(outline, true);
            final Object proxy = invokeProxy(getters[GETTER_PROXY]);
            outline.line("HTTP Proxy", (proxy != null)
                    ? string(proxy, PROXY_FIELDS, "proxyHost") + " : " + value(proxy, PROXY_FIELDS, "proxyPort") : "");
            for (int slot : CONNECTION_FIELD_ORDER) {
                connection(outline, slots.get(slot).get(connections));
            }
            wifi(outline, context, invoke(getters[GETTER_WIFI]));
            sims(outline, context, invoke(getters[GETTER_SIM]));
            return outline.toString();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void externalIp(ExportOutline outline, boolean ipv6) throws ReflectiveOperationException {
        for (String name : externalFields) {
            final Field holderField = externalState.getDeclaredField(name);
            holderField.setAccessible(true);
            final Object holder = holderField.get(null);
            final ExternalIp ip = ExternalIp.read(holder);
            if (ip == null || ip.address == null || (ip.address instanceof Inet6Address) != ipv6) {
                continue;
            }
            final String label = ipv6 ? EXTERNAL_IPV6 : EXTERNAL_IP;
            outline.line(label, ip.address);
            outline.line(label + HOSTNAME_SUFFIX, ip.hostname);
            if (hasLocation(holder)) {
                outline.line(label + AS_NUMBER_SUFFIX, ip.asNumber);
                outline.line(label + AS_NAME_SUFFIX, ip.asName);
            }
            return;
        }
    }

    private void connection(ExportOutline outline, Object connection) throws ReflectiveOperationException {
        if (!bool(connection, CONNECTION_FIELDS, "available")) {
            return;
        }
        outline.title(string(connection, CONNECTION_FIELDS, "name") + " Connection");
        outline.line("IP Address", (InetAddress) value(connection, CONNECTION_FIELDS, "address"));
        outline.line("Subnet Mask", (InetAddress) value(connection, CONNECTION_FIELDS, "mask"));
        outline.line("Default Gateway IP", (InetAddress) value(connection, CONNECTION_FIELDS, "gateway"));
        outline.line("DNS Server IP", addresses(connection, "dns"));
        outline.line("IPv6 Addresses", addresses(connection, "addresses6"));
        outline.line("Default Gateway IPv6", (InetAddress) value(connection, CONNECTION_FIELDS, "gateway6"));
        outline.line("DNS Server IPv6", addresses(connection, "dns6"));
    }

    private void wifi(ExportOutline outline, Context context, Object wifi) throws ReflectiveOperationException {
        final int linkSpeed = integer(wifi, WIFI_FIELDS, "linkSpeed");
        final int receiveSpeed = integer(wifi, WIFI_FIELDS, "rxLinkSpeed");
        final int channel = integer(wifi, WIFI_FIELDS, "channel");
        final int leaseTime = integer(wifi, WIFI_FIELDS, "leaseTime");
        final String mac = string(wifi, WIFI_FIELDS, "mac");
        final String bssid = string(wifi, WIFI_FIELDS, "bssid");
        final String ssid = string(wifi, WIFI_FIELDS, "ssid");
        outline.title("Wi-Fi Details");
        outline.line("Enabled", bool(wifi, WIFI_FIELDS, "enabled") ? YES : NO);
        final Enum<?> supplicantState = (Enum<?>) value(wifi, WIFI_FIELDS, "supplicantState");
        outline.line("Connection State", SUPPLICANT_STATES.get(supplicantState.name()));
        if (mac != null) {
            outline.line("Device MAC", mac);
        }
        outline.line("DHCP Lease Time", (leaseTime > 0) ? duration(leaseTime) : NOT_AVAILABLE);
        outline.line("SSID", ((ssid == null || ssid.isEmpty()) ? NOT_AVAILABLE : ssid)
                + (bool(wifi, WIFI_FIELDS, "hidden") ? " (hidden)" : ""));
        outline.line("BSSID", bssid);
        outline.line("Vendor", (bssid != null) ? OuiDatabase.vendor(context, bssid) : null);
        outline.line("Channel",
                (channel == 0) ? NOT_AVAILABLE : channel + " (" + integer(wifi, WIFI_FIELDS, "frequency") + "MHz)");
        final boolean standardSupported = Build.VERSION.SDK_INT > LAST_API_WITHOUT_WIFI_STANDARD;
        if (standardSupported) {
            outline.line("Standard", string(wifi, WIFI_FIELDS, "wifiStandard"));
        }
        if (receiveSpeed > 0) {
            final int transmitSpeed = integer(wifi, WIFI_FIELDS, "txLinkSpeed");
            outline.line("Speed (Down / Up)", receiveSpeed + " / " + transmitSpeed + " Mbps");
        } else {
            outline.line("Speed", (linkSpeed > 0) ? linkSpeed + " Mbps" : NOT_AVAILABLE);
        }
        if (standardSupported) {
            final int maxReceive = integer(wifi, WIFI_FIELDS, "maxRxLinkSpeed");
            final int maxTransmit = integer(wifi, WIFI_FIELDS, "maxTxLinkSpeed");
            outline.line("Max. Speed (Down / Up)",
                    (maxReceive > 0 && maxTransmit > 0) ? maxReceive + " / " + maxTransmit + " Mbps" : NOT_AVAILABLE);
        }
    }

    private void sims(ExportOutline outline, Context context, Object multiSim) throws ReflectiveOperationException {
        final boolean phoneStateGranted = context
            .checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        for (int slot = 1; slot <= SIM_COUNT; slot++) {
            final String title = (slot == 1 && !phoneStateGranted) ? "Cell Details" : "SIM" + slot + " Details";
            sim(outline, value(multiSim, MULTI_SIM_FIELDS, "sim" + slot), title);
        }
    }

    private void sim(ExportOutline outline, Object sim, String title) throws ReflectiveOperationException {
        if (!bool(sim, SIM_FIELDS, "available")) {
            return;
        }
        outline.title(title);
        outline.line("Data State", lookup(DATA_STATES, integer(sim, SIM_FIELDS, "dataState")));
        outline.line("Data Activity", lookup(DATA_ACTIVITIES, integer(sim, SIM_FIELDS, "dataActivity")));
        outline.line("Roaming", bool(sim, SIM_FIELDS, "roaming") ? YES : NO);
        outline.line("SIM State", lookup(SIM_STATES, integer(sim, SIM_FIELDS, "simState")));
        outline.line("SIM Operator Name", string(sim, SIM_FIELDS, "simOperatorName"));
        outline.line("SIM MCC/MNC",
                operatorCode(string(sim, SIM_FIELDS, "simMccMnc"), string(sim, SIM_FIELDS, "simCountryIso")));
        outline.line("Operator Name", string(sim, SIM_FIELDS, "operatorName"));
        outline.line("MCC/MNC", operatorCode(string(sim, SIM_FIELDS, "mccMnc"), string(sim, SIM_FIELDS, "countryIso")));
        outline.line("Network Type", lookup(NETWORK_TYPES, integer(sim, SIM_FIELDS, "networkType")));
        outline.line("Phone Type", lookup(PHONE_TYPES, integer(sim, SIM_FIELDS, "phoneType")));
    }

    private static String operatorCode(String mccMnc, String countryIso) {
        final String code = (mccMnc == null || mccMnc.length() < MCC_MNC_MIN_LENGTH) ? NOT_AVAILABLE
                : mccMnc.substring(0, MCC_LENGTH) + "/" + mccMnc.substring(MCC_LENGTH);
        return (countryIso != null && !countryIso.isEmpty()) ? code + " (" + countryIso + ")" : code;
    }

    private static String lookup(String[] table, int index) {
        return (index >= 0 && index < table.length) ? table[index] : NOT_AVAILABLE;
    }

    private static String duration(int totalSeconds) {
        final int days = totalSeconds / SECONDS_PER_DAY;
        final int secondsOfDay = totalSeconds % SECONDS_PER_DAY;
        final int secondsOfHour = secondsOfDay % SECONDS_PER_HOUR;
        return ((days != 0) ? days + "d " : "") + (secondsOfDay / SECONDS_PER_HOUR) + "h "
                + (secondsOfHour / SECONDS_PER_MINUTE) + "m " + (secondsOfHour % SECONDS_PER_MINUTE) + "s";
    }

    private static String addresses(Object connection, String property) throws ReflectiveOperationException {
        final StringBuilder joined = new StringBuilder();
        for (Object address : (Iterable<?>) value(connection, CONNECTION_FIELDS, property)) {
            if (joined.length() > 0) {
                joined.append(LIST_JOIN);
            }
            joined.append(((InetAddress) address).getHostAddress());
        }
        return joined.toString();
    }

    private static boolean hasLocation(Object stateHolder) throws ReflectiveOperationException {
        final Object state = stateHolder.getClass().getMethod(STATE_VALUE_METHOD).invoke(stateHolder);
        if (state == null) {
            return false;
        }
        for (Field field : instanceFields(state)) {
            final Class<?> type = field.getType();
            if (!type.isPrimitive() && type != InetAddress.class && type != String.class && field.get(state) != null) {
                return true;
            }
        }
        return false;
    }

    private static Object invoke(String name) throws ReflectiveOperationException {
        final Method method = netState.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(null);
    }

    private static Object invokeProxy(String name) throws ReflectiveOperationException {
        final Method method = netState.getDeclaredMethod(name, boolean.class);
        method.setAccessible(true);
        return method.invoke(null, true);
    }

    private static List<Field> instanceFields(Object object) {
        final List<Field> fields = new ArrayList<>();
        for (Field field : object.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        Collections.sort(fields, (left, right) -> left.getName().compareTo(right.getName()));
        return fields;
    }

    private static Object value(Object owner, Map<String, String> names, String property)
            throws ReflectiveOperationException {
        final Field field = owner.getClass().getDeclaredField(names.get(property));
        field.setAccessible(true);
        return field.get(owner);
    }

    private static String string(Object owner, Map<String, String> names, String property)
            throws ReflectiveOperationException {
        return (String) value(owner, names, property);
    }

    private static int integer(Object owner, Map<String, String> names, String property)
            throws ReflectiveOperationException {
        return ((Number) value(owner, names, property)).intValue();
    }

    private static boolean bool(Object owner, Map<String, String> names, String property)
            throws ReflectiveOperationException {
        return (Boolean) value(owner, names, property);
    }

    private static void parse(String entries, Map<String, String> target) {
        target.clear();
        for (String entry : entries.split(LIST_SEPARATOR)) {
            final String[] names = entry.split(NAME_SEPARATOR, 2);
            target.put(names[0], names[1]);
        }
    }

}
