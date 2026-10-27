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
package app.hxreborn.extension.castle;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Locale;

@SuppressWarnings("unused")
public final class DeviceGuid {

    private static final String MMKV_CLASS = "com.tencent.mmkv.MMKV";

    private static final String STORE = "PhoneDataConfig";

    private static final String KEY = "hx_service_oauth_device_id";

    private static final int MULTI_PROCESS_MODE = 2;

    private static final int GUID_BYTES = 18;

    private static final String BLACKLISTED = "\"inBlacklist\":true";

    private static final ThreadLocal<Boolean> RETRY_PENDING = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> RETRYING = new ThreadLocal<>();

    private static volatile String guid;

    private DeviceGuid() {

    }

    public static String switchedGuid() {
        if (guid == null) {
            guid = read();
        }
        return guid;
    }

    public static void observeResponse(String json) {
        if (Boolean.TRUE.equals(RETRYING.get()) || json == null || !json.contains(BLACKLISTED)) {
            return;
        }
        guid = newGuid();
        write(guid);
        RETRY_PENDING.set(Boolean.TRUE);
    }

    public static boolean shouldRetry() {
        boolean retry = Boolean.TRUE.equals(RETRY_PENDING.get());
        RETRY_PENDING.set(Boolean.FALSE);
        RETRYING.set(retry);
        return retry;
    }

    static String newGuid() {
        byte[] bytes = new byte[GUID_BYTES];
        new SecureRandom().nextBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte value : bytes) {
            hex.append(String.format(Locale.ROOT, "%02x", value));
        }
        return hex.toString();
    }

    private static String read() {
        try {
            Object store = store();
            Method decode = store.getClass().getMethod("decodeString", String.class);
            return (String) decode.invoke(store, KEY);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static void write(String value) {
        try {
            Object store = store();
            Method encode = store.getClass().getMethod("encode", String.class, String.class);
            encode.invoke(store, KEY, value);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return;
        }
    }

    private static Object store() throws ReflectiveOperationException {
        Method open = Class.forName(MMKV_CLASS).getMethod("mmkvWithID", String.class, int.class);
        return open.invoke(null, STORE, MULTI_PROCESS_MODE);
    }

}
