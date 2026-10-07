/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
