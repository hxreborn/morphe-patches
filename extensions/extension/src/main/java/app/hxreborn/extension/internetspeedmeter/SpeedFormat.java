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
package app.hxreborn.extension.internetspeedmeter;

import java.util.Locale;

final class SpeedFormat {

    private static final double KILO = 1024;

    private SpeedFormat() {

    }

    static String speed(long bytesPerSecond, boolean bits) {
        double kilo = (bits ? bytesPerSecond * 8 : bytesPerSecond) / KILO;
        if (kilo < KILO) {
            return String.format(Locale.getDefault(), "%.1f %s", kilo, bits ? "kb/s" : "KB/s");
        }
        return String.format(Locale.getDefault(), "%.2f %s", kilo / KILO, bits ? "mb/s" : "MB/s");
    }

    static String size(long bytes) {
        double mega = bytes / KILO / KILO;
        if (mega < KILO) {
            return String.format(Locale.getDefault(), "%.1f MB", mega);
        }
        return String.format(Locale.getDefault(), "%.2f GB", mega / KILO);
    }

    static String duration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }

}
