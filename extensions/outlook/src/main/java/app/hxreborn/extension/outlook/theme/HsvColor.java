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
package app.hxreborn.extension.outlook.theme;

import android.graphics.Color;

final class HsvColor {

    private static final int HUE = 0;

    private static final int SATURATION = 1;

    private static final int VALUE = 2;

    private static final int COMPONENTS = 3;

    private static final float FULL_TURN_DEGREES = 360f;

    private static final float MAX_FRACTION = 1f;

    private final float[] hsv = new float[COMPONENTS];

    void set(int color) {
        Color.colorToHSV(color, this.hsv);
    }

    int toColor() {
        return Color.HSVToColor(this.hsv);
    }

    int toFullValueColor() {
        return Color.HSVToColor(new float[] { this.hsv[HUE], this.hsv[SATURATION], MAX_FRACTION });
    }

    float hue() {
        return this.hsv[HUE];
    }

    float saturation() {
        return this.hsv[SATURATION];
    }

    float value() {
        return this.hsv[VALUE];
    }

    void setHueAndSaturation(float hueDegrees, float saturation) {
        float hue = hueDegrees % FULL_TURN_DEGREES;
        this.hsv[HUE] = (hue < 0) ? hue + FULL_TURN_DEGREES : hue;
        this.hsv[SATURATION] = clampFraction(saturation);
    }

    void setValue(float value) {
        this.hsv[VALUE] = clampFraction(value);
    }

    private static float clampFraction(float fraction) {
        return Math.max(0f, Math.min(MAX_FRACTION, fraction));
    }

}
