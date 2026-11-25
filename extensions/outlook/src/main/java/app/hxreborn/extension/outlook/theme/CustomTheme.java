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
package app.hxreborn.extension.outlook.theme;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.ContextThemeWrapper;

import app.hxreborn.extension.outlook.settings.PatchSettings;

@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.R)
public final class CustomTheme {

    private static final String PREFERENCES_NAME = "hx_outlook_custom_theme";

    private static final String COLOR_KEY = "color";

    private static final String PICTURE_KEY = "picture";

    private static final String PICTURE_THEME_KEY = "picture_theme";

    private static final String PICTURE_DARK_TOOLBAR_KEY = "picture_dark_toolbar";

    private static final long COLOR_SPACE_MASK = 0xFFFFFFFFL;

    private static final int ARGB_SHIFT = 32;

    private static final Map<Activity, Integer> preparedGenerations = Collections.synchronizedMap(new WeakHashMap<>());

    private static ThemeColorRamp composeRamp;

    private static Object palette;

    private static String loadedPicture;

    private static boolean selecting;

    private static volatile int generation;

    private CustomTheme() {
    }

    public static Object colorIcon() {
        return new ThemeIcon(colorIconId());
    }

    public static Object pictureIcon() {
        return new ThemeIcon(pictureIconId());
    }

    public static boolean isCustomOption(Object option) {
        return option == colorOption() || option == pictureOption();
    }

    public static void applySavedTheme(Context context) {
        SharedPreferences preferences = preferences(context);
        if (preferences.contains(COLOR_KEY)) {
            int color = preferences.getInt(COLOR_KEY, Color.BLACK);
            ThemeResources.attach(context, color, referenceColorId(), colorPairs());
            updateComposeRamp(context, color);
        } else if (composeRamp == null) {
            updateComposeRamp(context, context.getColor(referenceColorId()));
        }
        String picture = preferences.getString(PICTURE_KEY, null);
        if (picture != null && !picture.equals(loadedPicture)) {
            Object pictureTheme = photoTheme(preferences.getString(PICTURE_THEME_KEY, null));
            if (pictureTheme != null) {
                loadedPicture = picture;
                File pictureFile = PictureStore.file(context, picture);
                showPicture(application(context), pictureFile, PictureStore.darkModeFile(pictureFile), pictureTheme,
                        preferences.getBoolean(PICTURE_DARK_TOOLBAR_KEY, false));
            }
        }
        if (context instanceof Activity) {
            preparedGenerations.put((Activity) context, generation);
        }
    }

    public static boolean isOutdated(Activity activity) {
        Integer prepared = preparedGenerations.get(activity);
        return prepared != null && prepared != generation;
    }

    public static boolean openEditor(Object dynamicViewModel, Object appearanceViewModel, Object option,
            Context context) {
        if (selecting || !isCustomOption(option)) {
            return false;
        }
        Activity activity = activity(context);
        if (activity == null) {
            return false;
        }
        if (option == pictureOption()) {
            PicturePickerFragment.pick(activity, dynamicViewModel, appearanceViewModel);
            return true;
        }
        int initialColor = preferences(activity).getInt(COLOR_KEY, activity.getColor(referenceColorId()));
        CustomColorDialog.show(activity, initialColor,
                (color) -> applyColor(activity, dynamicViewModel, appearanceViewModel, color));
        return true;
    }

    public static Object palette(Object themeStyle) {
        if (themeStyle != customThemeStyle() || composeRamp == null) {
            return null;
        }
        if (palette == null) {
            palette = customPalette();
        }
        return palette;
    }

    public static boolean isCustomPaletteInDarkMode(Object themePalette, boolean darkTheme) {
        return darkTheme && themePalette != null && themePalette == palette;
    }

    public static int customSearchPillLayout(Context context) {
        if (!isNightMode(context) || currentOption(context) != colorOption()) {
            return 0;
        }
        return searchPillLayoutId();
    }

    public static long transformPackedColor(long packed) {
        if ((packed & COLOR_SPACE_MASK) != 0 || composeRamp == null) {
            return packed;
        }
        int argb = (int) (packed >>> ARGB_SHIFT);
        return ((long) composeRamp.transform(argb)) << ARGB_SHIFT;
    }

    public static Context prominentDialogContext(Context context) {
        if (themeStyle(currentOption(context)) != customThemeStyle()) {
            return null;
        }
        return new ContextThemeWrapper(context, prominentDialogStyleId());
    }

    public static int immersiveThemeOverlay(Context context, int overlay) {
        if (currentOption(context) != pictureOption()) {
            return overlay;
        }
        boolean darkToolbar = !isNightMode(context) && preferences(context).getBoolean(PICTURE_DARK_TOOLBAR_KEY, false);
        return darkToolbar ? darkToolbarOverlayId() : whiteToolbarOverlayId();
    }

    static void applyPicture(Context context, Object dynamicViewModel, Object appearanceViewModel, SavedPicture saved) {
        Context application = application(context);
        Object pictureTheme = nearestPhotoTheme(application, saved.averageColor);
        if (pictureTheme == null) {
            return;
        }
        boolean darkToolbar = !ThemeColorRamp.hasWhiteTextContrast(saved.averageColor);
        if (!preferences(application).edit()
            .putString(PICTURE_KEY, saved.file.getName())
            .putString(PICTURE_THEME_KEY, ((Enum<?>) pictureTheme).name())
            .putBoolean(PICTURE_DARK_TOOLBAR_KEY, darkToolbar)
            .commit()) {
            Log.w(PatchSettings.LOG_TAG, "Cannot save the custom theme picture");
            return;
        }
        generation++;
        PictureStore.deleteOtherPictures(saved);
        loadedPicture = saved.file.getName();
        showPicture(application, saved.file, saved.darkModeFile, pictureTheme, darkToolbar);
        Activity activity = activity(context);
        if (activity == null || activity.isDestroyed()) {
            saveThemeOption(application, pictureOption());
            return;
        }
        select(activity, dynamicViewModel, appearanceViewModel, pictureOption());
    }

    private static void applyColor(Activity activity, Object dynamicViewModel, Object appearanceViewModel, int color) {
        if (!preferences(activity).edit().putInt(COLOR_KEY, color).commit()) {
            Log.w(PatchSettings.LOG_TAG, "Cannot save the custom theme color");
            return;
        }
        generation++;
        ThemeResources.attach(activity, color, referenceColorId(), colorPairs());
        ThemeResources.attach(application(activity), color, referenceColorId(), colorPairs());
        updateComposeRamp(activity, color);
        select(activity, dynamicViewModel, appearanceViewModel, colorOption());
    }

    private static void select(Activity activity, Object dynamicViewModel, Object appearanceViewModel, Object option) {
        if (appearanceViewModel == null) {
            saveThemeOption(activity, option);
            activity.recreate();
            return;
        }
        selecting = true;
        try {
            selectTheme(dynamicViewModel, appearanceViewModel, option, activity);
        } finally {
            selecting = false;
        }
    }

    private static void showPicture(Context context, File picture, File darkModePicture, Object pictureTheme,
            boolean darkToolbar) {
        setLightThemeStatusBarTextDark(pictureOption(), darkToolbar);
        setPictureTheme(pictureOption(), context, Uri.fromFile(picture), Uri.fromFile(darkModePicture),
                themeStyle(pictureTheme));
    }

    private static boolean isNightMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static void updateComposeRamp(Context context, int color) {
        composeRamp = ThemeColorRamp.between(context.getColor(referenceColorId()), color);
        palette = null;
    }

    private static Object nearestPhotoTheme(Context context, int color) {
        Object nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Object option : photoThemeOptions()) {
            if (isCustomOption(option)) {
                continue;
            }
            double distance = ThemeColorRamp.distance(color, accentColor(option, context));
            if (distance < nearestDistance) {
                nearest = option;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static Object photoTheme(String name) {
        for (Object option : photoThemeOptions()) {
            if (((Enum<?>) option).name().equals(name)) {
                return option;
            }
        }
        return null;
    }

    private static Activity activity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    private static SharedPreferences preferences(Context context) {
        return application(context).getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static Context application(Context context) {
        Context application = context.getApplicationContext();
        return (application != null) ? application : context;
    }

    private static Object colorOption() {
        return null;
    }

    private static Object pictureOption() {
        return null;
    }

    private static Object customThemeStyle() {
        return null;
    }

    private static List<?> photoThemeOptions() {
        return null;
    }

    private static Object currentOption(Context context) {
        return null;
    }

    private static Object themeStyle(Object option) {
        return null;
    }

    private static int accentColor(Object option, Context context) {
        return 0;
    }

    private static void setPictureTheme(Object option, Context context, Uri picture, Uri darkModePicture,
            Object themeStyle) {
    }

    private static void setLightThemeStatusBarTextDark(Object option, boolean dark) {
    }

    private static void saveThemeOption(Context context, Object option) {
    }

    private static void selectTheme(Object dynamicViewModel, Object appearanceViewModel, Object option,
            Context context) {
    }

    private static Object customPalette() {
        return null;
    }

    private static int[] colorPairs() {
        return null;
    }

    private static int referenceColorId() {
        return 0;
    }

    private static int prominentDialogStyleId() {
        return 0;
    }

    private static int whiteToolbarOverlayId() {
        return 0;
    }

    private static int darkToolbarOverlayId() {
        return 0;
    }

    private static int searchPillLayoutId() {
        return 0;
    }

    private static int colorIconId() {
        return 0;
    }

    private static int pictureIconId() {
        return 0;
    }

}
