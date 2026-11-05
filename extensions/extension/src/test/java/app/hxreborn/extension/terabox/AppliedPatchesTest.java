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
    public void listsNothingBeforePatchesFillTheirDescriptions() {
        Assert.assertTrue(AppliedPatches.names().isEmpty());
    }

    @Test
    public void returnsNoDescriptionForUnknownName() {
        Assert.assertNull(AppliedPatches.description("Unknown patch"));
    }

    @Test
    @Config(shadows = AppliedAds.class)
    public void listsOnlyThePatchWhoseDescriptionWasFilled() {
        Assert.assertEquals(Collections.singletonList(AppliedPatches.HIDE_ADS), AppliedPatches.names());
        Assert.assertEquals(ADS_DESCRIPTION, AppliedPatches.description(AppliedPatches.HIDE_ADS));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void listsEveryAppliedPatchAlphabetically() {
        List<String> sorted = new ArrayList<>(AppliedPatches.names());
        Collections.sort(sorted);

        Assert.assertEquals(9, AppliedPatches.names().size());
        Assert.assertEquals(sorted, AppliedPatches.names());
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static class AppliedAds {

        @Implementation
        protected static String hideAdsDescription() {
            return ADS_DESCRIPTION;
        }

    }

}
