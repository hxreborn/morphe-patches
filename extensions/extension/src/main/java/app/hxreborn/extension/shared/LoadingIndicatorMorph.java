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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

final class LoadingIndicatorMorph {

    static final long MILLIS_PER_SHAPE = 650;

    static final long NANOS_PER_SHAPE = MILLIS_PER_SHAPE * 1_000_000L;

    private static final float INDICATOR_SIZE_RATIO = 38f / 48f;

    private static final float CONSTANT_ROTATION_PER_SHAPE_DEGREES = 50f;

    private static final float EXTRA_ROTATION_PER_SHAPE_DEGREES = 90f;

    private static final double SPRING_STIFFNESS = 200;

    private static final double SPRING_DAMPING_RATIO = 0.6;

    private static final double SPRING_VALUE_THRESHOLD = 0.01 * 0.75;

    private static final double SPRING_VELOCITY_THRESHOLD = SPRING_VALUE_THRESHOLD * 62.5;

    private static final double NANOS_PER_SECOND = 1e9;

    private static final float DETERMINATE_DEGREES_PER_PROGRESS = -180f;

    private static final float START_ANGLE_DEGREES = -90f;

    private static final float FULL_TURN_DEGREES = 360f;

    private static final int START_ANCHOR = 0;

    private static final int FIRST_CONTROL = 1;

    private static final int SECOND_CONTROL = 2;

    private static final int END_ANCHOR = 3;

    private static final int AXES = 2;

    private static float[][] morphs;

    private final Path path = new Path();

    private int morphTarget;

    private float morphFactor;

    private double morphVelocity;

    private long springFrameNanos;

    private float rotationDegrees;

    void reset() {
        this.morphTarget = 1;
        this.morphFactor = 0f;
        this.morphVelocity = 0;
        this.springFrameNanos = 0;
        this.rotationDegrees = 0f;
    }

    void advance(int morphTarget, float timeFraction, long frameNanos) {
        this.morphTarget = morphTarget;
        stepSpring(frameNanos);
        final float shapeBase = this.morphTarget - 1;
        this.rotationDegrees = ((CONSTANT_ROTATION_PER_SHAPE_DEGREES + EXTRA_ROTATION_PER_SHAPE_DEGREES) * shapeBase
                + CONSTANT_ROTATION_PER_SHAPE_DEGREES * timeFraction
                + EXTRA_ROTATION_PER_SHAPE_DEGREES * (this.morphFactor - shapeBase)) % FULL_TURN_DEGREES;
    }

    void draw(Canvas canvas, Paint paint, float centerX, float centerY, float radius, boolean indeterminate,
            float progress) {
        if (morphs == null) {
            morphs = LoadingIndicatorShapes.decodeMorphs();
        }
        final float[] morph;
        final float morphFraction;
        final float rotation;
        if (indeterminate) {
            final int shapeIndex = (int) Math.floor(this.morphFactor);
            final int shapeCount = LoadingIndicatorShapes.INDETERMINATE_CUBIC_COUNTS.length;
            morph = morphs[((shapeIndex % shapeCount) + shapeCount) % shapeCount];
            morphFraction = this.morphFactor - shapeIndex;
            rotation = this.rotationDegrees;
        } else {
            morph = morphs[LoadingIndicatorShapes.DETERMINATE_MORPH_INDEX];
            morphFraction = Math.max(0f, Math.min(1f, progress));
            rotation = progress * DETERMINATE_DEGREES_PER_PROGRESS;
        }
        buildPath(morph, morphFraction, radius * INDICATOR_SIZE_RATIO);
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(START_ANGLE_DEGREES + rotation);
        canvas.drawPath(this.path, paint);
        canvas.restore();
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
