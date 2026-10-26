/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/TabEndClick.java
 */
package app.hxreborn.extension.firefox;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

@SuppressWarnings("unused")
public final class TabEndClick implements Function0<Object> {

    private final Object base;

    private final boolean atEnd;

    private TabEndClick(Object base, boolean atEnd) {
        this.base = base;
        this.atEnd = atEnd;
    }

    public static TabEndClick of(Object base) {
        return new TabEndClick(base, true);
    }

    public static TabEndClick regular(Object base) {
        return new TabEndClick(base, false);
    }

    public Object invoke() {
        if (this.atEnd) {
            TabOrder.markEnd();
        } else {
            TabOrder.clearEnd();
        }
        ExtensionPin.invokeFunction0(this.base);
        return Unit.INSTANCE;
    }

}
