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
import android.content.res.Configuration;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class AppThemeTest {

    private static final String THEME = "theme";

    private Context application;

    private SharedPreferences preferences;

    @Before
    public void preparePreferences() {
        this.application = RuntimeEnvironment.getApplication();
        this.preferences = this.application.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
        clearPreferences();
    }

    @After
    public void clearPreferences() {
        this.preferences.edit().clear().commit();
        AppTheme.applyToApplication(this.application);
    }

    @Test
    public void selectStockByDefault() {
        Assert.assertFalse(AppTheme.amoledSelected(this.application));
        Assert.assertSame(this.application, AppTheme.applyToApplication(this.application));
        Assert.assertFalse(AppTheme.amoledApplied());
    }

    @Test
    public void saveAndReadAmoledSelection() {
        Assert.assertTrue(AppTheme.saveAmoledSelected(this.application, true));
        Assert.assertEquals("amoled", this.preferences.getString(THEME, null));
        Assert.assertTrue(AppTheme.amoledSelected(this.application));

        Assert.assertTrue(AppTheme.saveAmoledSelected(this.application, false));
        Assert.assertEquals("stock", this.preferences.getString(THEME, null));
        Assert.assertFalse(AppTheme.amoledSelected(this.application));
    }

    @Test
    public void keepSelectionUnappliedUntilApplicationStarts() {
        AppTheme.saveAmoledSelected(this.application, true);

        Assert.assertFalse(AppTheme.amoledApplied());
        Assert.assertSame(this.application, AppTheme.wrap(this.application));
    }

    @Test
    public void overrideOnlyNightModeAndNavigationForAmoled() {
        Configuration base = this.application.getResources().getConfiguration();
        AppTheme.saveAmoledSelected(this.application, true);

        Context themed = AppTheme.applyToApplication(this.application);

        Configuration configuration = themed.getResources().getConfiguration();
        Assert.assertTrue(AppTheme.amoledApplied());
        Assert.assertEquals(Configuration.UI_MODE_NIGHT_YES, configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK);
        Assert.assertEquals(Configuration.NAVIGATION_WHEEL, configuration.navigation);
        Assert.assertEquals(base.fontScale, configuration.fontScale, 0f);
        Assert.assertEquals(base.orientation, configuration.orientation);
        Assert.assertEquals(base.screenWidthDp, configuration.screenWidthDp);
    }

    @Test
    public void ignoreUnknownStoredTheme() {
        this.preferences.edit().putString(THEME, "sepia").commit();

        Assert.assertFalse(AppTheme.amoledSelected(this.application));
        Assert.assertSame(this.application, AppTheme.applyToApplication(this.application));
    }

}
