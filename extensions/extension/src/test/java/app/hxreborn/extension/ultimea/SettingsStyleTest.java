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
package app.hxreborn.extension.ultimea;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.widget.Switch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 26, 33 }, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SettingsStyleTest {

    private static final int WIDTH = 160;

    private static final int HEIGHT = 80;

    private static final int ACCENT = 0xFF541597;

    @Test
    public void preservesStockTrackGeometryAndDrawsOpaqueStates() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
                android.R.style.Theme_DeviceDefault);
        Switch control = new Switch(context);
        Drawable stock = control.getTrackDrawable().getConstantState().newDrawable(context.getResources()).mutate();
        SettingsStyle style = new SettingsStyle(context);
        SwitchColors colors = new SwitchColors(ACCENT, style.cardSurface, style.dark);
        style.styleSwitch(control);
        Assert.assertEquals(PorterDuff.Mode.SRC_IN, control.getThumbTintMode());
        for (boolean checked : new boolean[] { false, true }) {
            int[] state = checked ? new int[] { android.R.attr.state_enabled, android.R.attr.state_checked }
                    : new int[] { android.R.attr.state_enabled };
            Bitmap before = draw(stock, state);
            Bitmap after = draw(control.getTrackDrawable(), state);
            Assert.assertEquals(visibleBounds(before), visibleBounds(after));
            Assert.assertEquals(255, Color.alpha(after.getPixel(WIDTH / 2, HEIGHT / 2)));
            Assert.assertEquals(checked ? colors.checkedTrack : colors.uncheckedTrack,
                    after.getPixel(WIDTH / 2, HEIGHT / 2));
            Assert.assertEquals(stock.getIntrinsicHeight(), control.getTrackDrawable().getIntrinsicHeight());
            before.recycle();
            after.recycle();
        }
    }

    private static Bitmap draw(Drawable drawable, int[] state) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        drawable.setState(state);
        drawable.setBounds(0, 0, WIDTH, HEIGHT);
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }

    private static Rect visibleBounds(Bitmap bitmap) {
        int peakAlpha = Color.alpha(bitmap.getPixel(WIDTH / 2, HEIGHT / 2));
        Assert.assertTrue(peakAlpha > 0);
        Rect bounds = new Rect(WIDTH, HEIGHT, 0, 0);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) * 2 >= peakAlpha) {
                    bounds.left = Math.min(bounds.left, x);
                    bounds.top = Math.min(bounds.top, y);
                    bounds.right = Math.max(bounds.right, x + 1);
                    bounds.bottom = Math.max(bounds.bottom, y + 1);
                }
            }
        }
        return bounds;
    }

}
