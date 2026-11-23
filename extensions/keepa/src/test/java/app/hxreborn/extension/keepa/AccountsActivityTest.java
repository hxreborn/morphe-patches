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
package app.hxreborn.extension.keepa;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class AccountsActivityTest {

    private static final int SCREEN_CHILDREN = 3;

    @Test
    public void positionAddAccountBelowScrollableContent() {
        AccountsActivity activity = Robolectric.buildActivity(AccountsActivity.class).setup().get();
        ViewGroup content = activity.findViewById(android.R.id.content);
        LinearLayout screen = (LinearLayout) content.getChildAt(0);
        Assert.assertEquals(LinearLayout.VERTICAL, screen.getOrientation());
        Assert.assertEquals(SCREEN_CHILDREN, screen.getChildCount());
        Assert.assertEquals("Back", screen.getChildAt(0).getContentDescription());

        ScrollView scroll = (ScrollView) screen.getChildAt(1);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scroll.getLayoutParams();
        Assert.assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.width);
        Assert.assertEquals(0, params.height);
        Assert.assertEquals(1f, params.weight, 0f);
        Assert.assertTrue(scroll.isFillViewport());

        Button add = (Button) screen.getChildAt(screen.getChildCount() - 1);
        Assert.assertEquals("Add account", add.getText().toString());
    }

}
