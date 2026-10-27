/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

final class SearchIconDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int sizePx;

    SearchIconDrawable(int color, int sizePx) {
        this.sizePx = sizePx;
        this.paint.setColor(color);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setStrokeWidth(sizePx / 12f);
    }

    @Override
    public void draw(Canvas canvas) {
        final float left = getBounds().left;
        final float top = getBounds().top;
        final float unit = getBounds().width() / 24f;
        canvas.drawCircle(left + 10.5f * unit, top + 10.5f * unit, 6.5f * unit, this.paint);
        canvas.drawLine(left + 15.5f * unit, top + 15.5f * unit, left + 20.5f * unit, top + 20.5f * unit, this.paint);
    }

    @Override
    public int getIntrinsicWidth() {
        return this.sizePx;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.sizePx;
    }

    @Override
    public void setAlpha(int alpha) {
        this.paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
