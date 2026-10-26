/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/RangeSelectStart.java
 */
package app.hxreborn.extension.firefox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import android.util.Log;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
public final class RangeSelectStart implements Function1<Object, Object> {

    private static final String TAG = "FirefoxPatches";

    private final Object listState;

    private final Object state;

    RangeSelectStart(Object listState, Object state) {
        this.listState = listState;
        this.state = state;
    }

    public Object invoke(Object offset) {
        try {
            press(offset);
        } catch (Throwable ex) {
            Log.e(TAG, "range long press failed", ex);
        }
        return Unit.INSTANCE;
    }

    private void press(Object offset) throws Exception {
        if (this.listState == null || this.state == null || offset == null) {
            return;
        }
        Field packedField = offset.getClass().getDeclaredField("packedValue");
        packedField.setAccessible(true);
        long packed = packedField.getLong(offset);

        Object layout = Reflection.call(this.listState, "getLayoutInfo");
        boolean vertical = ((Enum) Reflection.call(layout, "getOrientation")).ordinal() == 0;
        int position = (int) Float.intBitsToFloat(vertical ? (int) packed : (int) (packed >> 32));

        Object hit = null;
        for (Object item : (List) Reflection.call(layout, "getVisibleItemsInfo")) {
            int offsetStart = (Integer) Reflection.call(item, "getOffset");
            int end = offsetStart + (Integer) Reflection.call(item, "getSize");
            if (offsetStart <= position && position <= end) {
                hit = item;
                break;
            }
        }
        if (hit == null) {
            return;
        }

        haptic();
        Object callback = Reflection.read(this.state, "onLongPress");
        Method invoke = callback.getClass().getMethod("invoke", Object.class);
        invoke.setAccessible(true);
        invoke.invoke(callback, hit);
    }

    private void haptic() {
        try {
            Object feedback = Reflection.read(this.state, "hapticFeedback");
            for (Method method : feedback.getClass().getMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (method.getName().startsWith("performHapticFeedback") && p.length == 1 && p[0] == int.class) {
                    method.setAccessible(true);
                    method.invoke(feedback, 0);
                    return;
                }
            }
        } catch (Throwable ex) {
            Log.e(TAG, "range select haptic failed", ex);
        }
    }

}
