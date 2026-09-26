/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.terabox;

import android.util.Log;

import java.lang.reflect.Field;

@SuppressWarnings("unused")
public final class TeraboxServerMembership {

    private static final String TAG = "TeraboxServerMembership";
    private static final String NOT_A_MEMBER = "0";

    private TeraboxServerMembership() {
    }

    public static boolean isVip(Object vipInfo) {
        return vipInfo != null && readField(vipInfo, "isVip", Boolean.FALSE);
    }

    public static String vipType(Object vipInfo) {
        return isVip(vipInfo) ? String.valueOf(readField(vipInfo, "vipLevel", 0)) : NOT_A_MEMBER;
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String name, T fallback) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            Log.w(TAG, "Could not read " + name, exception);
            return fallback;
        }
    }
}
