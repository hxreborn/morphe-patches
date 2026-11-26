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

import java.util.HashMap;
import java.util.Map;
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
@Config(manifest = Config.NONE, sdk = 28, shadows = ContactSearchTest.AppliedContactSearch.class)
public class ContactSearchTest {

    private static final String COLUMN = "[phonebookcontact].[low_display_name]";

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
    public void removeGreekTonosAndCase() {
        Assert.assertEquals("γιωργος", ContactSearch.foldAccents("ΓΙΏΡΓΟΣ"));
        Assert.assertEquals("γιωργος", ContactSearch.foldAccents("Γιώργος"));
    }

    @Test
    public void foldGreekFinalSigma() {
        Assert.assertEquals(ContactSearch.foldAccents("ΝΙΚΟΣ"), ContactSearch.foldAccents("Νίκος"));
    }

    @Test
    public void removeLatinAccents() {
        Assert.assertEquals("cerrajeria", ContactSearch.foldAccents("Cerrajería"));
        Assert.assertEquals("francois muller", ContactSearch.foldAccents("François Müller"));
    }

    @Test
    public void keepJapaneseVoicedKana() {
        Assert.assertEquals("が", ContactSearch.foldAccents("が"));
    }

    @Test
    public void keepHangulSyllables() {
        Assert.assertEquals("한국", ContactSearch.foldAccents("한국"));
    }

    @Test
    public void passNullThrough() {
        Assert.assertNull(ContactSearch.foldAccents(null));
    }

    @Test
    public void foldColumnAndTextWithEnabledSwitch() {
        Assert.assertEquals("hx_fold_accents(" + COLUMN + ")", ContactSearch.searchedColumn(COLUMN));
        Assert.assertEquals("γιωργος", ContactSearch.searchedText("Γιώργος"));
    }

    @Test
    public void keepStockQueryWithDisabledSwitch() {
        PatchSettings.setEnabled(PatchSetting.IGNORE_ACCENTS_IN_CONTACT_SEARCH.key, false);
        Assert.assertEquals(COLUMN, ContactSearch.searchedColumn(COLUMN));
        Assert.assertEquals("Γιώργος", ContactSearch.searchedText("Γιώργος"));
    }

    @Test
    public void addFoldFunctionUnderItsSqlName() {
        Map<String, Object> scalarFunctions = new HashMap<>();
        ContactSearch.addFoldAccentsFunction(scalarFunctions);
        Assert.assertTrue(scalarFunctions.get("hx_fold_accents") instanceof ContactSearch);
    }

    @Implements(AppliedPatches.class)
    @SuppressWarnings("unused")
    public static class AppliedContactSearch {

        @Implementation
        protected static boolean ignoreAccentsInContactSearch() {
            return true;
        }

    }

}
