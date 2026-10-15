/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;

import android.content.Context;
import android.util.Log;

@SuppressWarnings("unused")
public final class WhoisExport implements ExportSource {

    private static final String TAG = "hxNetAnalyzer";

    private static final String REGISTRATION_SEPARATOR = ";";

    private static final int WHOIS_HOLDER = 0;

    private static final int WHOIS_RESULT = 1;

    private static final int RDAP_HOLDER = 2;

    private static final int RDAP_RESULT = 3;

    private static final int ITEM_ADDRESS = 4;

    private static final int ITEM_USE_RDAP = 5;

    private static String[] registration;

    private static Object query;

    public static void register(String fields) {
        registration = fields.split(REGISTRATION_SEPARATOR);
    }

    public static void observe(Object toolsQuery) {
        query = toolsQuery;
    }

    @Override
    public String subject(Context context) {
        return "Whois '" + itemValue(ITEM_ADDRESS) + "'";
    }

    @Override
    public String text(Context context) {
        if (registration == null || query == null) {
            return null;
        }
        final boolean rdap = Boolean.TRUE.equals(itemValue(ITEM_USE_RDAP));
        try {
            final Object result = StaticStates
                .value(staticField(rdap ? RDAP_HOLDER : WHOIS_HOLDER, rdap ? RDAP_RESULT : WHOIS_RESULT));
            return (result instanceof String) ? (String) result : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot read the whois result", ex);
            return null;
        }
    }

    private static Object staticField(int holder, int field) throws ReflectiveOperationException {
        final Field resultState = Class.forName(registration[holder], false, WhoisExport.class.getClassLoader())
            .getDeclaredField(registration[field]);
        resultState.setAccessible(true);
        return resultState.get(null);
    }

    private static Object itemValue(int field) {
        final Object current = query;
        if (registration == null || current == null) {
            return null;
        }
        try {
            final Field value = current.getClass().getDeclaredField(registration[field]);
            value.setAccessible(true);
            return value.get(current);
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "Cannot read the whois query", ex);
            return null;
        }
    }

}
