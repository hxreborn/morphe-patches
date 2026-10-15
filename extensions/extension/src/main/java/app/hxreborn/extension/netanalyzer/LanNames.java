/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.Iterator;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

final class LanNames {

    static final int NAME = 0;

    static final int MAC = 1;

    static final int VENDOR = 2;

    private static final String TAG = "hxNetAnalyzer";

    private static final String PREFERENCES = "hxLanCustomNames";

    private static final String NAMES_KEY = "names";

    private static final String[] FIELDS = { "name", "mac", "vendor" };

    private static JSONObject names = new JSONObject();

    private static SharedPreferences preferences;

    private static volatile String currentNetwork;

    private LanNames() {
    }

    static synchronized void load(Context context) {
        if (preferences != null) {
            return;
        }
        preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        try {
            names = new JSONObject(preferences.getString(NAMES_KEY, "{}"));
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot read the custom LAN names", ex);
        }
    }

    static void setCurrentNetwork(String network) {
        currentNetwork = network;
    }

    static String currentNetwork() {
        return currentNetwork;
    }

    static synchronized String[] get(String network, String ip) {
        final JSONObject entry = (network != null) ? entry(network, ip) : null;
        if (entry == null) {
            return null;
        }
        final String[] values = new String[FIELDS.length];
        for (int i = 0; i < FIELDS.length; i++) {
            values[i] = entry.optString(FIELDS[i], "");
        }
        return values;
    }

    static synchronized void put(String network, String ip, String[] values) {
        if (preferences == null || network == null) {
            return;
        }
        try {
            JSONObject devices = names.optJSONObject(network);
            if (devices == null) {
                devices = new JSONObject();
                names.put(network, devices);
            }
            if (isBlank(values)) {
                devices.remove(ip);
            } else {
                final JSONObject entry = new JSONObject();
                for (int i = 0; i < FIELDS.length; i++) {
                    entry.put(FIELDS[i], values[i]);
                }
                devices.put(ip, entry);
            }
            removeEmptyNetworks();
            preferences.edit().putString(NAMES_KEY, names.toString()).apply();
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot save a custom LAN name", ex);
        }
    }

    private static JSONObject entry(String network, String ip) {
        final JSONObject devices = names.optJSONObject(network);
        return (devices != null) ? devices.optJSONObject(ip) : null;
    }

    private static void removeEmptyNetworks() {
        final Iterator<String> networks = names.keys();
        while (networks.hasNext()) {
            final JSONObject devices = names.optJSONObject(networks.next());
            if (devices == null || devices.length() == 0) {
                networks.remove();
            }
        }
    }

    private static boolean isBlank(String[] values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
