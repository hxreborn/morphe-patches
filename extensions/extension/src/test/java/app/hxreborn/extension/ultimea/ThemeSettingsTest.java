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
package app.hxreborn.extension.ultimea;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class ThemeSettingsTest {

    private static final int DARK = 1;

    private static final int AMOLED = 2;

    private static final int INVALID_MODE = 0;

    private static final int DARK_BACKGROUND = 0xff121216;

    private static final int AMOLED_BACKGROUND = 0xff000000;

    private ActivityController<Activity> controller;

    private Activity activity;

    @Before
    public void createActivity() {
        org.robolectric.util.ReflectionHelpers.setStaticField(ThemeSettings.class, "initialized", false);
        this.controller = Robolectric.buildActivity(Activity.class).setup();
        this.activity = this.controller.get();
        this.activity.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
        ThemeSettings.initialize(this.activity);
    }

    @Test
    public void rejectUnrelatedImageBuffers() {
        Assert.assertNull(ThemeArtwork.decodeHero(null, 0));
        Assert.assertNull(ThemeArtwork.decodeHero(ByteBuffer.allocate(23), 0));
        ByteBuffer unrelated = ByteBuffer.allocate(24);
        unrelated.putInt(0, 0x89504e47);
        unrelated.putInt(16, 1081);
        unrelated.putInt(20, 874);
        unrelated.position(4);
        Assert.assertNull(ThemeArtwork.decodeHero(unrelated, 0));
        Assert.assertEquals(4, unrelated.position());
    }

    @After
    public void destroyActivity() {
        this.activity.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
        this.controller.pause().stop().destroy();
    }

    @Test
    public void preserveAmoledChoiceAfterRejectingInvalidMode() throws IOException {
        Assert.assertTrue(ThemeSettings.setMode(this.activity, AMOLED));
        Assert.assertFalse(ThemeSettings.setMode(this.activity, INVALID_MODE));
        Assert.assertEquals(AMOLED, ThemeSettings.getMode(this.activity));
        try (FileInputStream input = new FileInputStream(new File(this.activity.getFilesDir(), "hx_ultimea_theme"))) {
            Assert.assertEquals(DARK, input.read());
            Assert.assertEquals(-1, input.read());
        }
    }

    @Test
    public void requestLightSystemIconsAndPreserveOtherStyleFields() throws JSONException {
        JSONObject style = new JSONObject().put("statusBarIconBrightness", "Brightness.dark")
            .put("systemNavigationBarIconBrightness", "Brightness.dark")
            .put("systemNavigationBarColor", -1)
            .put("unrelated", "preserved");
        ThemeSettings.rewriteSystemBars(style);
        Assert.assertEquals("Brightness.light", style.getString("statusBarIconBrightness"));
        Assert.assertEquals("Brightness.light", style.getString("systemNavigationBarIconBrightness"));
        Assert.assertEquals(DARK_BACKGROUND, style.getInt("statusBarColor"));
        Assert.assertEquals("preserved", style.getString("unrelated"));
        Assert.assertFalse(style.getBoolean("systemNavigationBarContrastEnforced"));
    }

    @Test
    public void useBlackSystemBarsAfterLoadingAmoledChoice() throws JSONException {
        Assert.assertEquals(DARK, ThemeSettings.getMode(this.activity));
        Assert.assertTrue(ThemeSettings.setMode(this.activity, AMOLED));
        org.robolectric.util.ReflectionHelpers.setStaticField(ThemeSettings.class, "initialized", false);
        ThemeSettings.initialize(this.activity);
        JSONObject style = new JSONObject();
        ThemeSettings.rewriteSystemBars(style);
        Assert.assertEquals(AMOLED_BACKGROUND, style.getInt("statusBarColor"));
        Assert.assertEquals(AMOLED_BACKGROUND, ThemeSettings.pageBackground());
    }

    @Test
    public void retainAppliedThemeUntilRestart() throws IOException {
        Assert.assertTrue(ThemeSettings.setMode(this.activity, AMOLED));
        ThemeSettings.initialize(this.activity);
        Assert.assertEquals(DARK_BACKGROUND, ThemeSettings.pageBackground());
        Assert.assertEquals(DARK_BACKGROUND, this.activity.getWindow().getStatusBarColor());
        Assert.assertEquals(AMOLED, ThemeSettings.getMode(this.activity));
        try (FileInputStream input = new FileInputStream(new File(this.activity.getFilesDir(), "hx_ultimea_theme"))) {
            Assert.assertEquals(DARK, input.read());
        }
    }

    @Test
    public void fallBackToNativeDefaultWhenModePublicationFails() {
        Assert.assertTrue(ThemeSettings.setMode(this.activity, AMOLED));
        org.robolectric.util.ReflectionHelpers.setStaticField(ThemeSettings.class, "initialized", false);
        try (ActivityController<MissingFilesActivity> broken = Robolectric.buildActivity(MissingFilesActivity.class)
            .setup()) {
            ThemeSettings.initialize(broken.get());
            Assert.assertEquals(DARK_BACKGROUND, ThemeSettings.pageBackground());
            Assert.assertEquals(DARK_BACKGROUND, broken.get().getWindow().getStatusBarColor());
            Assert.assertEquals(AMOLED, ThemeSettings.getMode(broken.get()));
        }
    }

    @Test
    public void preserveExistingAmoledFileWhenPublicationFails() throws IOException {
        Assert.assertTrue(ThemeSettings.setMode(this.activity, AMOLED));
        org.robolectric.util.ReflectionHelpers.setStaticField(ThemeSettings.class, "initialized", false);
        ThemeSettings.prepare(this.activity);
        File directory = this.activity.getFilesDir();
        File modeFile = new File(directory, "hx_ultimea_theme");
        Assert.assertTrue(ThemeSettings.setMode(this.activity, DARK));
        org.robolectric.util.ReflectionHelpers.setStaticField(ThemeSettings.class, "initialized", false);
        Context failingPublication = new android.content.ContextWrapper(this.activity) {
            private int directoryReads;

            @Override
            public File getFilesDir() {
                return ++this.directoryReads == 2 ? modeFile : directory;
            }
        };
        ThemeSettings.prepare(failingPublication);
        Assert.assertEquals(AMOLED_BACKGROUND, ThemeSettings.pageBackground());
        try (FileInputStream input = new FileInputStream(modeFile)) {
            Assert.assertEquals(AMOLED, input.read());
            Assert.assertEquals(-1, input.read());
        }
    }

    @Test
    public void preserveAppliedModeAfterSelectionCommitFailure() throws IOException {
        android.content.SharedPreferences preferences = this.activity
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, 0);
        int[] commits = { 0 };
        android.content.SharedPreferences failing = (android.content.SharedPreferences) java.lang.reflect.Proxy
            .newProxyInstance(android.content.SharedPreferences.class.getClassLoader(),
                    new Class<?>[] { android.content.SharedPreferences.class }, (proxy, method, arguments) -> {
                        if (!"edit".equals(method.getName())) {
                            return method.invoke(preferences, arguments);
                        }
                        android.content.SharedPreferences.Editor editor = preferences.edit();
                        return java.lang.reflect.Proxy.newProxyInstance(
                                android.content.SharedPreferences.Editor.class.getClassLoader(),
                                new Class<?>[] { android.content.SharedPreferences.Editor.class },
                                (editorProxy, operation, values) -> {
                                    Object result = operation.invoke(editor, values);
                                    if ("commit".equals(operation.getName())) {
                                        return ++commits[0] != 1;
                                    }
                                    return result == editor ? editorProxy : result;
                                });
                    });
        android.content.Context wrapped = new android.content.ContextWrapper(this.activity) {
            @Override
            public android.content.SharedPreferences getSharedPreferences(String name, int mode) {
                return failing;
            }
        };
        Assert.assertFalse(ThemeSettings.setMode(wrapped, AMOLED));
        Assert.assertEquals(DARK, ThemeSettings.getMode(this.activity));
        Assert.assertEquals(DARK_BACKGROUND, ThemeSettings.pageBackground());
        try (FileInputStream input = new FileInputStream(new File(this.activity.getFilesDir(), "hx_ultimea_theme"))) {
            Assert.assertEquals(DARK, input.read());
        }
        Assert.assertEquals(2, commits[0]);
    }

    @Test
    public void publishModeBeforeAnyActivityInitialization() throws IOException {
        Assert.assertTrue(ThemeSettings.setMode(this.activity, AMOLED));
        org.robolectric.util.ReflectionHelpers.setStaticField(ThemeSettings.class, "initialized", false);
        ThemeSettings.prepare(this.activity.getApplicationContext());
        Assert.assertEquals(AMOLED_BACKGROUND, ThemeSettings.pageBackground());
        try (FileInputStream input = new FileInputStream(new File(this.activity.getFilesDir(), "hx_ultimea_theme"))) {
            Assert.assertEquals(AMOLED, input.read());
        }
    }

    @Test
    @Config(sdk = 26)
    public void enforceDecorIconFlagsOnAndroidEight() {
        assertDecorIconFlags();
    }

    @Test
    @Config(sdk = 33)
    public void enforceDecorIconFlagsOnAndroidThirteen() {
        assertDecorIconFlags();
    }

    private void assertDecorIconFlags() {
        android.view.View decor = this.activity.getWindow().getDecorView();
        int unrelated = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        int light = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decor.setSystemUiVisibility(unrelated | light);
        ThemeSettings.initialize(this.activity);
        Assert.assertEquals(unrelated, decor.getSystemUiVisibility());
        decor.setSystemUiVisibility(unrelated | light);
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(decor, "dispatchSystemUiVisibilityChanged",
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class, unrelated | light));
        Assert.assertEquals(unrelated, decor.getSystemUiVisibility());
    }

    public static class MissingFilesActivity extends Activity {

        @Override
        public File getFilesDir() {
            return new File(super.getFilesDir(), "missing/child");
        }

    }

}
