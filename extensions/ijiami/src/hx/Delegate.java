/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.Log;

public final class Delegate extends AppComponentFactory {
    private static final String TAG = "hxreborn/ijiami";

    private AppComponentFactory packer;

    private AppComponentFactory packer(ClassLoader classLoader, ApplicationInfo info) {
        if (packer != null) return packer;

        String name = Boot.packerFactory(info);
        if (name.isEmpty()) {
            Log.e(TAG, "missing packer component factory name");
            return null;
        }

        try {
            packer = (AppComponentFactory) Class.forName(name, true, classLoader)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Throwable t) {
            Log.e(TAG, "cannot instantiate component factory " + name, t);
        }

        return packer;
    }

    @Override
    public ClassLoader instantiateClassLoader(ClassLoader classLoader, ApplicationInfo info) {
        Boot.restoreFactoryName(info);
        Boot.install(info);

        AppComponentFactory packer = packer(classLoader, info);
        ClassLoader appLoader = packer == null
                ? super.instantiateClassLoader(classLoader, info)
                : packer.instantiateClassLoader(classLoader, info);
        Boot.runAppHook(appLoader);
        return appLoader;
    }

    @Override
    public Application instantiateApplication(ClassLoader classLoader, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (packer == null) return super.instantiateApplication(classLoader, className);
        return packer.instantiateApplication(classLoader, className);
    }

    @Override
    public Activity instantiateActivity(ClassLoader classLoader, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (packer == null) return super.instantiateActivity(classLoader, className, intent);
        return packer.instantiateActivity(classLoader, className, intent);
    }

    @Override
    public BroadcastReceiver instantiateReceiver(ClassLoader classLoader, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (packer == null) return super.instantiateReceiver(classLoader, className, intent);
        return packer.instantiateReceiver(classLoader, className, intent);
    }

    @Override
    public Service instantiateService(ClassLoader classLoader, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (packer == null) return super.instantiateService(classLoader, className, intent);
        return packer.instantiateService(classLoader, className, intent);
    }

    @Override
    public ContentProvider instantiateProvider(ClassLoader classLoader, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (packer == null) return super.instantiateProvider(classLoader, className);
        return packer.instantiateProvider(classLoader, className);
    }
}
