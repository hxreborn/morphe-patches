/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.download;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class DownloadWatermark {

    private DownloadWatermark() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean removeLogo() {
        return PatchSettings.REMOVE_WATERMARK.get();
    }

}
