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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class SplitTunnelingRules {

    private static final String BROWSER_PROBE_URL = "https://example.com";

    private static final String BROWSER_PROBE_MIME_TYPE = "text/html";

    private SplitTunnelingRules() {

    }

    public static List<String> withMatchingApps(List<String> selected) {
        if (!SplitTunnelingRuleSettings.isActive()) {
            return selected;
        }

        final Context context = Utils.getContext();
        if (context == null) {
            return selected;
        }
        return withMatchingApps(context, selected);
    }

    static List<String> withMatchingApps(Context context, List<String> selected) {
        try {
            final Set<SplitTunnelingCategory> categories = SplitTunnelingRuleSettings.categories();
            final List<String> keywords = SplitTunnelingRuleSettings.keywords();
            final List<SplitTunnelingApp> matches = new SplitTunnelingRuleMatcher(categories, keywords)
                .select(installedApps(context));
            SplitTunnelingRuleSettings.saveMatches(categories, keywords, matches);
            return union(selected, SplitTunnelingApp.packageNames(matches));
        } catch (RuntimeException ex) {
            Log.w(VpnSettings.LOG_TAG, "Installed app lookup failed, keeping the apps these rules matched last time",
                    ex);
            SplitTunnelingRuleSettings.saveMatchCount(SplitTunnelingRuleSettings.UNKNOWN_MATCH_COUNT);
            return union(selected, SplitTunnelingRuleSettings.matchedApps());
        }
    }

    private static List<String> union(List<String> selected, List<String> matchedApps) {
        final Set<String> merged = new LinkedHashSet<>(selected);
        merged.addAll(matchedApps);
        return new ArrayList<>(merged);
    }

    static List<SplitTunnelingApp> installedApps(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final Set<String> browsers = browserPackages(packageManager);
        final List<SplitTunnelingApp> apps = new ArrayList<>();
        for (ApplicationInfo info : packageManager.getInstalledApplications(0)) {
            if (isSelectable(context, packageManager, info.packageName)) {
                apps.add(new SplitTunnelingApp(info.packageName, packageManager.getApplicationLabel(info).toString(),
                        applicationCategory(info), browsers.contains(info.packageName)));
            }
        }
        Collections.sort(apps, (left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.label, right.label));
        return apps;
    }

    private static Set<String> browserPackages(PackageManager packageManager) {
        final Uri probeUri = Uri.parse(BROWSER_PROBE_URL);
        final Intent untypedProbe = new Intent(Intent.ACTION_VIEW, probeUri).addCategory(Intent.CATEGORY_BROWSABLE);
        final Intent typedProbe = new Intent(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE)
            .setDataAndType(probeUri, BROWSER_PROBE_MIME_TYPE);
        final Set<String> browsers = new HashSet<>();
        for (Intent probe : Arrays.asList(untypedProbe, typedProbe)) {
            for (ResolveInfo resolved : packageManager.queryIntentActivities(probe, PackageManager.MATCH_ALL)) {
                browsers.add(resolved.activityInfo.packageName);
            }
        }
        return browsers;
    }

    private static boolean isSelectable(Context context, PackageManager packageManager, String packageName) {
        return !packageName.equals(context.getPackageName())
                && packageManager.getLaunchIntentForPackage(packageName) != null && packageManager
                    .checkPermission(Manifest.permission.INTERNET, packageName) == PackageManager.PERMISSION_GRANTED;
    }

    private static int applicationCategory(ApplicationInfo info) {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? info.category : ApplicationInfo.CATEGORY_UNDEFINED;
    }

}
