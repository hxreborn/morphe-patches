/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
        if (packer == null) return super.instantiateClassLoader(classLoader, info);

        return packer.instantiateClassLoader(classLoader, info);
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
