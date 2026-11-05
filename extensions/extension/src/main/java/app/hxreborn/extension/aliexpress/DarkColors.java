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

package app.hxreborn.extension.aliexpress;

final class DarkColors {

    static final int TEXT_TYPE = 0;

    static final int BACKGROUND_TYPE = 1;

    static final int LINE_TYPE = 2;

    private static final int ALPHA_SHIFT = 24;

    private static final int RED_SHIFT = 16;

    private static final int GREEN_SHIFT = 8;

    private static final int CHANNEL_MASK = 0xFF;

    private static final int MIN_VISIBLE_ALPHA = 0x40;

    private static final int RED_WEIGHT = 299;

    private static final int GREEN_WEIGHT = 587;

    private static final int BLUE_WEIGHT = 114;

    private static final int WEIGHT_TOTAL = 1000;

    private static final int NEUTRAL_SPREAD = 0x28;

    private static final int CARD_MIN_LUMA = 0xFA;

    private static final int PAGE_MIN_LUMA = 0xE8;

    private static final int FILL_MIN_LUMA = 0xC8;

    private static final int CARD_NIGHT = 0x1E1E1E;

    private static final int PAGE_NIGHT = 0x121212;

    private static final int FILL_NIGHT = 0x2B2B2B;

    private static final int PRIMARY_TEXT_MAX_LUMA = 0x48;

    private static final int SECONDARY_TEXT_MAX_LUMA = 0x80;

    private static final int TERTIARY_TEXT_MAX_LUMA = 0xB8;

    private static final int PRIMARY_TEXT_NIGHT = 0xE8E8E8;

    private static final int SECONDARY_TEXT_NIGHT = 0xB8B8B8;

    private static final int TERTIARY_TEXT_NIGHT = 0x8E8E8E;

    private static final int LINE_NIGHT = 0x3A3A3A;

    private static final int LIGHT_LINE_MIN_LUMA = 0xC8;

    private static final int NOT_REMAPPED = -1;

    private DarkColors() {
    }

    static int pageSurface() {
        return opaque(PAGE_NIGHT);
    }

    static int fillSurface() {
        return opaque(FILL_NIGHT);
    }

    static int primaryText() {
        return opaque(PRIMARY_TEXT_NIGHT);
    }

    static boolean hasSurface(int argb) {
        return ((argb >>> ALPHA_SHIFT) & CHANNEL_MASK) >= MIN_VISIBLE_ALPHA;
    }

    static int night(int type, int argb) {
        int alpha = (argb >>> ALPHA_SHIFT) & CHANNEL_MASK;
        int red = (argb >> RED_SHIFT) & CHANNEL_MASK;
        int green = (argb >> GREEN_SHIFT) & CHANNEL_MASK;
        int blue = argb & CHANNEL_MASK;
        int spread = Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
        if (alpha < MIN_VISIBLE_ALPHA || spread > NEUTRAL_SPREAD) {
            return argb;
        }
        int luma = (red * RED_WEIGHT + green * GREEN_WEIGHT + blue * BLUE_WEIGHT) / WEIGHT_TOTAL;
        int rgb = nightRgb(type, luma);
        if (rgb == NOT_REMAPPED) {
            return argb;
        }
        return (alpha << ALPHA_SHIFT) | rgb;
    }

    private static int opaque(int rgb) {
        return (CHANNEL_MASK << ALPHA_SHIFT) | rgb;
    }

    private static int nightRgb(int type, int luma) {
        switch (type) {
            case BACKGROUND_TYPE:
                return background(luma);
            case TEXT_TYPE:
                return foreground(luma);
            case LINE_TYPE:
                return (luma >= LIGHT_LINE_MIN_LUMA) ? LINE_NIGHT : foreground(luma);
            default:
                return NOT_REMAPPED;
        }
    }

    private static int background(int luma) {
        if (luma >= CARD_MIN_LUMA) {
            return CARD_NIGHT;
        }
        if (luma >= PAGE_MIN_LUMA) {
            return PAGE_NIGHT;
        }
        if (luma >= FILL_MIN_LUMA) {
            return FILL_NIGHT;
        }
        return NOT_REMAPPED;
    }

    private static int foreground(int luma) {
        if (luma <= PRIMARY_TEXT_MAX_LUMA) {
            return PRIMARY_TEXT_NIGHT;
        }
        if (luma <= SECONDARY_TEXT_MAX_LUMA) {
            return SECONDARY_TEXT_NIGHT;
        }
        if (luma <= TERTIARY_TEXT_MAX_LUMA) {
            return TERTIARY_TEXT_NIGHT;
        }
        return NOT_REMAPPED;
    }

}
