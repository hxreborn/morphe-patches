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

import org.junit.Test;

public final class LocationSummaryTest {

    @Test
    public void roundsCoordinatesToFourDecimals() {
        // given
        Coordinates coordinates = Coordinates.parse("48.857547", "2.351376");

        // when
        LocationSummary summary = LocationSummary.of("Paris, France", coordinates);

        // then
        assertEquals("48.8575, 2.3514", summary.coordinates);
    }

    @Test
    public void keepsTheLabelSeparateFromTheCoordinates() {
        // given
        Coordinates coordinates = Coordinates.parse("48.857547", "2.351376");

        // when
        LocationSummary summary = LocationSummary.of("  Paris, France ", coordinates);

        // then
        assertEquals("Paris, France", summary.label);
    }

    @Test
    public void leavesTheLabelEmptyForAMapPick() {
        // given
        Coordinates coordinates = Coordinates.parse("-33.8688", "151.2093");

        // when
        LocationSummary summary = LocationSummary.of("", coordinates);

        // then
        assertEquals("", summary.label);
        assertEquals("-33.8688, 151.2093", summary.coordinates);
    }

    @Test
    public void leavesCoordinatesEmptyWithoutAPosition() {
        // when
        LocationSummary summary = LocationSummary.of("Paris", null);

        // then
        assertEquals("", summary.coordinates);
    }

}
