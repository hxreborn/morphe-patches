/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.moonreader;

import android.content.Context;
import android.content.ContextWrapper;

@SuppressWarnings("unused")
public final class ProPackageContext extends ContextWrapper {

    private static final String PRO_PACKAGE = "com.flyersoft.moonreaderp";

    private ProPackageContext(Context base) {
        super(base);
    }

    public static Context wrap(Context base) {
        return new ContextWrapper(new ProPackageContext(base));
    }

    @Override
    public String getPackageName() {
        return PRO_PACKAGE;
    }

}
