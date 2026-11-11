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
package app.hxreborn.extension.readera;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SpeechScreenLockTest.ShadowSpeechScreenLock.class)
public final class SpeechScreenLockTest {

    private Context context;

    private DevicePolicyManager policy;

    private ComponentName admin;

    @Before
    public void prepare() {
        this.context = RuntimeEnvironment.getApplication();
        this.policy = (DevicePolicyManager) this.context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.admin = new ComponentName(this.context, ScreenLockAdmin.class);
        ShadowSpeechScreenLock.applied = true;
        Assert.assertTrue(SpeechScreenLock.applied());
        clearPreferences();
    }

    @After
    public void clearPreferences() {
        this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void keepSwitchOffByDefault() {
        Assert.assertFalse(SpeechScreenLock.enabled(this.context));
    }

    @Test
    public void requestGrantBeforeEnabling() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        SpeechScreenLock.onSwitchToggled(activity, true);
        Intent request = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
        Assert.assertEquals(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN, request.getAction());
        Assert.assertEquals(this.admin, request.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN));
        Assert.assertFalse(SpeechScreenLock.enabled(this.context));
    }

    @Test
    public void keepDeclinedGrantDisabled() {
        SpeechScreenLock.onActivationResult(this.context);
        Assert.assertFalse(SpeechScreenLock.enabled(this.context));
    }

    @Test
    public void enableAfterGrant() {
        Shadows.shadowOf(this.policy).setActiveAdmin(this.admin);
        SpeechScreenLock.onActivationResult(this.context);
        Assert.assertTrue(SpeechScreenLock.enabled(this.context));
    }

    @Test
    public void showSwitchOffAfterAdminRevoked() {
        SpeechScreenLock.setEnabled(this.context, true);
        Assert.assertFalse(SpeechScreenLock.active(this.context));
        Shadows.shadowOf(this.policy).setActiveAdmin(this.admin);
        Assert.assertTrue(SpeechScreenLock.active(this.context));
    }

    @Test
    public void removeAdminWhenDisabled() {
        Shadows.shadowOf(this.policy).setActiveAdmin(this.admin);
        SpeechScreenLock.setEnabled(this.context, true);
        SpeechScreenLock.onSwitchToggled(Robolectric.buildActivity(Activity.class).setup().get(), false);
        Assert.assertFalse(this.policy.isAdminActive(this.admin));
        Assert.assertFalse(SpeechScreenLock.enabled(this.context));
    }

    @Test
    public void removeAdminOnReset() {
        Shadows.shadowOf(this.policy).setActiveAdmin(this.admin);
        SpeechScreenLock.setEnabled(this.context, true);
        PatchSettings.resetToDefaults();
        Assert.assertFalse(this.policy.isAdminActive(this.admin));
        Assert.assertFalse(SpeechScreenLock.enabled(this.context));
    }

    @Test
    public void skipLockWhenSwitchOff() {
        Shadows.shadowOf(this.policy).setActiveAdmin(this.admin);
        SpeechScreenLock.onChapterSpeechStart(this.context);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertFalse(deviceLocked());
    }

    @Test
    public void lockScreenAfterGrant() {
        Shadows.shadowOf(this.policy).setActiveAdmin(this.admin);
        SpeechScreenLock.setEnabled(this.context, true);
        SpeechScreenLock.onChapterSpeechStart(this.context);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertTrue(deviceLocked());
    }

    @Test
    public void skipOmittedPatchWithRetainedPreference() {
        ShadowSpeechScreenLock.applied = false;
        Assert.assertFalse(SpeechScreenLock.applied());
        Shadows.shadowOf(this.policy).setActiveAdmin(this.admin);
        SpeechScreenLock.setEnabled(this.context, true);
        SpeechScreenLock.onChapterSpeechStart(this.context);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertFalse(deviceLocked());
    }

    @Test
    public void showToastWhenAdminMissing() {
        SpeechScreenLock.setEnabled(this.context, true);
        SpeechScreenLock.onChapterSpeechStart(this.context);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(SpeechScreenLock.ADMIN_MISSING_NOTICE, ShadowToast.getTextOfLatestToast());
        Assert.assertFalse(deviceLocked());
    }

    private boolean deviceLocked() {
        return ((KeyguardManager) this.context.getSystemService(Context.KEYGUARD_SERVICE)).isDeviceLocked();
    }

    @Implements(value = SpeechScreenLock.class, isInAndroidSdk = false)
    public static final class ShadowSpeechScreenLock {

        private static boolean applied;

        @Implementation
        protected static boolean applied() {
            return applied;
        }

    }

}
