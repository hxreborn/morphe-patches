/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx.util;

public final class Reflect {

    private Reflect() {
    }

    public static Object call(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }

    public static Object call(Object target, String method, Class<?>[] types, Object... args)
            throws ReflectiveOperationException {
        return target.getClass().getMethod(method, types).invoke(target, args);
    }

}
