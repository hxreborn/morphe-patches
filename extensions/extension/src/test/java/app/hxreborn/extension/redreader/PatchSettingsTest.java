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
package app.hxreborn.extension.redreader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class PatchSettingsTest {

    @Before
    public void initializePreferences() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.resetToDefaults();
    }

    @Test
    public void keepSavedRulesWhenReplacementIsInvalid() {
        PatchSettings.save("spoiler", "news", "discussion");
        assertThrows(IllegalArgumentException.class, () -> PatchSettings.save("replacement", "/[/", ""));
        assertEquals("spoiler", PatchSettings.value(PatchSettings.TITLES_KEY));
        assertEquals("news", PatchSettings.value(PatchSettings.SUBREDDITS_KEY));
        assertEquals("discussion", PatchSettings.value(PatchSettings.FLAIRS_KEY));
        assertTrue(PatchSettings.isFiltered("SPOILER", "pics", null));
        assertFalse(PatchSettings.isFiltered("replacement", "pics", null));
    }

    @Test
    public void applyRulesImmediatelyAndResetToEmptyRules() {
        PatchSettings.save("spoiler", "news", "discussion");
        assertTrue(PatchSettings.isFiltered("Title", "pics", "Discussion"));
        PatchSettings.resetToDefaults();
        assertFalse(PatchSettings.isFiltered("spoiler", "news", "discussion"));
        assertEquals("", PatchSettings.value(PatchSettings.TITLES_KEY));
        assertEquals("", PatchSettings.value(PatchSettings.SUBREDDITS_KEY));
        assertEquals("", PatchSettings.value(PatchSettings.FLAIRS_KEY));
    }

}
