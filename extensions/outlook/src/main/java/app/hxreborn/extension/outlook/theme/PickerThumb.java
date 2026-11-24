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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

final class PickerThumb {

    private static final int FILL_COLOR = Color.WHITE;

    private static final int STROKE_COLOR = Color.DKGRAY;

    private final float radius;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    PickerThumb(float radius, float strokeWidth) {
        this.radius = radius;
        this.fill.setColor(FILL_COLOR);
        this.stroke.setStyle(Paint.Style.STROKE);
        this.stroke.setStrokeWidth(strokeWidth);
        this.stroke.setColor(STROKE_COLOR);
    }

    float radius() {
        return this.radius;
    }

    void draw(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, this.radius, this.fill);
        canvas.drawCircle(x, y, this.radius, this.stroke);
    }

}
