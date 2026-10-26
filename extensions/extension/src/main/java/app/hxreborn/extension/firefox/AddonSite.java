/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@SuppressWarnings("unused")
public final class AddonSite {

    private static final String HOST = "addons.mozilla.org";

    private static final int DESKTOP = 1;

    private static final int TARGET_WINDOW_NEW = 2;

    private static final Map<Object, Integer> PREVIOUS_MODE = Collections.synchronizedMap(new WeakHashMap<>());

    private AddonSite() {
    }

    public static int userAgentMode(Object session, String uri, int target, int current) {
        if (target == TARGET_WINDOW_NEW) {
            return current;
        }
        if (PatchSettings.unlockAllExtensions() && isAddonSite(uri)) {
            if (!PREVIOUS_MODE.containsKey(session)) {
                PREVIOUS_MODE.put(session, current);
            }
            return DESKTOP;
        }
        Integer previous = PREVIOUS_MODE.remove(session);
        return (previous != null) ? previous : current;
    }

    private static boolean isAddonSite(String uri) {
        try {
            return HOST.equals(new URI(uri).getHost());
        } catch (URISyntaxException ex) {
            return false;
        }
    }

}
