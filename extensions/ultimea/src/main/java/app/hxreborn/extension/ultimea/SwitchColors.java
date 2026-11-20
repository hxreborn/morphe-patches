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
package app.hxreborn.extension.ultimea;

final class SwitchColors {

    private static final double CONTRAST_TARGET = 3.3;

    private static final double LUMINANCE_OFFSET = 0.05;

    private static final double RED_WEIGHT = 0.2126;

    private static final double GREEN_WEIGHT = 0.7152;

    private static final double BLUE_WEIGHT = 0.0722;

    private static final double SRGB_THRESHOLD = 0.04045;

    private static final double LINEAR_THRESHOLD = 0.0031308;

    private static final double SRGB_SCALE = 12.92;

    private static final double SRGB_OFFSET = 0.055;

    private static final double SRGB_EXPONENT = 2.4;

    private static final double CHANNEL_MAX = 255;

    private static final int OPAQUE = 0xFF000000;

    private static final int RED_SHIFT = 16;

    private static final int GREEN_SHIFT = 8;

    private static final int CHANNEL_MASK = 0xFF;

    private static final int OFF_TRACK_BASE = 0xFF808080;

    final int thumb = 0xFFFFFFFF;

    final int checkedTrack;

    final int uncheckedTrack;

    SwitchColors(int accent, int surface, boolean dark) {
        double thumbLuminance = luminance(this.thumb);
        double highest = (thumbLuminance + LUMINANCE_OFFSET) / CONTRAST_TARGET - LUMINANCE_OFFSET;
        double lowest = 0;
        if (dark) {
            lowest = CONTRAST_TARGET * (luminance(surface) + LUMINANCE_OFFSET) - LUMINANCE_OFFSET;
        } else {
            highest = Math.min(highest, (luminance(surface) + LUMINANCE_OFFSET) / CONTRAST_TARGET - LUMINANCE_OFFSET);
        }
        this.checkedTrack = tone(accent, lowest, highest);
        this.uncheckedTrack = tone(OFF_TRACK_BASE, lowest, highest);
    }

    private static int tone(int color, double lowest, double highest) {
        double original = luminance(color);
        double target = Math.max(lowest, Math.min(highest, original));
        double red = linear((color >> RED_SHIFT) & CHANNEL_MASK);
        double green = linear((color >> GREEN_SHIFT) & CHANNEL_MASK);
        double blue = linear(color & CHANNEL_MASK);
        if (target <= original) {
            double scale = (original == 0) ? 0 : target / original;
            return rgb(red * scale, green * scale, blue * scale);
        }
        double amount = (target - original) / (1 - original);
        return rgb(red + (1 - red) * amount, green + (1 - green) * amount, blue + (1 - blue) * amount);
    }

    private static int rgb(double red, double green, double blue) {
        return OPAQUE | (channel(red) << RED_SHIFT) | (channel(green) << GREEN_SHIFT) | channel(blue);
    }

    private static double luminance(int color) {
        return RED_WEIGHT * linear((color >> RED_SHIFT) & CHANNEL_MASK)
                + GREEN_WEIGHT * linear((color >> GREEN_SHIFT) & CHANNEL_MASK)
                + BLUE_WEIGHT * linear(color & CHANNEL_MASK);
    }

    private static double linear(int channel) {
        double value = channel / CHANNEL_MAX;
        return (value <= SRGB_THRESHOLD) ? value / SRGB_SCALE
                : Math.pow((value + SRGB_OFFSET) / (1 + SRGB_OFFSET), SRGB_EXPONENT);
    }

    private static int channel(double value) {
        return (int) Math.round(CHANNEL_MAX * ((value <= LINEAR_THRESHOLD) ? value * SRGB_SCALE
                : (1 + SRGB_OFFSET) * Math.pow(value, 1 / SRGB_EXPONENT) - SRGB_OFFSET));
    }

}
