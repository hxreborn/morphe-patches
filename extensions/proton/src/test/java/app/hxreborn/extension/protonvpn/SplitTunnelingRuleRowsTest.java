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

import java.util.EnumSet;

import android.content.Context;

import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class SplitTunnelingRuleRowsTest {

    @Before
    public void createEmptyPreferences() {
        final Context context = PatchedBuild.useApplicationContext();
        context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void leavesThePickedAppsSummaryAloneWithoutRules() {
        assertEquals("None", SplitTunnelingRuleRows.appsSummary("None"));
    }

    @Test
    public void addsTheRuleMatchesToThePickedAppsSummary() {
        SplitTunnelingRuleSettings.saveKeywordText("chrome");
        SplitTunnelingRuleSettings.saveMatchCount(1);

        assertEquals("None \u00b7 1 by rules", SplitTunnelingRuleRows.appsSummary("None"));
    }

    @Test
    public void addsTheRuleMatchesAfterPickedAppNames() {
        SplitTunnelingRuleSettings.saveKeywordText("chrome");
        SplitTunnelingRuleSettings.saveMatchCount(3);

        assertEquals("Gmail \u00b7 3 by rules", SplitTunnelingRuleRows.appsSummary("Gmail"));
    }

    @Test
    public void leavesThePickedAppsSummaryAloneWhenNothingMatches() {
        SplitTunnelingRuleSettings.saveKeywordText("chrome");
        SplitTunnelingRuleSettings.saveMatchCount(0);

        assertEquals("None", SplitTunnelingRuleRows.appsSummary("None"));
    }

    @Test
    public void leavesThePickedAppsSummaryAloneWhenTheCountIsUnknown() {
        SplitTunnelingRuleSettings.saveKeywordText("chrome");

        assertEquals("None", SplitTunnelingRuleRows.appsSummary("None"));
    }

    @Test
    public void summarizesNoRulesAsOff() {
        assertEquals("Off", SplitTunnelingRuleRows.rulesRowSummary());
    }

    @Test
    public void summarizesAKeywordWithItsAppCount() {
        SplitTunnelingRuleSettings.saveKeywordText("chrome");
        SplitTunnelingRuleSettings.saveMatchCount(1);

        assertEquals("chrome \u00b7 1 app", SplitTunnelingRuleRows.rulesRowSummary());
    }

    @Test
    public void summarizesCategoriesAndKeywordsByName() {
        SplitTunnelingRuleSettings.saveCategories(EnumSet.of(SplitTunnelingCategory.BROWSER));
        SplitTunnelingRuleSettings.saveKeywordText("bank");
        SplitTunnelingRuleSettings.saveMatchCount(5);

        assertEquals("Browsers, bank \u00b7 5 apps", SplitTunnelingRuleRows.rulesRowSummary());
    }

    @Test
    public void summarizesManyRulesByCount() {
        SplitTunnelingRuleSettings.saveCategories(
                EnumSet.of(SplitTunnelingCategory.GAME, SplitTunnelingCategory.NEWS, SplitTunnelingCategory.BROWSER));

        assertEquals("3 rules", SplitTunnelingRuleRows.rulesRowSummary());
    }

}
