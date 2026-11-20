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

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.morphe.extension.shared.Utils;

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
@Config(sdk = { 23, 36 }, shadows = { EveryPatchApplied.class, RecentPanelTest.HostPanels.class })
public final class RecentPanelTest {

    private static final int RECENT_TABS_ID = 0x7f010001;

    private static final int RECENT_CONTENT_ID = 0x7f010002;

    private static final int TRANSFER_BUTTON_ID = 0x7f010003;

    private static final String PENDING_TRANSFERS = "7";

    private LinearLayout home;

    private View recentTabs;

    private View recentContent;

    private FrameLayout transferButton;

    private TextView transferCount;

    @Before
    public void createHome() {
        Context application = RuntimeEnvironment.getApplication();
        Utils.setContext(application);
        Context context = new ContextWrapper(application) {
            private final Resources resources = new HomeResources(application.getResources());

            @Override
            public Resources getResources() {
                return this.resources;
            }
        };
        this.home = new LinearLayout(context);
        this.transferButton = new FrameLayout(context);
        this.transferButton.setId(TRANSFER_BUTTON_ID);
        this.transferCount = new TextView(context);
        this.transferCount.setText(PENDING_TRANSFERS);
        this.transferButton.addView(this.transferCount);
        this.home.addView(this.transferButton);
        this.recentTabs = new View(context);
        this.recentTabs.setId(RECENT_TABS_ID);
        this.home.addView(this.recentTabs);
        this.recentContent = new View(context);
        this.recentContent.setId(RECENT_CONTENT_ID);
        this.home.addView(this.recentContent);
    }

    @After
    public void clearPreferences() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void removesRecentPanel() {
        RecentPanel.updateRecentPanel(this.home);

        Assert.assertEquals(View.GONE, this.recentTabs.getVisibility());
        Assert.assertEquals(View.GONE, this.recentContent.getVisibility());
    }

    @Test
    public void preservesTransferStatusWhenRemovingRecentPanel() {
        RecentPanel.updateRecentPanel(this.home);

        Assert.assertEquals(View.VISIBLE, this.home.getVisibility());
        Assert.assertEquals(View.VISIBLE, this.transferButton.getVisibility());
        Assert.assertEquals(View.VISIBLE, this.transferCount.getVisibility());
        Assert.assertEquals(PENDING_TRANSFERS, this.transferCount.getText().toString());
        Assert.assertSame(this.home, this.transferButton.getParent());
        Assert.assertSame(this.transferButton, this.transferCount.getParent());
    }

    @Test
    public void restoresRecentPanelAfterRepeatedRemoval() {
        RecentPanel.updateRecentPanel(this.home);
        RecentPanel.updateRecentPanel(this.home);
        PatchSettings.setEnabled(AppliedPatches.REMOVE_RECENT_PANEL, false);

        RecentPanel.updateRecentPanel(this.home);

        Assert.assertEquals(View.VISIBLE, this.recentTabs.getVisibility());
        Assert.assertEquals(View.VISIBLE, this.recentContent.getVisibility());
    }

    @Test
    public void preservesHostHiddenPanelsWhenRemovalIsDisabled() {
        PatchSettings.setEnabled(AppliedPatches.REMOVE_RECENT_PANEL, false);
        this.recentContent.setVisibility(View.GONE);

        RecentPanel.updateRecentPanel(this.home);

        Assert.assertEquals(View.VISIBLE, this.recentTabs.getVisibility());
        Assert.assertEquals(View.GONE, this.recentContent.getVisibility());
    }

    @Implements(value = RecentPanel.class, isInAndroidSdk = false)
    public static final class HostPanels {

        @Implementation
        public static int panelTabsId() {
            return RECENT_TABS_ID;
        }

        @Implementation
        public static int panelContentId() {
            return RECENT_CONTENT_ID;
        }

    }

    @SuppressWarnings("deprecation")
    private static final class HomeResources extends Resources {

        HomeResources(Resources base) {
            super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
        }

        @Override
        public int getIdentifier(String name, String defType, String defPackage) {
            if ("id".equals(defType)) {
                switch (name) {
                    case "rb_trans_btn":
                        return TRANSFER_BUTTON_ID;
                    default:
                        break;
                }
            }
            return super.getIdentifier(name, defType, defPackage);
        }

    }

}
