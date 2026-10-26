/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/ModSettings.java
 */
package app.hxreborn.extension.firefox;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.util.Log;

@SuppressWarnings("unused")
public final class PatchSettings {

    static final Flag OLD_MENU = new Flag("old_menu");

    static final Flag PIN = new Flag("pin_ext");

    static final Flag SCROLL_MEMORY = new Flag("scroll_memory");

    static final Flag TAB_NEXT = new Flag("tab_next_to_current");

    static final Flag TAB_END = new Flag("tab_end_button");

    static final Flag CLOSE_DUPLICATES = new Flag("close_duplicates");

    static final Flag SELECT_PREVIOUS = new Flag("select_previous_tab");

    static final Flag TAB_SCROLL = new Flag("tab_scroll_button");

    static final Flag RANGE_SELECT = new Flag("range_select");

    static final Flag FULL_URL = new Flag("full_url");

    static final Flag HIDE_QR_SCANNER = new Flag("hide_qr_scanner");

    static final Flag HIDE_SIGN_IN = new Flag("hide_sign_in");

    static final Flag HIDE_MOST_VISITED = new Flag("hide_most_visited");

    static final Flag HIDE_FULLSCREEN_TOAST = new Flag("hide_fullscreen_toast");

    static final Flag NO_TAB_SUGGESTIONS = new Flag("no_tab_suggestions");

    static final Flag HOMEPAGE = new Flag("custom_homepage");

    static final Flag AMOLED = new Flag("amoled_theme");

    static final Flag ACCENT = new Flag("accent_color");

    static final Flag LOADING_INDICATOR = new Flag("loading_indicator");

    static final Flag KEEP_TABS = new Flag("keep_tabs_loaded");

    static final Flag HIDE_PROMOS = new Flag("hide_promos");

    static final Flag SILENCE_NOTIFICATIONS = new Flag("silence_promo_notifications");

    static final Flag FULL_ADDONS = new Flag("full_addon_collection");

    static final Flag UNLOCK_EXTENSIONS = new Flag("unlock_all_extensions");

    static final Flag EXTENDED_ADDON_LIST = new Flag("extended_addon_list");

    static final Flag ABOUT_CONFIG = new Flag("about_config");

    static final Flag SECRET_SETTINGS = new Flag("secret_settings");

    private static final String TAG = "FirefoxPatches";

    private static final String PREFS = "hx_patches_settings";

    private static final String HOMEPAGE_URL_KEY = "homepage_url";

    private static volatile String cachedHomepageUrl;

    private PatchSettings() {
    }

    public static void latch(Context context) {
        OLD_MENU.cached = OLD_MENU.saved(context);
    }

    public static boolean oldMenu() {
        return OLD_MENU.get();
    }

    public static boolean pinEnabled() {
        return PIN.get();
    }

    public static boolean scrollMemory() {
        return SCROLL_MEMORY.get();
    }

    public static boolean tabNext() {
        return TAB_NEXT.get();
    }

    public static boolean tabEnd() {
        return TAB_END.get();
    }

    public static boolean tabScroll() {
        return TAB_SCROLL.get();
    }

    public static boolean rangeSelect() {
        return RANGE_SELECT.get();
    }

    public static boolean closeDuplicates() {
        return CLOSE_DUPLICATES.get();
    }

    public static boolean selectPreviousTab() {
        return SELECT_PREVIOUS.get();
    }

    public static boolean showFullUrl() {
        return FULL_URL.get();
    }

    public static boolean hideQrScanner() {
        return HIDE_QR_SCANNER.get();
    }

    public static boolean hideSignIn() {
        return HIDE_SIGN_IN.get();
    }

    public static boolean hideFullscreenToast() {
        return HIDE_FULLSCREEN_TOAST.get();
    }

    public static boolean customHomepage() {
        return HOMEPAGE.get();
    }

    public static boolean amoledTheme() {
        return AMOLED.get();
    }

    public static boolean accentColor() {
        return ACCENT.get();
    }

    public static boolean loadingIndicator() {
        return LOADING_INDICATOR.get();
    }

    public static boolean keepTabsLoaded() {
        return KEEP_TABS.get();
    }

    public static boolean silencePromoNotifications() {
        return SILENCE_NOTIFICATIONS.get();
    }

    public static boolean hidePromos() {
        return HIDE_PROMOS.get();
    }

    public static boolean fullAddonCollection() {
        return FULL_ADDONS.get();
    }

    public static boolean unlockAllExtensions() {
        return UNLOCK_EXTENSIONS.get();
    }

    public static boolean extendedAddonList() {
        return EXTENDED_ADDON_LIST.get();
    }

    public static boolean aboutConfig() {
        return ABOUT_CONFIG.get();
    }

    public static boolean secretSettings() {
        return SECRET_SETTINGS.get();
    }

    public static Object mostVisited(Object stock) {
        return HIDE_MOST_VISITED.get() ? null : stock;
    }

    public static boolean sessionSuggestions(boolean stock) {
        return stock && !NO_TAB_SUGGESTIONS.get();
    }

    public static String homepage(String stock) {
        return HOMEPAGE.get() ? homepageUrl() : stock;
    }

    public static String defaultHomepageUrl() {
        return "";
    }

    public static boolean scrollMemoryApplied() {
        return false;
    }

    public static boolean tabNextApplied() {
        return false;
    }

    public static boolean tabScrollApplied() {
        return false;
    }

    public static boolean closeDuplicatesApplied() {
        return false;
    }

    public static boolean selectPreviousTabApplied() {
        return false;
    }

    public static boolean rangeSelectApplied() {
        return false;
    }

    public static boolean showFullUrlApplied() {
        return false;
    }

    public static boolean hideQrScannerApplied() {
        return false;
    }

    public static boolean hideSignInApplied() {
        return false;
    }

    public static boolean hideMostVisitedApplied() {
        return false;
    }

    public static boolean hideFullscreenToastApplied() {
        return false;
    }

    public static boolean noTabSuggestionsApplied() {
        return false;
    }

    public static boolean customHomepageApplied() {
        return false;
    }

    public static boolean amoledThemeApplied() {
        return false;
    }

    public static boolean accentColorApplied() {
        return false;
    }

    public static boolean loadingIndicatorApplied() {
        return false;
    }

    public static boolean keepTabsLoadedApplied() {
        return false;
    }

    public static boolean silencePromoNotificationsApplied() {
        return false;
    }

    public static boolean hidePromosApplied() {
        return false;
    }

    public static boolean fullAddonCollectionApplied() {
        return false;
    }

    public static boolean unlockAllExtensionsApplied() {
        return false;
    }

    public static boolean extendedAddonListApplied() {
        return false;
    }

    public static boolean aboutConfigApplied() {
        return false;
    }

    public static boolean secretSettingsApplied() {
        return false;
    }

    static boolean isWebUrl(String value) {
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return ("https".equals(scheme) || "http".equals(scheme)) && host != null && !host.isEmpty();
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    static String hostOf(String value) {
        try {
            String host = new URI(value.trim()).getHost();
            return (host != null) ? host : "";
        } catch (URISyntaxException ex) {
            return "";
        }
    }

    static String savedHomepageUrl(Context context) {
        String saved = prefs(context).getString(HOMEPAGE_URL_KEY, null);
        return (saved != null && isWebUrl(saved)) ? saved.trim() : defaultHomepageUrl();
    }

    static void saveHomepageUrl(Context context, String url) {
        String trimmed = url.trim();
        prefs(context).edit().putString(HOMEPAGE_URL_KEY, trimmed).apply();
        cachedHomepageUrl = trimmed;
    }

    private static String homepageUrl() {
        String value = cachedHomepageUrl;
        if (value == null) {
            try {
                value = savedHomepageUrl(Reflection.application());
                cachedHomepageUrl = value;
            } catch (Throwable ex) {
                Log.e(TAG, "homepage url failed", ex);
                value = defaultHomepageUrl();
            }
        }
        return value;
    }

    static void saveTabNext(Context context, boolean enabled) {
        SharedPreferences.Editor editor = prefs(context).edit().putBoolean(TAB_NEXT.key, enabled);
        if (!enabled) {
            editor.putBoolean(TAB_END.key, false);
            TAB_END.cached = false;
        }
        editor.apply();
        TAB_NEXT.cached = enabled;
    }

    static void saveTabEnd(Context context, boolean enabled) {
        SharedPreferences.Editor editor = prefs(context).edit().putBoolean(TAB_END.key, enabled);
        if (enabled) {
            editor.putBoolean(TAB_NEXT.key, true);
            TAB_NEXT.cached = true;
        }
        editor.apply();
        TAB_END.cached = enabled;
    }

    static void saveOldMenu(Context context, boolean oldMenu) {
        prefs(context).edit().putBoolean(OLD_MENU.key, oldMenu).apply();
    }

    public static void open(Context context) {
        try {
            Activity activity = findActivity(context);
            if (activity == null || activity.isFinishing()) {
                Log.w(TAG, "no activity for the patches settings");
                return;
            }
            OldMenu.dismiss();
            PatchesSettingsDialog.show(activity);
        } catch (Throwable ex) {
            Log.e(TAG, "open failed", ex);
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Activity findActivity(Context context) {
        Context c = context;
        while (c instanceof ContextWrapper) {
            if (c instanceof Activity) {
                return (Activity) c;
            }
            c = ((ContextWrapper) c).getBaseContext();
        }
        return null;
    }

    static final class Flag {

        private final String key;

        private volatile Boolean cached;

        private Flag(String key) {
            this.key = key;
        }

        boolean get() {
            Boolean value = this.cached;
            if (value == null) {
                try {
                    value = saved(Reflection.application());
                    this.cached = value;
                } catch (Throwable ex) {
                    Log.e(TAG, this.key + " state failed", ex);
                }
            }
            return (value == null) || value;
        }

        boolean saved(Context context) {
            return prefs(context).getBoolean(this.key, true);
        }

        void save(Context context, boolean enabled) {
            prefs(context).edit().putBoolean(this.key, enabled).apply();
            this.cached = enabled;
        }

    }

}
