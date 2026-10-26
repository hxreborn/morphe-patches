/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import androidx.compose.runtime.Composer;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;

@SuppressWarnings("unused")
public final class TabPillContent implements Function3<Object, Object, Object, Object> {

    private final Object state;

    private final Object addTab;

    public TabPillContent(Object state, Object addTab) {
        this.state = state;
        this.addTab = addTab;
    }

    public Object invoke(Object scope, Object composer, Object changed) {
        TabToolbar.pillContent(this.state, this.addTab, (Composer) composer);
        return Unit.INSTANCE;
    }

}
