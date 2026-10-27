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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

final class HostTools {

    private static final String TAG = "hxNetAnalyzer";

    private static final String REGISTRATION_SEPARATOR = ";";

    private static final String METHOD_SEPARATOR = ",";

    private static final int HOST_ARGUMENT = 0;

    private static final int PING_START = 0;

    private static final int PING_STOP = 1;

    private static final int TRACEROUTE_START = 2;

    private static final int TRACEROUTE_STOP = 3;

    private static Class<?> pingTool;

    private static Class<?> tracerouteTool;

    private static String probeFieldNames;

    private static String[] runMethods;

    private static Object[] lastPingStart;

    private static Object[] lastTracerouteStart;

    private HostTools() {
    }

    static void register(Class<?> ping, Class<?> traceroute, String registration) {
        final String[] parts = registration.split(REGISTRATION_SEPARATOR, 2);
        pingTool = ping;
        tracerouteTool = traceroute;
        probeFieldNames = parts[0];
        runMethods = parts[1].split(METHOD_SEPARATOR);
    }

    static void recordPingStart(Object[] arguments) {
        lastPingStart = arguments;
    }

    static void recordTracerouteStart(Object[] arguments) {
        lastTracerouteStart = arguments;
    }

    static String pingHost() {
        return host(lastPingStart);
    }

    static String tracerouteHost() {
        return host(lastTracerouteStart);
    }

    private static String host(Object[] startArguments) {
        return (startArguments != null) ? (String) startArguments[HOST_ARGUMENT] : "";
    }

    static boolean canRestartPing() {
        return lastPingStart != null;
    }

    static boolean canRestartTraceroute() {
        return lastTracerouteStart != null;
    }

    static void restartPing() {
        invoke(pingTool, runMethods[PING_START], lastPingStart);
    }

    static void stopPing() {
        invoke(pingTool, runMethods[PING_STOP], new Object[0]);
    }

    static void restartTraceroute() {
        invoke(tracerouteTool, runMethods[TRACEROUTE_START], lastTracerouteStart);
    }

    static void stopTraceroute() {
        invoke(tracerouteTool, runMethods[TRACEROUTE_STOP], new Object[0]);
    }

    private static void invoke(Class<?> tool, String name, Object[] arguments) {
        if (tool == null || arguments == null) {
            return;
        }
        try {
            for (Method method : tool.getDeclaredMethods()) {
                if (method.getName().equals(name) && Modifier.isStatic(method.getModifiers())
                        && method.getParameterCount() == arguments.length) {
                    method.setAccessible(true);
                    method.invoke(null, arguments);
                    return;
                }
            }
            throw new NoSuchMethodException(name + " on " + tool);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot run " + name + " on the host tool", ex);
        }
    }

    static Snapshot ping() {
        return snapshot(pingTool);
    }

    static Snapshot traceroute() {
        return snapshot(tracerouteTool);
    }

    private static Snapshot snapshot(Class<?> tool) {
        if (tool == null) {
            return new Snapshot(Collections.emptyList(), false);
        }
        List<Probe> probes = Collections.emptyList();
        boolean active = false;
        try {
            for (Object value : StaticStates.values(tool)) {
                if (value instanceof Boolean) {
                    active |= (Boolean) value;
                } else if (value instanceof List && !((List<?>) value).isEmpty()) {
                    probes = Probe.readAll((List<?>) value, probeFieldNames);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot read the host tool state", ex);
        }
        return new Snapshot(probes, active);
    }

    static final class Snapshot {

        final List<Probe> probes;

        final boolean active;

        Snapshot(List<Probe> probes, boolean active) {
            this.probes = new ArrayList<>(probes);
            this.active = active;
        }

    }

}
