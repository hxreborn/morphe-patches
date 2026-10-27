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
