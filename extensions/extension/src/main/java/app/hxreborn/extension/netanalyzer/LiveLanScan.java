/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

final class LiveLanScan {

    private static final String TAG = "hxNetAnalyzer";

    private static final Pattern IPV4 = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");

    private static final int IPV4_OCTETS = 4;

    private static final int MAX_OCTET = 255;

    private static final int OCTET_MASK = 0xFF;

    private static final int OCTET_BITS = 8;

    private static final String GET_VALUE = "getValue";

    private static final String SET_VALUE = "setValue";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static WeakReference<Object> scanItem = new WeakReference<>(null);

    private static WeakReference<Object> startCallback = new WeakReference<>(null);

    private static WeakReference<Object> stopCallback = new WeakReference<>(null);

    private static boolean running;

    private static Object pendingItem;

    private static Class<?> networkScanOwner;

    private static String networkScanMethod;

    private LiveLanScan() {
    }

    static void registerNetworkScanItem(Class<?> owner, String method) {
        networkScanOwner = owner;
        networkScanMethod = method;
    }

    static void observe(boolean isRunning, Object scanInfo, Object onStart, Object onStop) {
        running = isRunning;
        scanItem = new WeakReference<>(scanInfo);
        startCallback = new WeakReference<>(onStart);
        stopCallback = new WeakReference<>(onStop);
        if (!isRunning && pendingItem != null) {
            final Object item = pendingItem;
            pendingItem = null;
            MAIN.post(() -> start(item));
        }
    }

    static LanScan currentScan() {
        final Object item = scanItem.get();
        return (item != null) ? LanScan.read(item) : null;
    }

    static boolean isAvailable() {
        return scanItem.get() != null && startCallback.get() != null;
    }

    static InetAddress parseIpv4(String text) {
        final Matcher matcher = IPV4.matcher(text.trim());
        if (!matcher.matches()) {
            return null;
        }
        final byte[] octets = new byte[IPV4_OCTETS];
        for (int i = 0; i < IPV4_OCTETS; i++) {
            final int octet = Integer.parseInt(matcher.group(i + 1));
            if (octet > MAX_OCTET) {
                return null;
            }
            octets[i] = (byte) octet;
        }
        try {
            return InetAddress.getByAddress(octets);
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    static boolean isAscending(InetAddress start, InetAddress end) {
        return start != null && end != null && unsigned(start) < unsigned(end);
    }

    static boolean scan(String name, InetAddress start, InetAddress end) {
        final Object template = scanItem.get();
        if (template == null) {
            return false;
        }
        try {
            return scan(LanScan.customRangeItem(template, name, start, end));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot build the custom range scan item", ex);
            return false;
        }
    }

    static boolean scanNetwork() {
        if (networkScanOwner == null) {
            return false;
        }
        try {
            final Method networkItem = networkScanOwner.getDeclaredMethod(networkScanMethod);
            networkItem.setAccessible(true);
            final Object item = networkItem.invoke(null);
            return item != null && scan(item);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot build the network scan item", ex);
            return false;
        }
    }

    private static boolean scan(Object item) {
        if (startCallback.get() == null) {
            return false;
        }
        final Object onStop = stopCallback.get();
        if (running && onStop != null) {
            pendingItem = item;
            HostCallbacks.invoke(onStop);
        } else {
            start(item);
        }
        return true;
    }

    private static void start(Object item) {
        final Object onStart = startCallback.get();
        if (onStart == null) {
            return;
        }
        try {
            final Object state = scanState(onStart, item.getClass());
            method(state, SET_VALUE, 1).invoke(state, item);
            HostCallbacks.invoke(onStart);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot start the custom range scan", ex);
        }
    }

    private static Object scanState(Object onStart, Class<?> itemClass) throws ReflectiveOperationException {
        for (Field field : onStart.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            final Object state = field.get(onStart);
            if (state == null) {
                continue;
            }
            final Method getValue = method(state, GET_VALUE, 0);
            if (getValue != null && method(state, SET_VALUE, 1) != null
                    && itemClass.isInstance(getValue.invoke(state))) {
                return state;
            }
        }
        throw new NoSuchFieldException("No scan state in " + onStart.getClass());
    }

    private static Method method(Object owner, String name, int parameterCount) {
        for (Method method : owner.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static long unsigned(InetAddress address) {
        long value = 0;
        for (byte octet : address.getAddress()) {
            value = (value << OCTET_BITS) | (octet & OCTET_MASK);
        }
        return value;
    }

}
