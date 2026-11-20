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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public void listNothingBeforePatchesAreApplied() {
        Assert.assertTrue(AppliedPatches.applied().isEmpty());
    }

    @Test
    @Config(shadows = AppliedAnalytics.class)
    public void listOnlySelectedPatch() {
        Assert.assertEquals(Collections.singletonList("Disable analytics"), names());
        Assert.assertEquals(ANALYTICS_DESCRIPTION, AppliedPatches.applied().get(0).description);
    }

    @Test
    @Config(shadows = MixedCaseTitles.class)
    public void orderAppliedPatchesByTitleIgnoringCase() {
        Assert.assertEquals(Arrays.asList("Alpha analytics", "beta splash", "Gamma sponsored", "zeta updates"),
                names());
    }

    @Test
    @Config(shadows = AppliedWithoutDescription.class)
    public void keepSelectedPatchWhenDescriptionIsMissing() {
        Assert.assertEquals(Collections.singletonList("Renamed feature"), names());
        Assert.assertNull(AppliedPatches.applied().get(0).description);
    }

    private static List<String> names() {
        List<String> names = new ArrayList<>();
        for (AppliedPatches.AppliedPatch patch : AppliedPatches.applied()) {
            names.add(patch.name);
        }
        return names;
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class MixedCaseTitles {

        @Implementation
        protected static boolean disableAnalytics() {
            return true;
        }

        @Implementation
        protected static String disableAnalyticsName() {
            return "Alpha analytics";
        }

        @Implementation
        protected static boolean skipSplashPromotions() {
            return true;
        }

        @Implementation
        protected static String skipSplashPromotionsName() {
            return "beta splash";
        }

        @Implementation
        protected static boolean removeSponsoredItems() {
            return true;
        }

        @Implementation
        protected static String removeSponsoredItemsName() {
            return "Gamma sponsored";
        }

        @Implementation
        protected static boolean disableUpdatePrompts() {
            return true;
        }

        @Implementation
        protected static String disableUpdatePromptsName() {
            return "zeta updates";
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class AppliedWithoutDescription {

        @Implementation
        protected static boolean disableAnalytics() {
            return true;
        }

        @Implementation
        protected static String disableAnalyticsName() {
            return "Renamed feature";
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static class AppliedAnalytics {

        @Implementation
        protected static boolean disableAnalytics() {
            return true;
        }

        @Implementation
        protected static String disableAnalyticsName() {
            return "Disable analytics";
        }

        @Implementation
        protected static String disableAnalyticsDescription() {
            return ANALYTICS_DESCRIPTION;
        }

    }

}
