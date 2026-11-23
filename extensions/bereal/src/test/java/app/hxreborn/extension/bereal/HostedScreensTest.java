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
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class HostedScreensTest {

    @Test
    public void opensScreensThroughStockActivitiesOnMountInstalls() {
        assertHosted(PatchesSettingsActivity.class, HostedScreens.OPAQUE_HOST, PatchesSettingsActivity.class);
        assertHosted(GalleryPickerActivity.class, HostedScreens.TRANSLUCENT_HOST, GalleryPickerActivity.class);
        assertHosted(RetakeCountActivity.class, HostedScreens.TRANSLUCENT_HOST, RetakeCountActivity.class);
    }

    @Test
    public void opensDeclaredScreensDirectly() {
        final Context context = RuntimeEnvironment.getApplication();
        final ActivityInfo info = new ActivityInfo();
        info.packageName = context.getPackageName();
        info.name = RetakeCountActivity.class.getName();
        Shadows.shadowOf(context.getPackageManager()).addOrUpdateActivity(info);

        final Intent intent = HostedScreens.intent(context, RetakeCountActivity.class);

        assertEquals(RetakeCountActivity.class.getName(), intent.getComponent().getClassName());
        assertNull(intent.getAction());
    }

    @Test
    @Config(sdk = 27)
    public void opensScreensDirectlyWithoutComponentFactories() {
        final Intent intent = HostedScreens.intent(RuntimeEnvironment.getApplication(), RetakeCountActivity.class);

        assertEquals(RetakeCountActivity.class.getName(), intent.getComponent().getClassName());
        assertNull(intent.getAction());
    }

    @Test
    public void leavesOtherIntentsToTheApp() {
        assertNull(HostedScreens.instantiate(HostedScreens.OPAQUE_HOST, null));
        assertNull(HostedScreens.instantiate(HostedScreens.OPAQUE_HOST, new Intent()));
        assertNull(HostedScreens.instantiate(HostedScreens.OPAQUE_HOST, new Intent(Intent.ACTION_VIEW)));
    }

    @Test
    public void ignoresScreenActionsSentToOtherActivities() {
        final Intent settings = new Intent(HostedScreens.ACTION_SETTINGS);
        final Intent picker = new Intent(HostedScreens.ACTION_GALLERY_PICKER);

        assertNull(HostedScreens.instantiate("bereal.app.MainActivity", settings));
        assertNull(HostedScreens.instantiate(HostedScreens.TRANSLUCENT_HOST, settings));
        assertNull(HostedScreens.instantiate(HostedScreens.OPAQUE_HOST, picker));
    }

    private static void assertHosted(Class<? extends Activity> screen, String host, Class<?> created) {
        final Context context = RuntimeEnvironment.getApplication();
        final Intent intent = HostedScreens.intent(context, screen);

        assertEquals(new ComponentName(context, host), intent.getComponent());
        assertTrue(created.isInstance(HostedScreens.instantiate(intent.getComponent().getClassName(), intent)));
    }

}
