/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/ModClick.java
 */
package app.hxreborn.extension.firefox;

import android.content.Context;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

@SuppressWarnings("unused")
public final class PatchesMenuClick implements Function0<Object> {

    private final Context context;

    private PatchesMenuClick(Context context) {
        this.context = context;
    }

    public static PatchesMenuClick open(Context context) {
        return new PatchesMenuClick(context);
    }

    public Object invoke() {
        PatchSettings.open(this.context);
        return Unit.INSTANCE;
    }

}
