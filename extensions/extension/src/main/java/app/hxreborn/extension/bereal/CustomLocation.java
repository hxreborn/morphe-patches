/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.lang.reflect.Constructor;

import android.util.Log;

@SuppressWarnings("unused")
public final class CustomLocation {

    private static final String TAG = "CustomLocation";

    private CustomLocation() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static Object location(Object original, Class<?> locationType) {
        if (!PatchSettings.CUSTOM_LOCATION.get()) {
            return original;
        }
        final Coordinates coordinates = Coordinates.parse(PatchSettings.LOCATION_LATITUDE.get(),
                PatchSettings.LOCATION_LONGITUDE.get());
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

}
