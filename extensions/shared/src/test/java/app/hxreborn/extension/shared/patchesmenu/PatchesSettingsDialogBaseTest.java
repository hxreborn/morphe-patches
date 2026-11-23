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
package app.hxreborn.extension.shared.patchesmenu;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.LinearLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PatchesSettingsDialogBaseTest {

    @Test
    public void removePreviousContentBeforeBuildingReplacement() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        LinearLayout container = new LinearLayout(activity);
        container.addView(new View(activity));
        int[] childrenDuringBuild = { -1 };
        PatchesSettingsDialogBase screen = new PatchesSettingsDialogBase(activity, new Dialog(activity)) {
            @Override
            protected View createContent() {
                childrenDuringBuild[0] = container.getChildCount();
                return new View(activity);
            }
        };

        screen.refreshContent(container);

        assertEquals(0, childrenDuringBuild[0]);
        assertEquals(1, container.getChildCount());
    }

}
