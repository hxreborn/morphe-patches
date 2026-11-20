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
package app.hxreborn.extension.iiec;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class AmoledThemeTest {

    private static final String KEY = "appearance_editor_theme_dark";

    private SharedPreferences preferences;

    @Before
    public void startFromACleanLooper() {
        final Application application = RuntimeEnvironment.getApplication();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(application);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void attachWithChoice(String value) {
        this.preferences.edit().putString(KEY, value).commit();
        AmoledTheme.attach(RuntimeEnvironment.getApplication());
        shadowOf(Looper.getMainLooper()).idle();
    }

    private boolean recreationScheduledAfterChoosing(String value) {
        this.preferences.edit().putString(KEY, value).commit();
        return !shadowOf(Looper.getMainLooper()).isIdle();
    }

    @Test
    public void schedulesRecreationWhenAmoledIsChosen() {
        // given
        attachWithChoice("dark");

        // when
        final boolean scheduled = recreationScheduledAfterChoosing("amoled");

        // then
        assertTrue(scheduled);
    }

    @Test
    public void schedulesRecreationWhenAmoledIsLeft() {
        // given
        attachWithChoice("amoled");

        // when
        final boolean scheduled = recreationScheduledAfterChoosing("dark");

        // then
        assertTrue(scheduled);
    }

    @Test
    public void staysIdleWhenTheChoiceKeepsItsAmoledState() {
        attachWithChoice("dark");
        assertFalse(recreationScheduledAfterChoosing("light"));
        assertFalse(recreationScheduledAfterChoosing(""));
    }

    @Test
    public void matchesTheAmoledChoiceExactly() {
        for (String value : new String[] { "AMOLED", "Amoled", " amoled", "amoled ", "amole", "amoled2" }) {
            attachWithChoice("dark");
            assertFalse(value, recreationScheduledAfterChoosing(value));
        }
    }

    @Test
    public void treatsAMissingChoiceAsNotAmoled() {
        this.preferences.edit().remove(KEY).commit();
        AmoledTheme.attach(RuntimeEnvironment.getApplication());
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(recreationScheduledAfterChoosing("dark"));
        assertTrue(recreationScheduledAfterChoosing("amoled"));
    }

    @Test
    public void treatsAChoiceStoredWithAnotherTypeAsNotAmoled() {
        this.preferences.edit().putBoolean(KEY, true).commit();
        AmoledTheme.attach(RuntimeEnvironment.getApplication());
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(recreationScheduledAfterChoosing("dark"));
        assertTrue(recreationScheduledAfterChoosing("amoled"));
    }

    @Test
    public void ignoresChangesToOtherKeys() {
        // given
        attachWithChoice("dark");

        // when
        this.preferences.edit().putString("appearance_editor_theme_light", "amoled").commit();

        // then
        assertTrue(shadowOf(Looper.getMainLooper()).isIdle());
    }

}
