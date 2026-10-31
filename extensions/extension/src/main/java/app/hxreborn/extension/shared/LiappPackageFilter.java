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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcelable;

@SuppressWarnings("unused")
public final class LiappPackageFilter extends ProxyHandler {

    private static final String LIAPP_PACKAGE_PREFIX = "com.liapp.";

    private final Object systemPackageManager;

    private final String ownPackage;

    private LiappPackageFilter(Object systemPackageManager, String ownPackage) {
        this.systemPackageManager = systemPackageManager;
        this.ownPackage = ownPackage;
    }

    public static void install(Context context) {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object systemPackageManager = activityThread.getDeclaredMethod("getPackageManager").invoke(null);
            Class<?> packageManagerInterface = Class.forName("android.content.pm.IPackageManager");
            Object filteredPackageManager = Proxy.newProxyInstance(packageManagerInterface.getClassLoader(),
                    new Class<?>[] { packageManagerInterface },
                    new LiappPackageFilter(systemPackageManager, context.getPackageName()));

            Field packageManagerField = activityThread.getDeclaredField("sPackageManager");
            packageManagerField.setAccessible(true);
            packageManagerField.set(null, filteredPackageManager);

            PackageManager packageManager = context.getPackageManager();
            Field binderField = packageManager.getClass().getDeclaredField("mPM");
            binderField.setAccessible(true);
            binderField.set(packageManager, filteredPackageManager);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not wrap the package manager", ex);
        }
    }

    @Override
    protected Object handle(Method method, Object[] args) {
        Object result;
        try {
            result = method.invoke(this.systemPackageManager, args);
        } catch (InvocationTargetException ex) {
            throw sneakyThrow(ex.getCause());
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
        if (!(result instanceof Parcelable) || !calledFromLiapp()) {
            return result;
        }
        if (result instanceof PackageInfo) {
            PackageInfo packageInfo = (PackageInfo) result;
            return isSystemOrOwnPackage(packageInfo.packageName, packageInfo.applicationInfo) ? result : null;
        }
        if (result instanceof ApplicationInfo) {
            ApplicationInfo applicationInfo = (ApplicationInfo) result;
            return isSystemOrOwnPackage(applicationInfo.packageName, applicationInfo) ? result : null;
        }
        return null;
    }

    private boolean isSystemOrOwnPackage(String packageName, ApplicationInfo applicationInfo) {
        if (this.ownPackage.equals(packageName)) {
            return true;
        }
        return (applicationInfo != null) && (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private static boolean calledFromLiapp() {
        for (StackTraceElement element : new Throwable().getStackTrace()) {
            if (element.getClassName().startsWith(LIAPP_PACKAGE_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }

}
