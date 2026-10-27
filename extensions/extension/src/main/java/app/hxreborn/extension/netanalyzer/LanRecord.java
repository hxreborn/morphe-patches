/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class LanRecord {

    private static final String TO_STRING_METHOD = "toString";

    private static final Class<?>[] LAYOUT = { InetAddress.class, String.class, String.class, List.class, Object.class,
            String.class, String.class, String.class, String.class, String.class, boolean.class, boolean.class,
            boolean.class, boolean.class, boolean.class, String.class };

    private static final Class<?>[] NETBIOS_LAYOUT = { String.class, String.class, String.class };

    private static final int ADDRESS = 0;

    private static final int MAC = 1;

    private static final int VENDOR = 2;

    private static final int ADDRESSES6 = 3;

    private static final int NETBIOS = 4;

    private static final int DNS_NAME = 5;

    private static final int MDNS_NAME = 6;

    private static final int LLMNR_NAME = 7;

    private static final int SNMP_NAME = 8;

    private static final int PINGABLE = 10;

    private static final int UPNP = 11;

    private static final int BONJOUR = 12;

    private static final int SCANNING = 13;

    private static final int GATEWAY = 14;

    private static final int WEB_PROTOCOL = 15;

    private static final String JSON_ADDRESS = "address";

    private static final String JSON_MAC = "mac";

    private static final String JSON_VENDOR = "vendor";

    private static final String JSON_ADDRESSES6 = "addresses6";

    private static final String JSON_DNS_NAME = "dns_name";

    private static final String JSON_MDNS_NAME = "mdns_name";

    private static final String JSON_LLMNR_NAME = "llmnr_name";

    private static final String JSON_SNMP_NAME = "snmp_name";

    private static final String JSON_NETBIOS_DOMAIN = "netbios_domain";

    private static final String JSON_NETBIOS_NAME = "netbios_name";

    private static final String JSON_PINGABLE = "pingable";

    private static final String JSON_UPNP = "upnp";

    private static final String JSON_BONJOUR = "bonjour";

    private static final String JSON_SCANNING = "scanning";

    private static final String JSON_GATEWAY = "gateway";

    private static final String JSON_WEB_PROTOCOL = "web_protocol";

    private static final String JSON_DISPLAY_NAME = "display_name";

    private static final int NETBIOS_DOMAIN = 0;

    private static final int NETBIOS_COMPUTER = 1;

    private static volatile Field addressField;

    final InetAddress address;

    final String mac;

    final String vendor;

    final List<String> addresses6;

    final String dnsName;

    final String mdnsName;

    final String llmnrName;

    final String snmpName;

    final String netbiosDomain;

    final String netbiosName;

    final boolean pingable;

    final boolean upnp;

    final boolean bonjour;

    final boolean scanning;

    final boolean gateway;

    final String webProtocol;

    final String displayName;

    private LanRecord(Object record, Field[] fields) {
        this.address = (InetAddress) FieldLayout.get(fields[ADDRESS], record);
        this.mac = string(fields[MAC], record);
        this.vendor = string(fields[VENDOR], record);
        this.addresses6 = hostAddresses((List<?>) FieldLayout.get(fields[ADDRESSES6], record));
        this.dnsName = string(fields[DNS_NAME], record);
        this.mdnsName = string(fields[MDNS_NAME], record);
        this.llmnrName = string(fields[LLMNR_NAME], record);
        this.snmpName = string(fields[SNMP_NAME], record);
        final Object netbios = FieldLayout.get(fields[NETBIOS], record);
        if (netbios != null) {
            final Field[] netbiosFields = FieldLayout.instance(netbios.getClass(), NETBIOS_LAYOUT);
            this.netbiosDomain = string(netbiosFields[NETBIOS_DOMAIN], netbios);
            this.netbiosName = string(netbiosFields[NETBIOS_COMPUTER], netbios);
        } else {
            this.netbiosDomain = null;
            this.netbiosName = null;
        }
        this.pingable = flag(fields[PINGABLE], record);
        this.upnp = flag(fields[UPNP], record);
        this.bonjour = flag(fields[BONJOUR], record);
        this.scanning = flag(fields[SCANNING], record);
        this.gateway = flag(fields[GATEWAY], record);
        this.webProtocol = string(fields[WEB_PROTOCOL], record);
        this.displayName = displayName(record);
    }

    private LanRecord(JSONObject json) throws JSONException, UnknownHostException {
        this.address = InetAddress.getByName(json.getString(JSON_ADDRESS));
        this.mac = optString(json, JSON_MAC);
        this.vendor = optString(json, JSON_VENDOR);
        final JSONArray addresses = json.optJSONArray(JSON_ADDRESSES6);
        final List<String> hosts = new ArrayList<>();
        for (int i = 0; addresses != null && i < addresses.length(); i++) {
            hosts.add(addresses.getString(i));
        }
        this.addresses6 = hosts;
        this.dnsName = optString(json, JSON_DNS_NAME);
        this.mdnsName = optString(json, JSON_MDNS_NAME);
        this.llmnrName = optString(json, JSON_LLMNR_NAME);
        this.snmpName = optString(json, JSON_SNMP_NAME);
        this.netbiosDomain = optString(json, JSON_NETBIOS_DOMAIN);
        this.netbiosName = optString(json, JSON_NETBIOS_NAME);
        this.pingable = json.optBoolean(JSON_PINGABLE);
        this.upnp = json.optBoolean(JSON_UPNP);
        this.bonjour = json.optBoolean(JSON_BONJOUR);
        this.scanning = json.optBoolean(JSON_SCANNING);
        this.gateway = json.optBoolean(JSON_GATEWAY);
        this.webProtocol = optString(json, JSON_WEB_PROTOCOL);
        this.displayName = optString(json, JSON_DISPLAY_NAME);
    }

    static LanRecord fromJson(JSONObject json) throws JSONException, UnknownHostException {
        return new LanRecord(json);
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject().put(JSON_ADDRESS, ip())
            .put(JSON_MAC, this.mac)
            .put(JSON_VENDOR, this.vendor)
            .put(JSON_ADDRESSES6, new JSONArray(this.addresses6))
            .put(JSON_DNS_NAME, this.dnsName)
            .put(JSON_MDNS_NAME, this.mdnsName)
            .put(JSON_LLMNR_NAME, this.llmnrName)
            .put(JSON_SNMP_NAME, this.snmpName)
            .put(JSON_NETBIOS_DOMAIN, this.netbiosDomain)
            .put(JSON_NETBIOS_NAME, this.netbiosName)
            .put(JSON_PINGABLE, this.pingable)
            .put(JSON_UPNP, this.upnp)
            .put(JSON_BONJOUR, this.bonjour)
            .put(JSON_SCANNING, this.scanning)
            .put(JSON_GATEWAY, this.gateway)
            .put(JSON_WEB_PROTOCOL, this.webProtocol)
            .put(JSON_DISPLAY_NAME, this.displayName);
    }

    static InetAddress addressOf(Object record) {
        Field field = addressField;
        if (field == null || field.getDeclaringClass() != record.getClass()) {
            field = FieldLayout.instance(record.getClass(), LAYOUT)[ADDRESS];
            addressField = field;
        }
        return (InetAddress) FieldLayout.get(field, record);
    }

    static LanRecord read(Object record) {
        return new LanRecord(record, FieldLayout.instance(record.getClass(), LAYOUT));
    }

    String ip() {
        return this.address.getHostAddress();
    }

    String webUrl() {
        return (this.webProtocol != null) ? this.webProtocol + "://" + ip() : null;
    }

    private static String displayName(Object record) {
        Method found = null;
        for (Method method : record.getClass().getDeclaredMethods()) {
            if (method.getParameterTypes().length == 0 && method.getReturnType() == String.class
                    && !Modifier.isStatic(method.getModifiers()) && !TO_STRING_METHOD.equals(method.getName())) {
                if (found != null) {
                    throw new IllegalStateException("Ambiguous display name method on " + record.getClass());
                }
                found = method;
            }
        }
        if (found == null) {
            throw new IllegalStateException("No display name method on " + record.getClass());
        }
        try {
            return (String) found.invoke(record);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static List<String> hostAddresses(List<?> addresses) {
        final List<String> hosts = new ArrayList<>();
        for (Object address : addresses) {
            hosts.add(((InetAddress) address).getHostAddress());
        }
        return hosts;
    }

    private static String optString(JSONObject json, String key) {
        return json.isNull(key) ? null : json.optString(key, null);
    }

    private static String string(Field field, Object owner) {
        return (String) FieldLayout.get(field, owner);
    }

    private static boolean flag(Field field, Object owner) {
        return (Boolean) FieldLayout.get(field, owner);
    }

}
