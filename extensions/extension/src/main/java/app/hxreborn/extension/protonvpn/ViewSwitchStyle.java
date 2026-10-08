/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonvpn;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import app.hxreborn.extension.proton.AccentColor;
import app.hxreborn.extension.proton.PatchesTheme;
import app.hxreborn.extension.proton.SwitchStyle;

@SuppressWarnings("unused")
public final class ViewSwitchStyle {

    private ViewSwitchStyle() {

    }

    public static Drawable thumb(View view) {
        final Context context = view.getContext();
        return SwitchStyle.thumb(context, AccentColor.getAccentColor(PatchesTheme.isNightMode(context)));
    }

    public static Drawable track(View view) {
        final Context context = view.getContext();
        return SwitchStyle.track(context, AccentColor.getAccentColor(PatchesTheme.isNightMode(context)));
    }

}
