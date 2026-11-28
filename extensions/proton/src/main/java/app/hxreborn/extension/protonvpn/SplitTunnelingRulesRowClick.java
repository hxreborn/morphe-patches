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

import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import app.hxreborn.extension.proton.PatchContext;
import app.hxreborn.extension.shared.ProxyHandler;

final class SplitTunnelingRulesRowClick extends ProxyHandler {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    @Override
    protected String describe() {
        return SplitTunnelingRuleEditor.SETTINGS_ROW_TITLE;
    }

    @Override
    protected Object handle(Method method, Object[] args) {
        MAIN_HANDLER.post(SplitTunnelingRulesRowClick::showEditor);
        return null;
    }

    private static void showEditor() {
        final Activity activity = PatchContext.resumedActivity();
        if (activity != null) {
            SplitTunnelingRuleEditor.show(activity, activity::recreate);
        }
    }

}
