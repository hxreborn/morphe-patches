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
package app.hxreborn.extension.ultimea.equalizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

final class EqualizerIcon extends Drawable {

    private static final float VIEWPORT = 24f;

    private static final float STROKE = 1.8f;

    private static final int CIRCLE_VALUES = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path path = new Path();

    private final Glyph glyph;

    private Drawable stock;

    EqualizerIcon(Glyph glyph, int color) {
        this.glyph = glyph;
        this.paint.setColor(color);
        this.paint.setStrokeWidth(STROKE);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setStrokeJoin(Paint.Join.ROUND);
    }

    EqualizerIcon(Context context, Glyph glyph, int color) {
        this(glyph, color);
        if (glyph.ordinal() > Glyph.NIGHT.ordinal()) {
            return;
        }
        String name = glyph.name().toLowerCase(Locale.ROOT);
        try (InputStream icon = context.getAssets().open("hx_ultimea/eq/" + name + ".png")) {
            this.stock = new BitmapDrawable(context.getResources(), BitmapFactory.decodeStream(icon));
            this.stock.setTint(color);
        } catch (IOException ex) {
            Log.w("hxUltimea", "Equalizer icon could not be read", ex);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (this.stock != null) {
            this.stock.setBounds(bounds);
            this.stock.draw(canvas);
            return;
        }
        int saved = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / VIEWPORT, bounds.height() / VIEWPORT);
        this.path.reset();
        boolean start = true;
        for (int index = 0; index < this.glyph.lines.length; index += 2) {
            float x = this.glyph.lines[index];
            if (Float.isNaN(x)) {
                start = true;
            } else if (start) {
                this.path.moveTo(x, this.glyph.lines[index + 1]);
                start = false;
            } else {
                this.path.lineTo(x, this.glyph.lines[index + 1]);
            }
        }
        this.paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(this.path, this.paint);
        for (int index = 0; index < this.glyph.circles.length; index += CIRCLE_VALUES) {
            canvas.drawCircle(this.glyph.circles[index], this.glyph.circles[index + 1], this.glyph.circles[index + 2],
                    this.paint);
        }
        canvas.restoreToCount(saved);
    }

    @Override
    public void setAlpha(int alpha) {
        if (this.stock != null) {
            this.stock.setAlpha(alpha);
        }
        this.paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setTint(int color) {
        if (this.stock != null) {
            this.stock.setTint(color);
        }
        this.paint.setColor(color);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (this.stock != null) {
            this.stock.setColorFilter(colorFilter);
        }
        this.paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    enum Glyph {

        MOVIE(new float[] { 3, 10, 15, 10, 15, 21, 3, 21, 3, 10, Float.NaN, 0, 15, 13, 21, 10, 21, 21, 15, 18 },
                new float[] { 6, 6, 3, 12, 6, 3 }),
        MUSIC(new float[] { 10, 18, 10, 5, 20, 3, 20, 16, Float.NaN, 0, 10, 9, 20, 7 },
                new float[] { 7, 18, 3, 17, 16, 3 }),
        VOICE(new float[] { 3, 10, 3, 14, Float.NaN, 0, 7, 7, 7, 17, Float.NaN, 0, 11, 3, 11, 21, Float.NaN, 0, 15, 6,
                15, 18, Float.NaN, 0, 19, 9, 19, 15, Float.NaN, 0, 23, 11, 23, 13 }, new float[0]),
        SPORT(new float[] { 13, 7, 9, 12, 14, 15, 12, 22, Float.NaN, 0, 13, 7, 16, 11, 21, 11, Float.NaN, 0, 10, 9, 6,
                9, 3, 13, Float.NaN, 0, 10, 13, 7, 18, 3, 19 }, new float[] { 16, 3, 2 }),
        GAME(new float[] { 7, 6, 17, 6, 20, 9, 22, 19, 19, 21, 15, 16, 9, 16, 5, 21, 2, 19, 4, 9, 7, 6, Float.NaN, 0, 8,
                9, 8, 14, Float.NaN, 0, 5.5f, 11.5f, 10.5f, 11.5f }, new float[] { 16, 10, .6f, 18, 13, .6f }),
        NIGHT(new float[] { 18, 3, 13, 4, 9, 8, 9, 14, 13, 18, 20, 18, 17, 21, 10, 22, 4, 18, 2, 12, 4, 6, 9, 2, 15, 2,
                18, 3 }, new float[0]),
        CUSTOM(new float[] { 5, 3, 5, 8, Float.NaN, 0, 5, 13, 5, 21, Float.NaN, 0, 12, 3, 12, 13, Float.NaN, 0, 12, 18,
                12, 21, Float.NaN, 0, 19, 3, 19, 6, Float.NaN, 0, 19, 11, 19, 21, Float.NaN, 0, 2, 8, 8, 8, 8, 13, 2,
                13, 2, 8, Float.NaN, 0, 9, 13, 15, 13, 15, 18, 9, 18, 9, 13, Float.NaN, 0, 16, 6, 22, 6, 22, 11, 16, 11,
                16, 6 }, new float[0]),
        ADD(new float[] { 12, 4, 12, 20, Float.NaN, 0, 4, 12, 20, 12 }, new float[0]),
        UNDO(new float[] { 8, 4, 3, 9, 8, 14, Float.NaN, 0, 3, 9, 14, 9, 19, 11, 21, 15, 19, 19, 14, 21 },
                new float[0]),
        MORE(new float[0], new float[] { 12, 5, 1, 12, 12, 1, 12, 19, 1 }),
        PREVIOUS(new float[] { 15, 5, 8, 12, 15, 19 }, new float[0]),
        NEXT(new float[] { 9, 5, 16, 12, 9, 19 }, new float[0]), MINUS(new float[] { 5, 12, 19, 12 }, new float[0]),
        PLUS(new float[] { 5, 12, 19, 12, Float.NaN, 0, 12, 5, 12, 19 }, new float[0]);

        private final float[] lines;

        private final float[] circles;

        Glyph(float[] lines, float[] circles) {
            this.lines = lines;
            this.circles = circles;
        }

    }

}
