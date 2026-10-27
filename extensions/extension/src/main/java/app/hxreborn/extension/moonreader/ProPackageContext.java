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
