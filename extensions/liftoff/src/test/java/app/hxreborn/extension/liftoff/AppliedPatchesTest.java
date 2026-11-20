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
package app.hxreborn.extension.liftoff;

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

    @Test
    public void listNothingBeforePatchesAreApplied() {
        Assert.assertTrue(AppliedPatches.applied().isEmpty());
        Assert.assertFalse(AppliedPatches.unlockPremium());
        Assert.assertFalse(AppliedPatches.unlockCustomizations());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void listEveryAppliedPatchAlphabeticallyWithItsDescription() {
        List<AppliedPatches.AppliedPatch> applied = AppliedPatches.applied();

        Assert.assertEquals(Arrays.asList("Unlock customizations", "Unlock premium"), names());
        Assert.assertEquals(EveryPatchApplied.CUSTOMIZATIONS_DESCRIPTION, applied.get(0).description);
        Assert.assertEquals(EveryPatchApplied.PREMIUM_DESCRIPTION, applied.get(1).description);
    }

    @Test
    @Config(shadows = MixedCaseTitles.class)
    public void orderAppliedPatchesByTitleIgnoringCase() {
        Assert.assertEquals(Arrays.asList("Alpha customizations", "zeta premium"), names());
    }

    @Test
    @Config(shadows = OnlyCustomizationsApplied.class)
    public void listOnlySelectedPatch() {
        Assert.assertEquals(Collections.singletonList("Unlock customizations"), names());
        Assert.assertTrue(AppliedPatches.unlockCustomizations());
        Assert.assertFalse(AppliedPatches.unlockPremium());
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
        protected static boolean unlockPremium() {
            return true;
        }

        @Implementation
        protected static String unlockPremiumName() {
            return "zeta premium";
        }

        @Implementation
        protected static boolean unlockCustomizations() {
            return true;
        }

        @Implementation
        protected static String unlockCustomizationsName() {
            return "Alpha customizations";
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class AppliedWithoutDescription {

        @Implementation
        protected static boolean unlockCustomizations() {
            return true;
        }

        @Implementation
        protected static String unlockCustomizationsName() {
            return "Renamed feature";
        }

    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class OnlyCustomizationsApplied {

        @Implementation
        protected static boolean unlockCustomizations() {
            return true;
        }

        @Implementation
        protected static String unlockCustomizationsName() {
            return "Unlock customizations";
        }

        @Implementation
        protected static String unlockCustomizationsDescription() {
            return EveryPatchApplied.CUSTOMIZATIONS_DESCRIPTION;
        }

    }

}
