/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.cx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.TypedValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class AmoledThemeTest {

    private static final int NIGHT = Configuration.UI_MODE_NIGHT_YES;

    private static final int DAY = Configuration.UI_MODE_NIGHT_NO;

    private static final int OVERLAY = android.R.style.Theme_Translucent;

    private static ThemedActivity activity(int uiMode, int overlayStyle) {
        final ThemedActivity activity = Robolectric.buildActivity(ThemedActivity.class).create().get();
        activity.override = new OverlayResources(activity.getResources(), uiMode, overlayStyle);
        return activity;
    }

    private static ThemedActivity activityWithSwitch(Object value, int uiMode, int overlayStyle) {
        final ThemedActivity activity = activity(uiMode, overlayStyle);
        final SharedPreferences.Editor editor = activity
            .getSharedPreferences(activity.getPackageName() + "_preferences", Context.MODE_PRIVATE)
            .edit();
        if (value instanceof Boolean) {
            editor.putBoolean("hx_amoled", (Boolean) value);
        } else if (value != null) {
            editor.putString("hx_amoled", (String) value);
        }
        editor.commit();
        return activity;
    }

    private static boolean isTranslucent(Activity activity) {
        final TypedValue value = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.windowIsTranslucent, value, true);
        return value.data != 0;
    }

    private static void assertOverlayApplied(ThemedActivity activity) {
        assertFalse(isTranslucent(activity));
        AmoledTheme.applyOverlay(activity);
        assertTrue(isTranslucent(activity));
    }

    private static void assertNoOverlay(ThemedActivity activity) {
        AmoledTheme.applyOverlay(activity);
        assertFalse(isTranslucent(activity));
    }

    @Test
    public void mapsTheAmoledSwitchOntoTheNightModeKey() {
        assertEquals("night_mode", AmoledTheme.themeKey("hx_amoled"));
    }

    @Test
    public void leavesOtherKeysAlone() {
        for (String key : new String[] { "night_mode", "theme", "", "HX_AMOLED", "hx_amoled ", "hx_amoled_theme" }) {
            assertEquals(key, AmoledTheme.themeKey(key));
        }
        assertNull(AmoledTheme.themeKey(null));
    }

    @Test
    public void appliesTheOverlayInNightModeWhenTheSwitchIsOn() {
        // given
        final ThemedActivity activity = activityWithSwitch(Boolean.TRUE, NIGHT, OVERLAY);

        // when
        assertOverlayApplied(activity);

        // then
        assertEquals("hx_amoled_theme_overlay|style|" + activity.getPackageName(),
                ((OverlayResources) activity.getResources()).identifierQuery);
    }

    @Test
    public void appliesTheOverlayWhenOtherNightModeFlagsAreSet() {
        assertOverlayApplied(activityWithSwitch(Boolean.TRUE, NIGHT | Configuration.UI_MODE_TYPE_TELEVISION, OVERLAY));
    }

    @Test
    public void skipsTheOverlayWhenTheSwitchIsOff() {
        assertNoOverlay(activityWithSwitch(Boolean.FALSE, NIGHT, OVERLAY));
    }

    @Test
    public void skipsTheOverlayWhenTheSwitchWasNeverSet() {
        assertNoOverlay(activityWithSwitch(null, NIGHT, OVERLAY));
    }

    @Test
    public void skipsTheOverlayInDayMode() {
        // given
        final ThemedActivity activity = activityWithSwitch(Boolean.TRUE, DAY, OVERLAY);

        // when
        assertNoOverlay(activity);

        // then
        assertNull(((OverlayResources) activity.getResources()).identifierQuery);
    }

    @Test
    public void skipsTheOverlayWhenNightModeIsUndefined() {
        assertNoOverlay(activityWithSwitch(Boolean.TRUE, 0, OVERLAY));
    }

    @Test
    public void skipsTheOverlayWhenTheAppShipsNoOverlayStyle() {
        // given
        final ThemedActivity activity = activityWithSwitch(Boolean.TRUE, NIGHT, 0);

        // when
        assertNoOverlay(activity);

        // then
        assertEquals("hx_amoled_theme_overlay|style|" + activity.getPackageName(),
                ((OverlayResources) activity.getResources()).identifierQuery);
    }

    @Test
    public void skipsTheOverlayWhenThePreferenceHasAnotherType() {
        assertNoOverlay(activityWithSwitch("true", NIGHT, OVERLAY));
    }

    @Test
    public void skipsTheOverlayWhenReadingThePreferenceFails() {
        // given
        final ThemedActivity activity = activity(NIGHT, OVERLAY);
        activity.replacement = (SharedPreferences) Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),
                new Class<?>[] { SharedPreferences.class }, (proxy, method, args) -> {
                    throw new IllegalStateException();
                });

        // when
        assertNoOverlay(activity);
    }

    @Test
    public void skipsTheOverlayWhenPreferencesCannotBeOpened() {
        final ThemedActivity withheld = activity(NIGHT, OVERLAY);
        withheld.withhold = true;
        assertNoOverlay(withheld);

        final ThemedActivity failing = activity(NIGHT, OVERLAY);
        failing.openFailure = new IllegalStateException();
        assertNoOverlay(failing);
    }

    public static final class ThemedActivity extends Activity {

        Resources override;

        SharedPreferences replacement;

        boolean withhold;

        RuntimeException openFailure;

        @Override
        public Resources getResources() {
            return (this.override != null) ? this.override : super.getResources();
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            if (this.openFailure != null) {
                throw this.openFailure;
            }
            if (this.withhold) {
                return null;
            }
            return (this.replacement != null) ? this.replacement : super.getSharedPreferences(name, mode);
        }

    }

    @SuppressWarnings("deprecation")
    private static final class OverlayResources extends Resources {

        private final int overlayStyle;

        String identifierQuery;

        OverlayResources(Resources base, int uiMode, int overlayStyle) {
            super(base.getAssets(), base.getDisplayMetrics(), withUiMode(base.getConfiguration(), uiMode));
            this.overlayStyle = overlayStyle;
        }

        private static Configuration withUiMode(Configuration base, int uiMode) {
            final Configuration configuration = new Configuration(base);
            configuration.uiMode = uiMode;
            return configuration;
        }

        @Override
        public int getIdentifier(String name, String defType, String defPackage) {
            this.identifierQuery = name + "|" + defType + "|" + defPackage;
            return this.overlayStyle;
        }

    }

}
