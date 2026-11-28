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
package app.hxreborn.extension.protonvpn;

import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

final class SplitTunnelingAppAdapter extends BaseAdapter {

    private final Activity activity;

    private final List<SplitTunnelingApp> apps;

    SplitTunnelingAppAdapter(Activity activity, List<SplitTunnelingApp> apps) {
        this.activity = activity;
        this.apps = apps;
    }

    @Override
    public int getCount() {
        return this.apps.size();
    }

    @Override
    public SplitTunnelingApp getItem(int position) {
        return this.apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final SplitTunnelingAppRow row = (convertView instanceof SplitTunnelingAppRow)
                ? (SplitTunnelingAppRow) convertView : new SplitTunnelingAppRow(this.activity);
        row.bind(getItem(position));
        return row;
    }

}
