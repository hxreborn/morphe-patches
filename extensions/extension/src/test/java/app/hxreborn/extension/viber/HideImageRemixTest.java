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
package app.hxreborn.extension.viber;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, shadows = { HideImageRemixTest.AppliedImageRemix.class })
public final class HideImageRemixTest {

    @Before
    public void prepare() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.resetToDefaults();
    }

    @After
    public void reset() {
        PatchSettings.resetToDefaults();
    }

    @Test
    public void hideImageRemixByDefaultWhenApplied() {
        Assert.assertTrue(PatchSettings.hideImageRemix());
    }

    @Test
    public void showImageRemixWhenSwitchedOff() {
        PatchSettings.setEnabled(PatchSetting.HIDE_IMAGE_REMIX.key, false);
        Assert.assertFalse(PatchSettings.hideImageRemix());
    }

    @Test
    public void restoreDefaultsHidesImageRemixAgain() {
        PatchSettings.setEnabled(PatchSetting.HIDE_IMAGE_REMIX.key, false);
        PatchSettings.resetToDefaults();
        Assert.assertTrue(PatchSettings.hideImageRemix());
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class AppliedImageRemix {

        @Implementation
        public static boolean hideImageRemix() {
            return true;
        }

    }

}
