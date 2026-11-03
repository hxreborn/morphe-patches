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
package app.hxreborn.extension.patreon;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

final class SettingsStyle {

    static final float BAR_TITLE_SP = 24f;

    static final float SECTION_SP = 14f;

    static final float ROW_TITLE_SP = 17f;

    static final float BODY_SP = 14f;

    static final int SCREEN_INSET_DP = 16;

    static final int CARD_RADIUS_DP = 16;

    static final int ICON_DP = 24;

    private static final int DARK_SCREEN_BACKGROUND = 0xFF121212;

    private static final int DARK_CARD_SURFACE = 0xFF1E1E1E;

    private static final int LIGHT_CARD_SURFACE = 0xFFF3F3F3;

    private static final int DARK_DIVIDER = 0xFF2C2C2C;

    private static final int LIGHT_DIVIDER = 0xFFE2E2E2;

    private static final int LIGHT_PRIMARY_TEXT = 0xFF121212;

    private static final int DARK_SECONDARY_TEXT = 0xFFBFBFBF;

    private static final int LIGHT_SECONDARY_TEXT = 0xFF5C5C5C;

    private static final int DARK_SWITCH_ON_THUMB = 0xFF0D0D0D;

    private static final int DARK_SWITCH_OFF_TRACK = 0xFF2C2C2C;

    private static final int LIGHT_SWITCH_OFF_TRACK = 0xFFE2E2E2;

    private static final int DARK_SWITCH_OFF_THUMB = 0xFF9A9A9A;

    private static final int LIGHT_SWITCH_OFF_THUMB = 0xFF767676;

    private static final int DARK_RIPPLE = 0x33FFFFFF;

    private static final int LIGHT_RIPPLE = 0x1F000000;

    private static final String PATREON_PREFERENCES = "com.patreon.android.model.shared.PREFS";

    private static final String PATREON_THEME_KEY = PATREON_PREFERENCES + "+APP_THEME_SELECTION";

    private static final int PATREON_THEME_LIGHT = 1;

    private static final int PATREON_THEME_DARK = 2;

    private static final int SWITCH_ICON_DP = 16;

    final boolean dark;

    final int screenBackground;

    final int cardSurface;

    final int divider;

    final int primaryText;

    final int secondaryText;

    final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private final int switchOnThumb;

    private final int switchOffTrack;

    private final int switchOffThumb;

    private final int ripple;

    private final Context context;

    SettingsStyle(Context context) {
        this.context = context;
        this.dark = isPatreonDark(context);
        this.screenBackground = this.dark ? DARK_SCREEN_BACKGROUND : Color.WHITE;
        this.cardSurface = this.dark ? DARK_CARD_SURFACE : LIGHT_CARD_SURFACE;
        this.divider = this.dark ? DARK_DIVIDER : LIGHT_DIVIDER;
        this.primaryText = this.dark ? Color.WHITE : LIGHT_PRIMARY_TEXT;
        this.secondaryText = this.dark ? DARK_SECONDARY_TEXT : LIGHT_SECONDARY_TEXT;
        this.switchOnThumb = this.dark ? DARK_SWITCH_ON_THUMB : Color.WHITE;
        this.switchOffTrack = this.dark ? DARK_SWITCH_OFF_TRACK : LIGHT_SWITCH_OFF_TRACK;
        this.switchOffThumb = this.dark ? DARK_SWITCH_OFF_THUMB : LIGHT_SWITCH_OFF_THUMB;
        this.ripple = this.dark ? DARK_RIPPLE : LIGHT_RIPPLE;
    }

    int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                this.context.getResources().getDisplayMetrics()));
    }

    TextView text(String value, float sizeSp, int color, Typeface typeface) {
        TextView view = new TextView(this.context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface(typeface);
        return view;
    }

    GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    Drawable ripple(int radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(this.ripple), null, rounded(Color.WHITE, radiusDp));
    }

    Drawable hostIcon(String name, int tint) {
        Drawable icon = hostDrawable(name);
        if (icon != null) {
            icon.setTint(tint);
        }
        return icon;
    }

    void styleSwitch(Switch control) {
        Drawable thumbShape = hostDrawable("mtrl_switch_thumb");
        Drawable trackShape = hostDrawable("mtrl_switch_track");
        Drawable trackOutline = hostDrawable("mtrl_switch_track_decoration");
        Drawable checkIcon = hostDrawable("ic_m3_chip_check");
        if (thumbShape == null || trackShape == null || trackOutline == null || checkIcon == null) {
            return;
        }
        thumbShape.setTintList(checkedTint(this.switchOnThumb, this.switchOffThumb));
        trackShape.setTintList(checkedTint(this.primaryText, this.switchOffTrack));
        trackOutline.setTintList(checkedTint(Color.TRANSPARENT, this.switchOffThumb));
        StateListDrawable icon = new StateListDrawable();
        icon.addState(new int[] { android.R.attr.state_checked }, checkIcon);
        icon.setTint(this.primaryText);

        int inset = thumbShape.getIntrinsicWidth() - trackShape.getIntrinsicWidth() / 2;
        LayerDrawable thumb = new LayerDrawable(new Drawable[] { thumbShape, icon });
        thumb.setLayerInset(0, -inset, 0, -inset, 0);
        thumb.setLayerInset(1, -inset, 0, -inset, 0);
        thumb.setLayerGravity(1, Gravity.CENTER);
        thumb.setLayerSize(1, dp(SWITCH_ICON_DP), dp(SWITCH_ICON_DP));
        LayerDrawable track = new LayerDrawable(new Drawable[] { trackShape, trackOutline });
        track.setPadding(inset, 0, inset, 0);

        control.setSplitTrack(false);
        control.setThumbDrawable(thumb);
        control.setTrackDrawable(track);
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    private Drawable hostDrawable(String name) {
        int identifier = this.context.getResources().getIdentifier(name, "drawable", this.context.getPackageName());
        Drawable drawable = (identifier != 0) ? this.context.getDrawable(identifier) : null;
        return (drawable != null) ? drawable.mutate() : null;
    }

    private static ColorStateList checkedTint(int checked, int unchecked) {
        return new ColorStateList(new int[][] { { android.R.attr.state_checked }, {} },
                new int[] { checked, unchecked });
    }

    private static boolean isPatreonDark(Context context) {
        int theme;
        try {
            SharedPreferences preferences = context.getSharedPreferences(PATREON_PREFERENCES, Context.MODE_PRIVATE);
            theme = preferences.getInt(PATREON_THEME_KEY, 0);
        } catch (RuntimeException ex) {
            Log.w(PatchSettings.LOG_TAG, "Reading preference " + PATREON_THEME_KEY + " failed", ex);
            theme = 0;
        }
        if (theme == PATREON_THEME_DARK) {
            return true;
        }
        if (theme == PATREON_THEME_LIGHT) {
            return false;
        }
        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

}
