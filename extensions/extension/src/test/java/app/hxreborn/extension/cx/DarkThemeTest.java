/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.cx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Proxy;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class DarkThemeTest {

    private static final int FOLLOW_SYSTEM = -1;

    private static final int AUTO_TIME = 0;

    private static final int NO = 1;

    private static final int YES = 2;

    private static SharedPreferences preferencesOf(Context context, String fileName) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    private static Context contextStoring(Object value, String fileSuffix) {
        final Application application = RuntimeEnvironment.getApplication();
        final SharedPreferences.Editor editor = preferencesOf(application, application.getPackageName() + fileSuffix)
            .edit();
        if (value instanceof Boolean) {
            editor.putBoolean("night_mode", (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt("night_mode", (Integer) value);
        } else {
            editor.putString("night_mode", (String) value);
        }
        editor.commit();
        return application;
    }

    private static Context contextStoring(Object value) {
        return contextStoring(value, "_preferences");
    }

    private static SharedPreferences failingPreferences() {
        return (SharedPreferences) Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),
                new Class<?>[] { SharedPreferences.class }, (proxy, method, args) -> {
                    throw new IllegalStateException();
                });
    }

    @Test
    public void defaultsToLightWithoutAContext() {
        assertEquals(NO, DarkTheme.nightMode(null));
    }

    @Test
    public void defaultsToLightWhenNothingIsStored() {
        assertEquals(NO, DarkTheme.nightMode(RuntimeEnvironment.getApplication()));
    }

    @Test
    public void readsEachSupportedMode() {
        assertEquals(FOLLOW_SYSTEM, DarkTheme.nightMode(contextStoring("-1")));
        assertEquals(AUTO_TIME, DarkTheme.nightMode(contextStoring("0")));
        assertEquals(NO, DarkTheme.nightMode(contextStoring("1")));
        assertEquals(YES, DarkTheme.nightMode(contextStoring("2")));
    }

    @Test
    public void fallsBackToLightForUnsupportedNumbers() {
        for (String value : new String[] { "3", "-2", "99", "100", "-100" }) {
            assertEquals(value, NO, DarkTheme.nightMode(contextStoring(value)));
        }
    }

    @Test
    public void fallsBackToLightForValuesThatAreNotNumbers() {
        for (String value : new String[] { "", "dark", "2.0", " 2", "2 ", "0x2", "two" }) {
            assertEquals(value, NO, DarkTheme.nightMode(contextStoring(value)));
        }
    }

    @Test
    public void fallsBackToLightWhenTheValueHasAnotherType() {
        assertEquals(NO, DarkTheme.nightMode(contextStoring(Boolean.TRUE)));
        assertEquals(NO, DarkTheme.nightMode(contextStoring(2)));
    }

    @Test
    public void fallsBackToLightWhenReadingFails() {
        // given
        final Context context = new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return failingPreferences();
            }
        };

        // when
        final int mode = DarkTheme.nightMode(context);

        // then
        assertEquals(NO, mode);
    }

    @Test
    public void fallsBackToLightWhenPreferencesCannotBeOpened() {
        final Context withheld = new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return null;
            }
        };
        final Context failing = new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                throw new IllegalStateException();
            }
        };
        assertEquals(NO, DarkTheme.nightMode(withheld));
        assertEquals(NO, DarkTheme.nightMode(failing));
    }

    @Test
    public void readsThePreferenceFileNamedAfterThePackage() {
        assertEquals(YES, DarkTheme.nightMode(contextStoring("2")));
    }

    @Test
    public void ignoresTheModeStoredInAnotherPreferenceFile() {
        assertEquals(NO, DarkTheme.nightMode(contextStoring("2", "_other")));
        assertEquals(NO, DarkTheme.nightMode(contextStoring("2", "preferences")));
    }

    @Test
    public void opensThePreferenceFilePrivately() {
        // given
        final String[] requested = new String[1];
        final int[] requestedMode = { -1 };
        final Context context = new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                requested[0] = name;
                requestedMode[0] = mode;
                return super.getSharedPreferences(name, mode);
            }
        };

        // when
        DarkTheme.nightMode(context);

        // then
        assertEquals(context.getPackageName() + "_preferences", requested[0]);
        assertEquals(Context.MODE_PRIVATE, requestedMode[0]);
    }

    @Test
    public void exposesThePackagePreferences() {
        // given
        final Application application = RuntimeEnvironment.getApplication();

        // when
        final SharedPreferences preferences = DarkTheme.preferences(application);

        // then
        assertSame(preferencesOf(application, application.getPackageName() + "_preferences"), preferences);
    }

    @Test
    public void preferencesAreMissingWithoutAContext() {
        assertNull(DarkTheme.preferences(null));
    }

}
