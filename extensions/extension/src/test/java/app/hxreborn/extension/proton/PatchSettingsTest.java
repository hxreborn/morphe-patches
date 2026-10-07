/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import app.morphe.extension.shared.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class PatchSettingsTest {

    private static final String KEY = "some_feature";

    private Context context;

    @Before
    public void useApplicationContext() {
        this.context = PatchedBuild.useApplicationContext();
    }

    private SharedPreferences preferences() {
        return this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private void useContextWithoutPreferences() {
        Utils.setContext(new ContextWrapper(this.context) {

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                throw new IllegalStateException("no preferences");
            }

        });
    }

    @Test
    public void preferencesFileNameIsHxProtonmailPatches() {
        assertEquals("hx_protonmail_patches", PatchSettings.PREFERENCES_NAME);
    }

    @Test
    public void unpatchedFeatureIsDisabled() {
        assertFalse(PatchSettings.isFeatureEnabled(false, KEY));

        preferences().edit().putBoolean(KEY, true).apply();
        assertFalse(PatchSettings.isFeatureEnabled(false, KEY));
    }

    @Test
    public void patchedFeatureDefaultsToEnabled() {
        assertTrue(PatchSettings.isFeatureEnabled(true, KEY));
    }

    @Test
    public void patchedFeatureFollowsTheStoredSetting() {
        preferences().edit().putBoolean(KEY, false).apply();
        assertFalse(PatchSettings.isFeatureEnabled(true, KEY));

        preferences().edit().putBoolean(KEY, true).apply();
        assertTrue(PatchSettings.isFeatureEnabled(true, KEY));
    }

    @Test
    public void storedBooleanOverridesTheFallback() {
        assertTrue(PatchSettings.isEnabled(KEY, true));
        assertFalse(PatchSettings.isEnabled(KEY, false));

        PatchSettings.setEnabled(KEY, false);
        assertFalse(PatchSettings.isEnabled(KEY, true));

        PatchSettings.setEnabled(KEY, true);
        assertTrue(PatchSettings.isEnabled(KEY, false));
        assertTrue(preferences().getBoolean(KEY, false));
    }

    @Test
    public void storedStringOverridesTheFallback() {
        assertEquals("fallback", PatchSettings.getString(KEY, "fallback"));
        assertEquals("", PatchSettings.getString(KEY, ""));
        assertNull(PatchSettings.getString(KEY, null));

        PatchSettings.setString(KEY, "value");
        assertEquals("value", PatchSettings.getString(KEY, "fallback"));
        assertEquals("value", preferences().getString(KEY, null));

        PatchSettings.setString(KEY, "");
        assertEquals("", PatchSettings.getString(KEY, "fallback"));
    }

    @Test
    public void valuesLiveInTheNamedPreferencesFile() {
        PatchSettings.setEnabled(KEY, true);
        PatchSettings.setString("text", "value");

        final SharedPreferences named = this.context.getSharedPreferences("hx_protonmail_patches",
                Context.MODE_PRIVATE);
        assertTrue(named.getBoolean(KEY, false));
        assertEquals("value", named.getString("text", null));
        assertEquals(2, named.getAll().size());
    }

    @Test
    public void unavailablePreferencesReturnTheFallback() {
        useContextWithoutPreferences();

        assertTrue(PatchSettings.isEnabled(KEY, true));
        assertFalse(PatchSettings.isEnabled(KEY, false));
        assertEquals("fallback", PatchSettings.getString(KEY, "fallback"));
        assertEquals("", PatchSettings.getString(KEY, ""));
        assertNull(PatchSettings.getString(KEY, null));
        assertTrue(PatchSettings.isFeatureEnabled(true, KEY));
        assertFalse(PatchSettings.isFeatureEnabled(false, KEY));
    }

    @Test
    public void writesWithoutPreferencesAreIgnored() {
        useContextWithoutPreferences();

        PatchSettings.setEnabled(KEY, false);
        PatchSettings.setString(KEY, "value");

        assertTrue(PatchSettings.isEnabled(KEY, true));
        assertEquals("fallback", PatchSettings.getString(KEY, "fallback"));
        assertTrue(preferences().getAll().isEmpty());
    }

}
