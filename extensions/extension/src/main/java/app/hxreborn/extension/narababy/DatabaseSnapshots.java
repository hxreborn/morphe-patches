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
package app.hxreborn.extension.narababy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

@SuppressWarnings("unused")
public final class DatabaseSnapshots {

    private static final String TAG = "NaraDatabase";

    private static final String BRIDGE_PACKAGE = "com.facebook.react.bridge.";

    private static final String ENTITLEMENTS_KEY = "entitlementz";

    private static final String VERSION_BLOCK_KEY = "versionBlock";

    private static final String ENTITLEMENT_ID = "Nara Baby & Pregnancy Tracker Pro";

    private static final String PRODUCT_ID = "narababy_default_lifetime";

    private static final long STARTED_AT = 1577836800000L;

    private DatabaseSnapshots() {

    }

    public static void addLifetimeEntitlement(Object snapshotMap) {
        try {
            if (!isSnapshotOf(snapshotMap, ENTITLEMENTS_KEY)) {
                return;
            }
            ClassLoader loader = snapshotMap.getClass().getClassLoader();
            Map<?, ?> snapshot = (Map<?, ?>) bridgeClass(loader, "ReadableMap").getMethod("toHashMap")
                .invoke(snapshotMap);
            Map<String, Object> entitlements = new HashMap<>();
            Object stored = snapshot.get("value");
            if (stored instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) stored).entrySet()) {
                    entitlements.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            entitlements.put(ENTITLEMENT_ID, lifetimeEntry(currentUserId(loader)));
            replaceValue(snapshotMap, entitlements);
        } catch (Throwable ex) {
            Log.e(TAG, "Could not add the lifetime entitlement", ex);
        }
    }

    public static void removeVersionBlock(Object snapshotMap) {
        try {
            if (isSnapshotOf(snapshotMap, VERSION_BLOCK_KEY)) {
                replaceValue(snapshotMap, null);
            }
        } catch (Throwable ex) {
            Log.e(TAG, "Could not remove the version block", ex);
        }
    }

    private static boolean isSnapshotOf(Object snapshotMap, String key) throws ReflectiveOperationException {
        Class<?> readableMap = bridgeClass(snapshotMap.getClass().getClassLoader(), "ReadableMap");
        return key.equals(readableMap.getMethod("getString", String.class).invoke(snapshotMap, "key"));
    }

    private static void replaceValue(Object snapshotMap, Map<String, Object> value)
            throws ReflectiveOperationException {
        ClassLoader loader = snapshotMap.getClass().getClassLoader();
        Class<?> arguments = bridgeClass(loader, "Arguments");
        Class<?> writableMap = bridgeClass(loader, "WritableMap");
        Map<String, Object> children = (value != null) ? value : new HashMap<String, Object>();
        Object childKeys = arguments.getMethod("makeNativeArray", List.class)
            .invoke(null, new ArrayList<Object>(children.keySet()));

        writableMap.getMethod("putBoolean", String.class, boolean.class).invoke(snapshotMap, "exists", value != null);
        writableMap.getMethod("putBoolean", String.class, boolean.class)
            .invoke(snapshotMap, "hasChildren", !children.isEmpty());
        writableMap.getMethod("putDouble", String.class, double.class)
            .invoke(snapshotMap, "childrenCount", (double) children.size());
        writableMap.getMethod("putArray", String.class, bridgeClass(loader, "ReadableArray"))
            .invoke(snapshotMap, "childKeys", childKeys);
        if (value != null) {
            writableMap.getMethod("putMap", String.class, bridgeClass(loader, "ReadableMap"))
                .invoke(snapshotMap, "value", arguments.getMethod("makeNativeMap", Map.class).invoke(null, value));
        } else {
            writableMap.getMethod("putNull", String.class).invoke(snapshotMap, "value");
        }
    }

    private static Map<String, Object> lifetimeEntry(String userId) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("entitlementId", ENTITLEMENT_ID);
        entry.put("productId", PRODUCT_ID);
        entry.put("startedAt", STARTED_AT);
        if (userId != null) {
            Map<String, Object> selfTrackKeys = new HashMap<>();
            selfTrackKeys.put(userId, true);
            entry.put("subscriberUserKey", userId);
            entry.put("selfTrackKeyz", selfTrackKeys);
        }
        return entry;
    }

    private static String currentUserId(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> firebaseAuth = Class.forName("com.google.firebase.auth.FirebaseAuth", false, loader);
        Object auth = firebaseAuth.getMethod("getInstance").invoke(null);
        Object user = firebaseAuth.getMethod("getCurrentUser").invoke(auth);
        Class<?> firebaseUser = Class.forName("com.google.firebase.auth.FirebaseUser", false, loader);
        return (user != null) ? (String) firebaseUser.getMethod("getUid").invoke(user) : null;
    }

    private static Class<?> bridgeClass(ClassLoader loader, String name) throws ClassNotFoundException {
        return Class.forName(BRIDGE_PACKAGE + name, false, loader);
    }

}
