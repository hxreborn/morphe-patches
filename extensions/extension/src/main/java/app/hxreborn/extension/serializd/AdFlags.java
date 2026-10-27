/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.serializd;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class AdFlags {

    private static final Pattern ENABLED_FLAG = Pattern
        .compile("\"(shouldShowAds|shouldShowStickyNavigationAds|shouldUseSingletonMrec)\"(\\s*:\\s*)true");

    private AdFlags() {

    }

    public static String disable(String body) {
        if (body == null || !body.contains("\"should")) {
            return body;
        }
        return ENABLED_FLAG.matcher(body).replaceAll("\"$1\"$2false");
    }

}
