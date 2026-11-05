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
package app.hxreborn.extension.aliexpress;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ahe.android.hybridengine.widget.AHEWidgetNode;
import com.aliexpress.android.aehome.ahe.ahenode.HomeAHEAEImageViewWidgetNode;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.Q, manifest = Config.NONE, shadows = DarkThemeTest.AppliedTheme.class)
public class DarkThemeTest {

    @Before
    public void initializeDefaults() {
        clearPreferences();
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
    }

    @After
    public void clearPreferences() {
        DarkTheme.recordHostDarkMode(false);
        Context application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit();
        application.getSharedPreferences(DarkTheme.NATIVE_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void enableDarkThemeWhenPatchApplied() {
        Assert.assertTrue(PatchSettings.darkTheme());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void skipDarkThemeBeforeAndroid10() {
        Assert.assertFalse(PatchSettings.darkTheme());
    }

    @Test
    public void allowDarkWhenNativeSwitchWasNeverChanged() {
        Assert.assertTrue(DarkTheme.nativeSwitchAllowsDark(RuntimeEnvironment.getApplication()));
    }

    @Test
    public void refuseDarkWhenNativeSwitchWasTurnedOff() {
        Context application = RuntimeEnvironment.getApplication();
        application.getSharedPreferences(DarkTheme.NATIVE_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(DarkTheme.NATIVE_CHOICE_MADE_KEY, "true")
            .putString(DarkTheme.NATIVE_CHOICE_KEY, "false")
            .commit();

        Assert.assertFalse(DarkTheme.nativeSwitchAllowsDark(application));
    }

    @Test
    public void followDarkModeWhenNativeSwitchWasNeverChanged() {
        Assert.assertTrue(DarkTheme.followsDarkMode());
    }

    @Test
    public void ignoreDarkModeWhenNativeSwitchWasTurnedOff() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(DarkTheme.NATIVE_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(DarkTheme.NATIVE_CHOICE_MADE_KEY, "true")
            .putString(DarkTheme.NATIVE_CHOICE_KEY, "false")
            .commit();

        Assert.assertFalse(DarkTheme.followsDarkMode());
    }

    @Test
    public void keepFillerColorWhenHostHasNoDarkBackgroundToken() {
        Assert.assertEquals(Color.WHITE,
                DarkTheme.hostWhiteBackground(RuntimeEnvironment.getApplication(), Color.WHITE));
    }

    @Test
    @Config(qualifiers = "night")
    public void lightenQuantityControlAtNight() {
        Context application = RuntimeEnvironment.getApplication();
        LinearLayout control = new LinearLayout(application);
        Button minus = new Button(application);
        minus.setBackground(new ColorDrawable(Color.BLACK));
        TextView count = new TextView(application);
        count.setTextColor(0xFF191919);
        control.addView(minus);
        control.addView(count);

        DarkTheme.darkenQuantityControl(control);

        Assert.assertEquals(DarkColors.primaryText(), count.getCurrentTextColor());
        Assert.assertNotNull(minus.getBackground().getColorFilter());
    }

    @Test
    @Config(qualifiers = "night")
    public void dimWideImageBehindLightFrameAtNight() {
        ImageView image = backdropImageView(1348, 127);

        DarkTheme.dimBackdropImage(lightFrameImage(), image);
        ShadowLooper.idleMainLooper();

        Assert.assertNotNull(image.getColorFilter());
    }

    @Test
    @Config(qualifiers = "night")
    public void keepSquareImageBehindLightFrameAtNight() {
        ImageView image = backdropImageView(419, 353);

        DarkTheme.dimBackdropImage(lightFrameImage(), image);
        ShadowLooper.idleMainLooper();

        Assert.assertNull(image.getColorFilter());
    }

    @Test
    @Config(qualifiers = "night")
    public void dimSearchImageOnLightSurfaceAndLightenItsText() {
        ImageView image = backdropImageView(1440, 123);
        TextView text = new TextView(RuntimeEnvironment.getApplication());
        text.setTextColor(0xFF5A0020);
        ((ViewGroup) image.getParent()).addView(text);

        DarkTheme.dimSearchBackdropImage(new AHEWidgetNode(0xFFF6F8F9), image);
        ShadowLooper.idleMainLooper();

        Assert.assertNotNull(image.getColorFilter());
        Assert.assertEquals(DarkColors.primaryText(), text.getCurrentTextColor());
    }

    @Test
    @Config(qualifiers = "night")
    public void keepSearchImageWithoutLightSurface() {
        ImageView image = backdropImageView(1440, 123);

        DarkTheme.dimSearchBackdropImage(new AHEWidgetNode(0), image);
        ShadowLooper.idleMainLooper();

        Assert.assertNull(image.getColorFilter());
    }

    @Test
    @Config(qualifiers = "night")
    public void styleLoadedPageHeaderAtNight() {
        ScriptRecordingPage page = new ScriptRecordingPage(RuntimeEnvironment.getApplication());

        DarkTheme.revealWebPage(page, pageLoadedMessage());

        Assert.assertTrue(page.script.contains(".header-navbar"));
    }

    @Test
    @Config(qualifiers = "night")
    public void keepPageHeaderUntilLoadFinishes() {
        ScriptRecordingPage page = new ScriptRecordingPage(RuntimeEnvironment.getApplication());
        Message started = Message.obtain();
        started.what = 400;

        DarkTheme.revealWebPage(page, started);

        Assert.assertNull(page.script);
    }

    @Test
    public void keepPageHeaderByDay() {
        ScriptRecordingPage page = new ScriptRecordingPage(RuntimeEnvironment.getApplication());

        DarkTheme.revealWebPage(page, pageLoadedMessage());

        Assert.assertNull(page.script);
    }

    @Test
    public void keepWideImageByDay() {
        ImageView image = backdropImageView(1348, 127);

        DarkTheme.dimBackdropImage(lightFrameImage(), image);
        ShadowLooper.idleMainLooper();

        Assert.assertNull(image.getColorFilter());
    }

    @Test
    public void keepQuantityControlByDay() {
        Context application = RuntimeEnvironment.getApplication();
        LinearLayout control = new LinearLayout(application);
        TextView count = new TextView(application);
        count.setTextColor(0xFF191919);
        control.addView(count);

        DarkTheme.darkenQuantityControl(control);

        Assert.assertEquals(0xFF191919, count.getCurrentTextColor());
    }

    @Test
    @Config(qualifiers = "night")
    public void recolorTemplateWhenHostLeavesDarkModeOff() {
        Assert.assertEquals(DarkColors.night(DarkColors.BACKGROUND_TYPE, Color.WHITE),
                DarkTheme.templateColor(new AHEWidgetNode(Color.WHITE), DarkColors.BACKGROUND_TYPE, Color.WHITE));
    }

    @Test
    @Config(qualifiers = "night")
    public void keepTemplateColorWhenHostDarkModeIsOn() {
        DarkTheme.recordHostDarkMode(true);

        Assert.assertEquals(Color.WHITE,
                DarkTheme.templateColor(new AHEWidgetNode(Color.WHITE), DarkColors.BACKGROUND_TYPE, Color.WHITE));
    }

    @Test
    @Config(qualifiers = "night")
    public void keepQuantityControlWhenHostDarkModeIsOn() {
        DarkTheme.recordHostDarkMode(true);
        Context application = RuntimeEnvironment.getApplication();
        LinearLayout control = new LinearLayout(application);
        TextView count = new TextView(application);
        count.setTextColor(0xFF191919);
        control.addView(count);

        DarkTheme.darkenQuantityControl(control);

        Assert.assertEquals(0xFF191919, count.getCurrentTextColor());
    }

    private static Message pageLoadedMessage() {
        Message loaded = Message.obtain();
        loaded.what = 401;
        return loaded;
    }

    public static final class ScriptRecordingPage extends View {

        String script;

        public ScriptRecordingPage(Context context) {
            super(context);
        }

        public void evaluateJavascript(String script, ValueCallback<String> callback) {
            this.script = script;
        }

    }

    private static HomeAHEAEImageViewWidgetNode lightFrameImage() {
        AHEWidgetNode frame = new AHEWidgetNode(Color.WHITE);
        return (HomeAHEAEImageViewWidgetNode) frame.add(new HomeAHEAEImageViewWidgetNode());
    }

    private static ImageView backdropImageView(int width, int height) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        FrameLayout bar = new FrameLayout(activity);
        activity.setContentView(bar);
        ImageView image = new ImageView(activity);
        image.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        bar.addView(image);
        return image;
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static class AppliedTheme {

        @Implementation
        protected static boolean darkTheme() {
            return true;
        }

        @Implementation
        protected static String darkThemeName() {
            return "Fix dark mode";
        }

        @Implementation
        protected static String darkThemeDescription() {
            return "Fixes the screens and web pages the built-in Dark mode leaves white.";
        }

    }

}
