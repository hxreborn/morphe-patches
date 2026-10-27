/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.photoeditorpro;

import android.content.Context;
import app.morphe.extension.shared.Utils;

import app.hxreborn.extension.shared.SpoofSignature;

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
