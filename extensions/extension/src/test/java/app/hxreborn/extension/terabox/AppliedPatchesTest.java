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
package app.hxreborn.extension.terabox;

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
public final class AppliedPatchesTest {

    private static final String ADS_DESCRIPTION = "Removes feed, banner and video ads.";

    @Test
    public void listNothingBeforePatchesAreApplied() {
        Assert.assertTrue(AppliedPatches.applied().isEmpty());
    }

    @Test
    @Config(shadows = AppliedAds.class)
    public void listOnlySelectedPatch() {
        Assert.assertEquals(Collections.singletonList("Hide ads"), names());
        Assert.assertEquals(ADS_DESCRIPTION, AppliedPatches.applied().get(0).description);
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void listEveryAppliedPatchAlphabetically() {
        Assert.assertEquals(Arrays.asList("Clone app", "Fix Google login", "Fix transfers over VPN", "Hide ads",
                "Hide consent form", "Hide home shortcuts", "Hide promotions", "Hide video recommendations",
                "Remove recent panel", "Unlock Premium Plus"), names());
    }

    @Test
    @Config(shadows = MixedCaseTitles.class)
    public void orderAppliedPatchesByTitleIgnoringCase() {
        Assert.assertEquals(Arrays.asList("Alpha promotions", "beta premium", "zeta ads"), names());
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
        protected static boolean hideAds() {
            return true;
        }

        @Implementation
        protected static String hideAdsName() {
            return "zeta ads";
        }

        @Implementation
        protected static boolean hidePromotions() {
            return true;
        }

        @Implementation
        protected static String hidePromotionsName() {
            return "Alpha promotions";
        }

        @Implementation
        protected static boolean unlockPremium() {
            return true;
        }

        @Implementation
        protected static String unlockPremiumName() {
            return "beta premium";
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class AppliedWithoutDescription {

        @Implementation
        protected static boolean hideAds() {
            return true;
        }

        @Implementation
        protected static String hideAdsName() {
            return "Renamed feature";
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static class AppliedAds {

        @Implementation
        protected static boolean hideAds() {
            return true;
        }

        @Implementation
        protected static String hideAdsName() {
            return "Hide ads";
        }

        @Implementation
        protected static String hideAdsDescription() {
            return ADS_DESCRIPTION;
        }

    }

}
