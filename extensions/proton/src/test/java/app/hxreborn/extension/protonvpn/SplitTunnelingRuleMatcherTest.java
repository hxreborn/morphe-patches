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
import java.util.List;
import java.util.Set;

import android.content.pm.ApplicationInfo;

import org.junit.Test;

public final class SplitTunnelingRuleMatcherTest {

    private static final Set<SplitTunnelingCategory> NO_CATEGORIES = Collections.emptySet();

    private static final List<String> NO_KEYWORDS = Collections.emptyList();

    private static final SplitTunnelingApp CHROME = app("com.android.chrome", "Chrome");

    private static final SplitTunnelingApp GOOGLE_HOME = app("com.google.android.apps.chromecast.app", "Home");

    private static SplitTunnelingRuleMatcher matcher(Set<SplitTunnelingCategory> categories, String... keywords) {
        return new SplitTunnelingRuleMatcher(categories, Arrays.asList(keywords));
    }

    private static SplitTunnelingApp app(String packageName, String label) {
        return new SplitTunnelingApp(packageName, label, ApplicationInfo.CATEGORY_UNDEFINED, false);
    }

    private static SplitTunnelingApp app(String packageName, String label, int category) {
        return new SplitTunnelingApp(packageName, label, category, false);
    }

    private static SplitTunnelingApp browser(String packageName, String label) {
        return new SplitTunnelingApp(packageName, label, ApplicationInfo.CATEGORY_UNDEFINED, true);
    }

    @Test
    public void matchesAnAppInASelectedCategory() {
        final SplitTunnelingRuleMatcher matcher = matcher(EnumSet.of(SplitTunnelingCategory.GAME));

        assertTrue(matcher.matches(app("com.example.game", "Game", ApplicationInfo.CATEGORY_GAME)));
    }

    @Test
    public void skipsAnAppInAnotherCategory() {
        final SplitTunnelingRuleMatcher matcher = matcher(EnumSet.of(SplitTunnelingCategory.GAME));

        assertFalse(matcher.matches(app("com.example.video", "Video", ApplicationInfo.CATEGORY_VIDEO)));
    }

    @Test
    public void skipsAnAppWithoutACategory() {
        final SplitTunnelingRuleMatcher matcher = matcher(
                EnumSet.of(SplitTunnelingCategory.GAME, SplitTunnelingCategory.AUDIO, SplitTunnelingCategory.VIDEO,
                        SplitTunnelingCategory.IMAGE, SplitTunnelingCategory.SOCIAL, SplitTunnelingCategory.NEWS,
                        SplitTunnelingCategory.MAPS, SplitTunnelingCategory.PRODUCTIVITY));

        assertFalse(matcher.matches(app("com.example.plain", "Plain")));
    }

    @Test
    public void matchesABrowserInTheBrowsersCategory() {
        final SplitTunnelingRuleMatcher matcher = matcher(EnumSet.of(SplitTunnelingCategory.BROWSER));

        assertTrue(matcher.matches(browser("org.mozilla.firefox", "Firefox")));
    }

    @Test
    public void skipsAnAppThatIsNotABrowserInTheBrowsersCategory() {
        final SplitTunnelingRuleMatcher matcher = matcher(EnumSet.of(SplitTunnelingCategory.BROWSER));

        assertFalse(matcher.matches(app("com.google.android.gm", "Gmail")));
    }

    @Test
    public void matchesAKeywordAtTheStartOfTheLabel() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "bank");

        assertTrue(matcher.matches(app("com.example.mobile", "Bank Mobile")));
    }

    @Test
    public void matchesAKeywordAtTheStartOfALaterWord() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "bank");

        assertTrue(matcher.matches(app("com.example.mobile", "My Bank")));
    }

    @Test
    public void matchesAKeywordIgnoringCase() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "bank");

        assertTrue(matcher.matches(app("com.example.mobile", "MY BANK")));
    }

    @Test
    public void skipsAKeywordInTheMiddleOfAWord() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "bank");

        assertFalse(matcher.matches(app("com.example.mobile", "Embankment")));
    }

    @Test
    public void skipsAKeywordInsideAMixedCaseWord() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "bank");

        assertFalse(matcher.matches(app("com.example.mobile", "mBank")));
    }

    @Test
    public void skipsAKeywordThatOnlyTheLabelLacks() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "bank");

        assertFalse(matcher.matches(app("com.example.bank.mobile", "Mobile")));
    }

    @Test
    public void matchesChromeAndNotGoogleHome() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "chrome");

        assertEquals(Collections.singletonList(CHROME), matcher.select(Arrays.asList(CHROME, GOOGLE_HOME)));
    }

    @Test
    public void matchesTheStartOfAPackageNameWhenTheLabelIsThePackageName() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "bank");

        assertTrue(matcher.matches(app("com.example.bank", "com.example.bank")));
    }

    @Test
    public void skipsAnAppWithNoKeyword() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "bank", "wallet");

        assertFalse(matcher.matches(app("com.example.chat", "Chat")));
    }

    @Test
    public void matchesNothingWithoutRules() {
        final SplitTunnelingRuleMatcher matcher = new SplitTunnelingRuleMatcher(NO_CATEGORIES, NO_KEYWORDS);

        assertFalse(matcher.matches(app("com.example.game", "Game", ApplicationInfo.CATEGORY_GAME)));
    }

    @Test
    public void keepsTheOrderOfTheAppsItSelects() {
        final SplitTunnelingRuleMatcher matcher = matcher(NO_CATEGORIES, "bank");
        final SplitTunnelingApp first = app("com.example.a", "Bank A");
        final SplitTunnelingApp second = app("com.example.b", "Bank B");

        assertEquals(Arrays.asList(first, second),
                matcher.select(Arrays.asList(first, app("com.example.c", "Chat"), second)));
    }

}
