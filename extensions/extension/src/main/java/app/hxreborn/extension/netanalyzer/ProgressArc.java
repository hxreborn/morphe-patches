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

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;

import app.hxreborn.extension.shared.Dimensions;

final class ProgressArc extends View {

    private static final float STROKE_DP = 3f;

    private static final float FULL_CIRCLE = 360f;

    private static final float TOP = -90f;

    private static final float MIN_INDETERMINATE_SWEEP = 0.1f;

    private static final float MAX_INDETERMINATE_SWEEP = 0.87f;

    private static final long ROTATION_MS = 1333;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF bounds = new RectF();

    private final ValueAnimator spinner = ValueAnimator.ofFloat(0f, 1f);

    private float progress;

    ProgressArc(Context context, int color) {
        super(context);
        this.paint.setColor(color);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeCap(Paint.Cap.BUTT);
        this.paint.setStrokeWidth(Dimensions.dpExact(context, STROKE_DP));
        this.spinner.setDuration(ROTATION_MS);
        this.spinner.setRepeatCount(ValueAnimator.INFINITE);
        this.spinner.setInterpolator(new LinearInterpolator());
        this.spinner.addUpdateListener((animation) -> invalidate());
    }

    void setProgress(float progress) {
        this.spinner.cancel();
        this.progress = progress;
        invalidate();
    }

    void setIndeterminate() {
        if (!this.spinner.isRunning()) {
            this.spinner.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        this.spinner.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float inset = this.paint.getStrokeWidth() / 2;
        this.bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        if (this.spinner.isRunning()) {
            final float phase = (float) this.spinner.getAnimatedValue();
            final float wave = (float) (1 - Math.cos(phase * 2 * Math.PI)) / 2;
            final float sweep = MIN_INDETERMINATE_SWEEP + (MAX_INDETERMINATE_SWEEP - MIN_INDETERMINATE_SWEEP) * wave;
            canvas.drawArc(this.bounds, TOP + phase * FULL_CIRCLE, sweep * FULL_CIRCLE, false, this.paint);
            return;
        }
        if (this.progress > 0) {
            canvas.drawArc(this.bounds, TOP, this.progress * FULL_CIRCLE, false, this.paint);
        }
    }

}
