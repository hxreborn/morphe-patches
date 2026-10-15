/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

@SuppressWarnings("unused")
public final class Features {

    private static final String TAG = "hxNetAnalyzer";

    private static final String DETAILS_LABEL = "Details";

    private static final Map<String, Object> TOOLBAR_ACTIONS = new HashMap<>();

    private static Object menuClick;

    private Features() {
    }

    public static void attach(Activity activity) {
        HostActivity.attach(activity);
    }

    public static void observeLanScan(boolean running, Object scanInfo, Object onStart, Object onStop) {
        try {
            LiveLanScan.observe(running, scanInfo, onStart, onStop);
            LanNames.setCurrentNetwork(LanScan.networkKeyOf(scanInfo));
            final Activity activity = HostActivity.current();
            if (activity != null) {
                LanHistory.observe(activity, running, scanInfo);
            }
        } catch (RuntimeException ex) {
            Log.e(TAG, "Cannot observe the LAN scan", ex);
        }
    }

    public static void registerNetworkScanItem(Class<?> owner, String method) {
        LiveLanScan.registerNetworkScanItem(owner, method);
    }

    public static String lanDeviceName(Object record) {
        final String network = LanNames.currentNetwork();
        if (network == null) {
            return null;
        }
        try {
            final String[] assigned = LanNames.get(network, LanRecord.addressOf(record).getHostAddress());
            return (assigned != null && !assigned[LanNames.NAME].isEmpty()) ? assigned[LanNames.NAME] : null;
        } catch (RuntimeException ex) {
            Log.e(TAG, "Cannot read a custom LAN name", ex);
            return null;
        }
    }

    public static void registerDeviceNames(String registration) {
        try {
            DeviceNames.register(registration);
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "Cannot register the device name database", ex);
        }
    }

    public static void enableLanScanHistory() {
        LanHistory.enable();
    }

    public static synchronized Object onToolbarAction(Class<?> onClickType, String activityClass) {
        Object proxy = TOOLBAR_ACTIONS.get(activityClass);
        if (proxy == null) {
            proxy = HostCallbacks.create(onClickType, () -> HostActivity.open(activityClass, null));
            TOOLBAR_ACTIONS.put(activityClass, proxy);
        }
        return proxy;
    }

    public static synchronized Object onMenuClick(Class<?> onClickType, String entries) {
        if (menuClick == null) {
            menuClick = HostCallbacks.create(onClickType, () -> HostActivity.open(PatchesMenuActivity.class.getName(),
                    new Intent().putExtra(PatchesMenuActivity.EXTRA_ENTRIES, entries)));
        }
        return menuClick;
    }

    public static Object onHotspotClick(Class<?> onClickType, Object row) {
        return HostCallbacks.create(onClickType, () -> {
            try {
                WifiDetailsActivity.show(Hotspot.read(Hotspot.modelOf(row)));
            } catch (ReflectiveOperationException ex) {
                Log.e(TAG, "Cannot read the selected Wi-Fi network", ex);
            }
        });
    }

    public static String externalIpLabel(String reloadLabel, Object stateHolder) {
        return hasExternalIp(stateHolder) ? DETAILS_LABEL : reloadLabel;
    }

    public static Object onExternalIpClick(Object reload, Object stateHolder, boolean ipv6) {
        final Runnable reloadAction = () -> HostCallbacks.invoke(reload);
        return HostCallbacks.create(reload.getClass().getInterfaces(), () -> {
            if (hasExternalIp(stateHolder)) {
                ExternalIpActivity.show(stateHolder, ipv6, reloadAction);
            } else {
                reloadAction.run();
            }
        });
    }

    public static Object wrapLanDeviceClick(Class<?> onClickType, Object original) {
        if (Proxy.isProxyClass(original.getClass())
                && Proxy.getInvocationHandler(original) instanceof LanDeviceClickHandler) {
            return original;
        }
        return Proxy.newProxyInstance(Features.class.getClassLoader(), new Class<?>[] { onClickType },
                new LanDeviceClickHandler(original));
    }

    public static void registerToolsHistory(String registration) {
        try {
            ToolsHistory.register(registration);
        } catch (ClassNotFoundException ex) {
            Log.e(TAG, "Cannot register the tools history", ex);
        }
    }

    public static void captureNavigator(Object navigator) {
        ToolsHistory.captureNavigator(navigator);
    }

    public static void registerHostTools(Class<?> ping, Class<?> traceroute, String registration) {
        HostTools.register(ping, traceroute, registration);
    }

    public static void recordPingStart(String host, int delay, int payloadSize, boolean preferIpv6, Object protocol,
            int port, boolean dontFragment, boolean audible) {
        HostTools.recordPingStart(
                new Object[] { host, delay, payloadSize, preferIpv6, protocol, port, dontFragment, audible });
    }

    public static void recordTracerouteStart(String host, int maxDelay, int minDelay, boolean preferIpv6,
            boolean udpProbes) {
        HostTools.recordTracerouteStart(new Object[] { host, maxDelay, minDelay, preferIpv6, udpProbes });
    }

    public static Object onProbeClick(Class<?> onClickType, Object probe, String fieldNames) {
        return HostCallbacks.create(onClickType, () -> {
            try {
                ProbeDetailsActivity.show(Probe.read(probe, fieldNames));
            } catch (ReflectiveOperationException ex) {
                Log.e(TAG, "Cannot read the selected probe", ex);
            }
        });
    }

    public static Object applyToolOptions(Object query, String fieldNames) {
        final Activity activity = HostActivity.current();
        if (activity == null) {
            return query;
        }
        try {
            return ToolOptions.apply(activity, query, fieldNames);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot apply the tool options", ex);
            return query;
        }
    }

    public static void open(String activityClass) {
        HostActivity.open(activityClass, null);
    }

    private static boolean hasExternalIp(Object stateHolder) {
        try {
            final ExternalIp details = ExternalIp.read(stateHolder);
            return details != null && details.address != null;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static final class LanDeviceClickHandler implements InvocationHandler {

        private final Object original;

        LanDeviceClickHandler(Object original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws ReflectiveOperationException {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this.original, args);
            }
            LanDeviceActivity.show(LanRecord.read(args[0]), LanScan.read(args[1]), Boolean.TRUE.equals(args[2]), () -> {
                try {
                    method.invoke(this.original, args);
                } catch (ReflectiveOperationException ex) {
                    Log.e(TAG, "Cannot run the original LAN device action", ex);
                }
            });
            return null;
        }

    }

}
