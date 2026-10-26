/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/PinIconContinuation.java
 */
package app.hxreborn.extension.firefox;

import android.graphics.Bitmap;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public final class PinIconContinuation implements Continuation<Object> {

    private final PinState state;

    PinIconContinuation(PinState state) {
        this.state = state;
    }

    @Override
    public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
    }

    @Override
    public void resumeWith(Object result) {
        try {
            if (result instanceof Bitmap) {
                this.state.bitmap = (Bitmap) result;
                ExtensionPin.refresh();
            }
        } catch (Throwable ignored) {
        }
    }

}
