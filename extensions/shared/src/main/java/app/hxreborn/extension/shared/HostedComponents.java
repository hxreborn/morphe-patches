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
package app.hxreborn.extension.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class HostedComponents {

    private static final String ENTRY_SEPARATOR = ";";

    private static final String HOST_SEPARATOR = "=";

    private static final String UNREACHABLE_MESSAGE = "This screen needs Android 9 or newer on a mount install";

    private static volatile Map<String, String> hosts;

    private HostedComponents() {
    }

    public static String hostedComponents() {
        return "";
    }

    public static Intent route(Context context, Intent intent) {
        return route(context, intent, hosts());
    }

    static Intent route(Context context, Intent intent, Map<String, String> hosts) {
        ComponentName component = intent.getComponent();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || component == null
                || !context.getPackageName().equals(component.getPackageName())) {
            return intent;
        }
        String host = hosts.get(component.getClassName());
        if (host == null || isDeclared(context, component)) {
            return intent;
        }
        return intent.setClassName(context, host).setAction(component.getClassName());
    }

    public static boolean isReachable(Context context, Intent intent) {
        return isReachable(context, intent, hosts());
    }

    static boolean isReachable(Context context, Intent intent, Map<String, String> hosts) {
        ComponentName component = intent.getComponent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || component == null
                || !context.getPackageName().equals(component.getPackageName())
                || !hosts.containsKey(component.getClassName())) {
            return true;
        }
        return isDeclared(context, component);
    }

    public static Intent routeOrShowUnreachableMessage(Context context, Intent intent) {
        if (!isReachable(context, intent)) {
            showUnreachableMessage(context);
            return null;
        }
        return route(context, intent);
    }

    public static void startActivity(Context context, Intent intent) {
        if (!isReachable(context, intent)) {
            showUnreachableMessage(context);
            return;
        }
        context.startActivity(route(context, intent));
    }

    static String hostedClass(String className, Intent intent, Map<String, String> hosts) {
        String component = (intent != null) ? intent.getAction() : null;
        if (component == null || !className.equals(hosts.get(component))) {
            return null;
        }
        return component;
    }

    static Map<String, String> parseHosts(String encoded) {
        Map<String, String> parsed = new HashMap<>();
        for (String entry : encoded.split(ENTRY_SEPARATOR)) {
            int separator = entry.indexOf(HOST_SEPARATOR);
            if (separator > 0) {
                parsed.put(entry.substring(0, separator), entry.substring(separator + 1));
            }
        }
        return Collections.unmodifiableMap(parsed);
    }

    static Map<String, String> hosts() {
        Map<String, String> parsed = hosts;
        if (parsed == null) {
            parsed = parseHosts(hostedComponents());
            hosts = parsed;
        }
        return parsed;
    }

    private static void showUnreachableMessage(Context context) {
        Toast.makeText(context, UNREACHABLE_MESSAGE, Toast.LENGTH_LONG).show();
    }

    private static boolean isDeclared(Context context, ComponentName component) {
        return ManifestDeclarations.isActivityDeclared(context, component)
                || ManifestDeclarations.isReceiverDeclared(context, component);
    }

}
