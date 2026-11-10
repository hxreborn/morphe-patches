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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class ThemeSettings {

    private static final String MODE_FILE = "hx_ultimea_theme";

    private static final String MODE = "theme_mode";

    private static final int DARK = 1;

    private static final int AMOLED = 2;

    private static final int DARK_BACKGROUND = 0xff121216;

    private static final int AMOLED_BACKGROUND = 0xff000000;

    private static int currentMode = DARK;

    private static boolean initialized;

    private ThemeSettings() {
    }

    public static synchronized void prepare(Context context) {
        if (!initialized) {
            int requested = getMode(context);
            currentMode = writeMode(context, requested) ? requested : readMode(context);
            initialized = true;
        }
    }

    public static void initialize(Activity activity) {
        prepare(activity);
        int mode = currentMode;
        Window window = activity.getWindow();
        int background = (mode == AMOLED) ? AMOLED_BACKGROUND : DARK_BACKGROUND;
        window.setStatusBarColor(background);
        window.setNavigationBarColor(background);
        View decor = window.getDecorView();
        decor.setOnSystemUiVisibilityChangeListener((visibility) -> setDarkBars(decor, visibility));
        setDarkBars(decor, decor.getSystemUiVisibility());
    }

    public static void rewriteSystemBars(JSONObject style) {
        int background = (currentMode == AMOLED) ? AMOLED_BACKGROUND : DARK_BACKGROUND;
        try {
            style.put("statusBarIconBrightness", "Brightness.light");
            style.put("systemNavigationBarIconBrightness", "Brightness.light");
            style.put("statusBarColor", background);
            style.put("systemNavigationBarColor", background);
            style.put("systemNavigationBarDividerColor", background);
            style.put("systemStatusBarContrastEnforced", false);
            style.put("systemNavigationBarContrastEnforced", false);
        } catch (JSONException ex) {
            Log.e(PatchSettings.LOG_TAG, "Could not set system bar colors", ex);
        }
    }

    public static int pageBackground() {
        return (currentMode == AMOLED) ? AMOLED_BACKGROUND : DARK_BACKGROUND;
    }

    public static void addMenuRow(Activity activity, LinearLayout card) {
        new ThemeChoice(activity).addTo(card);
    }

    public static void addAppliedNames(List<String> names) {
        names.add("Dark theme");
    }

    static int getMode(Context context) {
        return context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE).getInt(MODE, DARK);
    }

    static boolean setMode(Context context, int mode) {
        if (mode != DARK && mode != AMOLED) {
            return false;
        }
        SharedPreferences preferences = context.getSharedPreferences(PatchSettings.PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        int previous = getMode(context);
        if (preferences.edit().putInt(MODE, mode).commit()) {
            return true;
        }
        if (!preferences.edit().putInt(MODE, previous).commit()) {
            Log.e(PatchSettings.LOG_TAG, "Previous theme selection could not be restored");
        }
        return false;
    }

    private static int readMode(Context context) {
        try (FileInputStream input = new FileInputStream(new File(context.getFilesDir(), MODE_FILE))) {
            int mode = input.read();
            return (mode == AMOLED) ? AMOLED : DARK;
        } catch (IOException ex) {
            Log.w(PatchSettings.LOG_TAG, "Saved theme mode could not be read", ex);
            return DARK;
        }
    }

    private static boolean writeMode(Context context, int mode) {
        File file = new File(context.getFilesDir(), MODE_FILE);
        File replacement = null;
        try {
            replacement = File.createTempFile(MODE_FILE, null, context.getFilesDir());
            try (FileOutputStream output = new FileOutputStream(replacement)) {
                output.write(mode);
                output.getFD().sync();
            }
            if (!replacement.renameTo(file)) {
                throw new IOException("Theme mode replacement failed");
            }
            return true;
        } catch (IOException ex) {
            Log.e(PatchSettings.LOG_TAG, "Could not save theme mode", ex);
            return false;
        } finally {
            if (replacement != null && replacement.exists() && !replacement.delete()) {
                Log.w(PatchSettings.LOG_TAG, "Temporary theme mode could not be removed");
            }
        }
    }

    private static void setDarkBars(View decor, int visibility) {
        int flags = visibility & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        if (flags != visibility) {
            decor.setSystemUiVisibility(flags);
        }
    }

}
