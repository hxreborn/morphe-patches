/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class LanScan {

    private static final Class<?>[] LAYOUT = { boolean.class, String.class, String.class, String.class, String.class,
            InetAddress.class, InetAddress.class, Date.class, boolean.class, boolean.class, boolean.class, String.class,
            int.class, List.class, Map.class };

    private static final Class<?>[] CUSTOM_NAME_LAYOUT = { String.class, String.class, String.class };

    private static final int MAC = 1;

    private static final int NAME = 2;

    private static final int SSID = 3;

    private static final int BSSID = 4;

    private static final int IP_START = 5;

    private static final int IP_END = 6;

    private static final int DATE = 7;

    private static final int CUSTOM_RANGE = 8;

    private static final int FAVORITE = 9;

    private static final int DEVICES = 13;

    private static final int CUSTOM_NAMES = 14;

    private static final int CUSTOM_NAME_TEXT = 0;

    private static final int CUSTOM_NAME_MAC = 1;

    private static final int CUSTOM_NAME_VENDOR = 2;

    private static final String JSON_NAME = "name";

    private static final String JSON_SSID = "ssid";

    private static final String JSON_BSSID = "bssid";

    private static final String JSON_IP_START = "ip_start";

    private static final String JSON_IP_END = "ip_end";

    private static final String JSON_DATE = "date";

    private static final String JSON_CUSTOM_RANGE = "custom_range";

    private static final String JSON_DEVICES = "devices";

    final String name;

    final String ssid;

    final String bssid;

    final String ipStart;

    final String ipEnd;

    final long date;

    final boolean customRange;

    final List<LanRecord> devices;

    private final Map<?, ?> customNames;

    private LanScan(Object scanInfo, Field[] fields) {
        this.name = (String) FieldLayout.get(fields[NAME], scanInfo);
        this.ssid = (String) FieldLayout.get(fields[SSID], scanInfo);
        this.bssid = (String) FieldLayout.get(fields[BSSID], scanInfo);
        this.ipStart = host((InetAddress) FieldLayout.get(fields[IP_START], scanInfo));
        this.ipEnd = host((InetAddress) FieldLayout.get(fields[IP_END], scanInfo));
        final Date scanDate = (Date) FieldLayout.get(fields[DATE], scanInfo);
        this.date = (scanDate != null) ? scanDate.getTime() : System.currentTimeMillis();
        this.customRange = (Boolean) FieldLayout.get(fields[CUSTOM_RANGE], scanInfo);
        final List<LanRecord> records = new ArrayList<>();
        for (Object device : (List<?>) FieldLayout.get(fields[DEVICES], scanInfo)) {
            records.add(LanRecord.read(device));
        }
        this.devices = records;
        this.customNames = (Map<?, ?>) FieldLayout.get(fields[CUSTOM_NAMES], scanInfo);
    }

    private LanScan(JSONObject json) throws JSONException, UnknownHostException {
        this.name = json.optString(JSON_NAME);
        this.ssid = json.isNull(JSON_SSID) ? null : json.optString(JSON_SSID, null);
        this.bssid = json.isNull(JSON_BSSID) ? null : json.optString(JSON_BSSID, null);
        this.ipStart = json.optString(JSON_IP_START);
        this.ipEnd = json.optString(JSON_IP_END);
        this.date = json.optLong(JSON_DATE);
        this.customRange = json.optBoolean(JSON_CUSTOM_RANGE);
        final List<LanRecord> records = new ArrayList<>();
        final JSONArray devices = json.optJSONArray(JSON_DEVICES);
        for (int i = 0; devices != null && i < devices.length(); i++) {
            records.add(LanRecord.fromJson(devices.getJSONObject(i)));
        }
        this.devices = records;
        this.customNames = Collections.emptyMap();
    }

    static LanScan read(Object scanInfo) {
        return new LanScan(scanInfo, FieldLayout.instance(scanInfo.getClass(), LAYOUT));
    }

    static Object customRangeItem(Object template, String name, InetAddress start, InetAddress end)
            throws ReflectiveOperationException {
        final Field[] fields = FieldLayout.instance(template.getClass(), LAYOUT);
        final Object[] values = new Object[fields.length];
        final Class<?>[] types = new Class<?>[fields.length];
        for (int i = 0; i < fields.length; i++) {
            values[i] = FieldLayout.get(fields[i], template);
            types[i] = fields[i].getType();
        }
        values[MAC] = null;
        values[NAME] = name;
        values[SSID] = null;
        values[BSSID] = null;
        values[IP_START] = start;
        values[IP_END] = end;
        values[DATE] = new Date();
        values[CUSTOM_RANGE] = true;
        values[FAVORITE] = false;
        final Constructor<?> constructor = template.getClass().getDeclaredConstructor(types);
        constructor.setAccessible(true);
        return constructor.newInstance(values);
    }

    static String networkKeyOf(Object scanInfo) {
        final Field[] fields = FieldLayout.instance(scanInfo.getClass(), LAYOUT);
        final String bssid = (String) FieldLayout.get(fields[BSSID], scanInfo);
        if (bssid != null && !bssid.isEmpty()) {
            return bssid;
        }
        final String ssid = (String) FieldLayout.get(fields[SSID], scanInfo);
        return (ssid != null && !ssid.isEmpty()) ? ssid : (String) FieldLayout.get(fields[NAME], scanInfo);
    }

    static LanScan fromJson(JSONObject json) throws JSONException, UnknownHostException {
        return new LanScan(json);
    }

    JSONObject toJson() throws JSONException {
        final JSONArray records = new JSONArray();
        for (LanRecord device : this.devices) {
            records.put(device.toJson());
        }
        return new JSONObject().put(JSON_NAME, this.name)
            .put(JSON_SSID, this.ssid)
            .put(JSON_BSSID, this.bssid)
            .put(JSON_IP_START, this.ipStart)
            .put(JSON_IP_END, this.ipEnd)
            .put(JSON_DATE, this.date)
            .put(JSON_CUSTOM_RANGE, this.customRange)
            .put(JSON_DEVICES, records);
    }

    String networkKey() {
        if (this.bssid != null && !this.bssid.isEmpty()) {
            return this.bssid;
        }
        return (this.ssid != null && !this.ssid.isEmpty()) ? this.ssid : this.name;
    }

    String name(LanRecord record) {
        final String custom = custom(record, CUSTOM_NAME_TEXT);
        return (custom != null) ? custom : record.displayName;
    }

    String mac(LanRecord record) {
        if (record.mac != null) {
            return record.mac;
        }
        return custom(record, CUSTOM_NAME_MAC);
    }

    String vendor(Context context, LanRecord record) {
        if (record.vendor != null) {
            return record.vendor;
        }
        final String custom = custom(record, CUSTOM_NAME_VENDOR);
        if (custom != null) {
            return custom;
        }
        final String mac = mac(record);
        return (mac != null) ? OuiDatabase.vendor(context, mac) : null;
    }

    private String custom(LanRecord record, int index) {
        final Object entry = this.customNames.get(record.address);
        if (entry != null) {
            final String value = (String) FieldLayout
                .get(FieldLayout.instance(entry.getClass(), CUSTOM_NAME_LAYOUT)[index], entry);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        final String[] assigned = LanNames.get(networkKey(), record.ip());
        if (assigned == null) {
            return null;
        }
        final String value = assigned[index];
        return (value == null || value.isEmpty()) ? null : value;
    }

    private static String host(InetAddress address) {
        return (address != null) ? address.getHostAddress() : "";
    }

}
