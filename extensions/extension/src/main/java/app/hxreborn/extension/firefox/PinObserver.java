/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/PinObserver.java
 */
package app.hxreborn.extension.firefox;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public final class PinObserver implements Function1<Object, Object> {

    public Object invoke(Object state) {
        try {
            ExtensionPin.onState(state);
        } catch (Throwable ignored) {
        }
        return Unit.INSTANCE;
    }

}
