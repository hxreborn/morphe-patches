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
package app.hxreborn.extension.iiec;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import app.hxreborn.extension.shared.ActivityCallbacksAdapter;
import app.hxreborn.extension.shared.NightMode;

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

        application.registerActivityLifecycleCallbacks(new ActivityCallbacksAdapter() {
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
        if (!NightMode.isActive(activity)) {
            return false;
        }

        return isAmoledValue(PreferenceManager.getDefaultSharedPreferences(activity));
    }

}
