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
package app.hxreborn.extension.bereal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

@SuppressWarnings("unused")
public final class CustomLocation {

    private static final String TAG = "CustomLocation";

    private static final String CLIENT_INTERFACE = "com.google.android.gms.location.FusedLocationProviderClient";

    private static final String TASKS_CLASS = "com.google.android.gms.tasks.Tasks";

    private static final float ACCURACY_METERS = 10f;

    private CustomLocation() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static Object location(Object original, Class<?> locationType) {
        final Coordinates coordinates = chosenCoordinates();
        if (coordinates == null) {
            return original;
        }
        try {
            final Constructor<?> constructor = locationType.getDeclaredConstructor(double.class, double.class);
            constructor.setAccessible(true);
            return constructor.newInstance(coordinates.longitude, coordinates.latitude);
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "Custom location not applied", ex);
            return original;
        }
    }

    public static Object locationClient(Object client) {
        if (client == null) {
            return null;
        }
        final ClassLoader loader = client.getClass().getClassLoader();
        try {
            final Class<?> clientInterface = Class.forName(CLIENT_INTERFACE, false, loader);
            final Method forResult = Class.forName(TASKS_CLASS, false, loader).getMethod("forResult", Object.class);
            return Proxy.newProxyInstance(loader, new Class<?>[] { clientInterface },
                    (proxy, method, args) -> callClientOrChosenLocation(client, forResult, method, args));
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            Log.e(TAG, "Location client not wrapped", ex);
            return client;
        }
    }

    private static Object callClientOrChosenLocation(Object client, Method forResult, Method method, Object[] args)
            throws Throwable {
        final String name = method.getName();
        if ("getLastLocation".equals(name) || "getCurrentLocation".equals(name)) {
            final Coordinates coordinates = chosenCoordinates();
            if (coordinates != null) {
                return forResult.invoke(null, locationAt(coordinates));
            }
        }
        try {
            return method.invoke(client, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static Location locationAt(Coordinates coordinates) {
        final Location location = new Location("fused");
        location.setLatitude(coordinates.latitude);
        location.setLongitude(coordinates.longitude);
        location.setAccuracy(ACCURACY_METERS);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return location;
    }

    private static Coordinates chosenCoordinates() {
        if (!PatchSettings.CUSTOM_LOCATION.get()) {
            return null;
        }
        return Coordinates.parse(PatchSettings.LOCATION_LATITUDE.get(), PatchSettings.LOCATION_LONGITUDE.get());
    }

}
