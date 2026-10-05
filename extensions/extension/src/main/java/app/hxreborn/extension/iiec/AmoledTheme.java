/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.iiec;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

@SuppressWarnings("unused")
public final class AmoledTheme {

    private static final String EDITOR_THEME_DARK_PREFERENCE_KEY = "appearance_editor_theme_dark";

    private static final String AMOLED_VALUE = "amoled";

    private static final String OVERLAY_STYLE = "hx_iiec_amoled_theme_overlay";

    private static final String[] APP_PACKAGE_PREFIXES = { "ru.iiec.", "iiec.androidterm." };

    private static final List<Activity> APP_ACTIVITIES = new ArrayList<>();

    private static SharedPreferences.OnSharedPreferenceChangeListener editorThemeListener;

    private static boolean amoledSelected;

    private AmoledTheme() {

    }

    public static void attach(Application application) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
        amoledSelected = isAmoledValue(preferences);
        editorThemeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (!EDITOR_THEME_DARK_PREFERENCE_KEY.equals(key)) {
                    return;
                }

                boolean selected = isAmoledValue(sharedPreferences);
                if (selected == amoledSelected) {
                    return;
                }

                amoledSelected = selected;
                recreateAppActivities();
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(editorThemeListener);

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPreCreated(Activity activity, Bundle savedInstanceState) {
                applyOverlay(activity);
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (isAppActivity(activity)) {
                    APP_ACTIVITIES.add(activity);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return;
                }

                if (applyOverlay(activity)) {
                    reapplyWindowBackground(activity);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                APP_ACTIVITIES.remove(activity);
            }
        });
    }

    private static void recreateAppActivities() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (Activity activity : new ArrayList<>(APP_ACTIVITIES)) {
                    if (!activity.isFinishing()) {
                        activity.recreate();
                    }
                }
            }
        });
    }

    private static boolean isAmoledValue(SharedPreferences preferences) {
        try {
            return AMOLED_VALUE.equals(preferences.getString(EDITOR_THEME_DARK_PREFERENCE_KEY, ""));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean applyOverlay(Activity activity) {
        if (!isAppActivity(activity) || !isAmoledSelected(activity)) {
            return false;
        }

        int style = activity.getResources().getIdentifier(OVERLAY_STYLE, "style", activity.getPackageName());
        if (style == 0) {
            return false;
        }

        activity.getTheme().applyStyle(style, true);

        return true;
    }

    private static void reapplyWindowBackground(Activity activity) {
        TypedArray attributes = activity.getTheme()
            .obtainStyledAttributes(new int[] { android.R.attr.windowBackground });

        try {
            Drawable background = attributes.getDrawable(0);
            if (background != null) {
                activity.getWindow().setBackgroundDrawable(background);
            }
        } finally {
            attributes.recycle();
        }
    }

    private static boolean isAppActivity(Activity activity) {
        String name = activity.getClass().getName();

        for (String prefix : APP_PACKAGE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAmoledSelected(Activity activity) {
        if (!isNight(activity)) {
            return false;
        }

        return isAmoledValue(PreferenceManager.getDefaultSharedPreferences(activity));
    }

    private static boolean isNight(Activity activity) {
        int uiMode = activity.getResources().getConfiguration().uiMode;

        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

}
