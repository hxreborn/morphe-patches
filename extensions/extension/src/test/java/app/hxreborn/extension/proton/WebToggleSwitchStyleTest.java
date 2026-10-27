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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.Switch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class WebToggleSwitchStyleTest {

    private static final int[] CHECKED = { android.R.attr.state_checked };

    private static final int[] UNCHECKED = {};

    private static final int MARK_SIZE = 16;

    private Switch control;

    @Before
    public void styleASwitch() {
        this.control = new Switch(RuntimeEnvironment.getApplication());
        WebToggleSwitchStyle.apply(this.control);
    }

    private static Drawable current(Drawable drawable, int[] state) {
        drawable.setState(state);
        return drawable.getCurrent();
    }

    private static int fill(Drawable shape) {
        return ((GradientDrawable) shape).getColor().getDefaultColor();
    }

    private int thumbFill(int[] state) {
        final LayerDrawable thumb = (LayerDrawable) current(this.control.getThumbDrawable(), state);
        return fill(thumb.getDrawable(0));
    }

    private int trackFill(int[] state) {
        final LayerDrawable track = (LayerDrawable) this.control.getTrackDrawable();
        return fill(current(track.getDrawable(0), state));
    }

    private int markPixel(int[] state, int x, int y) {
        final LayerDrawable thumb = (LayerDrawable) current(this.control.getThumbDrawable(), state);
        final Drawable mark = thumb.getDrawable(1);
        final Bitmap bitmap = Bitmap.createBitmap(MARK_SIZE, MARK_SIZE, Bitmap.Config.ARGB_8888);
        mark.setBounds(0, 0, MARK_SIZE, MARK_SIZE);
        mark.draw(new Canvas(bitmap));
        return bitmap.getPixel(x, y);
    }

    @Test
    public void sizesTheTrackTo52dp() {
        assertEquals(52, this.control.getSwitchMinWidth());
        assertEquals(52, this.control.getTrackDrawable().getIntrinsicWidth());
        assertEquals(32, this.control.getTrackDrawable().getIntrinsicHeight());
    }

    @Test
    public void dropsTheOnOffLabels() {
        assertFalse(this.control.getShowText());
    }

    @Test
    public void dropsTheSplitTrack() {
        assertFalse(this.control.getSplitTrack());
    }

    @Test
    public void insetsTheTrackByTheThumbTravelMargin() {
        // given
        final Rect padding = new Rect();

        // when
        this.control.getTrackDrawable().getPadding(padding);

        // then
        assertEquals(6, padding.left);
        assertEquals(6, padding.right);
    }

    @Test
    public void paintsTheCheckedTrackInTheProtonAccent() {
        assertEquals(0xFF6D4AFF, trackFill(CHECKED));
    }

    @Test
    public void paintsTheUncheckedTrackAsAFaintOutlineTint() {
        assertEquals(0x335C5958, trackFill(UNCHECKED));
    }

    @Test
    @Config(qualifiers = "night")
    public void paintsTheUncheckedTrackWithTheDarkOutlineTint() {
        assertEquals(0x33A7A4B5, trackFill(UNCHECKED));
    }

    @Test
    public void paintsTheUncheckedThumbInTheOutlineColor() {
        assertEquals(0xFF5C5958, thumbFill(UNCHECKED));
    }

    @Test
    @Config(qualifiers = "night")
    public void paintsTheUncheckedThumbInTheDarkOutlineColor() {
        assertEquals(0xFFA7A4B5, thumbFill(UNCHECKED));
    }

    @Test
    public void paintsTheCheckedThumbWhiteWithoutAnInvertedTextColor() {
        assertEquals(Color.WHITE, thumbFill(CHECKED));
    }

    @Test
    public void drawsACrossOnTheUncheckedThumb() {
        assertNotEquals(Color.TRANSPARENT, markPixel(UNCHECKED, 7, 7));
        assertEquals(Color.TRANSPARENT, markPixel(UNCHECKED, 8, 3));
    }

    @Test
    public void drawsACheckOnTheCheckedThumb() {
        assertEquals(Color.TRANSPARENT, markPixel(CHECKED, 7, 7));
        assertNotEquals(Color.TRANSPARENT, markPixel(CHECKED, 6, 11));
    }

    @Test
    public void colorsTheCheckWithTheAccent() {
        assertEquals(0xFF6D4AFF, markPixel(CHECKED, 6, 11));
    }

    @Test
    public void colorsTheCrossWithTheSurface() {
        assertEquals(0xFFF5F4F2, markPixel(UNCHECKED, 7, 7));
    }

    @Test
    @Config(qualifiers = "night")
    public void colorsTheCrossWithTheDarkSurfaceInDarkTheme() {
        assertEquals(0xFF292733, markPixel(UNCHECKED, 7, 7));
    }

}
