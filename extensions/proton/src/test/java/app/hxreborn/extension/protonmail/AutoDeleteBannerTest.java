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
package app.hxreborn.extension.protonmail;

import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
public final class AutoDeleteBannerTest {

    @Test
    public void unfilledChecksKeepEveryState() {
        for (BannerState state : BannerState.values()) {
            assertSame(state, AutoDeleteBanner.resolveState(state));
        }
    }

    @Test
    @Config(shadows = { UpsellHidden.class })
    public void hiddenUpsellTurnsOnlyTheUpsellStateIntoDisabled() {
        assertSame(BannerState.AutoDeleteDisabled, AutoDeleteBanner.resolveState(BannerState.AutoDeleteUpsell));
        assertSame(BannerState.AutoDeleteEnabled, AutoDeleteBanner.resolveState(BannerState.AutoDeleteEnabled));
        assertSame(BannerState.AutoDeleteDisabled, AutoDeleteBanner.resolveState(BannerState.AutoDeleteDisabled));
    }

    @Test
    @Config(shadows = { ScheduledAndUpsellHidden.class })
    public void scheduledDeletionWinsOverHiddenUpsell() {
        for (BannerState state : BannerState.values()) {
            assertSame(BannerState.AutoDeleteEnabled, AutoDeleteBanner.resolveState(state));
        }
    }

    @Test
    @Config(shadows = { UpsellHidden.class })
    public void nonEnumStateIsReturnedUnchanged() {
        final Object state = new Object();

        assertSame(state, AutoDeleteBanner.resolveState(state));
    }

    private enum BannerState {

        AutoDeleteUpsell, AutoDeleteDisabled, AutoDeleteEnabled

    }

    @Implements(AutoDeleteBanner.class)
    @SuppressWarnings("unused")
    public static final class UpsellHidden {

        @Implementation
        public static boolean isUpsellHidden() {
            return true;
        }

    }

    @Implements(AutoDeleteBanner.class)
    @SuppressWarnings("unused")
    public static final class ScheduledAndUpsellHidden {

        @Implementation
        public static boolean isScheduled() {
            return true;
        }

        @Implementation
        public static boolean isUpsellHidden() {
            return true;
        }

    }

}
