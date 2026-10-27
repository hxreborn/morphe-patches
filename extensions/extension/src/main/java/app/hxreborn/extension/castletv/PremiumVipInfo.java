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
package app.hxreborn.extension.castletv;

@SuppressWarnings("unused")
public final class PremiumVipInfo {

    private static final String END_DATE = "4102358400000";

    private static final String VIP_NAME = "Premium";

    private static final int PREMIUM_TYPE = 2;

    private static final int ACTIVE_STATUS = 0;

    private static Object instance;

    private PremiumVipInfo() {

    }

    public static synchronized Object get(Class<?> vipInfoClass) {
        if (instance == null) {
            try {
                instance = vipInfoClass.getConstructor(String.class, String.class, Integer.class, Integer.class)
                    .newInstance(END_DATE, VIP_NAME, PREMIUM_TYPE, ACTIVE_STATUS);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return instance;
    }

}
