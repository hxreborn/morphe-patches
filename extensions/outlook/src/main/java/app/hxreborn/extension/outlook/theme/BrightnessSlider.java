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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

@SuppressLint("ViewConstructor")
final class BrightnessSlider extends ColorPickerControl {

    private static final float MIN_TRAVEL = 1f;

    private final HsvColor color;

    private final PickerThumb thumb;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF track = new RectF();

    private int trackEndColor;

    BrightnessSlider(Context context, HsvColor color, PickerThumb thumb, Runnable onChange) {
        super(context, onChange);
        this.color = color;
        this.thumb = thumb;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        this.track.set(0, 0, width, height);
        shadeTrack(this.color.toFullValueColor());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int endColor = this.color.toFullValueColor();
        if (endColor != this.trackEndColor) {
            shadeTrack(endColor);
        }
        float corner = this.track.height() / 2f;
        canvas.drawRoundRect(this.track, corner, corner, this.trackPaint);
        float thumbX = this.thumb.radius() + this.color.value() * thumbTravel();
        this.thumb.draw(canvas, thumbX, this.track.centerY());
    }

    @Override
    void pickAt(float x, float y) {
        this.color.setValue((x - this.thumb.radius()) / thumbTravel());
    }

    private float thumbTravel() {
        return Math.max(MIN_TRAVEL, this.track.width() - 2 * this.thumb.radius());
    }

    private void shadeTrack(int endColor) {
        this.trackEndColor = endColor;
        float darkestX = this.thumb.radius();
        float brightestX = darkestX + thumbTravel();
        this.trackPaint
            .setShader(new LinearGradient(darkestX, 0, brightestX, 0, Color.BLACK, endColor, Shader.TileMode.CLAMP));
    }

}
