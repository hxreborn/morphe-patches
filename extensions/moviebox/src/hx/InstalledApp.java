/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import android.content.Context;

final class InstalledApp {
    private static final String ACTIVITY_THREAD = "android.app.ActivityThread";

    private InstalledApp() {
    }

    static Context context() throws ReflectiveOperationException {
        return (Context) Class.forName(ACTIVITY_THREAD).getMethod("currentApplication").invoke(null);
    }
}
