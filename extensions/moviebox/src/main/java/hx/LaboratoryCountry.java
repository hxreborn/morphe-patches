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
package hx;

import android.content.SharedPreferences;
import android.util.Log;
import hx.gateway.ChosenCountry;
import hx.util.Logs;

final class LaboratoryCountry implements ChosenCountry {

    private static final String COUNTRY_STORE_ID = "vshow";

    private static final String COUNTRY_KEY = "custom_local_iso";

    private static final String NO_COUNTRY = "";

    private final ClassLoader loader;

    private SharedPreferences store;

    private boolean unreadable;

    LaboratoryCountry(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public synchronized String isoCode() {
        if (this.store == null && !this.unreadable) {
            try {
                this.store = Laboratory.openStore(this.loader, COUNTRY_STORE_ID);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                Log.e(Logs.TAG, "cannot read the Laboratory country", ex);
                this.unreadable = true;
            }
        }
        return (this.store != null) ? this.store.getString(COUNTRY_KEY, NO_COUNTRY) : NO_COUNTRY;
    }

}
