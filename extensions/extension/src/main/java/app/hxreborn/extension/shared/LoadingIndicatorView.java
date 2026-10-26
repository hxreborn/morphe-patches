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
 */
package app.hxreborn.extension.shared;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public final class LoadingIndicatorView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final LoadingIndicatorMorph morph = new LoadingIndicatorMorph();

    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

    private boolean indeterminate;

    private float progress;

    private int morphTarget;

    public LoadingIndicatorView(Context context) {
        super(context);
        this.paint.setStyle(Paint.Style.FILL);
        this.animator.setDuration(LoadingIndicatorMorph.MILLIS_PER_SHAPE);
        this.animator.setInterpolator(null);
        this.animator.setRepeatCount(ValueAnimator.INFINITE);
        this.animator.addUpdateListener((animation) -> onAnimationFrame(animation.getAnimatedFraction()));
        this.animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                LoadingIndicatorView.this.morphTarget++;
            }
        });
    }

    public void setIndicatorColor(int color) {
        if (this.paint.getColor() != color) {
            this.paint.setColor(color);
            invalidate();
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        if (this.indeterminate != indeterminate) {
            this.indeterminate = indeterminate;
            updateAnimator();
            invalidate();
        }
    }

    public void setProgress(float progress) {
        if (this.progress != progress) {
            this.progress = progress;
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimator();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float radius = Math.min(getWidth(), getHeight()) / 2f;
        this.morph.draw(canvas, this.paint, getWidth() / 2f, getHeight() / 2f, radius, this.indeterminate,
                this.progress);
    }

    private void updateAnimator() {
        final boolean running = this.indeterminate && isAttachedToWindow();
        if (running && !this.animator.isStarted()) {
            this.morphTarget = 1;
            this.morph.reset();
            this.animator.start();
        } else if (!running && this.animator.isStarted()) {
            this.animator.cancel();
        }
    }

    private void onAnimationFrame(float animationFraction) {
        final float timeFraction = (animationFraction == 1f) ? 0f : animationFraction;
        this.morph.advance(this.morphTarget, timeFraction, System.nanoTime());
        invalidate();
    }

}
