/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import android.content.Context;

import app.hxreborn.extension.shared.SpoofSignature;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public class PatchApplication extends SpoofSignature {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Utils.setContext(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PatchPanel.install(this);
    }
}
