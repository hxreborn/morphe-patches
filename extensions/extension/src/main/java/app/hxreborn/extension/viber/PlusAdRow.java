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
package app.hxreborn.extension.viber;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ReplacementSpan;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class PlusAdRow {

    private static final String PATCHES_LABEL = "hxreborn patches";

    private static final float LABEL_TEXT_SCALE = 0.75f;

    private static final int CHIP_HORIZONTAL_PADDING_DP = 6;

    private static final int CHIP_VERTICAL_PADDING_DP = 2;

    private static final int CHIP_RADIUS_DP = 6;

    private PlusAdRow() {
    }

    static int adsFreeRowId() {
        return 0;
    }

    static int adsFreeTitleViewId() {
        return 0;
    }

    static int adsFreeTitleText() {
        return 0;
    }

    public static void bind(CompoundButton toggle) {
        if (!AppliedPatches.hideAds()) {
            return;
        }
        Context context = toggle.getContext();
        View root = toggle.getRootView();
        View row = root.findViewById(adsFreeRowId());
        View titleView = root.findViewById(adsFreeTitleViewId());
        if (row == null || !(titleView instanceof TextView)) {
            Log.w(PatchSettings.LOG_TAG, "Viber Plus ad row not found");
            return;
        }
        TextView title = (TextView) titleView;
        String originalTitle = context.getString(adsFreeTitleText());
        SpannableString titleText = new SpannableString(originalTitle + "  " + PATCHES_LABEL);
        int labelStart = titleText.length() - PATCHES_LABEL.length();
        titleText.setSpan(new PatchChip(new SettingsStyle(context)), labelStart, titleText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(titleText);
        toggle.setChecked(true);
        toggle.setEnabled(false);
        toggle.setClickable(false);
        toggle.setFocusable(false);
        row.setVisibility(View.VISIBLE);
        row.setOnClickListener((view) -> context.startActivity(new Intent(context, PatchesSettingsActivity.class)));
    }

    private static final class PatchChip extends ReplacementSpan {

        private final int backgroundColor;

        private final int textColor;

        private final int horizontalPadding;

        private final int verticalPadding;

        private final int radius;

        private PatchChip(SettingsStyle style) {
            this.backgroundColor = style.cardSurface;
            this.textColor = style.primaryText;
            this.horizontalPadding = style.dp(CHIP_HORIZONTAL_PADDING_DP);
            this.verticalPadding = style.dp(CHIP_VERTICAL_PADDING_DP);
            this.radius = style.dp(CHIP_RADIUS_DP);
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fontMetrics) {
            Paint labelPaint = scaledPaint(paint);
            if (fontMetrics != null) {
                paint.getFontMetricsInt(fontMetrics);
                Paint.FontMetricsInt labelMetrics = labelPaint.getFontMetricsInt();
                float center = (fontMetrics.ascent + fontMetrics.descent) / 2f;
                float halfHeight = (labelMetrics.descent - labelMetrics.ascent) / 2f + this.verticalPadding;
                fontMetrics.ascent = Math.min(fontMetrics.ascent, (int) Math.floor(center - halfHeight));
                fontMetrics.descent = Math.max(fontMetrics.descent, (int) Math.ceil(center + halfHeight));
                fontMetrics.top = Math.min(fontMetrics.top, fontMetrics.ascent);
                fontMetrics.bottom = Math.max(fontMetrics.bottom, fontMetrics.descent);
            }
            return (int) Math.ceil(labelPaint.measureText(PATCHES_LABEL) + 2 * this.horizontalPadding);
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
                Paint paint) {
            Paint labelPaint = scaledPaint(paint);
            Paint.FontMetrics hostMetrics = paint.getFontMetrics();
            Paint.FontMetrics labelMetrics = labelPaint.getFontMetrics();
            float center = y + (hostMetrics.ascent + hostMetrics.descent) / 2f;
            float baseline = center - (labelMetrics.ascent + labelMetrics.descent) / 2f;
            float width = labelPaint.measureText(PATCHES_LABEL) + 2 * this.horizontalPadding;
            labelPaint.setColor(this.backgroundColor);
            canvas.drawRoundRect(x, baseline + labelMetrics.ascent - this.verticalPadding, x + width,
                    baseline + labelMetrics.descent + this.verticalPadding, this.radius, this.radius, labelPaint);
            labelPaint.setColor(this.textColor);
            canvas.drawText(PATCHES_LABEL, x + this.horizontalPadding, baseline, labelPaint);
        }

        private Paint scaledPaint(Paint paint) {
            Paint scaled = new Paint(paint);
            scaled.setTextSize(paint.getTextSize() * LABEL_TEXT_SCALE);
            return scaled;
        }

    }

}
