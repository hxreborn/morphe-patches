/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.imo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class HomeTabs {

    private static final Integer VOICE_CLUB_TAB_ID = 2;

    private HomeTabs() {

    }

    public static List<Integer> withoutVoiceClub(List<Integer> tabIds) {
        List<Integer> visible = new ArrayList<>(tabIds);
        visible.remove(VOICE_CLUB_TAB_ID);
        return visible;
    }

}
