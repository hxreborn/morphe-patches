/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/TabFab.java
 */
package app.hxreborn.extension.firefox;

import android.util.Log;
import androidx.compose.runtime.Composer;
import kotlin.jvm.functions.Function0;

@SuppressWarnings("unused")
public final class TabToolbar {

    private static final String TAG = "FirefoxPatches";

    private TabToolbar() {
    }

    public static void trayPill(Object toolbar, Composer composer) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void pill(Object state, Object addTab, Composer composer) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void pillContent(Object state, Object addTab, Composer composer) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void button(Function0<?> onClick, Composer composer, int icon, int key, String label) {
        throw new IllegalStateException("Patch not applied");
    }

    public static boolean pillVisible(Object state) {
        return endVisible(state) || scrollVisible(state);
    }

    public static boolean endVisible(Object state) {
        return isPage(state, "NormalTabs") && endButton();
    }

    public static boolean scrollVisible(Object state) {
        return scrollButtonsApplied() && (isPage(state, "NormalTabs") || isPage(state, "PrivateTabs"))
                && PatchSettings.tabScroll();
    }

    public static boolean scrollButtonsApplied() {
        return false;
    }

    public static int endIcon() {
        return Drawables.id("hx_ic_tab_add_end");
    }

    public static boolean endButton() {
        return PatchSettings.tabNext() && PatchSettings.tabEnd();
    }

    private static boolean isPage(Object state, String name) {
        try {
            Object page = Reflection.read(state, "selectedPage");
            return (page instanceof Enum) && name.equals(((Enum<?>) page).name());
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "selectedPage read failed", ex);
            return false;
        }
    }

}
