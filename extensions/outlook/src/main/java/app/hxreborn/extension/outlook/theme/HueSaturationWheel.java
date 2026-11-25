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
import android.graphics.ComposeShader;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;

@SuppressLint("ViewConstructor")
final class HueSaturationWheel extends ColorPickerControl {

    private static final int[] HUES_CLOCKWISE = { Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN,
            Color.YELLOW, Color.RED };

    private static final float MIN_RADIUS = 1f;

    private final HsvColor color;

    private final PickerThumb thumb;

    private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float centerX;

    private float centerY;

    private float radius;

    HueSaturationWheel(Context context, HsvColor color, PickerThumb thumb, Runnable onChange) {
        super(context, onChange);
        this.color = color;
        this.thumb = thumb;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        this.centerX = width / 2f;
        this.centerY = height / 2f;
        this.radius = Math.max(MIN_RADIUS, Math.min(width, height) / 2f - this.thumb.radius());
        Shader hues = new SweepGradient(this.centerX, this.centerY, HUES_CLOCKWISE, null);
        Shader whiteCenter = new RadialGradient(this.centerX, this.centerY, this.radius, Color.WHITE, Color.TRANSPARENT,
                Shader.TileMode.CLAMP);
        this.wheelPaint.setShader(new ComposeShader(hues, whiteCenter, PorterDuff.Mode.SRC_OVER));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(this.centerX, this.centerY, this.radius, this.wheelPaint);
        double angle = Math.toRadians(this.color.hue());
        float distance = this.color.saturation() * this.radius;
        this.thumb.draw(canvas, this.centerX + (float) Math.cos(angle) * distance,
                this.centerY - (float) Math.sin(angle) * distance);
    }

    @Override
    void pickAt(float x, float y) {
        float dx = x - this.centerX;
        float dy = this.centerY - y;
        this.color.setHueAndSaturation((float) Math.toDegrees(Math.atan2(dy, dx)),
                (float) Math.hypot(dx, dy) / this.radius);
    }

}
