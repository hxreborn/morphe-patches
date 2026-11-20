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
package app.hxreborn.extension.proton;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.view.View;

import org.robolectric.Robolectric;

public final class RecordingSettingsScreen implements SettingsScreen {

    private final Activity activity = Robolectric.buildActivity(Activity.class).get();

    private final List<String> titles = new ArrayList<>();

    private final List<View> views = new ArrayList<>();

    public List<String> titles() {
        return this.titles;
    }

    public List<View> views() {
        return this.views;
    }

    public List<View> rowsFrom(AppSettings settings, String section) {
        this.views.clear();
        this.titles.clear();
        if ("account".equals(section)) {
            settings.accountRows(this, this.views);
        } else if ("general".equals(section)) {
            settings.generalRows(this, this.views);
        } else {
            settings.layoutRows(this, this.views);
        }
        return this.views;
    }

    @Override
    public Activity activity() {
        return this.activity;
    }

    @Override
    public View row(String title, String summary, Runnable onClick) {
        return record(title);
    }

    @Override
    public View switchRow(String title, String summary, boolean checked, CheckedChangeListener onChange) {
        return record(title);
    }

    @Override
    public View layoutSwitchRow(String title, String summary, String key) {
        return record(title);
    }

    @Override
    public void recreateContent() {
    }

    private View record(String title) {
        this.titles.add(title);
        return new View(this.activity);
    }

}
