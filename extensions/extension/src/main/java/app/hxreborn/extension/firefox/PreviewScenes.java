/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import android.graphics.Color;

import app.hxreborn.extension.firefox.PreviewView.Drawing;
import app.hxreborn.extension.firefox.PreviewView.Pen;
import app.hxreborn.extension.firefox.PreviewView.Scene;

final class PreviewScenes {

    static final float WIDTH = 296;

    static final float HEIGHT = 56;

    static final float THUMB_WIDTH = 132;

    static final float THUMB_HEIGHT = 84;

    private static final float SKELETON = 0.35f;

    private static final int GREY_BAR = 0xFF312F33;

    private static final int GREY_SURFACE = 0xFF1D1B1F;

    private PreviewScenes() {
    }

    static Scene scene(Drawing stock, Drawing patched, String stockText, String patchedText) {
        return new Scene(WIDTH, HEIGHT, stock, patched, stockText, patchedText);
    }

    static Scene amoled() {
        return scene((pen) -> amoledFrame(pen, GREY_SURFACE, GREY_BAR),
                (pen) -> amoledFrame(pen, Color.BLACK, GREY_SURFACE), "Dark theme with grey surfaces",
                "Dark theme with pure black surfaces");
    }

    static Scene accent() {
        return scene((pen) -> accentUi(pen, pen.palette.defaultAccent), (pen) -> accentUi(pen, pen.palette.accent),
                "Interface in the default Firefox accent", "Interface in the chosen accent");
    }

    static Scene loading() {
        return scene((pen) -> pen.arc(148, 28, 14, -60, 270, pen.palette.accent, 3.5f), null,
                "Circular refresh indicator", "Morphing refresh indicator");
    }

    static Scene pinExtensions() {
        return scene((pen) -> pinToolbar(pen, false), (pen) -> pinToolbar(pen, true),
                "Toolbar without pinned extensions", "Toolbar with a pinned extension button");
    }

    static Scene scrollMemory() {
        return scene((pen) -> trayList(pen, 1, false), (pen) -> trayList(pen, 7, true),
                "Tab tray opens at the first tab", "Tab tray opens where you left it");
    }

    static Scene tabNext() {
        return scene((pen) -> tabStrip(pen, 5), (pen) -> tabStrip(pen, 2), "New tab opens at the end of the strip",
                "New tab opens next to the current tab");
    }

    static Scene tabEnd() {
        return scene((pen) -> trayToolbar(pen, false), (pen) -> trayToolbar(pen, true),
                "Tab tray toolbar with one add tab button", "Tab tray toolbar with an extra add-to-end button");
    }

    static Scene scrollButtons() {
        return scene((pen) -> trayScroll(pen, false), (pen) -> trayScroll(pen, true), "Tab tray without scroll buttons",
                "Tab tray with first and last tab buttons");
    }

    static Scene closeDuplicates() {
        return scene((pen) -> trayMenu(pen, false), (pen) -> trayMenu(pen, true),
                "Tab tray menu without Close duplicate tabs", "Tab tray menu with Close duplicate tabs");
    }

    static Scene selectPrevious() {
        return scene((pen) -> closedTab(pen, 3), (pen) -> closedTab(pen, 0),
                "Closing a tab selects the one beside it", "Closing a tab selects the one you came from");
    }

    static Scene rangeSelect() {
        return scene((pen) -> selection(pen, false), (pen) -> selection(pen, true),
                "Selecting two tabs selects only those two", "Selecting two tabs selects every tab between them");
    }

    static Scene keepTabs() {
        return scene((pen) -> backgroundTabs(pen, true), (pen) -> backgroundTabs(pen, false),
                "Background tabs reload when you return", "Background tabs stay loaded");
    }

    static Scene fullUrl() {
        return scene((pen) -> urlBar(pen, "", "mozilla.org"), (pen) -> urlBar(pen, "https://www.", "mozilla.org"),
                "Address bar shows mozilla.org", "Address bar shows https://www.mozilla.org");
    }

    static Scene qrScanner() {
        return scene((pen) -> searchBar(pen, true), (pen) -> searchBar(pen, false),
                "Address bar with the QR scanner button", "Address bar without the QR scanner button");
    }

    static Scene tabSuggestions() {
        return scene((pen) -> suggestions(pen, true), (pen) -> suggestions(pen, false),
                "Search suggestions include Switch to tab rows", "Search suggestions without Switch to tab rows");
    }

    static HomepageScene homepage(String host) {
        return new HomepageScene(host);
    }

    static Scene mostVisited() {
        return scene((pen) -> shortcuts(pen, 5, false, true), (pen) -> shortcuts(pen, 2, true, true),
                "Shortcuts include most visited sites", "Shortcuts keep only pinned sites");
    }

    static Scene promos() {
        return scene((pen) -> homeCard(pen, true), (pen) -> homeCard(pen, false), "Home shows a promotional card",
                "Home without a promotional card");
    }

    static Scene notifications() {
        return scene((pen) -> notificationShade(pen, true), (pen) -> notificationShade(pen, false),
                "Promotional notifications arrive", "No promotional notifications");
    }

    static Scene fullscreenToast() {
        return scene((pen) -> fullscreen(pen, true), (pen) -> fullscreen(pen, false),
                "Fullscreen video shows the exit hint", "Fullscreen video without the exit hint");
    }

    static Scene addons() {
        return scene((pen) -> addonList(pen, false), (pen) -> addonList(pen, true), "Add-on list stops at 50",
                "Add-on list shows all add-ons");
    }

    static Scene signIn() {
        return scene((pen) -> settingsList(pen, true), (pen) -> settingsList(pen, false),
                "Settings starts with the Sign in card", "Settings without the Sign in card");
    }

    static Scene aboutConfig() {
        return scene((pen) -> aboutConfigPage(pen, false), (pen) -> aboutConfigPage(pen, true),
                "about:config is not available", "about:config lists preferences");
    }

    static Scene secretSettings() {
        return scene((pen) -> secretList(pen, false), (pen) -> secretList(pen, true),
                "Settings without Secret Settings", "Settings with Secret Settings");
    }

    static Scene menuThumbnail(boolean sheet) {
        String name = sheet ? "Bottom sheet menu" : "Classic popup menu";
        Drawing drawing = (pen) -> menuThumb(pen, sheet);
        return new Scene(THUMB_WIDTH, THUMB_HEIGHT, drawing, drawing, name + " preview", name + " preview, selected");
    }

    private static int skeleton(Pen pen) {
        return SettingsStyle.fade(pen.palette.weak, SKELETON);
    }

    private static void amoledFrame(Pen pen, int surface, int bar) {
        pen.fill(16, 8, 280, 48, 8, surface);
        pen.fill(24, 14, 144, 24, 5, bar);
        pen.fill(24, 30, 254, 42, 6, bar);
        pen.circle(262, 19, 5, 0xFFCDB7FF);
    }

    private static void accentUi(Pen pen, int color) {
        pen.fill(20, 12, 84, 44, 16, color);
        pen.fill(94, 12, 180, 44, 8, pen.palette.bar);
        pen.fill(104, 26, 170, 30, 2, color);
        pen.fill(204, 18, 252, 44, 13, color);
        pen.circle(240, 31, 9, pen.palette.onAccent);
        pen.skeleton(94, 36, 40, 3, skeleton(pen));
    }

    private static void urlBar(Pen pen, String prefix, String host) {
        pen.fill(16, 12, 280, 44, 16, pen.palette.bar);
        pen.ring(34, 28, 6.5f, pen.palette.weak, 1.5f);
        pen.text(prefix, 50, 32.5f, 12, pen.palette.weak, false);
        pen.text(host, 50 + pen.measure(prefix, 12), 32.5f, 12, pen.palette.text, false);
    }

    private static void searchBar(Pen pen, boolean qr) {
        pen.fill(16, 12, 280, 44, 16, pen.palette.bar);
        pen.circle(34, 28, 9, pen.palette.card);
        pen.text("Search or enter address", 50, 32, 11, pen.palette.weak, false);
        if (qr) {
            int color = pen.palette.text;
            pen.outline(246, 21, 253, 28, 1.5f, color, 1.4f);
            pen.outline(257, 21, 264, 28, 1.5f, color, 1.4f);
            pen.outline(246, 32, 253, 39, 1.5f, color, 1.4f);
            pen.fill(258, 33, 261, 36, 0.6f, color);
            pen.fill(262, 37, 265, 40, 0.6f, color);
        }
    }

    private static void pinToolbar(Pen pen, boolean extension) {
        pen.fill(16, 12, 216, 44, 16, pen.palette.bar);
        pen.ring(34, 28, 6.5f, pen.palette.weak, 1.5f);
        pen.skeleton(50, 26, 90, 4, skeleton(pen));
        int dots = 258;
        if (extension) {
            pen.fill(224, 16, 248, 40, 7, pen.palette.accentContainer);
            pen.polyline(pen.palette.onAccentContainer, 1.6f, 231, 28, 241, 28);
            pen.polyline(pen.palette.onAccentContainer, 1.6f, 236, 23, 236, 33);
            dots = 266;
        }
        for (int dot = 0; dot < 3; dot++) {
            pen.circle(dots, 22 + 6 * dot, 1.7f, pen.palette.text);
        }
    }

    private static void trayCard(Pen pen, float top, String label, boolean current) {
        pen.fill(16, top, 248, top + 13, 5, pen.palette.bar);
        if (current) {
            pen.outline(16, top, 248, top + 13, 5, pen.palette.accent, 1.5f);
        }
        pen.text(label, 24, top + 9.5f, 9, pen.palette.text, false);
        pen.skeleton(188, top + 5, 48, 3, skeleton(pen));
    }

    private static void trayList(Pen pen, int first, boolean remembered) {
        for (int row = 0; row < 3; row++) {
            trayCard(pen, 6 + 16 * row, "Tab " + (first + row), remembered && row == 1);
        }
        pen.fill(258, remembered ? 26 : 8, 262, remembered ? 42 : 24, 2, pen.palette.weak);
    }

    private static void tabStrip(Pen pen, int newIndex) {
        int label = 1;
        for (int slot = 0; slot < 6; slot++) {
            float left = 16 + 46 * slot;
            if (slot == newIndex) {
                pen.fill(left, 12, left + 40, 44, 8, pen.palette.accent);
                pen.textCenter("+", left + 20, 33, 16, pen.palette.onAccent, false);
                continue;
            }
            pen.fill(left, 12, left + 40, 44, 8, pen.palette.bar);
            if (label == 2) {
                pen.outline(left, 12, left + 40, 44, 8, pen.palette.accent, 1.5f);
            }
            pen.textCenter(String.valueOf(label), left + 20, 32, 12, pen.palette.text, false);
            label++;
        }
    }

    private static void trayToolbar(Pen pen, boolean end) {
        pen.fill(40, 10, 256, 46, 18, pen.palette.bar);
        float cx = end ? 116 : 148;
        pen.circle(cx, 28, 11, pen.palette.accent);
        pen.line(cx - 5, 28, cx + 5, 28, pen.palette.onAccent, 1.8f);
        pen.line(cx, 23, cx, 33, pen.palette.onAccent, 1.8f);
        if (end) {
            pen.ring(180, 28, 11, pen.palette.accent, 1.6f);
            pen.polyline(pen.palette.accent, 1.6f, 174, 28, 183, 28);
            pen.polyline(pen.palette.accent, 1.6f, 180, 23, 185, 28, 180, 33);
            pen.line(188, 22, 188, 34, pen.palette.accent, 1.6f);
        }
    }

    private static void trayScroll(Pen pen, boolean buttons) {
        for (int row = 0; row < 3; row++) {
            pen.fill(16, 6 + 16 * row, 216, 19 + 16 * row, 5, pen.palette.bar);
            pen.skeleton(24, 11 + 16 * row, 70, 3, skeleton(pen));
        }
        if (!buttons) {
            return;
        }
        for (int button = 0; button < 2; button++) {
            float cy = (button == 0) ? 16 : 40;
            pen.circle(250, cy, 11, pen.palette.accent);
            float tip = (button == 0) ? cy - 3 : cy + 3;
            float base = (button == 0) ? cy + 2 : cy - 2;
            pen.polyline(pen.palette.onAccent, 1.8f, 245, base, 250, tip, 255, base);
        }
    }

    private static void trayMenu(Pen pen, boolean closeDuplicates) {
        pen.fill(76, 3, 220, 53, 8, pen.palette.card);
        pen.outline(76, 3, 220, 53, 8, pen.palette.outlineSoft, 1);
        String[] labels = closeDuplicates
                ? new String[] { "Select tabs", "Share all tabs", "Close duplicate tabs", "Close all tabs" }
                : new String[] { "Select tabs", "Share all tabs", "Close all tabs" };
        float first = closeDuplicates ? 15 : 19;
        float step = closeDuplicates ? 11 : 14;
        for (int row = 0; row < labels.length; row++) {
            boolean added = closeDuplicates && row == 2;
            pen.text(labels[row], 86, first + step * row, 9.5f, added ? pen.palette.accent : pen.palette.text, added);
        }
    }

    private static void closedTab(Pen pen, int selected) {
        for (int tab = 0; tab < 4; tab++) {
            float x = 16 + 68 * tab;
            boolean closed = tab == 2;
            if (closed) {
                pen.outline(x, 8, x + 60, 48, 8, pen.palette.outline, 1);
                continue;
            }
            pen.fill(x, 8, x + 60, 48, 8, pen.palette.bar);
            pen.skeleton(x + 8, 18, 36, 3, skeleton(pen));
            pen.skeleton(x + 8, 26, 44, 3, skeleton(pen));
            if (tab == selected) {
                pen.outline(x, 8, x + 60, 48, 8, pen.palette.accent, 2);
            }
        }
    }

    private static void notificationShade(Pen pen, boolean promos) {
        int rows = promos ? 3 : 1;
        for (int row = 0; row < rows; row++) {
            float top = 5 + 17 * row;
            pen.fill(16, top, 280, top + 14, 6, pen.palette.bar);
            pen.circle(27, top + 7, 4, (row == 0) ? pen.palette.outline : pen.palette.accent);
            pen.skeleton(38, top + 4, 96, 3, skeleton(pen));
            pen.skeleton(38, top + 9, 150, 2.5f, skeleton(pen));
        }
    }

    private static void selection(Pen pen, boolean range) {
        for (int row = 0; row < 4; row++) {
            float top = 5 + 13 * row;
            boolean selected = range ? row >= 1 : (row == 1 || row == 3);
            pen.fill(16, top, 280, top + 11, 4, (range && selected) ? pen.palette.accentContainer : pen.palette.bar);
            if (selected) {
                pen.circle(27, top + 5.5f, 4, pen.palette.accent);
                pen.polyline(pen.palette.onAccent, 1.2f, 25, top + 5.5f, 26.5f, top + 7, 29.5f, top + 3.5f);
            } else {
                pen.ring(27, top + 5.5f, 3.6f, pen.palette.outline, 1.1f);
            }
            pen.skeleton(38, top + 4, 96, 3, skeleton(pen));
        }
    }

    private static void backgroundTabs(Pen pen, boolean reload) {
        for (int tab = 0; tab < 2; tab++) {
            float x = 36 + 132 * tab;
            pen.fill(x, 6, x + 92, 50, 8, pen.palette.bar);
            pen.skeleton(x + 8, 14, 50, 3, skeleton(pen));
            pen.skeleton(x + 8, 22, 76, 3, skeleton(pen));
            pen.skeleton(x + 8, 29, 60, 3, skeleton(pen));
            if (reload) {
                pen.fill(x, 6, x + 92, 50, 8, SettingsStyle.fade(pen.palette.sheet, 0.6f));
                pen.arc(x + 46, 28, 8, -70, 250, pen.palette.text, 2);
            }
        }
    }

    private static void suggestionRow(Pen pen, String label, float baseline, boolean switchToTab) {
        pen.circle(26, baseline - 3.5f, 5, pen.palette.bar);
        pen.text(label, 38, baseline, 10, pen.palette.text, false);
        if (switchToTab) {
            pen.textEnd("Switch to tab", 272, baseline, 9, pen.palette.accent, true);
        }
    }

    private static void suggestions(Pen pen, boolean switchToTab) {
        pen.fill(16, 4, 280, 20, 8, pen.palette.bar);
        pen.text("wiki", 26, 15.5f, 10, pen.palette.text, false);
        suggestionRow(pen, "Wikipedia - Wikimedia Foundation", 30, false);
        suggestionRow(pen, "Firefox - Wikipedia", 42, false);
        if (switchToTab) {
            suggestionRow(pen, "GitHub - Wikipedia", 54, true);
        }
    }

    private static void shortcuts(Pen pen, int count, boolean addTile, boolean pins) {
        String[] letters = { "G", "W", "E", "N", "M" };
        for (int index = 0; index < count; index++) {
            float cx = 38 + 54 * index;
            pen.circle(cx, 22, 12, pen.palette.bar);
            pen.textCenter(letters[index], cx, 26, 11, pen.palette.text, false);
            if (index < 2 && pins) {
                pen.circle(cx - 10, 11, 2.8f, pen.palette.accent);
            }
            pen.skeleton(cx - 11, 40, 22, 3, skeleton(pen));
        }
        if (addTile) {
            pen.ring(146, 22, 11.5f, pen.palette.weak, 1.3f);
            pen.line(141, 22, 151, 22, pen.palette.weak, 1.4f);
            pen.line(146, 17, 146, 27, pen.palette.weak, 1.4f);
        }
    }

    private static void homeCard(Pen pen, boolean card) {
        pen.fill(16, 6, 280, 20, 7, pen.palette.bar);
        pen.skeleton(24, 11.5f, 60, 3, skeleton(pen));
        if (card) {
            pen.fill(16, 24, 280, 52, 10, pen.palette.accentContainer);
            pen.skeleton(26, 31, 110, 3.5f, pen.palette.onAccentContainer);
            pen.skeleton(26, 39, 70, 3, SettingsStyle.fade(pen.palette.onAccentContainer, 0.6f));
            pen.fill(214, 32, 270, 44, 6, pen.palette.accent);
            pen.skeleton(224, 36.5f, 36, 3, pen.palette.onAccent);
        } else {
            for (int tile = 0; tile < 4; tile++) {
                pen.circle(40 + 62 * tile, 36, 8, pen.palette.bar);
            }
        }
    }

    private static void fullscreen(Pen pen, boolean toast) {
        pen.fill(16, 5, 280, 51, 8, pen.palette.bar);
        pen.skeleton(26, 12, 60, 3, skeleton(pen));
        pen.skeleton(26, 19, 36, 3, skeleton(pen));
        pen.circle(148, 24, 9, pen.palette.card);
        pen.polygon(pen.palette.text, 146, 19.5f, 152, 24, 146, 28.5f);
        if (toast) {
            pen.fill(78, 36, 218, 48, 6, pen.palette.text);
            pen.textCenter("Swipe up to exit full screen", 148, 44.6f, 8, pen.palette.sheet, false);
        }
    }

    private static void addonList(Pen pen, boolean all) {
        for (int row = 0; row < 3; row++) {
            pen.fill(16, 6 + 13 * row, 280, 17 + 13 * row, 4, pen.palette.bar);
            pen.circle(27, 11.5f + 13 * row, 3.6f, pen.palette.accentContainer);
            pen.skeleton(38, 10 + 13 * row, 86, 3, skeleton(pen));
        }
        pen.fill(206, 41, 280, 52, 5.5f, all ? pen.palette.successContainer : pen.palette.errorContainer);
        pen.textCenter(all ? "248 add-ons" : "50 of 248", 243, 49, 8.5f,
                all ? pen.palette.text : pen.palette.onErrorContainer, true);
    }

    private static void settingsRow(Pen pen, String label, float baseline) {
        pen.text(label, 24, baseline, 10.5f, pen.palette.text, false);
        pen.skeleton(228, baseline - 4, 36, 3, skeleton(pen));
    }

    private static void settingsList(Pen pen, boolean card) {
        if (card) {
            pen.fill(14, 5, 282, 29, 10, pen.palette.bar);
            pen.circle(28, 17, 7, pen.palette.accent);
            pen.text("Sign in", 42, 16, 10.5f, pen.palette.text, true);
            pen.skeleton(42, 21, 76, 3, skeleton(pen));
            settingsRow(pen, "Search", 42);
            settingsRow(pen, "Tabs", 56);
        } else {
            settingsRow(pen, "Search", 18);
            settingsRow(pen, "Tabs", 34);
            settingsRow(pen, "Homepage", 50);
        }
    }

    private static void aboutConfigPage(Pen pen, boolean listed) {
        pen.fill(16, 6, 280, 20, 7, pen.palette.bar);
        pen.text("about:config", 30, 16.5f, 9.5f, pen.palette.text, false);
        if (!listed) {
            pen.textCenter("This page is not available", 148, 40, 10, pen.palette.weak, false);
            return;
        }
        String[] names = { "browser.search.region", "network.http.max-connections",
                "privacy.trackingprotection.enabled" };
        for (int row = 0; row < names.length; row++) {
            pen.fill(16, 24 + 10 * row, 280, 32.5f + 10 * row, 3, pen.palette.bar);
            pen.text(names[row], 22, 30.5f + 10 * row, 7.5f, pen.palette.weak, false);
        }
    }

    private static void secretList(Pen pen, boolean secret) {
        float step = secret ? 12 : 14;
        float first = secret ? 12 : 16;
        String[] labels = { "Search", "Tabs", "Homepage" };
        for (int row = 0; row < labels.length; row++) {
            settingsRow(pen, labels[row], first + step * row);
        }
        if (secret) {
            pen.fill(14, 40, 282, 52, 6, pen.palette.accentContainer);
            pen.text("Secret Settings", 24, 48.5f, 10, pen.palette.onAccentContainer, true);
        }
    }

    private static void menuThumb(Pen pen, boolean sheet) {
        int line = skeleton(pen);
        pen.fill(14, 10, 118, 18, 3, pen.palette.bar);
        pen.skeleton(16, 24, 56, 3, line);
        pen.skeleton(16, 31, 80, 3, line);
        if (sheet) {
            pen.fill(6, 44, 126, 92, 12, pen.palette.card);
            pen.outline(6, 44, 126, 92, 12, pen.palette.outlineSoft, 1);
            pen.fill(56, 48, 76, 51, 1.5f, pen.palette.weak);
            for (int row = 0; row < 3; row++) {
                pen.circle(20, 60 + 10 * row, 3, pen.palette.text);
                pen.skeleton(28, 58.5f + 10 * row, 52, 3.5f, pen.palette.text);
            }
        } else {
            pen.fill(58, 12, 114, 72, 8, pen.palette.card);
            pen.outline(58, 12, 114, 72, 8, pen.palette.outlineSoft, 1);
            for (int row = 0; row < 4; row++) {
                pen.circle(68, 24 + 12 * row, 3, pen.palette.text);
                pen.skeleton(76, 22.5f + 12 * row, 30, 3.5f, pen.palette.text);
            }
        }
    }

    static final class HomepageScene extends Scene {

        private final String[] host;

        private HomepageScene(String host) {
            this(new String[] { host });
        }

        private HomepageScene(String[] host) {
            super(WIDTH, HEIGHT, (pen) -> newTab(pen, null), (pen) -> newTab(pen, host),
                    "New tab shows the Firefox home", "");
            this.host = host;
        }

        void setHost(String value) {
            this.host[0] = value;
        }

        @Override
        String describe(boolean on) {
            return on ? ("New tab opens " + this.host[0]) : super.describe(false);
        }

        private static void newTab(Pen pen, String[] host) {
            pen.fill(16, 6, 280, 22, 8, pen.palette.bar);
            int skeleton = SettingsStyle.fade(pen.palette.weak, SKELETON);
            if (host == null) {
                pen.text("Search", 30, 17, 10, pen.palette.weak, false);
                for (int tile = 0; tile < 4; tile++) {
                    float cx = 44 + 70 * tile;
                    pen.circle(cx, 38, 8.5f, pen.palette.bar);
                    pen.skeleton(cx - 9, 50, 18, 3, skeleton);
                }
            } else {
                pen.text(host[0], 30, 17, 10, pen.palette.text, false);
                pen.skeleton(16, 30, 120, 6, skeleton);
                pen.skeleton(16, 40, 230, 3, skeleton);
                pen.skeleton(16, 47, 180, 3, skeleton);
            }
        }

    }

}
