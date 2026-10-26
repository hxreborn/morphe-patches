/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/PinState.java
 */
package app.hxreborn.extension.firefox;

import android.graphics.Bitmap;

final class PinState {

    volatile Bitmap bitmap;

    Integer bg;

    Boolean enabled;

    Integer fg;

    boolean hidden;

    String id;

    Object loader;

    String text;

    String title;

}
