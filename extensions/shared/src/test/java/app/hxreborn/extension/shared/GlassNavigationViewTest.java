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
package app.hxreborn.extension.shared;

import android.graphics.Color;
import android.view.View;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 23, 28, 30 })
public class GlassNavigationViewTest {

    private static final int TRANSLUCENT_ALPHA = 150;

    @Test
    public void showTranslucentBackgroundOnOlderAndroid() {
        GlassNavigationView navigation = new GlassNavigationView(RuntimeEnvironment.getApplication(), null);
        View child = new View(RuntimeEnvironment.getApplication());
        child.setBackgroundColor(Color.WHITE);
        navigation.addView(child);
        navigation.onFinishInflate();
        Assert.assertEquals(TRANSLUCENT_ALPHA, child.getBackground().getAlpha());
    }

}
