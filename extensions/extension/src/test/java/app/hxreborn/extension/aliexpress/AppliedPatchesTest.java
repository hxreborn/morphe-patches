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
package app.hxreborn.extension.aliexpress;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AppliedPatchesTest {

    private static final String ANALYTICS_DESCRIPTION = "Stops usage statistics and affiliate link reports.";

    @Test
    public void listNothingBeforePatchesFillTheirDescriptions() {
        Assert.assertTrue(AppliedPatches.names().isEmpty());
    }

    @Test
    public void returnNoDescriptionForUnknownName() {
        Assert.assertNull(AppliedPatches.description("Unknown patch"));
    }

    @Test
    @Config(shadows = AppliedAnalytics.class)
    public void listPatchWhoseDescriptionWasFilled() {
        Assert.assertEquals(Collections.singletonList(AppliedPatches.DISABLE_ANALYTICS), AppliedPatches.names());
        Assert.assertEquals(ANALYTICS_DESCRIPTION, AppliedPatches.description(AppliedPatches.DISABLE_ANALYTICS));
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static class AppliedAnalytics {

        @Implementation
        protected static String disableAnalyticsDescription() {
            return ANALYTICS_DESCRIPTION;
        }

    }

}
