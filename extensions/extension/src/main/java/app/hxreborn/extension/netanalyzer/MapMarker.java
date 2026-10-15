/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

final class MapMarker {

    static final int TYPE_HOP = 0;

    static final int TYPE_DESTINATION = 1;

    static final int TYPE_USER_LOCATION = 2;

    final double latitude;

    final double longitude;

    final int type;

    final String title;

    final String snippet;

    MapMarker(double latitude, double longitude, int type, String title, String snippet) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.title = title;
        this.snippet = snippet;
    }

}
