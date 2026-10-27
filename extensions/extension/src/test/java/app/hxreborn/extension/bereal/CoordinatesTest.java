/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class CoordinatesTest {

    @Test
    public void rejectsNaNLatitude() {
        // when
        Coordinates coordinates = Coordinates.parse("NaN", "0");

        // then
        assertNull(coordinates);
    }

    @Test
    public void rejectsNaNLongitude() {
        // when
        Coordinates coordinates = Coordinates.parse("0", "NaN");

        // then
        assertNull(coordinates);
    }

    @Test
    public void acceptsTheBounds() {
        // when
        Coordinates coordinates = Coordinates.parse("-90", "180");

        // then
        assertEquals(-90, coordinates.latitude, 0);
        assertEquals(180, coordinates.longitude, 0);
    }

    @Test
    public void rejectsLatitudePastNinety() {
        // when
        Coordinates coordinates = Coordinates.parse("90.5", "0");

        // then
        assertNull(coordinates);
    }

    @Test
    public void rejectsLongitudePastOneEighty() {
        // when
        Coordinates coordinates = Coordinates.parse("0", "-180.5");

        // then
        assertNull(coordinates);
    }

    @Test
    public void readsCommaDecimals() {
        // when
        Coordinates coordinates = Coordinates.parse("41,65", "-0,88");

        // then
        assertEquals(41.65, coordinates.latitude, 1e-9);
        assertEquals(-0.88, coordinates.longitude, 1e-9);
    }

}
