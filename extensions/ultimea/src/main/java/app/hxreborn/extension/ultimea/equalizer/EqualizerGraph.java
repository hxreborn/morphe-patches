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

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;

import app.hxreborn.extension.ultimea.HostStyle;

@SuppressLint("ViewConstructor")
final class EqualizerGraph extends ViewGroup {

    private static final int AXIS_DP = 32;

    private static final float MAX_TOP_HEIGHT_FRACTION = .2f;

    private static final float MAX_BOTTOM_HEIGHT_FRACTION = .35f;

    private static final float BASS_CENTER = .2f;

    private static final float MIDRANGE_CENTER = .55f;

    private static final float TREBLE_CENTER = .85f;

    private static final float BASS_END = .4f;

    private static final float MIDRANGE_END = .7f;

    private static final int RIGHT_INSET_DP = 8;

    private static final float BAR_WIDTH_FRACTION = 0.75f;

    private static final int GROUP_INSET_DP = 4;

    private static final int TOP_DP = 40;

    private static final int BOTTOM_DP = 56;

    private static final int LABEL_SP = HostStyle.CAPTION_SP;

    private static final int MAX_LABEL_DP = HostStyle.CAPTION_SP;

    private static final float GRIP_HALF_WIDTH_FRACTION = 1f / 3f;

    private static final float GRIP_OFFSET_FRACTION = 1f / 6f;

    private static final int TRACK_DP = 2;

    private static final int GRIP_DP = 1;

    private static final int MIN_HEIGHT_DP = 320;

    private static final int VALUE_PADDING_DP = 4;

    private static final int FREQUENCY_INSET_DP = 8;

    private static final int KILOHERTZ = 1000;

    private static final int TENTHS_PER_DB = 10;

    private final HostStyle style;

    private final EqualizerBandListener listener;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path response = new Path();

    private final RectF thumb = new RectF();

    private final Drawable bandThumb;

    private int selected = EqualizerCurve.BAND_COUNT / 2;

    private boolean hasCurve;

    EqualizerGraph(Context context, HostStyle style, EqualizerBandListener listener) {
        super(context);
        this.style = style;
        this.listener = listener;
        this.bandThumb = new BandThumb(dp(GRIP_DP), style.pillInk);
        setWillNotDraw(false);
        setMinimumHeight(dp(MIN_HEIGHT_DP));
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            addView(new Band(context, band));
        }
    }

    void show(EqualizerCurve curve, boolean enabled) {
        this.hasCurve = curve != null;
        for (int band = 0; band < getChildCount(); band++) {
            Band slider = (Band) getChildAt(band);
            slider.setEnabled(enabled);
            slider.setProgress((curve != null) ? curve.gain(band) - EqualizerCurve.MIN_GAIN_TENTHS
                    : -EqualizerCurve.MIN_GAIN_TENTHS);
            slider.setContentDescription(EqualizerCurve.frequency(band) + " Hz, "
                    + ((curve != null) ? gainLabel(curve.gain(band)) + " dB" : "not connected"));
            slider.invalidate();
        }
        invalidate();
    }

    int selectedBand() {
        return this.selected;
    }

    void select(int band) {
        this.selected = Math.max(0, Math.min(EqualizerCurve.BAND_COUNT - 1, band));
        this.listener.onSelected(this.selected);
        for (int index = 0; index < getChildCount(); index++) {
            getChildAt(index).invalidate();
        }
        invalidate();
    }

    private float trackTop() {
        return Math.min(dp(TOP_DP), getHeight() * MAX_TOP_HEIGHT_FRACTION);
    }

    private float trackBottom() {
        return Math.max(trackTop(), getHeight() - Math.min(dp(BOTTOM_DP), getHeight() * MAX_BOTTOM_HEIGHT_FRACTION));
    }

    private float gainY(int gain) {
        return trackTop() + (EqualizerCurve.MAX_GAIN_TENTHS - gain) * (trackBottom() - trackTop())
                / (EqualizerCurve.MAX_GAIN_TENTHS - EqualizerCurve.MIN_GAIN_TENTHS);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = resolveSize(dp(MIN_HEIGHT_DP), heightMeasureSpec);
        setMeasuredDimension(width, height);
        int bandWidth = Math.max(1, (width - dp(AXIS_DP + RIGHT_INSET_DP)) / EqualizerCurve.BAND_COUNT);
        for (int band = 0; band < getChildCount(); band++) {
            getChildAt(band).measure(MeasureSpec.makeMeasureSpec(bandWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int inset = dp(AXIS_DP);
        int available = getWidth() - inset - dp(RIGHT_INSET_DP);
        for (int band = 0; band < getChildCount(); band++) {
            int bandLeft = inset + available * band / EqualizerCurve.BAND_COUNT;
            int bandRight = inset + available * (band + 1) / EqualizerCurve.BAND_COUNT;
            getChildAt(band).layout(bandLeft, 0, bandRight, getHeight());
        }
    }

    @Override
    @SuppressLint("DrawAllocation")
    @SuppressWarnings("NullableProblems")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(this.style.secondaryText);
        this.paint.setTextSize(labelSize());
        this.paint.setTextAlign(Paint.Align.LEFT);
        for (int gain : new int[] { EqualizerCurve.MAX_GAIN_TENTHS, 0, EqualizerCurve.MIN_GAIN_TENTHS }) {
            String label = ((gain > 0) ? "+" : "") + gain / TENTHS_PER_DB + "dB";
            canvas.drawText(label, 0, gainY(gain) + labelSize() / 2, this.paint);
        }
        this.paint.setTextAlign(Paint.Align.CENTER);
        int available = getWidth() - dp(AXIS_DP + RIGHT_INSET_DP);
        float start = dp(AXIS_DP);
        float groupY = getHeight() - dp(GROUP_INSET_DP);
        canvas.drawText("BASS", start + available * BASS_CENTER, groupY, this.paint);
        canvas.drawText("MIDRANGE", start + available * MIDRANGE_CENTER, groupY, this.paint);
        canvas.drawText("TREBLE", start + available * TREBLE_CENTER, groupY, this.paint);
        canvas.drawText("|", start + available * BASS_END, groupY, this.paint);
        canvas.drawText("|", start + available * MIDRANGE_END, groupY, this.paint);
    }

    static String gainLabel(int gain) {
        return (gain != 0) ? String.format(Locale.ROOT, "%+.1f", gain / (double) TENTHS_PER_DB).replace("-", "−")
                : "0.0";
    }

    private float labelSize() {
        float labelSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, LABEL_SP,
                getResources().getDisplayMetrics());
        return Math.min(labelSize, dp(MAX_LABEL_DP));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class BandThumb extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final int gripColor;

        BandThumb(int strokeWidth, int gripColor) {
            this.paint.setStrokeWidth(strokeWidth);
            this.gripColor = gripColor;
        }

        @Override
        public void draw(Canvas canvas) {
            float x = getBounds().exactCenterX();
            float y = getBounds().exactCenterY();
            float radius = getBounds().width() / 2f;
            this.paint.setColor(android.graphics.Color.WHITE);
            this.paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, radius, this.paint);
            this.paint.setColor(this.gripColor);
            float left = x - radius * GRIP_HALF_WIDTH_FRACTION;
            float right = x + radius * GRIP_HALF_WIDTH_FRACTION;
            float offset = radius * GRIP_OFFSET_FRACTION;
            canvas.drawLine(left, y - offset, right, y - offset, this.paint);
            canvas.drawLine(left, y + offset, right, y + offset, this.paint);
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

    private final class Band extends SeekBar {

        private final int band;

        private boolean dragging;

        private boolean changing;

        private float dragStartY;

        private int dragStartGain;

        Band(Context context, int band) {
            super(context);
            this.band = band;
            setMax(EqualizerCurve.MAX_GAIN_TENTHS - EqualizerCurve.MIN_GAIN_TENTHS);
            setBackground(null);
            setPadding(0, 0, 0, 0);
            setFocusable(true);
            setOnFocusChangeListener((view, focused) -> {
                if (focused) {
                    select(this.band);
                }
            });
        }

        @Override
        protected synchronized void onDraw(Canvas canvas) {
            Paint pen = EqualizerGraph.this.paint;
            float x = getWidth() / 2f;
            float radius = getWidth() * BAR_WIDTH_FRACTION / 2;
            int gain = getProgress() + EqualizerCurve.MIN_GAIN_TENTHS;
            boolean active = isEnabled() && this.band == EqualizerGraph.this.selected;
            EqualizerGraph.this.thumb.set(x - radius, trackTop(), x + radius, trackBottom());
            EqualizerGraph.this.response.reset();
            EqualizerGraph.this.response.addRoundRect(EqualizerGraph.this.thumb, radius, radius, Path.Direction.CW);
            pen.setStyle(Paint.Style.FILL);
            pen.setColor(EqualizerGraph.this.style.barTrack);
            canvas.drawPath(EqualizerGraph.this.response, pen);
            if (EqualizerGraph.this.hasCurve) {
                int saved = canvas.save();
                canvas.clipPath(EqualizerGraph.this.response);
                pen.setColor(active ? EqualizerGraph.this.style.accent
                        : isEnabled() ? EqualizerGraph.this.style.selectedSurface : EqualizerGraph.this.style.barFill);
                canvas.drawRect(x - radius, gainY(gain), x + radius, trackBottom(), pen);
                canvas.restoreToCount(saved);
            }
            if (isEnabled() && EqualizerGraph.this.hasCurve) {
                float center = gainY(gain);
                EqualizerGraph.this.bandThumb.setBounds(Math.round(x - radius), Math.round(center - radius),
                        Math.round(x + radius), Math.round(center + radius));
                EqualizerGraph.this.bandThumb.draw(canvas);
            }
            if (active) {
                pen.setStyle(Paint.Style.STROKE);
                pen.setStrokeWidth(dp(TRACK_DP));
                pen.setColor(EqualizerGraph.this.style.accent);
                canvas.drawCircle(x, gainY(gain), radius + dp(TRACK_DP), pen);
                pen.setStyle(Paint.Style.FILL);
            }
            pen.setTextSize(labelSize());
            pen.setTextAlign(Paint.Align.CENTER);
            pen.setColor(active ? EqualizerGraph.this.style.accent : EqualizerGraph.this.style.secondaryText);
            if (EqualizerGraph.this.hasCurve) {
                canvas.drawText(gainLabel(gain), x, labelSize() + dp(VALUE_PADDING_DP), pen);
            }
            int frequency = EqualizerCurve.frequency(this.band);
            String label = (frequency >= KILOHERTZ) ? frequency / KILOHERTZ + "k" : String.valueOf(frequency);
            canvas.drawText(label, x, getHeight() - dp(FREQUENCY_INSET_DP + LABEL_SP), pen);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isEnabled()) {
                return false;
            }
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                select(this.band);
                requestFocus();
                this.dragging = true;
                this.changing = false;
                this.dragStartY = event.getY();
                this.dragStartGain = getProgress() + EqualizerCurve.MIN_GAIN_TENTHS;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            if (!this.dragging) {
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
                float distance = event.getY() - this.dragStartY;
                if (!this.changing && Math.abs(distance) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    this.changing = true;
                    EqualizerGraph.this.listener.onStarted();
                }
                if (this.changing && trackBottom() > trackTop()) {
                    float fraction = distance / (trackBottom() - trackTop());
                    int gain = Math.round(this.dragStartGain
                            - fraction * (EqualizerCurve.MAX_GAIN_TENTHS - EqualizerCurve.MIN_GAIN_TENTHS));
                    change(gain);
                }
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                this.dragging = false;
                EqualizerGraph.this.listener.onFinished();
                getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            select(this.band);
            return true;
        }

        private void change(int gain) {
            int bounded = Math.max(EqualizerCurve.MIN_GAIN_TENTHS, Math.min(EqualizerCurve.MAX_GAIN_TENTHS, gain));
            if (bounded != getProgress() + EqualizerCurve.MIN_GAIN_TENTHS) {
                EqualizerGraph.this.listener.onChanged(this.band, bounded);
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,
                    EqualizerCurve.MIN_GAIN_TENTHS / (float) TENTHS_PER_DB,
                    EqualizerCurve.MAX_GAIN_TENTHS / (float) TENTHS_PER_DB,
                    (getProgress() + EqualizerCurve.MIN_GAIN_TENTHS) / (float) TENTHS_PER_DB));
            info.setSelected(this.band == EqualizerGraph.this.selected);
        }

        @Override
        public boolean performAccessibilityAction(int action, Bundle arguments) {
            if (!isEnabled()) {
                return false;
            }
            select(this.band);
            int gain = getProgress() + EqualizerCurve.MIN_GAIN_TENTHS;
            if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                gain++;
            } else if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
                gain--;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()
                    && arguments != null) {
                gain = Math
                    .round(arguments.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE) * TENTHS_PER_DB);
            } else {
                return super.performAccessibilityAction(action, arguments);
            }
            EqualizerGraph.this.listener.onStarted();
            change(gain);
            EqualizerGraph.this.listener.onFinished();
            return true;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (isEnabled() && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                int target = Math.max(0,
                        Math.min(getChildCount() - 1, this.band + ((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) ? 1 : -1)));
                getChildAt(target).requestFocus();
                select(target);
                return true;
            }
            boolean increase = keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_PLUS
                    || keyCode == KeyEvent.KEYCODE_EQUALS;
            boolean decrease = keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_MINUS;
            if (isEnabled() && (increase || decrease)) {
                select(this.band);
                EqualizerGraph.this.listener.onStarted();
                change(getProgress() + EqualizerCurve.MIN_GAIN_TENTHS + (increase ? 1 : -1));
                EqualizerGraph.this.listener.onFinished();
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }

    }

}
