/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/build.gradle
 */
package app.hxreborn.extension.firefox;

@SuppressWarnings("unused")
public final class AddonCollection {

    private static final String COLLECTION_USER = "18187371";

    private static final String COLLECTION_NAME = "Iceraven_Addon_Collection";

    private AddonCollection() {
    }

    public static String user(String stock) {
        return PatchSettings.extendedAddonList() ? COLLECTION_USER : stock;
    }

    public static String name(String stock) {
        return PatchSettings.extendedAddonList() ? COLLECTION_NAME : stock;
    }

}
