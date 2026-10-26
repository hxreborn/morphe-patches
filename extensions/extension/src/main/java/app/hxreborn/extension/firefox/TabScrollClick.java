/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/TabScrollClick.java
 */
package app.hxreborn.extension.firefox;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

@SuppressWarnings("unused")
public final class TabScrollClick implements Function0<Object> {

    static final TabScrollClick UP = new TabScrollClick(false);
    static final TabScrollClick DOWN = new TabScrollClick(true);

    private final boolean down;

    private TabScrollClick(boolean down) {
        this.down = down;
    }

    public Object invoke() {
        TabScroll.go(this.down);
        return Unit.INSTANCE;
    }

}
