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

import android.view.View;
import android.widget.FrameLayout;
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
@Config(manifest = Config.NONE, sdk = 28, shadows = ListGapsTest.AppliedGaps.class)
public class ListGapsTest {

    private static final int WIDTH = 400;

    private static final int CONTAINER_HEIGHT = 700;

    private static final int HEADER_HEIGHT = 200;

    private static final int SHORT_CONTENT_HEIGHT = 400;

    private static final int LONG_CONTENT_HEIGHT = 550;

    private static final int FOOTER_HEIGHT = 80;

    @Before
    public void initializePreferences() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.resetToDefaults();
    }

    @After
    public void resetPreferences() {
        PatchSettings.resetToDefaults();
    }

    @Test
    public void removeChatFooterByDefault() {
        Assert.assertEquals(0, ListGaps.chatFooterHeight(FOOTER_HEIGHT));
    }

    @Test
    public void preserveChatFooterWithDisabledSwitch() {
        PatchSettings.setEnabled(PatchSetting.REMOVE_LIST_GAPS.key, false);
        Assert.assertEquals(FOOTER_HEIGHT, ListGaps.chatFooterHeight(FOOTER_HEIGHT));
    }

    @Test
    public void stopHeaderScrollingWhenEveryRowFits() {
        FrameLayout list = list(SHORT_CONTENT_HEIGHT);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        Assert.assertFalse(list.isNestedScrollingEnabled());
    }

    @Test
    public void preserveScrollingWhenRowsExceedVisibleSpace() {
        FrameLayout list = list(LONG_CONTENT_HEIGHT);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        Assert.assertTrue(list.isNestedScrollingEnabled());
    }

    @Test
    public void restoreScrollingWhenRowsNoLongerFit() {
        FrameLayout list = list(SHORT_CONTENT_HEIGHT);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        list.getChildAt(0).layout(0, 0, WIDTH, LONG_CONTENT_HEIGHT);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        Assert.assertTrue(list.isNestedScrollingEnabled());
    }

    @Test
    public void restoreScrollingWithDisabledSwitch() {
        FrameLayout list = list(SHORT_CONTENT_HEIGHT);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        PatchSettings.setEnabled(PatchSetting.REMOVE_LIST_GAPS.key, false);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        Assert.assertTrue(list.isNestedScrollingEnabled());
    }

    @Test
    public void preserveScrollingBeforeRowsAreLaidOut() {
        FrameLayout list = list(0);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        Assert.assertTrue(list.isNestedScrollingEnabled());
    }

    @Test
    public void preserveScrollingWhenAncestorClipsList() {
        FrameLayout list = list(SHORT_CONTENT_HEIGHT);
        FrameLayout parent = (FrameLayout) list.getParent();
        FrameLayout ancestor = new FrameLayout(parent.getContext());
        ancestor.addView(parent);
        ancestor.layout(0, 0, WIDTH, SHORT_CONTENT_HEIGHT);
        parent.layout(0, 0, WIDTH, CONTAINER_HEIGHT);
        list.layout(0, HEADER_HEIGHT, WIDTH, CONTAINER_HEIGHT);
        list.getChildAt(0).layout(0, 0, WIDTH, SHORT_CONTENT_HEIGHT);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        Assert.assertTrue(list.isNestedScrollingEnabled());
    }

    @Test
    public void preserveScrollingForListsWithMoreContent() {
        FrameLayout list = list(SHORT_CONTENT_HEIGHT);
        list.setScrollY(1);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        Assert.assertTrue(list.isNestedScrollingEnabled());
    }

    @Test
    public void preserveOriginallyDisabledNestedScrolling() {
        FrameLayout list = new FrameLayout(RuntimeEnvironment.getApplication());
        list.setNestedScrollingEnabled(false);
        ListGaps.observeList(list);
        PatchSettings.setEnabled(PatchSetting.REMOVE_LIST_GAPS.key, false);
        list.getViewTreeObserver().dispatchOnGlobalLayout();
        Assert.assertFalse(list.isNestedScrollingEnabled());
    }

    @Test
    public void acceptMissingList() {
        ListGaps.observeList(null);
    }

    private static FrameLayout list(int contentHeight) {
        FrameLayout parent = new FrameLayout(RuntimeEnvironment.getApplication());
        FrameLayout list = new FrameLayout(parent.getContext()) {
            @Override
            public boolean canScrollVertically(int direction) {
                return getScrollY() != 0;
            }
        };
        list.setNestedScrollingEnabled(true);
        parent.addView(list);
        parent.layout(0, 0, WIDTH, CONTAINER_HEIGHT);
        list.layout(0, HEADER_HEIGHT, WIDTH, CONTAINER_HEIGHT);
        if (contentHeight > 0) {
            View row = new View(parent.getContext());
            list.addView(row);
            row.layout(0, 0, WIDTH, contentHeight);
        }
        ListGaps.observeList(list);
        return list;
    }

    @Implements(AppliedPatches.class)
    public static class AppliedGaps {

        @Implementation
        protected static boolean removeListGaps() {
            return true;
        }

    }

}
