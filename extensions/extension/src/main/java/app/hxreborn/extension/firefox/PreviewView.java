/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.provider.Settings;
import android.view.View;

final class PreviewView extends View implements SettingsRows.Stateful {

    private static final long CROSSFADE_MS = 160;

    private static final float FRAME_RADIUS_DP = 12;

    private static final float SELECTED_TINT = 0.35f;

    private final Scene scene;

    private final SettingsStyle palette;

    private final TimeInterpolator interpolator;

    private final Pen pen;

    private final RectF bounds = new RectF();

    private final boolean selectable;

    private float progress;

    private boolean on;

    private ValueAnimator animator;

    PreviewView(Context context, SettingsStyle palette, TimeInterpolator interpolator, Scene scene,
            boolean selectable) {
        super(context);
        this.palette = palette;
        this.interpolator = interpolator;
        this.scene = scene;
        this.selectable = selectable;
        this.pen = new Pen(palette);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        updateDescription();
    }

    @Override
    public void setState(boolean on, boolean animate) {
        this.on = on;
        updateDescription();
        float target = on ? 1f : 0f;
        if (this.animator != null) {
            this.animator.cancel();
        }
        if (!animate || animationsDisabled(getContext())) {
            this.progress = target;
            invalidate();
            return;
        }
        this.animator = ValueAnimator.ofFloat(this.progress, target);
        this.animator.setDuration(CROSSFADE_MS);
        this.animator.setInterpolator(this.interpolator);
        this.animator.addUpdateListener((animation) -> {
            this.progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        this.animator.start();
    }

    void refresh() {
        updateDescription();
        invalidate();
    }

    private void updateDescription() {
        setContentDescription(this.scene.describe(this.on));
    }

    private float unit(float availableWidth) {
        float density = getResources().getDisplayMetrics().density;
        return Math.min(density, availableWidth / this.scene.width);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = View.MeasureSpec.getSize(widthSpec);
        float density = getResources().getDisplayMetrics().density;
        float height = this.selectable ? (width * this.scene.height / this.scene.width)
                : (this.scene.height + 2) * density;
        setMeasuredDimension(width, Math.round(height));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float unit = unit(getWidth());
        float shown = Math.max(0f, Math.min(1f, this.progress));
        this.pen.begin(canvas, unit);
        drawFrame(canvas, shown);
        float drawnWidth = this.scene.width * unit;
        canvas.save();
        canvas.translate((getWidth() - drawnWidth) / 2f, (getHeight() - this.scene.height * unit) / 2f);
        this.bounds.set(0, 0, drawnWidth, this.scene.height * unit);
        layer(canvas, 1f - shown, this.scene.stock);
        layer(canvas, shown, this.scene.patched);
        canvas.restore();
    }

    private void drawFrame(Canvas canvas, float shown) {
        float stroke = this.selectable ? (1f + shown) : 1f;
        int fill = this.selectable
                ? SettingsStyle.mix(SettingsStyle.mix(this.palette.accentContainer, this.palette.sheet, SELECTED_TINT),
                        this.palette.sheet, shown)
                : this.palette.sheet;
        int line = this.selectable ? SettingsStyle.mix(this.palette.accent, this.palette.outlineSoft, shown)
                : this.palette.outlineSoft;
        float right = getWidth() / this.pen.unit();
        float bottom = getHeight() / this.pen.unit();
        this.pen.fill(0, 0, right, bottom, FRAME_RADIUS_DP, fill);
        this.pen.outline(0, 0, right, bottom, FRAME_RADIUS_DP, line, stroke);
    }

    private void layer(Canvas canvas, float alpha, Drawing drawing) {
        if (alpha <= 0f || drawing == null) {
            return;
        }
        if (alpha >= 1f) {
            drawing.draw(this.pen);
            return;
        }
        int saved = canvas.saveLayerAlpha(this.bounds.left, this.bounds.top, this.bounds.right, this.bounds.bottom,
                Math.round(alpha * 255));
        drawing.draw(this.pen);
        canvas.restoreToCount(saved);
    }

    static boolean animationsDisabled(Context context) {
        return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE,
                1f) == 0f;
    }

    interface Drawing {

        void draw(Pen pen);

    }

    static class Scene {

        final float width;

        final float height;

        final Drawing stock;

        final Drawing patched;

        private final String stockText;

        private final String patchedText;

        Scene(float width, float height, Drawing stock, Drawing patched, String stockText, String patchedText) {
            this.width = width;
            this.height = height;
            this.stock = stock;
            this.patched = patched;
            this.stockText = stockText;
            this.patchedText = patchedText;
        }

        String describe(boolean on) {
            return on ? this.patchedText : this.stockText;
        }

    }

    static final class Pen {

        final SettingsStyle palette;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final RectF rect = new RectF();

        private final Path path = new Path();

        private Canvas canvas;

        private float unit;

        Pen(SettingsStyle palette) {
            this.palette = palette;
        }

        void begin(Canvas canvas, float unit) {
            this.canvas = canvas;
            this.unit = unit;
        }

        float unit() {
            return this.unit;
        }

        void fill(float left, float top, float right, float bottom, float radius, int color) {
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(color);
            this.rect.set(left * this.unit, top * this.unit, right * this.unit, bottom * this.unit);
            this.canvas.drawRoundRect(this.rect, radius * this.unit, radius * this.unit, this.paint);
        }

        void outline(float left, float top, float right, float bottom, float radius, int color, float width) {
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(width * this.unit);
            this.paint.setColor(color);
            float inset = width * this.unit / 2f;
            this.rect.set(left * this.unit + inset, top * this.unit + inset, right * this.unit - inset,
                    bottom * this.unit - inset);
            this.canvas.drawRoundRect(this.rect, radius * this.unit, radius * this.unit, this.paint);
        }

        void circle(float cx, float cy, float radius, int color) {
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(color);
            this.canvas.drawCircle(cx * this.unit, cy * this.unit, radius * this.unit, this.paint);
        }

        void ring(float cx, float cy, float radius, int color, float width) {
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(width * this.unit);
            this.paint.setColor(color);
            this.canvas.drawCircle(cx * this.unit, cy * this.unit, radius * this.unit, this.paint);
        }

        void arc(float cx, float cy, float radius, float start, float sweep, int color, float width) {
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(width * this.unit);
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setColor(color);
            this.rect.set((cx - radius) * this.unit, (cy - radius) * this.unit, (cx + radius) * this.unit,
                    (cy + radius) * this.unit);
            this.canvas.drawArc(this.rect, start, sweep, false, this.paint);
        }

        void line(float x1, float y1, float x2, float y2, int color, float width) {
            polyline(color, width, x1, y1, x2, y2);
        }

        void polyline(int color, float width, float... points) {
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(width * this.unit);
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
            this.paint.setColor(color);
            tracePath(points);
            this.canvas.drawPath(this.path, this.paint);
        }

        void polygon(int color, float... points) {
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(color);
            tracePath(points);
            this.path.close();
            this.canvas.drawPath(this.path, this.paint);
        }

        void skeleton(float left, float top, float width, float height, int color) {
            fill(left, top, left + width, top + height, height / 2f, color);
        }

        void text(String value, float x, float baseline, float size, int color, boolean bold) {
            prepareText(size, color, bold);
            this.paint.setTextAlign(Paint.Align.LEFT);
            this.canvas.drawText(value, x * this.unit, baseline * this.unit, this.paint);
        }

        void textEnd(String value, float x, float baseline, float size, int color, boolean bold) {
            prepareText(size, color, bold);
            this.paint.setTextAlign(Paint.Align.RIGHT);
            this.canvas.drawText(value, x * this.unit, baseline * this.unit, this.paint);
        }

        void textCenter(String value, float x, float baseline, float size, int color, boolean bold) {
            prepareText(size, color, bold);
            this.paint.setTextAlign(Paint.Align.CENTER);
            this.canvas.drawText(value, x * this.unit, baseline * this.unit, this.paint);
        }

        float measure(String value, float size) {
            this.paint.setTextSize(size * this.unit);
            this.paint.setTypeface(Typeface.DEFAULT);
            return this.paint.measureText(value) / this.unit;
        }

        private void tracePath(float[] points) {
            this.path.rewind();
            this.path.moveTo(points[0] * this.unit, points[1] * this.unit);
            for (int index = 2; index < points.length; index += 2) {
                this.path.lineTo(points[index] * this.unit, points[index + 1] * this.unit);
            }
        }

        private void prepareText(float size, int color, boolean bold) {
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(color);
            this.paint.setTextSize(size * this.unit);
            this.paint.setTypeface(bold ? Typeface.create("sans-serif-medium", Typeface.NORMAL) : Typeface.DEFAULT);
        }

    }

}
