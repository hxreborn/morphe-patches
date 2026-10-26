/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;

import app.hxreborn.extension.firefox.PreviewView.Scene;

final class PatchCatalog {

    private PatchCatalog() {
    }

    static List<Group> groups(Context app) {
        Scene homepage = PreviewScenes.homepage(PatchSettings.hostOf(PatchSettings.savedHomepageUrl(app)));
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("Appearance",
                Arrays.asList(
                        Row.of("AMOLED dark theme", "AMOLED dark theme", "Pure black in dark theme",
                                PatchSettings.AMOLED, PatchSettings.amoledThemeApplied(), PreviewScenes.amoled()),
                        Row.of("Accent color", "Accent color", "Wallpaper or custom accent", PatchSettings.ACCENT,
                                PatchSettings.accentColorApplied(), PreviewScenes.accent()),
                        Row.loading("M3E loading indicator", "M3E loading indicator", "Morphing refresh indicator",
                                PatchSettings.LOADING_INDICATOR, PatchSettings.loadingIndicatorApplied()))));
        groups.add(new Group("Menu",
                Arrays.asList(Row.menuStyle("Old-style menu and extension pinning"),
                        Row.of("Old-style menu and extension pinning", "Pin extensions to toolbar",
                                "Long press an extension to pin", PatchSettings.PIN, true,
                                PreviewScenes.pinExtensions(), (context, on) -> {
                                    PatchSettings.PIN.save(context, on);
                                    ExtensionPin.applySetting();
                                }))));
        groups.add(new Group("Tabs", Arrays.asList(
                Row.of("Remember tab scroll position", "Remember tab scroll position", "Tray reopens where you left it",
                        PatchSettings.SCROLL_MEMORY, PatchSettings.scrollMemoryApplied(), PreviewScenes.scrollMemory()),
                Row.of("New tab next to current tab", "New tab next to current tab", "Opens new tabs after current",
                        PatchSettings.TAB_NEXT, PatchSettings.tabNextApplied(), PreviewScenes.tabNext(),
                        PatchSettings::saveTabNext),
                Row.of("New tab next to current tab", "Add-to-end button", "Open a tab at the end",
                        PatchSettings.TAB_END, PatchSettings.tabNextApplied(), PreviewScenes.tabEnd(),
                        PatchSettings::saveTabEnd),
                Row.of("Tab scroll button", "Scroll buttons", "Jump to first or last tab", PatchSettings.TAB_SCROLL,
                        PatchSettings.tabScrollApplied(), PreviewScenes.scrollButtons()),
                Row.of("Close duplicate tabs", "Close duplicate tabs", "Closes tabs with the same URL",
                        PatchSettings.CLOSE_DUPLICATES, PatchSettings.closeDuplicatesApplied(),
                        PreviewScenes.closeDuplicates()),
                Row.of("Long press to select tab range", "Long press to select range", "Long press a second tab",
                        PatchSettings.RANGE_SELECT, PatchSettings.rangeSelectApplied(), PreviewScenes.rangeSelect()),
                Row.of("Keep background tabs loaded", "Keep background tabs loaded", "Stops background tab reloads",
                        PatchSettings.KEEP_TABS, PatchSettings.keepTabsLoadedApplied(), PreviewScenes.keepTabs()))));
        groups.add(new Group("Address bar and search", Arrays.asList(
                Row.of("Show full URL", "Show full URL", "Shows https:// and www.", PatchSettings.FULL_URL,
                        PatchSettings.showFullUrlApplied(), PreviewScenes.fullUrl()),
                Row.of("Hide QR scanner button", "Hide QR scanner button", "Removes the QR button",
                        PatchSettings.HIDE_QR_SCANNER, PatchSettings.hideQrScannerApplied(), PreviewScenes.qrScanner()),
                Row.of("Disable Switch to tab suggestions", "Disable Switch to tab suggestions",
                        "Removes open tabs from search", PatchSettings.NO_TAB_SUGGESTIONS,
                        PatchSettings.noTabSuggestionsApplied(), PreviewScenes.tabSuggestions()))));
        groups.add(new Group("Home", Arrays.asList(
                Row.homepage("Custom homepage", "Custom homepage", "Opens your page on new tabs",
                        PatchSettings.HOMEPAGE, PatchSettings.customHomepageApplied(), homepage),
                Row.of("Hide most visited shortcuts", "Hide most visited shortcuts", "Keeps only pinned shortcuts",
                        PatchSettings.HIDE_MOST_VISITED, PatchSettings.hideMostVisitedApplied(),
                        PreviewScenes.mostVisited()),
                Row.of("Hide promotional prompts", "Hide promotional prompts", "Removes promo cards and prompts",
                        PatchSettings.HIDE_PROMOS, PatchSettings.hidePromosApplied(), PreviewScenes.promos()))));
        groups.add(new Group("Browsing",
                Arrays.asList(
                        Row.of("Hide fullscreen toast", "Hide fullscreen toast", "Removes the exit hint",
                                PatchSettings.HIDE_FULLSCREEN_TOAST, PatchSettings.hideFullscreenToastApplied(),
                                PreviewScenes.fullscreenToast()),
                        Row.of("Load full add-on collection", "Load full add-on collection", "Lists add-ons past 50",
                                PatchSettings.FULL_ADDONS, PatchSettings.fullAddonCollectionApplied(),
                                PreviewScenes.addons()))));
        groups.add(new Group("Privacy and security",
                Arrays.asList(Row.spoken("Hide sign-in button", "Hide sign-in button", "Removes Sign in entries",
                                "Removes Sign in entries from Settings, the menu, Synced tabs, sharing, Bookmarks and "
                                        + "Passwords.",
                                PatchSettings.HIDE_SIGN_IN, PatchSettings.hideSignInApplied(),
                                PreviewScenes.signIn()))));
        groups.add(new Group("Advanced",
                Arrays.asList(Row.of("Enable about:config", "Enable about:config", "Unlocks about:config",
                        PatchSettings.ABOUT_CONFIG, PatchSettings.aboutConfigApplied(), PreviewScenes.aboutConfig()),
                        Row.of("Unlock secret settings", "Unlock secret settings", "Shows Secret Settings menu",
                                PatchSettings.SECRET_SETTINGS, PatchSettings.secretSettingsApplied(),
                                PreviewScenes.secretSettings()))));
        List<Group> visible = new ArrayList<>();
        for (Group group : groups) {
            Group shown = group.visible();
            if (!shown.rows.isEmpty()) {
                visible.add(shown);
            }
        }
        return visible;
    }

    static int patchCount(List<Group> groups) {
        List<String> patches = new ArrayList<>();
        for (Group group : groups) {
            for (Row row : group.rows) {
                if (!patches.contains(row.patch)) {
                    patches.add(row.patch);
                }
            }
        }
        return patches.size();
    }

    enum Kind {

        SWITCH, LOADING, MENU_STYLE, HOMEPAGE

    }

    interface Change {

        void apply(Context context, boolean on);

    }

    static final class Group {

        final String title;

        final List<Row> rows;

        Group(String title, List<Row> rows) {
            this.title = title;
            this.rows = rows;
        }

        Group visible() {
            List<Row> shown = new ArrayList<>();
            for (Row row : this.rows) {
                if (row.applied) {
                    shown.add(row);
                }
            }
            return new Group(this.title, shown);
        }

    }

    static final class Row {

        final Kind kind;

        final String patch;

        final String title;

        final String effect;

        final String spoken;

        final PatchSettings.Flag flag;

        final boolean applied;

        final Scene scene;

        final Change change;

        private Row(Kind kind, String patch, String title, String effect, String spoken, PatchSettings.Flag flag,
                boolean applied, Scene scene, Change change) {
            this.kind = kind;
            this.patch = patch;
            this.title = title;
            this.effect = effect;
            this.spoken = spoken;
            this.flag = flag;
            this.applied = applied;
            this.scene = scene;
            this.change = change;
        }

        static Row of(String patch, String title, String effect, PatchSettings.Flag flag, boolean applied,
                Scene scene) {
            return of(patch, title, effect, flag, applied, scene, flag::save);
        }

        static Row of(String patch, String title, String effect, PatchSettings.Flag flag, boolean applied, Scene scene,
                Change change) {
            return new Row(Kind.SWITCH, patch, title, effect, effect, flag, applied, scene, change);
        }

        static Row spoken(String patch, String title, String effect, String spoken, PatchSettings.Flag flag,
                boolean applied, Scene scene) {
            return new Row(Kind.SWITCH, patch, title, effect, spoken, flag, applied, scene, flag::save);
        }

        static Row loading(String patch, String title, String effect, PatchSettings.Flag flag, boolean applied) {
            return new Row(Kind.LOADING, patch, title, effect, effect, flag, applied, PreviewScenes.loading(),
                    flag::save);
        }

        static Row homepage(String patch, String title, String effect, PatchSettings.Flag flag, boolean applied,
                Scene scene) {
            return new Row(Kind.HOMEPAGE, patch, title, effect, effect, flag, applied, scene, flag::save);
        }

        static Row menuStyle(String patch) {
            return new Row(Kind.MENU_STYLE, patch, "Menu style", "", "", PatchSettings.OLD_MENU, true, null,
                    PatchSettings::saveOldMenu);
        }

    }

}
