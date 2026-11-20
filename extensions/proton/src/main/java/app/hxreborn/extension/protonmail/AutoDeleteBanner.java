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

@SuppressWarnings("unused")
public final class AutoDeleteBanner {

    private static final String UPSELL_STATE_NAME = "AutoDeleteUpsell";

    private static final String DISABLED_STATE_NAME = "AutoDeleteDisabled";

    private static final String ENABLED_STATE_NAME = "AutoDeleteEnabled";

    private AutoDeleteBanner() {

    }

    public static boolean isScheduled() {
        return false;
    }

    public static boolean isUpsellHidden() {
        return false;
    }

    public static Object resolveState(Object currentState) {
        try {
            final String currentStateName = ((Enum<?>) currentState).name();
            if (isScheduled()) {
                return enumConstant(currentState, ENABLED_STATE_NAME);
            }
            if (isUpsellHidden() && UPSELL_STATE_NAME.equals(currentStateName)) {
                return enumConstant(currentState, DISABLED_STATE_NAME);
            }
        } catch (Throwable ignored) {
        }
        return currentState;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object enumConstant(Object currentState, String name) {
        return Enum.valueOf((Class) ((Enum<?>) currentState).getDeclaringClass(), name);
    }

}
