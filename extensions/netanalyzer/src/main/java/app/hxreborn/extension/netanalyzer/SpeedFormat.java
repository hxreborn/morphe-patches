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
package app.hxreborn.extension.netanalyzer;

import java.util.Locale;

final class SpeedFormat {

    private static final double BITS_PER_BYTE = 8;

    private static final double VISIBLE_MINIMUM = 0.1;

    private static final double KILO = 1e3;

    private static final double MEGA = 1e6;

    private static final int SIGNIFICANT_TEXT_LENGTH = 4;

    private static final String DECIMAL_POINT = ".";

    private SpeedFormat() {
    }

    static String bits(double bytesPerSecond) {
        if (bytesPerSecond < VISIBLE_MINIMUM) {
            return "0 kbps";
        }
        final double bits = bytesPerSecond * BITS_PER_BYTE;
        if (bits < KILO) {
            return String.format(Locale.US, "%.0f bps", bits);
        }
        if (bits < MEGA) {
            return significant(bits / KILO) + " kbps";
        }
        return significant(bits / MEGA) + " Mbps";
    }

    static String bytes(double bytesPerSecond) {
        if (bytesPerSecond < KILO) {
            return String.format(Locale.US, "%.0f B/s", bytesPerSecond);
        }
        if (bytesPerSecond < MEGA) {
            return significant(bytesPerSecond / KILO) + " kB/s";
        }
        return significant(bytesPerSecond / MEGA) + " MB/s";
    }

    static String both(double bytesPerSecond) {
        return bits(bytesPerSecond) + " (" + bytes(bytesPerSecond) + ")";
    }

    private static String significant(double value) {
        String text = String.format(Locale.US, "%.3f", value);
        if (text.contains(DECIMAL_POINT) && text.length() > SIGNIFICANT_TEXT_LENGTH) {
            text = text.substring(0, SIGNIFICANT_TEXT_LENGTH);
            if (text.endsWith(DECIMAL_POINT)) {
                text = text.substring(0, SIGNIFICANT_TEXT_LENGTH - 1);
            }
        }
        return text;
    }

}
