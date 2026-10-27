/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.protonmail;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.proton.PatchedBuild;

@RunWith(RobolectricTestRunner.class)
public final class SidebarEntriesTest {

    @Before
    public void useApplicationContext() {
        PatchedBuild.useApplicationContext();
    }

    @Test
    public void ignoresAStoredHideSettingWhenUnpatched() {
        // given
        PatchSettings.setEnabled(SidebarEntries.SUBSCRIPTION, true);
        PatchSettings.setEnabled(SidebarEntries.REPORT_A_PROBLEM, true);

        // when
        assertFalse(SidebarEntries.isSubscriptionHidden());
        assertFalse(SidebarEntries.isReportAProblemHidden());
    }

    @Test
    @Config(shadows = Patched.class)
    public void showsEveryEntryUntilHidden() {
        assertFalse(SidebarEntries.isSubscriptionHidden());
        assertFalse(SidebarEntries.isReportAProblemHidden());
    }

    @Test
    @Config(shadows = Patched.class)
    public void hidesSubscriptionWithoutHidingReportAProblem() {
        // given
        PatchSettings.setEnabled(SidebarEntries.SUBSCRIPTION, true);

        // when
        assertTrue(SidebarEntries.isSubscriptionHidden());
        assertFalse(SidebarEntries.isReportAProblemHidden());
    }

    @Test
    @Config(shadows = Patched.class)
    public void hidesReportAProblemWithoutHidingSubscription() {
        // given
        PatchSettings.setEnabled(SidebarEntries.REPORT_A_PROBLEM, true);

        // when
        assertTrue(SidebarEntries.isReportAProblemHidden());
        assertFalse(SidebarEntries.isSubscriptionHidden());
    }

    @Implements(SidebarEntries.class)
    public static final class Patched {

        @Implementation
        public static boolean isPatched() {
            return true;
        }

    }

}
