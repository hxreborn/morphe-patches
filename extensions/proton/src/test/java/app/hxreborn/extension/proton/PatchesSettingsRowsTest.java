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
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
public final class PatchesSettingsRowsTest {

    private static final List<String> FEATURE_ROW_TITLES = Arrays.asList("Hide upgrade promotions",
            "Floating navigation bar", "Custom accent color", "M3E loading indicator");

    private static final List<String> SECTION_TITLES = Arrays.asList("General", "Layout", "Appearance");

    @Before
    public void useApplicationContext() {
        PatchedBuild.useApplicationContext();
    }

    @Test
    @Config(shadows = { PatchedBuild.Applied.class })
    public void unfilledRowStubsShowNoFeatureRow() {
        final List<String> texts = texts(settings());

        for (String title : FEATURE_ROW_TITLES) {
            assertFalse(title, texts.contains(title));
        }
    }

    @Test
    @Config(shadows = { PatchedBuild.Applied.class, FilledRowStubs.class })
    public void filledRowStubsShowTheFeatureRowsUnderTheirSections() {
        final List<String> shown = new ArrayList<>();
        for (String text : texts(settings())) {
            if (FEATURE_ROW_TITLES.contains(text) || SECTION_TITLES.contains(text)) {
                shown.add(text);
            }
        }

        assertEquals(Arrays.asList("General", "Hide upgrade promotions", "Layout", "Floating navigation bar",
                "Appearance", "Custom accent color", "M3E loading indicator"), shown);
    }

    private static Activity settings() {
        return Robolectric.buildActivity(PatchesSettingsActivity.class).setup().get();
    }

    private static List<String> texts(Activity activity) {
        final List<String> texts = new ArrayList<>();
        collectTexts(activity.findViewById(android.R.id.content), texts);
        return texts.subList(0, texts.indexOf("About"));
    }

    private static void collectTexts(View view, List<String> texts) {
        if (view instanceof TextView) {
            texts.add(((TextView) view).getText().toString());
        }
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                collectTexts(group.getChildAt(index), texts);
            }
        }
    }

    @Implements(PatchesSettingsActivity.class)
    public static final class FilledRowStubs extends ShadowActivity {

        @Implementation
        public static void addUpgradePromotionRows(PatchesSettingsActivity activity, List<View> rows) {
            UpgradePromotionRows.add(activity, rows);
        }

        @Implementation
        public static void addNavigationRows(PatchesSettingsActivity activity, List<View> rows) {
            NavigationBarRows.add(activity, rows);
        }

        @Implementation
        public static void addAccentColorRows(PatchesSettingsActivity activity, List<View> rows) {
            AccentColorRows.add(activity, rows);
        }

        @Implementation
        public static void addLoadingIndicatorRows(PatchesSettingsActivity activity, List<View> rows) {
            LoadingIndicatorRows.add(activity, rows);
        }

    }

}
