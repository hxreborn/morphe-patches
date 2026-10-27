/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.bereal;

import java.util.Locale;

final class LocationSummary {

    final String label;

    final String coordinates;

    private LocationSummary(String label, String coordinates) {
        this.label = label;
        this.coordinates = coordinates;
    }

    static LocationSummary of(String label, Coordinates coordinates) {
        final String name = label.trim();
        if (coordinates == null) {
            return new LocationSummary(name, "");
        }
        return new LocationSummary(name,
                String.format(Locale.US, "%.4f, %.4f", coordinates.latitude, coordinates.longitude));
    }

}
