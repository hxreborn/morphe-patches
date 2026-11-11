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
package app.hxreborn.extension.readera;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class AccentColorTest {

    private static final int STOCK_JUMP_BAR_COLOR = 0xFF33B5E5;

    private final Context application = RuntimeEnvironment.getApplication();

    @After
    public void clearPreferences() {
        preferences().edit().clear().commit();
    }

    @Test
    public void keepStockJumpBarColorWithoutAccentResource() {
        AccentPalette.setPreset(this.application, AccentPalette.Fixed.PINK.preset);

        AccentColor.applyToApplication(this.application);

        Assert.assertEquals(STOCK_JUMP_BAR_COLOR, AccentColor.jumpBarColor(STOCK_JUMP_BAR_COLOR));
    }

    private SharedPreferences preferences() {
        return this.application.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

}
