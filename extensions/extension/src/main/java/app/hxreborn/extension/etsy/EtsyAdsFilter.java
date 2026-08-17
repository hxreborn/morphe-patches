/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.etsy;

import java.util.ArrayList;
import java.util.List;

public final class EtsyAdsFilter {
    private EtsyAdsFilter() {}

    public static List<?> removeAds(List<?> components) {
        if (components == null || components.isEmpty()) {
            return components;
        }

        ArrayList<Object> kept = new ArrayList<>(components.size());
        for (Object component : components) {
            if (component == null || !isPromoted(component)) {
                kept.add(component);
            }
        }
        return kept;
    }

    private static boolean isPromoted(Object component) {
        return component.toString().contains("AdLabelApiModel");
    }
}
