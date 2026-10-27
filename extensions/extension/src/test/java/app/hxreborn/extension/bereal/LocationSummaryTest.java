/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
