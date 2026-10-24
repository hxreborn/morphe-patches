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
import android.graphics.Path;
import android.view.View;

public final class LoadingIndicatorView extends View {

    private static final long DURATION_PER_SHAPE_MS = 650;

    private static final float CONSTANT_ROTATION_PER_SHAPE_DEGREES = 50f;

    private static final float EXTRA_ROTATION_PER_SHAPE_DEGREES = 90f;

    private static final double SPRING_STIFFNESS = 200;

    private static final double SPRING_DAMPING_RATIO = 0.6;

    private static final double SPRING_VALUE_THRESHOLD = 0.01 * 0.75;

    private static final double SPRING_VELOCITY_THRESHOLD = SPRING_VALUE_THRESHOLD * 62.5;

    private static final double NANOS_PER_SECOND = 1e9;

    private static final float INDICATOR_SIZE_RATIO = 38f / 48f;

    private static final float DETERMINATE_DEGREES_PER_PROGRESS = -180f;

    private static final float START_ANGLE_DEGREES = -90f;

    private static final float FULL_TURN_DEGREES = 360f;

    private static final int START_ANCHOR = 0;

    private static final int FIRST_CONTROL = 1;

    private static final int SECOND_CONTROL = 2;

    private static final int END_ANCHOR = 3;

    private static final int AXES = 2;

    private static float[][] morphs;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path path = new Path();

    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

    private boolean indeterminate;

    private float progress;

    private int morphTarget;

    private float morphFactor;

    private double morphVelocity;

    private long springFrameNanos;

    private float rotationDegrees;

    public LoadingIndicatorView(Context context) {
        super(context);
        this.paint.setStyle(Paint.Style.FILL);
        this.animator.setDuration(DURATION_PER_SHAPE_MS);
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
        if (morphs == null) {
            morphs = LoadingIndicatorShapes.decodeMorphs();
        }
        final float[] morph;
        final float morphFraction;
        final float rotation;
        if (this.indeterminate) {
            final int shapeIndex = (int) Math.floor(this.morphFactor);
            final int shapeCount = LoadingIndicatorShapes.INDETERMINATE_CUBIC_COUNTS.length;
            morph = morphs[((shapeIndex % shapeCount) + shapeCount) % shapeCount];
            morphFraction = this.morphFactor - shapeIndex;
            rotation = this.rotationDegrees;
        } else {
            morph = morphs[LoadingIndicatorShapes.DETERMINATE_MORPH_INDEX];
            morphFraction = Math.max(0f, Math.min(1f, this.progress));
            rotation = this.progress * DETERMINATE_DEGREES_PER_PROGRESS;
        }
        final float radius = Math.min(getWidth(), getHeight()) / 2f * INDICATOR_SIZE_RATIO;
        buildPath(morph, morphFraction, radius);
        canvas.save();
        canvas.translate(getWidth() / 2f, getHeight() / 2f);
        canvas.rotate(START_ANGLE_DEGREES + rotation);
        canvas.drawPath(this.path, this.paint);
        canvas.restore();
    }

    private void updateAnimator() {
        final boolean running = this.indeterminate && isAttachedToWindow();
        if (running && !this.animator.isStarted()) {
            this.morphTarget = 1;
            this.morphFactor = 0f;
            this.morphVelocity = 0;
            this.springFrameNanos = 0;
            this.rotationDegrees = 0f;
            this.animator.start();
        } else if (!running && this.animator.isStarted()) {
            this.animator.cancel();
        }
    }

    private void onAnimationFrame(float animationFraction) {
        stepSpring(System.nanoTime());
        final float shapeBase = this.morphTarget - 1;
        final float timeFraction = (animationFraction == 1f) ? 0f : animationFraction;
        this.rotationDegrees = ((CONSTANT_ROTATION_PER_SHAPE_DEGREES + EXTRA_ROTATION_PER_SHAPE_DEGREES) * shapeBase
                + CONSTANT_ROTATION_PER_SHAPE_DEGREES * timeFraction
                + EXTRA_ROTATION_PER_SHAPE_DEGREES * (this.morphFactor - shapeBase)) % FULL_TURN_DEGREES;
        invalidate();
    }

    private void stepSpring(long frameNanos) {
        final double elapsedSeconds = (this.springFrameNanos == 0) ? 0
                : (frameNanos - this.springFrameNanos) / NANOS_PER_SECOND;
        this.springFrameNanos = frameNanos;
        final double naturalFrequency = Math.sqrt(SPRING_STIFFNESS);
        final double dampedFrequency = naturalFrequency * Math.sqrt(1 - SPRING_DAMPING_RATIO * SPRING_DAMPING_RATIO);
        final double decayRate = SPRING_DAMPING_RATIO * naturalFrequency;
        final double displacement = this.morphFactor - this.morphTarget;
        final double sineCoefficient = (this.morphVelocity + decayRate * displacement) / dampedFrequency;
        final double decay = Math.exp(-decayRate * elapsedSeconds);
        final double cosine = Math.cos(dampedFrequency * elapsedSeconds);
        final double sine = Math.sin(dampedFrequency * elapsedSeconds);
        double nextDisplacement = decay * (displacement * cosine + sineCoefficient * sine);
        double nextVelocity = -decayRate * nextDisplacement
                + decay * dampedFrequency * (sineCoefficient * cosine - displacement * sine);
        if (Math.abs(nextDisplacement) < SPRING_VALUE_THRESHOLD && Math.abs(nextVelocity) < SPRING_VELOCITY_THRESHOLD) {
            nextDisplacement = 0;
            nextVelocity = 0;
        }
        this.morphFactor = (float) (this.morphTarget + nextDisplacement);
        this.morphVelocity = nextVelocity;
    }

    private void buildPath(float[] morph, float fraction, float scale) {
        this.path.rewind();
        final int cubics = morph.length / LoadingIndicatorShapes.FLOATS_PER_CUBIC;
        this.path.moveTo(coordinate(morph, 0, START_ANCHOR, 0, fraction, scale),
                coordinate(morph, 0, START_ANCHOR, 1, fraction, scale));
        for (int cubic = 0; cubic < cubics; cubic++) {
            final boolean last = cubic == cubics - 1;
            final int endCubic = (last) ? 0 : cubic;
            final int endAnchor = (last) ? START_ANCHOR : END_ANCHOR;
            this.path.cubicTo(coordinate(morph, cubic, FIRST_CONTROL, 0, fraction, scale),
                    coordinate(morph, cubic, FIRST_CONTROL, 1, fraction, scale),
                    coordinate(morph, cubic, SECOND_CONTROL, 0, fraction, scale),
                    coordinate(morph, cubic, SECOND_CONTROL, 1, fraction, scale),
                    coordinate(morph, endCubic, endAnchor, 0, fraction, scale),
                    coordinate(morph, endCubic, endAnchor, 1, fraction, scale));
        }
        this.path.close();
    }

    private static float coordinate(float[] morph, int cubic, int point, int axis, float fraction, float scale) {
        final int index = cubic * LoadingIndicatorShapes.FLOATS_PER_CUBIC + (point * AXES + axis) * 2;
        final float from = morph[index];
        final float to = morph[index + 1];
        return (from + (to - from) * fraction) * scale;
    }

}
