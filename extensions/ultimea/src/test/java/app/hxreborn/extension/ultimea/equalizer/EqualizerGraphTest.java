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
package app.hxreborn.extension.ultimea.equalizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import app.hxreborn.extension.ultimea.HostStyle;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class EqualizerGraphTest {

    @Test
    public void drawIdenticalThumbsBeforeAndAfterSelectedBand() {
        Context context = RuntimeEnvironment.getApplication();
        EqualizerGraph graph = new EqualizerGraph(context, new HostStyle(context), new EqualizerBandListener() {
            @Override
            public void onSelected(int band) {
            }

            @Override
            public void onStarted() {
            }

            @Override
            public void onChanged(int band, int gain) {
            }

            @Override
            public void onFinished() {
            }
        });
        graph.show(new EqualizerCurve(EqualizerCurve.CUSTOM_MODE, new int[10]), true);
        graph.select(5);
        graph.measure(View.MeasureSpec.makeMeasureSpec(440, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY));
        graph.layout(0, 0, 440, 360);
        Bitmap bitmap = Bitmap.createBitmap(440, 360, Bitmap.Config.ARGB_8888);
        graph.draw(new Canvas(bitmap));
        int[] reference = new int[17 * 13];
        bitmap.getPixels(reference, 0, 17, 44, 166, 17, 13);
        int white = 0;
        int grip = 0;
        for (int pixel : reference) {
            white += pixel == Color.WHITE ? 1 : 0;
            grip += pixel == new HostStyle(context).pillInk ? 1 : 0;
        }
        Assert.assertTrue("White thumb positive control", white > 0);
        Assert.assertTrue("Grip positive control", grip > 0);
        for (int band = 1; band < 10; band++) {
            int[] actual = new int[reference.length];
            bitmap.getPixels(actual, 0, 17, 44 + 40 * band, 166, 17, 13);
            Assert.assertArrayEquals("Band " + band, reference, actual);
        }
    }

}
