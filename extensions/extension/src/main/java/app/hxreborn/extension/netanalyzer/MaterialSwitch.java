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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.widget.Checkable;

import app.hxreborn.extension.shared.Dimensions;

final class MaterialSwitch extends View implements Checkable {

    private static final float TRACK_WIDTH_DP = 52f;

    private static final float TRACK_HEIGHT_DP = 32f;

    private static final float BORDER_DP = 2f;

    private static final float CHECKED_THUMB_DP = 24f;

    private static final float UNCHECKED_THUMB_DP = 16f;

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF bounds = new RectF();

    private final HostPalette palette;

    private SwitchChangeListener listener;

    private boolean checked;

    MaterialSwitch(Context context, HostPalette palette) {
        super(context);
        this.palette = palette;
        this.border.setStyle(Paint.Style.STROKE);
        this.border.setStrokeWidth(dp(BORDER_DP));
        setClickable(true);
        setOnClickListener((view) -> toggle());
    }

    void setListener(SwitchChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.checked == checked) {
            return;
        }
        this.checked = checked;
        invalidate();
        if (this.listener != null) {
            this.listener.onChanged(checked);
        }
    }

    @Override
    public boolean isChecked() {
        return this.checked;
    }

    @Override
    public void toggle() {
        setChecked(!this.checked);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(Math.round(dp(TRACK_WIDTH_DP)), Math.round(dp(TRACK_HEIGHT_DP)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float radius = getHeight() / 2f;
        this.bounds.set(0, 0, getWidth(), getHeight());
        this.track.setColor(this.checked ? this.palette.accent : this.palette.surfaceVariant);
        canvas.drawRoundRect(this.bounds, radius, radius, this.track);
        final float inset = this.border.getStrokeWidth() / 2;
        this.bounds.inset(inset, inset);
        this.border.setColor(this.checked ? this.palette.accent : this.palette.rowSubsubtitle);
        canvas.drawRoundRect(this.bounds, radius - inset, radius - inset, this.border);
        this.thumb.setColor(this.checked ? this.palette.onAccent : this.palette.rowSubsubtitle);
        final float thumbRadius = dp(this.checked ? CHECKED_THUMB_DP : UNCHECKED_THUMB_DP) / 2;
        final float centerX = this.checked ? getWidth() - radius : radius;
        canvas.drawCircle(centerX, radius, thumbRadius, this.thumb);
    }

    private float dp(float value) {
        return Dimensions.dpExact(getContext(), value);
    }

}
