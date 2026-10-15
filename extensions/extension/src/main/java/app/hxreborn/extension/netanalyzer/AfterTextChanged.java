/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import android.text.Editable;
import android.text.TextWatcher;

final class AfterTextChanged implements TextWatcher {

    private final Listener listener;

    AfterTextChanged(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void beforeTextChanged(CharSequence text, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable text) {
        this.listener.onText(text.toString());
    }

    interface Listener {

        void onText(String text);

    }

}
