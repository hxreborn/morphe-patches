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
package app.hxreborn.extension.keepa;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import app.hxreborn.extension.shared.patchesmenu.PatchesSettingsPanelBase;

final class AccountsScreen extends PatchesSettingsPanelBase {

    private final View header;

    private final ScrollView accounts;

    private final View addAccount;

    AccountsScreen(AccountsActivity activity, View header, ScrollView accounts, View addAccount) {
        super(activity);
        this.header = header;
        this.accounts = accounts;
        this.addAccount = addAccount;
    }

    @Override
    public View createContent() {
        LinearLayout screen = createColumn();
        screen.addView(this.header);
        addWeightedContent(screen, this.accounts);
        screen.addView(this.addAccount);
        return screen;
    }

}
