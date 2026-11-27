/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.exiftool;

final class PhotoMapItem {

    final Object photo;

    final double latitude;

    final double longitude;

    PhotoMapItem(Object photo, double latitude, double longitude) {
        this.photo = photo;
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
