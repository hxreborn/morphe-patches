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
package app.hxreborn.extension.netanalyzer;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class PatchesMenuActivity extends HostScreenActivity {

    static final String EXTRA_ENTRIES = "app.hxreborn.extension.netanalyzer.MENU_ENTRIES";

    private static final String ENTRY_SEPARATOR = ";";

    private static final String FIELD_SEPARATOR = "\\|";

    private static final int SECTION = 0;

    private static final int TITLE = 1;

    private static final int ACTIVITY = 2;

    private static final int FIELDS = 3;

    @Override
    String screenTitle() {
        return "hxreborn patches";
    }

    @Override
    void buildContent() {
        final String entries = getIntent().getStringExtra(EXTRA_ENTRIES);
        if (entries == null || entries.isEmpty()) {
            finish();
            return;
        }
        String currentSection = null;
        for (String entry : entries.split(ENTRY_SEPARATOR)) {
            final String[] fields = entry.split(FIELD_SEPARATOR, FIELDS);
            if (fields.length != FIELDS) {
                continue;
            }
            if (!fields[SECTION].equals(currentSection)) {
                currentSection = fields[SECTION];
                section(currentSection);
            }
            final String activity = fields[ACTIVITY];
            clickableRow(fields[TITLE], "", (view) -> startActivity(new Intent().setClassName(this, activity)));
        }
    }

}
