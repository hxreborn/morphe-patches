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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import android.content.Context;
import app.morphe.extension.shared.settings.BooleanSetting;

final class PatchGroup {

    final String title;

    final List<Row> rows = new ArrayList<>();

    PatchGroup(String title) {
        this.title = title;
    }

    void addToggle(String iconName, String title, String description, BooleanSetting setting) {
        addToggle(iconName, title, description, setting, false);
    }

    void addToggle(String iconName, String title, String description, BooleanSetting setting, boolean restart) {
        this.rows.add(new Row(Kind.TOGGLE, iconName, title, description, setting, restart, null, null));
    }

    void addExperimentalToggle(String iconName, String title, String description, BooleanSetting setting) {
        this.rows.add(new Row(Kind.TOGGLE, iconName, title, description, setting, false, "Experimental", null));
    }

    void addLocation(String iconName, String title, String description, BooleanSetting setting) {
        this.rows.add(new Row(Kind.LOCATION, iconName, title, description, setting, false, null, null));
    }

    void addFolder(String iconName, String title, String description) {
        this.rows.add(new Row(Kind.FOLDER, iconName, title, description, null, false, null, null));
    }

    void addAction(String iconName, String title, String description, Consumer<Context> action) {
        this.rows.add(new Row(Kind.ACTION, iconName, title, description, null, false, null, action));
    }

    void addAlwaysOn(String iconName, String title, String description) {
        this.rows.add(new Row(Kind.ALWAYS_ON, iconName, title, description, null, false, null, null));
    }

    boolean isEmpty() {
        return this.rows.isEmpty();
    }

    int onCount() {
        int alwaysOn = 0;
        final boolean[] switchStates = new boolean[this.rows.size()];
        int switches = 0;
        for (Row row : this.rows) {
            if (row.setting == null) {
                alwaysOn++;
            } else {
                switchStates[switches++] = row.setting.get();
            }
        }
        return PatchSummary.countOn(alwaysOn, Arrays.copyOf(switchStates, switches));
    }

    enum Kind {

        TOGGLE, LOCATION, FOLDER, ALWAYS_ON, ACTION

    }

    static final class Row {

        final Kind kind;

        final String iconName;

        final String title;

        final String description;

        final BooleanSetting setting;

        final boolean restart;

        final String chip;

        final Consumer<Context> action;

        private Row(Kind kind, String iconName, String title, String description, BooleanSetting setting,
                boolean restart, String chip, Consumer<Context> action) {
            this.kind = kind;
            this.iconName = iconName;
            this.title = title;
            this.description = description;
            this.setting = setting;
            this.restart = restart;
            this.chip = chip;
            this.action = action;
        }

    }

}
