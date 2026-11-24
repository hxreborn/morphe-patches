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

import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppComponentFactory;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.P)
@SuppressWarnings("unused")
public class HostedComponentFactory extends AppComponentFactory {

    @Override
    @SuppressWarnings("NullableProblems")
    public Activity instantiateActivity(ClassLoader classLoader, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return instantiateActivity(classLoader, className, intent, HostedComponents.hosts());
    }

    Activity instantiateActivity(ClassLoader classLoader, String className, Intent intent, Map<String, String> hosts)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String hosted = HostedComponents.hostedClass(className, intent, hosts);
        if (hosted != null) {
            return super.instantiateActivity(classLoader, hosted, intent);
        }
        Activity activity = super.instantiateActivity(classLoader, className, intent);
        watchActivity(activity);
        return activity;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public BroadcastReceiver instantiateReceiver(ClassLoader classLoader, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return instantiateReceiver(classLoader, className, intent, HostedComponents.hosts());
    }

    BroadcastReceiver instantiateReceiver(ClassLoader classLoader, String className, Intent intent,
            Map<String, String> hosts) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String hosted = HostedComponents.hostedClass(className, intent, hosts);
        return super.instantiateReceiver(classLoader, (hosted != null) ? hosted : className, intent);
    }

    public static void watchActivity(Activity activity) {
    }

}
