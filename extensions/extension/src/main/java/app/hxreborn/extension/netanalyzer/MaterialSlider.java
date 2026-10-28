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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import app.hxreborn.extension.shared.Dimensions;

final class MaterialSlider extends View {

    private static final float TRACK_HEIGHT_DP = 16f;

    private static final float THUMB_WIDTH_DP = 4f;

    private static final float THUMB_HEIGHT_DP = 44f;

    private static final float THUMB_GAP_DP = 6f;

    private static final float STOP_DP = 4f;

    private static final float MIN_WIDTH_DP = 48f;

    private final Paint active = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint inactive = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF rect = new RectF();

    private final int steps;

    private int position;

    private SliderPositionListener listener;

    MaterialSlider(Context context, HostPalette palette, int steps) {
        super(context);
        this.steps = steps;
        this.active.setColor(palette.accent);
        this.inactive.setColor(palette.inactiveTrack);
    }

    void setPosition(int position) {
        this.position = Math.max(0, Math.min(this.steps, position));
        invalidate();
    }

    void setListener(SliderPositionListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(Math.round(dp(MIN_WIDTH_DP)), widthMeasureSpec),
                Math.round(dp(THUMB_HEIGHT_DP)));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        final float usable = getWidth() - dp(THUMB_WIDTH_DP);
        final int next = Math.round((event.getX() - dp(THUMB_WIDTH_DP) / 2) / usable * this.steps);
        if (Math.max(0, Math.min(this.steps, next)) != this.position) {
            setPosition(next);
            if (this.listener != null) {
                this.listener.onPosition(this.position);
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float thumbWidth = dp(THUMB_WIDTH_DP);
        final float gap = dp(THUMB_GAP_DP);
        final float trackRadius = dp(TRACK_HEIGHT_DP) / 2;
        final float centerY = getHeight() / 2f;
        final float top = centerY - trackRadius;
        final float bottom = centerY + trackRadius;
        final float thumbStart = (getWidth() - thumbWidth) * this.position / this.steps;
        if (thumbStart - gap > 0) {
            this.rect.set(0, top, thumbStart - gap, bottom);
            canvas.drawRoundRect(this.rect, trackRadius, trackRadius, this.active);
        }
        final float inactiveStart = thumbStart + thumbWidth + gap;
        if (inactiveStart < getWidth()) {
            this.rect.set(inactiveStart, top, getWidth(), bottom);
            canvas.drawRoundRect(this.rect, trackRadius, trackRadius, this.inactive);
            canvas.drawCircle(getWidth() - trackRadius, centerY, dp(STOP_DP) / 2, this.active);
        }
        this.rect.set(thumbStart, 0, thumbStart + thumbWidth, getHeight());
        canvas.drawRoundRect(this.rect, thumbWidth / 2, thumbWidth / 2, this.active);
    }

    private float dp(float value) {
        return Dimensions.dpExact(getContext(), value);
    }

}
