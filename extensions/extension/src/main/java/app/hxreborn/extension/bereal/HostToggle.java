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
package app.hxreborn.extension.bereal;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.CompoundButton;
import android.widget.Switch;

import app.hxreborn.extension.shared.Dimensions;

final class HostToggle extends CompoundButton {

    private static final int TRACK_ON = 0xFF007AFF;

    private static final int TRACK_OFF = 0xFF6E6E72;

    private static final int THUMB = 0xFFFFFFFF;

    private static final int TRACK_WIDTH_DP = 52;

    private static final int TRACK_HEIGHT_DP = 32;

    private static final int TARGET_HEIGHT_DP = 48;

    private static final int THUMB_ON_DP = 24;

    private static final int THUMB_OFF_DP = 16;

    private static final int ANIMATION_MS = 150;

    private static final int DISABLED_ALPHA = 102;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF track = new RectF();

    private final ArgbEvaluator colors = new ArgbEvaluator();

    private float progress;

    private ValueAnimator animator;

    HostToggle(Context context) {
        super(context);
        setBackground(null);
        setMinimumHeight(Dimensions.dp(context, TARGET_HEIGHT_DP));
    }

    @Override
    public void setChecked(boolean checked) {
        final boolean changed = checked != isChecked();
        super.setChecked(checked);
        final float target = checked ? 1f : 0f;
        if (!changed || !isLaidOut()) {
            this.progress = target;
            invalidate();
            return;
        }
        if (this.animator != null) {
            this.animator.cancel();
        }
        this.animator = ValueAnimator.ofFloat(this.progress, target);
        this.animator.setDuration(ANIMATION_MS);
        this.animator.addUpdateListener((animation) -> {
            this.progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        this.animator.start();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Switch.class.getName();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(Dimensions.dp(getContext(), TRACK_WIDTH_DP) + getPaddingLeft() + getPaddingRight(),
                Math.max(getSuggestedMinimumHeight(), Dimensions.dp(getContext(), TRACK_HEIGHT_DP)));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.animator != null) {
            this.animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float trackWidth = Dimensions.dp(getContext(), TRACK_WIDTH_DP);
        final float trackHeight = Dimensions.dp(getContext(), TRACK_HEIGHT_DP);
        final float left = getPaddingLeft();
        final float top = (getHeight() - trackHeight) / 2f;
        this.track.set(left, top, left + trackWidth, top + trackHeight);
        this.paint.setColor((int) this.colors.evaluate(this.progress, TRACK_OFF, TRACK_ON));
        if (!isEnabled()) {
            this.paint.setAlpha(DISABLED_ALPHA);
        }
        canvas.drawRoundRect(this.track, trackHeight / 2f, trackHeight / 2f, this.paint);

        final float thumbOff = Dimensions.dp(getContext(), THUMB_OFF_DP);
        final float thumbOn = Dimensions.dp(getContext(), THUMB_ON_DP);
        final float radius = (thumbOff + (thumbOn - thumbOff) * this.progress) / 2f;
        final float inset = (trackHeight - thumbOff) / 2f;
        final float startX = left + inset + thumbOff / 2f;
        final float endX = left + trackWidth - (trackHeight - thumbOn) / 2f - thumbOn / 2f;
        this.paint.setColor(THUMB);
        if (!isEnabled()) {
            this.paint.setAlpha(DISABLED_ALPHA);
        }
        canvas.drawCircle(startX + (endX - startX) * this.progress, top + trackHeight / 2f, radius, this.paint);
    }

}
