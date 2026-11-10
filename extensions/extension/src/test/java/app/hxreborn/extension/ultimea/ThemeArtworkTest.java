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

import java.io.InputStream;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, shadows = ThemeArtworkTest.HeaderShadow.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ThemeArtworkTest {

    private static final int WIDTH = 1081;

    private static final int HEIGHT = 874;

    private static final int LOGO_LEFT = 842;

    private static final int LOGO_TOP = 209;

    private static final int LOGO_RIGHT = 1037;

    private static final int LOGO_BOTTOM = 282;

    private static final int LOGO_COLOR = 0x00ececee;

    private static final long GENERATOR = 91L;

    private static final int ALPHA_LEVELS = 256;

    @Test
    public void decodePinnedHeroAndRecolorOnlyLogo() throws Exception {
        byte[] png;
        try (InputStream input = ThemeArtworkTest.class.getResourceAsStream("/ultimea/d80-hero.png")) {
            Assert.assertNotNull(input);
            png = input.readAllBytes();
        }
        Bitmap original = BitmapFactory.decodeByteArray(png, 0, png.length);
        Assert.assertNotNull(original);
        ByteBuffer input = ByteBuffer.wrap(png);
        input.position(7);
        Bitmap themed = ThemeArtwork.decodeHero(input, GENERATOR);
        Assert.assertNotNull(themed);
        Assert.assertEquals(7, input.position());
        Assert.assertEquals(WIDTH, themed.getWidth());
        Assert.assertEquals(HEIGHT, themed.getHeight());
        Assert.assertEquals(1, HeaderShadow.headers);
        Assert.assertEquals(GENERATOR, HeaderShadow.generator);
        Assert.assertEquals(WIDTH, HeaderShadow.width);
        Assert.assertEquals(HEIGHT, HeaderShadow.height);
        int[] before = new int[WIDTH * HEIGHT];
        int[] after = new int[WIDTH * HEIGHT];
        original.getPixels(before, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
        themed.getPixels(after, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
        int[] logoColors = new int[ALPHA_LEVELS];
        for (int alpha = 0; alpha < ALPHA_LEVELS; alpha++) {
            logoColors[alpha] = (alpha << 24) | LOGO_COLOR;
        }
        Bitmap alphaSamples = Bitmap.createBitmap(logoColors, ALPHA_LEVELS, 1, Bitmap.Config.ARGB_8888);
        alphaSamples.getPixels(logoColors, 0, ALPHA_LEVELS, 0, 0, ALPHA_LEVELS, 1);
        for (int y = LOGO_TOP; y < LOGO_BOTTOM; y++) {
            for (int x = LOGO_LEFT; x < LOGO_RIGHT; x++) {
                int offset = y * WIDTH + x;
                before[offset] = logoColors[before[offset] >>> 24];
            }
        }
        Assert.assertArrayEquals(before, after);
    }

    @Implements(value = ThemeArtwork.class, isInAndroidSdk = false)
    public static class HeaderShadow {

        private static int headers;

        private static long generator;

        private static int width;

        private static int height;

        @Implementation
        protected static void onHeader(long value, int imageWidth, int imageHeight) {
            headers++;
            generator = value;
            width = imageWidth;
            height = imageHeight;
        }

    }

}
