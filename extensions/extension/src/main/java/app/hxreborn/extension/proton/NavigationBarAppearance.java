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
package app.hxreborn.extension.proton;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.S)
final class NavigationBarAppearance {

    private static final float CORNER_RADIUS_DP = 28f;

    private static final float BORDER_WIDTH_DP = 1f;

    private static final float BLUR_TINT_ALPHA = 0.55f;

    private static final float GLASS_TINT_ALPHA = 0.38f;

    private static final int GLASS_EDGE_COLOR = 0x65ffffff;

    private static final int GLASS_TOP_COLOR = 0x1fffffff;

    private static final int GLASS_BOTTOM_COLOR = 0x0f000000;

    private final float density;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    NavigationBarAppearance(float density) {
        this.density = density;
    }

    void draw(Canvas destination, RenderNode blurNode, float width, float height, int color, boolean glass,
            boolean ready) {
        if (ready) {
            destination.drawRenderNode(blurNode);
        }
        this.paint.setShader(null);
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(color);
        this.paint.setAlpha(ready ? Math.round(Color.alpha(color) * (glass ? GLASS_TINT_ALPHA : BLUR_TINT_ALPHA))
                : Color.alpha(color));
        final float corner = CORNER_RADIUS_DP * this.density;
        destination.drawRoundRect(0f, 0f, width, height, corner, corner, this.paint);
        if (ready && glass) {
            drawGlass(destination, width, height, corner);
        }
    }

    private void drawGlass(Canvas destination, float width, float height, float corner) {
        this.paint.setColor(Color.WHITE);
        this.paint.setAlpha(Color.alpha(Color.WHITE));
        this.paint.setShader(
                new LinearGradient(0f, 0f, 0f, height, GLASS_TOP_COLOR, GLASS_BOTTOM_COLOR, Shader.TileMode.CLAMP));
        destination.drawRoundRect(0f, 0f, width, height, corner, corner, this.paint);
        this.paint.setShader(null);
        this.paint.setColor(GLASS_EDGE_COLOR);
        this.paint.setStyle(Paint.Style.STROKE);
        final float border = BORDER_WIDTH_DP * this.density;
        this.paint.setStrokeWidth(border);
        final float inset = border / 2f;
        destination.drawRoundRect(inset, inset, width - inset, height - inset, corner - inset, corner - inset,
                this.paint);
    }

}
