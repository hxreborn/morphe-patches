/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.Switch;

public final class SwitchStyle {
    private static final int TRACK_WIDTH_DP = 52;
    private static final int TRACK_HEIGHT_DP = 32;
    private static final int TRACK_HORIZONTAL_PADDING_DP = 6;
    private static final int TRACK_OUTLINE_DP = 2;
    private static final int THUMB_WIDTH_DP = 20;
    private static final int THUMB_DIAMETER_DP = 24;
    private static final int THUMB_PRESSED_DIAMETER_DP = 28;
    private static final float ICON_STROKE_DP = 2f;
    private static final int LABEL_GAP_DP = 16;
    private static final int UNCHECKED_TRACK_ALPHA = 0x33;
    private static final float BLACK_CONTENT_LUMINANCE_THRESHOLD = 0.179f;

    private SwitchStyle() {}

    public static void apply(Switch control, int accentColor) {
        final Context context = control.getContext();
        final float density = context.getResources().getDisplayMetrics().density;
        final int uncheckedColor =
                PatchesTheme.resolveColorAttribute(context, PatchesTheme.TEXT_WEAK);
        final int uncheckedIconColor =
                PatchesTheme.resolveColorAttribute(context, PatchesTheme.BACKGROUND_SECONDARY);

        control.setSplitTrack(false);
        control.setSwitchMinWidth(PatchesTheme.dpToPx(context, TRACK_WIDTH_DP));
        control.setSwitchPadding(PatchesTheme.dpToPx(context, LABEL_GAP_DP));
        control.setTrackDrawable(new Track(density, accentColor, uncheckedColor));
        control.setThumbDrawable(
                new Thumb(density, accentColor, uncheckedColor, uncheckedIconColor));
        control.refreshDrawableState();
    }

    private static int contentColorOn(int background) {
        return relativeLuminance(background) > BLACK_CONTENT_LUMINANCE_THRESHOLD
                ? Color.BLACK
                : Color.WHITE;
    }

    private static float relativeLuminance(int color) {
        return 0.2126f * linearChannel(Color.red(color))
                + 0.7152f * linearChannel(Color.green(color))
                + 0.0722f * linearChannel(Color.blue(color));
    }

    private static float linearChannel(int channel) {
        final float value = channel / 255f;
        return value <= 0.03928f
                ? value / 12.92f
                : (float) Math.pow((value + 0.055f) / 1.055f, 2.4f);
    }

    private abstract static class StatefulDrawable extends Drawable {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float density;
        final int accentColor;
        final int uncheckedColor;
        boolean checked;
        boolean pressed;

        StatefulDrawable(float density, int accentColor, int uncheckedColor) {
            this.density = density;
            this.accentColor = accentColor;
            this.uncheckedColor = uncheckedColor;
        }

        final float dp(float value) {
            return value * density;
        }

        final int px(int value) {
            return Math.round(dp(value));
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean nowChecked = false;
            boolean nowPressed = false;
            for (int attribute : state) {
                if (attribute == android.R.attr.state_checked) nowChecked = true;
                if (attribute == android.R.attr.state_pressed) nowPressed = true;
            }

            final boolean changed = nowChecked != checked || nowPressed != pressed;
            checked = nowChecked;
            pressed = nowPressed;
            return changed;
        }

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(ColorFilter colorFilter) {}

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final class Track extends StatefulDrawable {
        private final RectF trackRect = new RectF();
        private final int uncheckedFillColor;

        Track(float density, int accentColor, int uncheckedColor) {
            super(density, accentColor, uncheckedColor);
            uncheckedFillColor = Color.argb(UNCHECKED_TRACK_ALPHA, Color.red(uncheckedColor),
                    Color.green(uncheckedColor), Color.blue(uncheckedColor));
        }

        @Override
        public int getIntrinsicHeight() {
            return px(TRACK_HEIGHT_DP);
        }

        @Override
        public boolean getPadding(Rect padding) {
            padding.set(px(TRACK_HORIZONTAL_PADDING_DP), 0, px(TRACK_HORIZONTAL_PADDING_DP), 0);
            return true;
        }

        @Override
        public void draw(Canvas canvas) {
            trackRect.set(getBounds());
            final float radius = trackRect.height() / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(checked ? accentColor : uncheckedFillColor);
            canvas.drawRoundRect(trackRect, radius, radius, paint);
            if (checked) return;

            final float strokeWidth = dp(TRACK_OUTLINE_DP);
            final float halfStroke = strokeWidth / 2f;
            trackRect.inset(halfStroke, halfStroke);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(uncheckedColor);
            canvas.drawRoundRect(trackRect, radius - halfStroke, radius - halfStroke, paint);
        }
    }

    private static final class Thumb extends StatefulDrawable {
        private final Path icon = new Path();
        private final int uncheckedIconColor;
        private final int checkedThumbColor;

        Thumb(float density, int accentColor, int uncheckedColor, int uncheckedIconColor) {
            super(density, accentColor, uncheckedColor);
            this.uncheckedIconColor = uncheckedIconColor;
            checkedThumbColor = contentColorOn(accentColor);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        public int getIntrinsicWidth() {
            return px(THUMB_WIDTH_DP) + 2 * px(TRACK_HORIZONTAL_PADDING_DP);
        }

        @Override
        public int getIntrinsicHeight() {
            return px(TRACK_HEIGHT_DP);
        }

        @Override
        public boolean getPadding(Rect padding) {
            padding.set(px(TRACK_HORIZONTAL_PADDING_DP), 0, px(TRACK_HORIZONTAL_PADDING_DP), 0);
            return true;
        }

        @Override
        public void draw(Canvas canvas) {
            final Rect bounds = getBounds();
            final float centerX = bounds.exactCenterX();
            final float centerY = bounds.exactCenterY();
            final float radius =
                    dp(pressed ? THUMB_PRESSED_DIAMETER_DP : THUMB_DIAMETER_DP) / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(checked ? checkedThumbColor : uncheckedColor);
            canvas.drawCircle(centerX, centerY, radius, paint);

            icon.reset();
            if (checked) {
                icon.moveTo(centerX - dp(3.5f), centerY + dp(0.5f));
                icon.lineTo(centerX - dp(1f), centerY + dp(3f));
                icon.lineTo(centerX + dp(3.5f), centerY - dp(2.5f));
            } else {
                icon.moveTo(centerX - dp(3f), centerY - dp(3f));
                icon.lineTo(centerX + dp(3f), centerY + dp(3f));
                icon.moveTo(centerX + dp(3f), centerY - dp(3f));
                icon.lineTo(centerX - dp(3f), centerY + dp(3f));
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(ICON_STROKE_DP));
            paint.setColor(checked ? accentColor : uncheckedIconColor);
            canvas.drawPath(icon, paint);
        }
    }
}
