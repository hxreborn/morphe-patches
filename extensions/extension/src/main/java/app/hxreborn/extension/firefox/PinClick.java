/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/PinClick.java
 */
package app.hxreborn.extension.firefox;

import kotlin.jvm.functions.Function0;

public final class PinClick implements Function0<Object> {

    final Object base;

    final Object onLong;

    PinClick(Object base, Object onLong) {
        this.base = base;
        this.onLong = onLong;
    }

    public Object invoke() {
        return ExtensionPin.invokeFunction0(this.base);
    }

}
