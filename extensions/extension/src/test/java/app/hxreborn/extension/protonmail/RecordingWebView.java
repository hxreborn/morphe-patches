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
package app.hxreborn.extension.protonmail;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.webkit.ValueCallback;
import android.webkit.WebView;

final class RecordingWebView extends WebView {

    final List<String> scripts = new ArrayList<>();

    final List<ValueCallback<String>> callbacks = new ArrayList<>();

    RecordingWebView(Activity activity, int visibility) {
        super(activity);
        activity.setContentView(this);
        setVisibility(visibility);
    }

    @Override
    public void evaluateJavascript(String script, ValueCallback<String> callback) {
        this.scripts.add(script);
        this.callbacks.add(callback);
    }

    String lastScript() {
        return this.scripts.get(this.scripts.size() - 1);
    }

    ValueCallback<String> lastCallback() {
        return this.callbacks.get(this.callbacks.size() - 1);
    }

}
