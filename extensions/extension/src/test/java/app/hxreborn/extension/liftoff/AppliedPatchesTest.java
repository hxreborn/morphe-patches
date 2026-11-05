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

import java.util.Arrays;
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
public final class AppliedPatchesTest {

    @Test
    public void listsNothingBeforePatchesFillTheirDescriptions() {
        Assert.assertTrue(AppliedPatches.names().isEmpty());
        Assert.assertFalse(AppliedPatches.unlockPremium());
        Assert.assertFalse(AppliedPatches.unlockCustomizations());
    }

    @Test
    public void returnsNoDescriptionForUnknownName() {
        Assert.assertNull(AppliedPatches.description("Unknown patch"));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void listsEveryAppliedPatchAlphabeticallyWithItsDescription() {
        Assert.assertEquals(Arrays.asList(AppliedPatches.UNLOCK_CUSTOMIZATIONS, AppliedPatches.UNLOCK_PREMIUM),
                AppliedPatches.names());
        Assert.assertEquals(EveryPatchApplied.PREMIUM_DESCRIPTION,
                AppliedPatches.description(AppliedPatches.UNLOCK_PREMIUM));
        Assert.assertEquals(EveryPatchApplied.CUSTOMIZATIONS_DESCRIPTION,
                AppliedPatches.description(AppliedPatches.UNLOCK_CUSTOMIZATIONS));
    }

    @Test
    @Config(shadows = OnlyCustomizationsApplied.class)
    public void listsOnlyPatchWhoseDescriptionWasFilled() {
        Assert.assertEquals(Collections.singletonList(AppliedPatches.UNLOCK_CUSTOMIZATIONS), AppliedPatches.names());
        Assert.assertTrue(AppliedPatches.unlockCustomizations());
        Assert.assertFalse(AppliedPatches.unlockPremium());
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class OnlyCustomizationsApplied {

        @Implementation
        protected static String unlockCustomizationsDescription() {
            return EveryPatchApplied.CUSTOMIZATIONS_DESCRIPTION;
        }

    }

}
