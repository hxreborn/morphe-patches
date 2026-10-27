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
package app.hxreborn.extension.protonvpn;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.Context;
import android.util.TypedValue;
import app.morphe.extension.shared.Utils;

import app.hxreborn.extension.proton.MaterialSwitches;
import app.hxreborn.extension.proton.PatchesTheme;
import app.hxreborn.extension.shared.ProxyHandler;

@SuppressWarnings("unused")
public final class SwitchThumbIcon {

    private static final int TINT_FROM_CONTENT_COLOR = 0x8;

    private static final String THUMB_COLOR_ATTRIBUTE = "proton_text_inverted";

    private static boolean checked;

    private static long tint;

    private static volatile Object checkedContent;

    private static volatile Object uncheckedContent;

    private SwitchThumbIcon() {

    }

    public static synchronized Object thumbContent(Class<?> composableLambdaType, boolean checked) {
        if (!MaterialSwitches.isEnabled()) {
            return null;
        }
        if (checked) {
            if (checkedContent == null) {
                checkedContent = newThumbContent(composableLambdaType, true);
            }
            return checkedContent;
        }
        if (uncheckedContent == null) {
            uncheckedContent = newThumbContent(composableLambdaType, false);
        }
        return uncheckedContent;
    }

    public static long checkedThumbColor(long original) {
        final Context context = Utils.getContext();
        if (context == null || !MaterialSwitches.isEnabled()) {
            return original;
        }

        final Context themed = PatchesTheme.withApplicationTheme(context);
        final int attribute = themed.getResources()
            .getIdentifier(THUMB_COLOR_ATTRIBUTE, "attr", themed.getPackageName());
        final TypedValue value = new TypedValue();
        if (attribute == 0 || !themed.getTheme().resolveAttribute(attribute, value, true)) {
            return original;
        }
        final int argb = (value.resourceId != 0) ? themed.getColor(value.resourceId) : value.data;
        return ((long) argb) << 32;
    }

    public static void remember(boolean isChecked, long checkedTrackColor, long uncheckedTrackColor) {
        checked = isChecked;
        tint = isChecked ? checkedTrackColor : uncheckedTrackColor;
    }

    public static void drawThumbIcon(Object composer) {
        if (MaterialSwitches.isEnabled()) {
            drawIcon(composer, checked, tint, 0);
        }
    }

    static void drawIcon(Object composer, boolean isChecked, long iconTint, int defaults) {
    }

    private static Object newThumbContent(Class<?> composableLambdaType, boolean checked) {
        return Proxy.newProxyInstance(SwitchThumbIcon.class.getClassLoader(), new Class<?>[] { composableLambdaType },
                new ThumbContentHandler(checked));
    }

    private static final class ThumbContentHandler extends ProxyHandler {

        private final boolean checked;

        ThumbContentHandler(boolean checked) {
            this.checked = checked;
        }

        @Override
        protected String describe() {
            return (this.checked) ? "SwitchThumbIcon(checked)" : "SwitchThumbIcon(unchecked)";
        }

        @Override
        protected Object handle(Method method, Object[] args) {
            drawIcon(args[0], this.checked, 0L, TINT_FROM_CONTENT_COLOR);
            return null;
        }

    }

}
