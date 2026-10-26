/*
 * Copyright (C) 2024 The Android Open Source Project
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: Apache-2.0
 *
 * Adapted from material-components/material-components-android:
 * https://github.com/material-components/material-components-android/commit/66b0eeb53afeb6025ffbbd37734b08e5bce197ed
 * Commit 66b0eeb53afeb6025ffbbd37734b08e5bce197ed (2026-10-12),
 * lib/java/com/google/android/material/loadingindicator/LoadingIndicatorAnimatorDelegate.java and
 * lib/java/com/google/android/material/loadingindicator/LoadingIndicatorDrawingDelegate.java
 *
 * Ported from hxreborn/pixel-nits:
 * app/src/main/kotlin/eu/hxreborn/pixelinjector/xposed/hook/firefox/LoadingIndicatorDrawable.kt
 */
package app.hxreborn.extension.shared;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

public final class LoadingIndicatorDrawable extends Drawable implements Drawable.Callback {

    private final Drawable stock;

    private final Progress progress;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint containerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final LoadingIndicatorMorph morph = new LoadingIndicatorMorph();

    private long startNanos;

    public LoadingIndicatorDrawable(Drawable stock, int containerColor, int indicatorColor, Progress progress) {
        this.stock = stock;
        this.progress = progress;
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(indicatorColor);
        this.containerPaint.setStyle(Paint.Style.FILL);
        this.containerPaint.setColor(containerColor);
    }

    @Override
    public void draw(Canvas canvas) {
        final boolean running = ((Animatable) this.stock).isRunning();
        if (running) {
            final long now = System.nanoTime();
            if (this.startNanos == 0) {
                this.startNanos = now;
                this.morph.reset();
            }
            final long elapsed = now - this.startNanos;
            this.morph.advance(1 + (int) (elapsed / LoadingIndicatorMorph.NANOS_PER_SHAPE),
                    (float) (elapsed % LoadingIndicatorMorph.NANOS_PER_SHAPE) / LoadingIndicatorMorph.NANOS_PER_SHAPE,
                    now);
        } else {
            this.startNanos = 0;
        }
        final Rect area = getBounds();
        final float radius = Math.min(area.width(), area.height()) / 2f;
        canvas.drawCircle(area.exactCenterX(), area.exactCenterY(), radius, this.containerPaint);
        this.paint.setAlpha(this.stock.getAlpha());
        final float pull = Math.max(0f, Math.min(1f, this.progress.get()));
        this.morph.draw(canvas, this.paint, area.exactCenterX(), area.exactCenterY(), radius, running, pull);
        if (running) {
            invalidateSelf();
        }
    }

    @Override
    public void setAlpha(int alpha) {
        this.stock.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    public interface Progress {

        float get();

    }

}
