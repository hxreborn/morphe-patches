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
package app.hxreborn.extension.xtreamplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.BAKLAVA, manifest = Config.NONE)
public final class AndroidAutoInstallerTest {

    private static final String PACKAGE_INSTALLER = "com.google.android.packageinstaller";

    private static final String ANDROID_AUTO = "com.google.android.projection.gearhead";

    private static final String PLAY_STORE = "com.android.vending";

    private static final String SIDELOAD_INSTALLER = "app.morphe.manager";

    private static final String PREFERENCES = "hx_xtreamplayer_android_auto";

    private static final long UPDATE_TIME = 123456L;

    private ActivityController<Activity> controller;

    private Activity activity;

    private ShadowPackageManager packages;

    @Before
    public void createSideloadedActivity() throws Exception {
        ReflectionHelpers.setStaticField(AndroidAutoInstaller.class, "promptShown", false);
        this.controller = Robolectric.buildActivity(Activity.class).setup().visible();
        this.activity = this.controller.get();
        this.packages = Shadows.shadowOf(this.activity.getPackageManager());
        installPackage(PACKAGE_INSTALLER);
        installPackage(ANDROID_AUTO);
        this.packages.setInstallSourceInfo(this.activity.getPackageName(), SIDELOAD_INSTALLER, SIDELOAD_INSTALLER);
        PackageInfo installed = this.activity.getPackageManager().getPackageInfo(this.activity.getPackageName(), 0);
        installed.lastUpdateTime = UPDATE_TIME;
        this.packages.installPackage(installed);
        this.activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @After
    public void clearPromptState() {
        AlertDialog prompt = ShadowAlertDialog.getLatestAlertDialog();
        if (prompt != null) {
            prompt.dismiss();
        }
        this.activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
        this.controller.pause().stop().destroy();
        ReflectionHelpers.setStaticField(AndroidAutoInstaller.class, "promptShown", false);
    }

    @Test
    public void promptsForSideloadedInstallation() {
        Assert.assertNotNull(prompt());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public void skipsPromptBeforeAndroid16() {
        Assert.assertNull(prompt());
    }

    @Test
    public void skipsPromptForPlayInitiator() {
        this.packages.setInstallSourceInfo(this.activity.getPackageName(), PLAY_STORE, SIDELOAD_INSTALLER);
        Assert.assertNull(prompt());
    }

    @Test
    public void skipsPromptForPlayInstallThroughPackageInstaller() {
        this.packages.setInstallSourceInfo(this.activity.getPackageName(), PACKAGE_INSTALLER, PLAY_STORE);
        Assert.assertNull(prompt());
    }

    @Test
    public void promptsWhenPlayInstallerHasDifferentInitiator() {
        this.packages.setInstallSourceInfo(this.activity.getPackageName(), SIDELOAD_INSTALLER, PLAY_STORE);
        Assert.assertNotNull(prompt());
    }

    @Test
    public void skipsPromptWithoutAndroidAuto() {
        this.packages.removePackage(ANDROID_AUTO);
        Assert.assertNull(prompt());
    }

    @Test
    public void skipsPromptWithoutPackageInstaller() {
        this.packages.removePackage(PACKAGE_INSTALLER);
        Assert.assertNull(prompt());
    }

    @Test
    public void remembersDismissalForInstalledUpdate() {
        prompt().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();
        ReflectionHelpers.setStaticField(AndroidAutoInstaller.class, "promptShown", false);
        AndroidAutoInstaller.promptIfNeeded(this.activity);
        ShadowLooper.idleMainLooper();
        Assert.assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
    }

    @Test
    public void promptsAfterAppUpdate() throws Exception {
        AlertDialog dismissed = prompt();
        dismissed.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();
        ReflectionHelpers.setStaticField(AndroidAutoInstaller.class, "promptShown", false);
        PackageInfo installed = this.activity.getPackageManager().getPackageInfo(this.activity.getPackageName(), 0);
        installed.lastUpdateTime = UPDATE_TIME + 1;
        this.packages.installPackage(installed);
        Assert.assertNotSame(dismissed, prompt());
        Assert.assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
    }

    @Test
    public void remembersCancelledPromptForInstalledUpdate() {
        prompt().cancel();
        ShadowLooper.idleMainLooper();
        ReflectionHelpers.setStaticField(AndroidAutoInstaller.class, "promptShown", false);
        AndroidAutoInstaller.promptIfNeeded(this.activity);
        ShadowLooper.idleMainLooper();
        Assert.assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
    }

    @Test
    public void opensInstallerWithReadPermissionOnContinue() {
        prompt().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        Intent installation = Shadows.shadowOf(this.activity).getNextStartedActivity();
        Assert.assertEquals(Intent.ACTION_INSTALL_PACKAGE, installation.getAction());
        Assert.assertEquals(PACKAGE_INSTALLER, installation.getPackage());
        Assert.assertEquals(SourceApkProvider.uri(this.activity), installation.getData());
        Assert.assertEquals(SourceApkProvider.APK_MIME_TYPE, installation.getType());
        Assert.assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, installation.getFlags());
        Assert.assertEquals(PLAY_STORE, installation.getStringExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME));
        Assert.assertTrue(installation.getBooleanExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, false));
    }

    private AlertDialog prompt() {
        AndroidAutoInstaller.promptIfNeeded(this.activity);
        ShadowLooper.idleMainLooper();
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    private void installPackage(String name) {
        PackageInfo installed = new PackageInfo();
        installed.packageName = name;
        this.packages.installPackage(installed);
    }

}
