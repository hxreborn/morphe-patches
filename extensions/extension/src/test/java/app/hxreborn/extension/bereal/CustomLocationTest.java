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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class CustomLocationTest {

    private static final HostLocation ORIGINAL = new HostLocation(2.17, 41.38);

    @Before
    public void chooseParis() {
        Utils.setContext(RuntimeEnvironment.getApplication());
        PatchSettings.CUSTOM_LOCATION.save(true);
        PatchSettings.LOCATION_LATITUDE.save("48.8566");
        PatchSettings.LOCATION_LONGITUDE.save("2.3522");
    }

    @After
    public void restoreDefaults() {
        PatchSettings.CUSTOM_LOCATION.save(false);
        PatchSettings.LOCATION_LATITUDE.save("");
        PatchSettings.LOCATION_LONGITUDE.save("");
    }

    @Test
    public void buildsTheHostLocationWithLongitudeFirst() {
        final HostLocation sent = (HostLocation) CustomLocation.location(ORIGINAL, HostLocation.class);

        assertEquals(2.3522, sent.longitude, 0);
        assertEquals(48.8566, sent.latitude, 0);
    }

    @Test
    public void sendsTheOriginalLocationWhenTheSettingIsOff() {
        // given
        PatchSettings.CUSTOM_LOCATION.save(false);

        // when
        final Object sent = CustomLocation.location(ORIGINAL, HostLocation.class);

        // then
        assertSame(ORIGINAL, sent);
    }

    @Test
    public void sendsTheOriginalLocationForInvalidCoordinates() {
        // given
        PatchSettings.LOCATION_LATITUDE.save("91");

        // when
        final Object sent = CustomLocation.location(ORIGINAL, HostLocation.class);

        // then
        assertSame(ORIGINAL, sent);
    }

    @Test
    public void sendsTheOriginalLocationWhenTheTypeHasNoCoordinateConstructor() {
        final Object sent = CustomLocation.location(ORIGINAL, String.class);

        assertSame(ORIGINAL, sent);
    }

    private static final class HostLocation {

        private final double longitude;

        private final double latitude;

        private HostLocation(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }

    }

}
