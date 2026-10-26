/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/ModCompose.java
 */
package app.hxreborn.extension.firefox;

@SuppressWarnings("unused")
public final class MenuCompose {

    private MenuCompose() {
    }

    public static void divider(Object composer) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void managerRow(Object composer, Object onClick) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void extensionsPage(Object content, Object onBack, Object composer) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void libraryItem(Object composer, int label, int icon, Object onClick) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void libraryList(Object history, Object bookmarks, Object downloads, Object passwords,
            Object composer) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void libraryGroup(Object bookmarks, Object history, Object downloads, Object passwords,
            Object composer) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void libraryGroupFlagged(boolean showPasswords, boolean flag, Object bookmarks, Object history,
            Object downloads, Object passwords, Object composer) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void patchesRow(Object composer) {
        throw new IllegalStateException("Patch not applied");
    }

    public static void patchesRowContent(Object composer) {
        throw new IllegalStateException("Patch not applied");
    }

}
