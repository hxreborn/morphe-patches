/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

@SuppressWarnings("unused")
public final class SponsoredPosts {

    private static final String TAG = "SponsoredPosts";

    private static final String SPONSORED_FIELD = "isSponsored_";

    private SponsoredPosts() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static Iterable<?> withoutSponsored(Iterable<?> items) {
        final List<Object> kept = new ArrayList<>();
        try {
            for (Object item : items) {
                if (!isSponsored(item)) {
                    kept.add(item);
                }
            }
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "Feed items left unfiltered", ex);
            return items;
        }
        return kept;
    }

    private static boolean isSponsored(Object item) throws ReflectiveOperationException {
        final Field field = item.getClass().getDeclaredField(SPONSORED_FIELD);
        field.setAccessible(true);
        return field.getBoolean(item);
    }

}
