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
package app.hxreborn.extension.protonvpn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class SplitTunnelingRuleSettingsTest {

    @Before
    public void createEmptyPreferences() {
        final Context context = PatchedBuild.useApplicationContext();
        context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void startsWithoutRules() {
        assertFalse(SplitTunnelingRuleSettings.isActive());
        assertTrue(SplitTunnelingRuleSettings.categories().isEmpty());
        assertTrue(SplitTunnelingRuleSettings.keywords().isEmpty());
    }

    @Test
    public void keepsTheSavedCategories() {
        SplitTunnelingRuleSettings
            .saveCategories(EnumSet.of(SplitTunnelingCategory.SOCIAL, SplitTunnelingCategory.GAME));

        assertEquals(EnumSet.of(SplitTunnelingCategory.GAME, SplitTunnelingCategory.SOCIAL),
                SplitTunnelingRuleSettings.categories());
        assertTrue(SplitTunnelingRuleSettings.isActive());
    }

    @Test
    public void normalizesKeywordsToLowerCaseWithoutDuplicates() {
        SplitTunnelingRuleSettings.saveKeywordText(" Bank,\nWALLET ,bank,, ");

        assertEquals(Arrays.asList("bank", "wallet"), SplitTunnelingRuleSettings.keywords());
        assertEquals("bank, wallet", SplitTunnelingRuleSettings.keywordText());
        assertTrue(SplitTunnelingRuleSettings.isActive());
    }

    @Test
    public void returnsTheMatchesOfTheRulesTheyWereComputedFrom() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");

        SplitTunnelingRuleSettings.saveMatches(EnumSet.noneOf(SplitTunnelingCategory.class),
                Collections.singletonList("bank"), Collections.singletonList(app("com.example.bank")));

        assertEquals(Collections.singletonList("com.example.bank"), SplitTunnelingRuleSettings.matchedApps());
    }

    @Test
    public void ignoresMatchesComputedFromRulesSavedAfterwards() {
        SplitTunnelingRuleSettings.saveKeywordText("game");

        SplitTunnelingRuleSettings.saveMatches(EnumSet.noneOf(SplitTunnelingCategory.class),
                Collections.singletonList("bank"), Collections.singletonList(app("com.example.bank")));

        assertTrue(SplitTunnelingRuleSettings.matchedApps().isEmpty());
    }

    private static SplitTunnelingApp app(String packageName) {
        return new SplitTunnelingApp(packageName, packageName, ApplicationInfo.CATEGORY_UNDEFINED, false);
    }

    @Test
    public void clearsTheRulesWhenSavedEmpty() {
        SplitTunnelingRuleSettings.saveCategories(EnumSet.of(SplitTunnelingCategory.NEWS));
        SplitTunnelingRuleSettings.saveKeywordText("bank");

        SplitTunnelingRuleSettings.saveCategories(Collections.emptySet());
        SplitTunnelingRuleSettings.saveKeywordText("  ");

        assertFalse(SplitTunnelingRuleSettings.isActive());
    }

}
