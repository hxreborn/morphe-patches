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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public final class SplitTunnelingRuleEditorTest {

    private static final long PREVIEW_TIMEOUT_MS = 5000;

    private static final long PREVIEW_POLL_MS = 10;

    private static final int SAVED_MATCH_COUNT = 4;

    private static final String BANK_APP = "com.example.bank";

    public static final class UnreadableAppsActivity extends Activity {

        @Override
        public Context getApplicationContext() {
            return new ContextWrapper(super.getApplicationContext()) {

                @Override
                public PackageManager getPackageManager() {
                    throw new IllegalStateException("The package manager is down");
                }

            };
        }

    }

    private Activity activity;

    @Before
    public void openAnActivityWithoutRules() {
        final Context context = PatchedBuild.useApplicationContext();
        context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit();
        this.activity = Robolectric.buildActivity(Activity.class).setup().get();
    }

    private void installBankApp() {
        final ShadowPackageManager packages = shadowOf(this.activity.getPackageManager());
        final PackageInfo info = new PackageInfo();
        info.packageName = BANK_APP;
        info.applicationInfo = new ApplicationInfo();
        info.applicationInfo.packageName = BANK_APP;
        info.applicationInfo.nonLocalizedLabel = BANK_APP;
        info.requestedPermissions = new String[] { Manifest.permission.INTERNET };
        info.requestedPermissionsFlags = new int[] { PackageInfo.REQUESTED_PERMISSION_GRANTED };
        packages.installPackage(info);
        final ComponentName launcher = new ComponentName(BANK_APP, BANK_APP + ".Main");
        packages.addActivityIfNotPresent(launcher);
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);
        packages.addIntentFilterForActivity(launcher, filter);
    }

    private static TextView findText(View root, String text) {
        if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) {
                final TextView found = findText(group.getChildAt(index), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static EditText findEditText(View root) {
        if (root instanceof EditText) {
            return (EditText) root;
        }
        if (root instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) {
                final EditText found = findEditText(group.getChildAt(index));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static TextView awaitText(View root, String text) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + PREVIEW_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper();
            final TextView found = findText(root, text);
            if (found != null) {
                return found;
            }
            Thread.sleep(PREVIEW_POLL_MS);
        }
        return null;
    }

    @Test
    public void dismissesTheDialogBeforeReportingTheSavedRules() {
        final List<Boolean> dialogShowingWhenReported = new ArrayList<>();
        SplitTunnelingRuleEditor.show(this.activity,
                () -> dialogShowingWhenReported.add(ShadowDialog.getLatestDialog().isShowing()));
        final Dialog dialog = ShadowDialog.getLatestDialog();
        final TextView save = findText(dialog.getWindow().getDecorView(), "Save");
        assertNotNull(save);

        save.performClick();

        assertEquals(1, dialogShowingWhenReported.size());
        assertFalse(dialogShowingWhenReported.get(0));
    }

    @Test
    public void asksForTheStartOfAWordInTheName() {
        SplitTunnelingRuleEditor.show(this.activity, () -> {
        });

        assertNotNull(
                findText(ShadowDialog.getLatestDialog().getWindow().getDecorView(), "A word in the name starts with"));
    }

    @Test
    public void showsOneLinePreviewWithoutRules() {
        SplitTunnelingRuleEditor.show(this.activity, () -> {
        });

        final View decor = ShadowDialog.getLatestDialog().getWindow().getDecorView();
        assertNotNull(findText(decor, "No rule set."));
        assertTrue(findText(decor, "View apps").getVisibility() == View.GONE);
    }

    @Test
    public void savesZeroWhenTheLookupSucceedsWithoutMatches() throws InterruptedException {
        SplitTunnelingRuleSettings.saveKeywordText("zzzzz");
        SplitTunnelingRuleSettings.saveMatchCount(SAVED_MATCH_COUNT);
        SplitTunnelingRuleEditor.show(this.activity, () -> {
        });
        final View decor = ShadowDialog.getLatestDialog().getWindow().getDecorView();

        assertNotNull(awaitText(decor, "No installed app matches."));
        findText(decor, "Save").performClick();

        assertEquals(0, SplitTunnelingRuleSettings.matchCount());
    }

    @Test
    public void savesTheMatchesOfThePreviewWithTheChangedRules() throws InterruptedException {
        installBankApp();
        SplitTunnelingRuleSettings.saveKeywordText("zzzzz");
        SplitTunnelingRuleEditor.show(this.activity, () -> {
        });
        final View decor = ShadowDialog.getLatestDialog().getWindow().getDecorView();
        findEditText(decor).setText("bank");

        assertNotNull(awaitText(decor, "1 app matches"));
        findText(decor, "Save").performClick();

        assertEquals(Collections.singletonList(BANK_APP), SplitTunnelingRuleSettings.matchedApps());
    }

    @Test
    public void showsAnErrorAndSavesAnUnknownCountWhenTheLookupFails() throws InterruptedException {
        final Activity unreadable = Robolectric.buildActivity(UnreadableAppsActivity.class).setup().get();
        SplitTunnelingRuleSettings.saveKeywordText("zzzzz");
        SplitTunnelingRuleSettings.saveMatchCount(SAVED_MATCH_COUNT);
        SplitTunnelingRuleEditor.show(unreadable, () -> {
        });
        final View decor = ShadowDialog.getLatestDialog().getWindow().getDecorView();

        assertNotNull(awaitText(decor, "Could not read installed apps."));
        assertNull(findText(decor, "No installed app matches."));
        findText(decor, "Save").performClick();

        assertEquals(SplitTunnelingRuleSettings.UNKNOWN_MATCH_COUNT, SplitTunnelingRuleSettings.matchCount());
    }

    @Test
    public void boundsTheVisibleHeightOfTheKeywordField() {
        SplitTunnelingRuleSettings.saveKeywordText("a, b, c, d, e, f, g, h");
        SplitTunnelingRuleEditor.show(this.activity, () -> {
        });

        final EditText keywords = findEditText(ShadowDialog.getLatestDialog().getWindow().getDecorView());

        assertNotNull(keywords);
        assertTrue(keywords.getMaxLines() < Integer.MAX_VALUE);
    }

}
