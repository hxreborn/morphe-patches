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
package hx.gateway;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import hx.platform.InstalledApp;
import hx.util.Logs;
import org.json.JSONException;
import org.json.JSONObject;

final class ClientIdentity {

    private static final String PREFERENCES = "hx_client_identity";

    private static final String DEVICE_ID = "device_id";

    private static final String[] REQUIRED_FIELDS = { "package_name", "version_code", DEVICE_ID, "gaid" };

    private static final int LOGGED_ID_LENGTH = 8;

    private final JSONObject fallback;

    private final SharedPreferences preferences;

    private final Set<String> reportedFields = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private volatile String deviceId;

    ClientIdentity(String fallback, SharedPreferences preferences) {
        try {
            this.fallback = new JSONObject(fallback);
        } catch (JSONException ex) {
            throw new IllegalArgumentException(ex);
        }
        this.preferences = preferences;
        this.deviceId = (preferences != null) ? preferences.getString(DEVICE_ID, null) : null;
    }

    static ClientIdentity forInstalledApp() {
        return new ClientIdentity(ClientInfo.forInstalledApp(), appPreferences());
    }

    private static SharedPreferences appPreferences() {
        try {
            return InstalledApp.context().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        } catch (ReflectiveOperationException ex) {
            Log.w(Logs.TAG, "app preferences unavailable, device id kept for this process only", ex);
            return null;
        }
    }

    String deviceId() {
        return this.deviceId;
    }

    synchronized void replaceDeviceId(String rejected) {
        if (!Objects.equals(rejected, this.deviceId)) {
            return;
        }
        this.deviceId = UUID.randomUUID().toString().replace("-", "");
        if (this.preferences != null) {
            this.preferences.edit().putString(DEVICE_ID, this.deviceId).apply();
        }
        Log.w(Logs.TAG,
                "upgrade notice for " + ((rejected != null) ? "device id " + shown(rejected) : "the app's device id")
                        + ", now sending " + shown(this.deviceId));
    }

    String rewrite(String clientInfo) {
        JSONObject info;
        try {
            info = new JSONObject(clientInfo);
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "client info not rewritten: " + ex.getMessage());
            return clientInfo;
        }
        boolean changed = false;
        String deviceId = this.deviceId;
        if (deviceId != null && !deviceId.equals(info.optString(DEVICE_ID))) {
            put(info, DEVICE_ID, deviceId);
            changed = true;
        }
        for (String field : REQUIRED_FIELDS) {
            if (info.optString(field).isEmpty() && this.fallback.has(field)) {
                put(info, field, this.fallback.opt(field));
                reportMissing(field);
                changed = true;
            }
        }
        return changed ? info.toString() : clientInfo;
    }

    private static String shown(String deviceId) {
        return deviceId.substring(0, Math.min(deviceId.length(), LOGGED_ID_LENGTH));
    }

    private static void put(JSONObject info, String field, Object value) {
        try {
            info.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void reportMissing(String field) {
        if (this.reportedFields.add(field)) {
            Log.w(Logs.TAG, "client info had no " + field + ", sent the hook's own");
        }
    }

}
