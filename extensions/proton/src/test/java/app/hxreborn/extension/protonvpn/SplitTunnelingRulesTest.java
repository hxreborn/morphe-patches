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
import static org.junit.Assert.assertSame;
import static org.robolectric.Shadows.shadowOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public final class SplitTunnelingRulesTest {

    private static final String PICKED_APP = "com.example.picked";

    private static final String BANK_APP = "com.example.bank";

    private static final String GAME_APP = "com.example.game";

    private Context context;

    private ShadowPackageManager packages;

    @Before
    public void installTheApps() {
        this.context = PatchedBuild.useApplicationContext();
        this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit();
        this.packages = shadowOf(this.context.getPackageManager());
        install(PICKED_APP, ApplicationInfo.CATEGORY_UNDEFINED, true, true);
        install(BANK_APP, ApplicationInfo.CATEGORY_UNDEFINED, true, true);
        install(GAME_APP, ApplicationInfo.CATEGORY_GAME, true, true);
    }

    private void install(String packageName, int category, boolean launchable, boolean internet) {
        install(packageName, packageName, category, launchable, internet);
    }

    private void install(String packageName, String label, int category, boolean launchable, boolean internet) {
        final PackageInfo info = new PackageInfo();
        info.packageName = packageName;
        info.applicationInfo = new ApplicationInfo();
        info.applicationInfo.packageName = packageName;
        info.applicationInfo.nonLocalizedLabel = label;
        info.applicationInfo.category = category;
        if (internet) {
            info.requestedPermissions = new String[] { Manifest.permission.INTERNET };
            info.requestedPermissionsFlags = new int[] { PackageInfo.REQUESTED_PERMISSION_GRANTED };
        }
        this.packages.installPackage(info);
        if (launchable) {
            final ComponentName activity = new ComponentName(packageName, packageName + ".Main");
            this.packages.addActivityIfNotPresent(activity);
            final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_LAUNCHER);
            this.packages.addIntentFilterForActivity(activity, filter);
        }
    }

    private void installBrowser(String packageName, String label) {
        install(packageName, label, ApplicationInfo.CATEGORY_UNDEFINED, true, true);
        final ComponentName activity = new ComponentName(packageName, packageName + ".Browse");
        this.packages.addActivityIfNotPresent(activity);
        final IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addCategory(Intent.CATEGORY_BROWSABLE);
        filter.addDataScheme("https");
        this.packages.addIntentFilterForActivity(activity, filter);
    }

    private void installTypedBrowser(String packageName, String label) {
        install(packageName, label, ApplicationInfo.CATEGORY_UNDEFINED, true, true);
        final ComponentName activity = new ComponentName(packageName, packageName + ".Browse");
        this.packages.addActivityIfNotPresent(activity);
        final IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addCategory(Intent.CATEGORY_BROWSABLE);
        filter.addDataScheme("https");
        filter.addDataAuthority("*", null);
        try {
            filter.addDataType("text/html");
        } catch (IntentFilter.MalformedMimeTypeException ex) {
            throw new IllegalStateException(ex);
        }
        this.packages.addIntentFilterForActivity(activity, filter);
    }

    private static List<String> list(String... values) {
        return Arrays.asList(values);
    }

    @Test
    public void keepsTheSelectedAppsWhenNoRuleIsSet() {
        final List<String> selected = list(PICKED_APP);

        assertSame(selected, SplitTunnelingRules.withMatchingApps(selected));
    }

    @Test
    public void addsEveryAppInASelectedCategory() {
        SplitTunnelingRuleSettings.saveCategories(EnumSet.of(SplitTunnelingCategory.GAME));

        assertEquals(list(PICKED_APP, GAME_APP), SplitTunnelingRules.withMatchingApps(list(PICKED_APP)));
    }

    @Test
    public void addsEveryAppMatchingAKeyword() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");

        assertEquals(list(PICKED_APP, BANK_APP), SplitTunnelingRules.withMatchingApps(list(PICKED_APP)));
    }

    @Test
    public void listsAnAppOnceWhenPickedAndMatched() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");

        assertEquals(list(BANK_APP), SplitTunnelingRules.withMatchingApps(list(BANK_APP)));
    }

    @Test
    public void givesMatchesWithNoPickedApps() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");

        assertEquals(list(BANK_APP), SplitTunnelingRules.withMatchingApps(Collections.emptyList()));
    }

    @Test
    public void picksUpAnAppInstalledAfterTheRuleWasSaved() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");
        install("com.later.bank", ApplicationInfo.CATEGORY_UNDEFINED, true, true);

        assertEquals(list(BANK_APP, "com.later.bank"), SplitTunnelingRules.withMatchingApps(Collections.emptyList()));
    }

    @Test
    public void skipsAppsWithoutALauncherEntry() {
        SplitTunnelingRuleSettings.saveKeywordText("service");
        install("com.example.service", ApplicationInfo.CATEGORY_UNDEFINED, false, true);

        assertEquals(Collections.emptyList(), SplitTunnelingRules.withMatchingApps(Collections.emptyList()));
    }

    @Test
    public void skipsAppsWithoutInternetAccess() {
        SplitTunnelingRuleSettings.saveKeywordText("offline");
        install("com.example.offline", ApplicationInfo.CATEGORY_UNDEFINED, true, false);

        assertEquals(Collections.emptyList(), SplitTunnelingRules.withMatchingApps(Collections.emptyList()));
    }

    @Test
    public void skipsTheHostApp() {
        SplitTunnelingRuleSettings.saveKeywordText(this.context.getPackageName());
        install(this.context.getPackageName(), ApplicationInfo.CATEGORY_UNDEFINED, true, true);

        assertEquals(Collections.emptyList(), SplitTunnelingRules.withMatchingApps(Collections.emptyList()));
    }

    @Test
    public void matchesKeywordsAgainstTheAppLabel() {
        SplitTunnelingRuleSettings.saveKeywordText("northwind");
        install("com.example.pay", "Northwind Pay", ApplicationInfo.CATEGORY_UNDEFINED, true, true);

        assertEquals(list("com.example.pay"), SplitTunnelingRules.withMatchingApps(Collections.emptyList()));
    }

    @Test
    public void addsEveryBrowserAndNoOtherApp() {
        SplitTunnelingRuleSettings.saveCategories(EnumSet.of(SplitTunnelingCategory.BROWSER));
        installBrowser("com.example.browser.one", "Browser One");
        installBrowser("com.example.browser.two", "Browser Two");

        assertEquals(list("com.example.browser.one", "com.example.browser.two"),
                SplitTunnelingRules.withMatchingApps(Collections.emptyList()));
    }

    @Test
    public void addsBrowsersWhoseFiltersDeclareAMimeType() {
        SplitTunnelingRuleSettings.saveCategories(EnumSet.of(SplitTunnelingCategory.BROWSER));
        installBrowser("com.example.browser.plain", "Plain Browser");
        installTypedBrowser("com.example.browser.typed", "Typed Browser");
        install("com.example.mailer", "Mailer", ApplicationInfo.CATEGORY_UNDEFINED, true, true);

        assertEquals(list("com.example.browser.plain", "com.example.browser.typed"),
                SplitTunnelingRules.withMatchingApps(Collections.emptyList()));
    }

    @Test
    public void matchesChromeAndNotGoogleHomeByKeyword() {
        SplitTunnelingRuleSettings.saveKeywordText("chrome");
        installBrowser("com.android.chrome", "Chrome");
        install("com.google.android.apps.chromecast.app", "Home", ApplicationInfo.CATEGORY_UNDEFINED, true, true);

        assertEquals(list("com.android.chrome"), SplitTunnelingRules.withMatchingApps(Collections.emptyList()));
    }

    @Test
    public void readsTheInstalledAppsSortedByLabel() {
        install("com.example.zeta", "Zeta", ApplicationInfo.CATEGORY_UNDEFINED, true, true);
        install("com.example.alpha", "alpha", ApplicationInfo.CATEGORY_UNDEFINED, true, true);

        final List<String> labels = new ArrayList<>();
        for (SplitTunnelingApp app : SplitTunnelingRules.installedApps(this.context)) {
            labels.add(app.label);
        }

        assertEquals(list("alpha", BANK_APP, GAME_APP, PICKED_APP, "Zeta"), labels);
    }

    @Test
    public void recordsHowManyAppsTheRulesMatched() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");

        SplitTunnelingRules.withMatchingApps(Collections.emptyList());

        assertEquals(1, SplitTunnelingRuleSettings.matchCount());
    }

    @Test
    public void forgetsTheMatchCountWhenTheAppLookupFails() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");
        SplitTunnelingRuleSettings.saveMatchCount(4);

        SplitTunnelingRules.withMatchingApps(failingPackageManager(), list(PICKED_APP));

        assertEquals(SplitTunnelingRuleSettings.UNKNOWN_MATCH_COUNT, SplitTunnelingRuleSettings.matchCount());
    }

    @Test
    public void keepsTheAppsTheRulesMatchedBeforeWhenTheAppLookupFails() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");
        SplitTunnelingRules.withMatchingApps(this.context, list(PICKED_APP));

        assertEquals(list(PICKED_APP, BANK_APP),
                SplitTunnelingRules.withMatchingApps(failingPackageManager(), list(PICKED_APP)));
    }

    @Test
    public void dropsTheAppsOfChangedRulesWhenTheAppLookupFails() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");
        SplitTunnelingRules.withMatchingApps(this.context, list(PICKED_APP));
        SplitTunnelingRuleSettings.saveKeywordText("game");

        assertEquals(list(PICKED_APP), SplitTunnelingRules.withMatchingApps(failingPackageManager(), list(PICKED_APP)));
    }

    @Test
    public void routesOnlyTheSelectedAppsWhenNoRuleMatchedBeforeTheAppLookupFails() {
        SplitTunnelingRuleSettings.saveKeywordText("bank");

        assertEquals(list(PICKED_APP), SplitTunnelingRules.withMatchingApps(failingPackageManager(), list(PICKED_APP)));
    }

    private Context failingPackageManager() {
        return new ContextWrapper(this.context) {

            @Override
            public PackageManager getPackageManager() {
                throw new IllegalStateException("The package manager is down");
            }

        };
    }

}
