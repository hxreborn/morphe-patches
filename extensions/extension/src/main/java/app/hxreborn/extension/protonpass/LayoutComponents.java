/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.protonpass;

import app.hxreborn.extension.proton.PatchSettings;

@SuppressWarnings("unused")
public final class LayoutComponents {

    public static final String HELP_CENTER = "hide_help_center";

    public static final String SPREAD_THE_WORD = "hide_spread_the_word";

    public static final String ABOUT = "hide_about_section";

    private LayoutComponents() {

    }

    public static boolean isPatched() {
        return false;
    }

    public static boolean hasSpreadTheWord() {
        return false;
    }

    public static boolean isHelpCenterHidden() {
        return isHidden(HELP_CENTER);
    }

    public static boolean isSpreadTheWordHidden() {
        return isHidden(SPREAD_THE_WORD);
    }

    public static boolean isAboutHidden() {
        return isHidden(ABOUT);
    }

    public static boolean isHidden(String key) {
        return isPatched() && PatchSettings.isEnabled(key, false);
    }

}
