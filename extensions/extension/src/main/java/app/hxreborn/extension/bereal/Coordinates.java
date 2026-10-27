/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

final class Coordinates {

    final double latitude;

    final double longitude;

    private Coordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    static Coordinates parse(String latitude, String longitude) {
        try {
            final double lat = Double.parseDouble(latitude.trim().replace(',', '.'));
            final double lon = Double.parseDouble(longitude.trim().replace(',', '.'));
            if (!(lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180)) {
                return null;
            }
            return new Coordinates(lat, lon);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
