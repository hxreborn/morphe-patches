/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        this.rows.add(new Row(Kind.TOGGLE, iconName, title, description, setting, restart, null));
    }

    void addExperimentalToggle(String iconName, String title, String description, BooleanSetting setting) {
        this.rows.add(new Row(Kind.TOGGLE, iconName, title, description, setting, false, "Experimental"));
    }

    void addLocation(String iconName, String title, String description, BooleanSetting setting) {
        this.rows.add(new Row(Kind.LOCATION, iconName, title, description, setting, false, null));
    }

    void addFolder(String iconName, String title, String description) {
        this.rows.add(new Row(Kind.FOLDER, iconName, title, description, null, false, null));
    }

    void addAlwaysOn(String iconName, String title, String description) {
        this.rows.add(new Row(Kind.ALWAYS_ON, iconName, title, description, null, false, null));
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

        TOGGLE, LOCATION, FOLDER, ALWAYS_ON

    }

    static final class Row {

        final Kind kind;

        final String iconName;

        final String title;

        final String description;

        final BooleanSetting setting;

        final boolean restart;

        final String chip;

        private Row(Kind kind, String iconName, String title, String description, BooleanSetting setting,
                boolean restart, String chip) {
            this.kind = kind;
            this.iconName = iconName;
            this.title = title;
            this.description = description;
            this.setting = setting;
            this.restart = restart;
            this.chip = chip;
        }

    }

}
