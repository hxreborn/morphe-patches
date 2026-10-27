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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.View;

@TargetApi(Build.VERSION_CODES.Q)
final class StarButton extends View {

    private static final int POINTS = 5;

    private static final float STAR_RADIUS_DP = 10f;

    private static final float INNER_RATIO = 0.4f;

    private static final float RIPPLE_ALPHA = 0.12f;

    private static final double START_ANGLE = -Math.PI / 2;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path star = new Path();

    private final int onColor;

    private final int offColor;

    StarButton(Context context, HostPalette palette, boolean checked, View.OnClickListener onToggle) {
        super(context);
        this.onColor = palette.yellow;
        this.offColor = palette.grey;
        setChecked(checked);
        setBackground(new RippleDrawable(
                ColorStateList.valueOf(HostScreenActivity.withAlpha(palette.keyText, RIPPLE_ALPHA)), null, null));
        setOnClickListener(onToggle);
    }

    void setChecked(boolean checked) {
        this.paint.setColor(checked ? this.onColor : this.offColor);
        setContentDescription(checked ? "Unfavorite" : "Favorite");
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float density = getResources().getDisplayMetrics().density;
        final float outer = STAR_RADIUS_DP * density;
        final float inner = outer * INNER_RATIO;
        final float centerX = getWidth() / 2f;
        final float centerY = getHeight() / 2f;
        this.star.reset();
        for (int i = 0; i < POINTS * 2; i++) {
            final double angle = START_ANGLE + Math.PI * i / POINTS;
            final float radius = (i % 2 == 0) ? outer : inner;
            final float x = centerX + (float) Math.cos(angle) * radius;
            final float y = centerY + (float) Math.sin(angle) * radius;
            if (i == 0) {
                this.star.moveTo(x, y);
            } else {
                this.star.lineTo(x, y);
            }
        }
        this.star.close();
        canvas.drawPath(this.star, this.paint);
    }

}
