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
package app.hxreborn.extension.bereal.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.ZoneId;

import org.junit.Test;

public final class ExifValuesTest {

    private static final long TAKEN_AT = 1793046563000L;

    private static final ZoneId ATHENS = ZoneId.of("Europe/Athens");

    @Test
    public void formatsTheLocalDateTimeAndOffset() {
        assertEquals("2026:10:26 22:29:23", ExifValues.dateTime(TAKEN_AT, ATHENS));
        assertEquals("+02:00", ExifValues.offset(TAKEN_AT, ATHENS));
        assertEquals("-05:00", ExifValues.offset(TAKEN_AT, ZoneId.of("America/Bogota")));
    }

    @Test
    public void splitsDegreesIntoRationals() {
        assertEquals("48/1,51/1,2376/100", ExifValues.rational(48.8566));
        assertEquals("48/1,51/1,2376/100", ExifValues.rational(-48.8566));
    }

    @Test
    public void picksTheHemisphereRefs() {
        assertEquals("N", ExifValues.latitudeRef(48.8566));
        assertEquals("S", ExifValues.latitudeRef(-33.9));
        assertEquals("E", ExifValues.longitudeRef(2.35));
        assertEquals("W", ExifValues.longitudeRef(-70.6));
    }

    @Test
    public void dropsACaptionExifCannotHold() {
        assertEquals("Lunch", ExifValues.description("Lunch"));
        assertNull(ExifValues.description("café"));
        assertNull(ExifValues.description(""));
    }

}
