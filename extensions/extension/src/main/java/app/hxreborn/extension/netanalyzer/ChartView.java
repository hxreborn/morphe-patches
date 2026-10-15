/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

final class ChartView extends View {

    private static final float LABEL_TEXT_DP = 10f;

    private static final float LINE_WIDTH_DP = 1f;

    private static final float PLOT_WIDTH_DP = 2f;

    private static final float CIRCLE_RADIUS_DP = 1f;

    private static final float LIMIT_WIDTH_DP = 2f;

    private static final float MIN_OFFSET_DP = 15f;

    private static final float AXIS_LABEL_GAP_DP = 5f;

    private static final float X_LABEL_GAP_DP = 4f;

    private static final float BAR_WIDTH = 0.85f;

    private static final float LABEL_BASELINE_DIVISOR = 2.5f;

    private static final String HEIGHT_SAMPLE = "A";

    private static final int DECADE = 10;

    final Axis xAxis = new Axis();

    final Axis yAxis = new Axis();

    private final Paint gridPaint = new Paint();

    private final Paint axisPaint = new Paint();

    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint plotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint barPaint = new Paint();

    private final Paint limitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path path = new Path();

    private final Rect textBounds = new Rect();

    private final List<float[]> line = new ArrayList<>();

    private final List<Bar> bars = new ArrayList<>();

    private int plotColor;

    private double limit = Double.NaN;

    ChartView(Context context, int gridColor, int labelColor) {
        super(context);
        this.gridPaint.setColor(gridColor);
        this.gridPaint.setStrokeWidth(dp(LINE_WIDTH_DP));
        this.axisPaint.setColor(gridColor);
        this.axisPaint.setStrokeWidth(dp(LINE_WIDTH_DP));
        this.labelPaint.setColor(labelColor);
        this.labelPaint.setTextSize(dp(LABEL_TEXT_DP));
        this.plotPaint.setStrokeWidth(dp(PLOT_WIDTH_DP));
        this.limitPaint.setStyle(Paint.Style.STROKE);
        this.limitPaint.setStrokeWidth(dp(LIMIT_WIDTH_DP));
    }

    void setLine(List<float[]> points, int color) {
        this.line.clear();
        this.line.addAll(points);
        this.plotColor = color;
        invalidate();
    }

    void setBars(List<Bar> bars) {
        this.bars.clear();
        this.bars.addAll(bars);
        invalidate();
    }

    void setLimit(double value, int color) {
        this.limit = value;
        this.limitPaint.setColor(color);
        invalidate();
    }

    void clearLimit() {
        this.limit = Double.NaN;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final double[] yEntries = this.yAxis.entries();
        final double[] xEntries = this.xAxis.entries();
        final float left = Math.max(dp(MIN_OFFSET_DP), 2 * dp(AXIS_LABEL_GAP_DP) + longestLabelWidth(yEntries));
        final float top = dp(MIN_OFFSET_DP);
        final float right = getWidth() - dp(MIN_OFFSET_DP);
        final float bottom = getHeight() - Math.max(dp(MIN_OFFSET_DP), dp(X_LABEL_GAP_DP) + labelHeight(xEntries));
        if (right <= left || bottom <= top) {
            return;
        }

        for (double entry : yEntries) {
            final float y = y(entry, top, bottom);
            canvas.drawLine(left, y, right, y, this.gridPaint);
        }
        if (this.xAxis.gridEnabled) {
            for (double entry : xEntries) {
                final float x = x(entry, left, right);
                canvas.drawLine(x, top, x, bottom, this.gridPaint);
            }
        }

        canvas.save();
        canvas.clipRect(left, top, right, bottom);
        drawBars(canvas, left, top, right, bottom);
        drawLine(canvas, left, top, right, bottom);
        canvas.restore();

        if (this.yAxis.axisLineEnabled) {
            canvas.drawLine(left, top, left, bottom, this.axisPaint);
        }
        if (this.xAxis.axisLineEnabled) {
            canvas.drawLine(left, bottom, right, bottom, this.axisPaint);
        }
        if (!Double.isNaN(this.limit)) {
            final float y = y(this.limit, top, bottom);
            canvas.drawLine(left, y, right, y, this.limitPaint);
        }

        final float labelOffset = textHeight(HEIGHT_SAMPLE) / LABEL_BASELINE_DIVISOR;
        this.labelPaint.setTextAlign(Paint.Align.RIGHT);
        for (double entry : yEntries) {
            canvas.drawText(this.yAxis.format(entry), left - dp(AXIS_LABEL_GAP_DP), y(entry, top, bottom) + labelOffset,
                    this.labelPaint);
        }
        this.labelPaint.setTextAlign(Paint.Align.CENTER);
        final float labelTop = bottom + dp(X_LABEL_GAP_DP) - this.labelPaint.ascent();
        for (double entry : xEntries) {
            canvas.drawText(this.xAxis.format(entry), x(entry, left, right), labelTop, this.labelPaint);
        }
    }

    private void drawLine(Canvas canvas, float left, float top, float right, float bottom) {
        if (this.line.isEmpty()) {
            return;
        }
        this.path.reset();
        for (int i = 0; i < this.line.size(); i++) {
            final float[] point = this.line.get(i);
            final float x = x(point[0], left, right);
            final float y = y(point[1], top, bottom);
            if (i == 0) {
                this.path.moveTo(x, y);
            } else {
                this.path.lineTo(x, y);
            }
        }
        this.plotPaint.setColor(this.plotColor);
        this.plotPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(this.path, this.plotPaint);
        this.plotPaint.setStyle(Paint.Style.FILL);
        for (float[] point : this.line) {
            canvas.drawCircle(x(point[0], left, right), y(point[1], top, bottom), dp(CIRCLE_RADIUS_DP), this.plotPaint);
        }
    }

    private void drawBars(Canvas canvas, float left, float top, float right, float bottom) {
        for (Bar bar : this.bars) {
            final float start = x(bar.x - BAR_WIDTH / 2, left, right);
            final float end = x(bar.x + BAR_WIDTH / 2, left, right);
            double base = 0;
            for (int i = 0; i < bar.values.length; i++) {
                this.barPaint.setColor(bar.colors[i]);
                canvas.drawRect(start, y(base + bar.values[i], top, bottom), end, y(base, top, bottom), this.barPaint);
                base += bar.values[i];
            }
        }
    }

    private float x(double value, float left, float right) {
        return (float) (left + (value - this.xAxis.min) / (this.xAxis.max - this.xAxis.min) * (right - left));
    }

    private float y(double value, float top, float bottom) {
        return (float) (bottom - (value - this.yAxis.min) / (this.yAxis.max - this.yAxis.min) * (bottom - top));
    }

    private float longestLabelWidth(double[] entries) {
        float width = 0;
        for (double entry : entries) {
            width = Math.max(width, this.labelPaint.measureText(this.yAxis.format(entry)));
        }
        return width;
    }

    private float labelHeight(double[] entries) {
        String longest = "";
        for (double entry : entries) {
            final String label = this.xAxis.format(entry);
            if (label.length() > longest.length()) {
                longest = label;
            }
        }
        return textHeight(longest);
    }

    private float textHeight(String text) {
        this.labelPaint.getTextBounds(text, 0, text.length(), this.textBounds);
        return this.textBounds.height();
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private static float roundToNextSignificant(double number) {
        if (Double.isInfinite(number) || Double.isNaN(number) || number == 0) {
            return 0;
        }
        final float digits = (float) Math.ceil((float) Math.log10(Math.abs(number)));
        final float magnitude = (float) Math.pow(DECADE, 1 - (int) digits);
        return Math.round(number * magnitude) / magnitude;
    }

    static final class Bar {

        final double x;

        final double[] values;

        final int[] colors;

        Bar(double x, double[] values, int[] colors) {
            this.x = x;
            this.values = values;
            this.colors = colors;
        }

    }

    interface Formatter {

        String format(double value);

    }

    static final class Axis {

        private static final int SIGNIFICANT_DIGIT_LIMIT = 5;

        double min;

        double max;

        int labelCount;

        boolean forced;

        double granularity;

        boolean gridEnabled = true;

        boolean axisLineEnabled = true;

        Formatter formatter = String::valueOf;

        void setRange(double min, double max) {
            this.min = min;
            this.max = max;
        }

        void setLabelCount(int count, boolean force) {
            this.labelCount = count;
            this.forced = force;
        }

        String format(double value) {
            return this.formatter.format(value);
        }

        double[] entries() {
            final double range = Math.abs(this.max - this.min);
            if (this.labelCount == 0 || range <= 0 || Double.isInfinite(range)) {
                return new double[0];
            }
            if (this.forced) {
                final double interval = range / (this.labelCount - 1);
                final double[] entries = new double[this.labelCount];
                for (int i = 0; i < entries.length; i++) {
                    entries[i] = this.min + interval * i;
                }
                return entries;
            }
            double interval = roundToNextSignificant(range / this.labelCount);
            interval = Math.max(interval, this.granularity);
            final double magnitude = roundToNextSignificant(Math.pow(DECADE, (int) Math.log10(interval)));
            if ((int) (interval / magnitude) > SIGNIFICANT_DIGIT_LIMIT) {
                interval = Math.floor(DECADE * magnitude);
            }
            if (interval == 0) {
                return new double[0];
            }
            final double first = Math.ceil(this.min / interval) * interval;
            final double last = Math.nextUp(Math.floor(this.max / interval) * interval);
            final List<Double> values = new ArrayList<>();
            for (double value = first; value <= last; value += interval) {
                values.add(value);
            }
            final double[] entries = new double[values.size()];
            for (int i = 0; i < entries.length; i++) {
                entries[i] = values.get(i);
            }
            return entries;
        }

    }

}
