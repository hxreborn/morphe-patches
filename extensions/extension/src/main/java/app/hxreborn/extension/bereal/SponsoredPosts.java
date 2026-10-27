/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
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
