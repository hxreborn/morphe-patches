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
package app.hxreborn.extension.internetspeedmeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import app.hxreborn.extension.shared.Dimensions;

final class SpeedGraphView extends View {

    private static final int SAMPLES = 61;

    private static final int GRID_LINES = 4;

    private static final float LINE_WIDTH_DP = 2f;

    private static final float LABEL_TEXT_SP = 11f;

    private static final float PADDING_DP = 8f;

    private static final long MINIMUM_SCALE = 1024;

    private final long[] download = new long[SAMPLES];

    private final long[] upload = new long[SAMPLES];

    private final Paint downloadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint uploadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint gridPaint = new Paint();

    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path path = new Path();

    private final float padding;

    private final boolean bits;

    private int count;

    SpeedGraphView(Context context, int downloadColor, int uploadColor, boolean bits) {
        super(context);
        this.bits = bits;
        this.padding = dp(PADDING_DP);
        configureLine(this.downloadPaint, downloadColor);
        configureLine(this.uploadPaint, uploadColor);
        this.gridPaint.setColor(Color.argb(40, 0, 0, 0));
        this.gridPaint.setStrokeWidth(1f);
        this.labelPaint.setColor(Color.GRAY);
        this.labelPaint.setTextSize(Dimensions.spExact(context, LABEL_TEXT_SP));
    }

    void add(long downloadBytesPerSecond, long uploadBytesPerSecond) {
        System.arraycopy(this.download, 1, this.download, 0, SAMPLES - 1);
        System.arraycopy(this.upload, 1, this.upload, 0, SAMPLES - 1);
        this.download[SAMPLES - 1] = downloadBytesPerSecond;
        this.upload[SAMPLES - 1] = uploadBytesPerSecond;
        this.count = Math.min(SAMPLES, this.count + 1);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long scale = MINIMUM_SCALE;
        for (int index = 0; index < SAMPLES; index++) {
            scale = Math.max(scale, Math.max(this.download[index], this.upload[index]));
        }

        float top = this.padding + this.labelPaint.getTextSize();
        float bottom = getHeight() - this.padding;
        float left = this.padding;
        float right = getWidth() - this.padding;
        for (int line = 0; line <= GRID_LINES; line++) {
            float y = top + (bottom - top) * line / GRID_LINES;
            canvas.drawLine(left, y, right, y, this.gridPaint);
        }
        canvas.drawText(SpeedFormat.speed(scale, this.bits), left, top - this.padding / 2, this.labelPaint);

        if (this.count >= 2) {
            drawLine(canvas, this.upload, scale, left, top, right, bottom, this.uploadPaint);
            drawLine(canvas, this.download, scale, left, top, right, bottom, this.downloadPaint);
        }
    }

    private void drawLine(Canvas canvas, long[] samples, long scale, float left, float top, float right, float bottom,
            Paint paint) {
        this.path.reset();
        float step = (right - left) / (SAMPLES - 1);
        for (int index = SAMPLES - this.count; index < SAMPLES; index++) {
            float x = left + step * index;
            float y = bottom - (bottom - top) * samples[index] / scale;
            if (index == SAMPLES - this.count) {
                this.path.moveTo(x, y);
            } else {
                this.path.lineTo(x, y);
            }
        }
        canvas.drawPath(this.path, paint);
    }

    private void configureLine(Paint paint, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(LINE_WIDTH_DP));
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    private float dp(float value) {
        return Dimensions.dpExact(getContext(), value);
    }

}
