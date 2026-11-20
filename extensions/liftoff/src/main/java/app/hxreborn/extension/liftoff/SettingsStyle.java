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
package app.hxreborn.extension.liftoff;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import app.hxreborn.extension.shared.Dimensions;

final class SettingsStyle {

    static final float TITLE_SP = 24;
    static final float MESSAGE_SP = 14;

    static final float BUTTON_SP = 14;

    static final float BAR_TITLE_SP = 24;

    static final float SECTION_SP = 14;

    static final float ROW_TITLE_SP = 17;

    static final float BODY_SP = 14;

    static final int SCREEN_INSET_DP = 16;

    static final int CARD_RADIUS_DP = 16;

    static final int ICON_DP = 24;

    private static final int BUTTON_PADDING_DP = 16;

    private static final int BUTTON_HEIGHT_DP = 48;

    private static final int BUTTON_RADIUS_DP = 8;

    private static final int RIPPLE_ALPHA = 48;

    private static final int OPAQUE_ALPHA = 255;

    private static final int CARD_TINT_PERCENT = 7;

    private static final int PERCENT = 100;

    private static final int SECONDARY_TEXT_ALPHA = 179;

    private static final int SWITCH_TRACK_ALPHA = 128;

    private static final int SWITCH_OFF_THUMB_ALPHA = 153;

    private static final int SWITCH_ICON_DP = 16;

    final int screenBackground;

    final int cardSurface;

    final int divider;

    final int primaryText;

    final int secondaryText;

    final int accent;

    final boolean dark;

    final Typeface medium;

    private final Context context;

    private final Typeface regular;

    SettingsStyle(Context context, TextView template) {
        this.context = context;
        TextPaint hostText = hostTextPaint(template);
        this.primaryText = hostText.getColor();
        this.secondaryText = withAlpha(this.primaryText, SECONDARY_TEXT_ALPHA);
        this.accent = this.primaryText;
        View page = settingsPage(template);
        this.screenBackground = pageBackground(page);
        this.cardSurface = blend(this.screenBackground, this.primaryText, CARD_TINT_PERCENT);
        this.divider = this.screenBackground;
        this.dark = brightness(this.screenBackground) < brightness(this.primaryText);
        this.regular = hostText.getTypeface();
        this.medium = Typeface.create(this.regular, Typeface.BOLD);
    }

    int dp(float value) {
        return Dimensions.dp(this.context, value);
    }

    TextView text(String value, float sizeSp, int color, Typeface typeface) {
        TextView view = new TextView(this.context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface((typeface != null) ? typeface : this.regular);
        return view;
    }

    TextView textButton(String value, View.OnClickListener listener) {
        TextView button = text(value, BUTTON_SP, this.accent, this.medium);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(BUTTON_HEIGHT_DP));
        button.setPadding(dp(BUTTON_PADDING_DP), 0, dp(BUTTON_PADDING_DP), 0);
        button.setBackground(ripple(BUTTON_RADIUS_DP));
        button.setOnClickListener(listener);
        return button;
    }

    GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    Drawable ripple(int radiusDp) {
        int color = Color.argb(RIPPLE_ALPHA, Color.red(this.primaryText), Color.green(this.primaryText),
                Color.blue(this.primaryText));
        return new RippleDrawable(ColorStateList.valueOf(color), null, rounded(this.primaryText, radiusDp));
    }

    Drawable hostIcon(String name, int tint) {
        int identifier = this.context.getResources().getIdentifier(name, "drawable", this.context.getPackageName());
        Drawable drawable = (identifier != 0) ? this.context.getDrawable(identifier) : null;
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(tint);
        }
        return drawable;
    }

    void styleSwitch(Switch control) {
        Drawable thumbShape = hostIcon("mtrl_switch_thumb", this.primaryText);
        Drawable trackShape = hostIcon("mtrl_switch_track", this.primaryText);
        Drawable trackOutline = hostIcon("mtrl_switch_track_decoration", this.primaryText);
        Drawable checkIcon = hostIcon("mtrl_ic_check_mark", this.primaryText);
        if (thumbShape == null || trackShape == null || trackOutline == null || checkIcon == null) {
            styleLegacySwitch(control);
            return;
        }
        int offThumb = withAlpha(this.primaryText, SWITCH_OFF_THUMB_ALPHA);
        thumbShape.setTintList(checkedTint(this.screenBackground, offThumb));
        trackShape.setTintList(checkedTint(this.primaryText, this.divider));
        trackOutline.setTintList(checkedTint(Color.TRANSPARENT, offThumb));
        StateListDrawable icon = new StateListDrawable();
        icon.addState(new int[] { android.R.attr.state_checked }, checkIcon);

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

    private void styleLegacySwitch(Switch control) {
        Drawable thumb = hostIcon("abc_switch_thumb_material", this.primaryText);
        Drawable track = hostIcon("abc_switch_track_mtrl_alpha", this.primaryText);
        if (thumb != null && track != null) {
            control.setThumbDrawable(thumb);
            control.setTrackDrawable(track);
        }
        control.setThumbTintList(checkedTint(this.primaryText, this.secondaryText));
        control.setTrackTintList(checkedTint(withAlpha(this.primaryText, SWITCH_TRACK_ALPHA),
                withAlpha(this.primaryText, RIPPLE_ALPHA)));
    }

    static TextPaint hostTextPaint(TextView template) {
        TextPaint paint = new TextPaint(template.getPaint());
        paint.setColor(template.getCurrentTextColor());
        paint.setTypeface(template.getTypeface());
        paint.setTextSize(template.getTextSize());
        CharSequence text = template.getText();
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            for (CharacterStyle span : spanned.getSpans(0, text.length(), CharacterStyle.class)) {
                if (spanned.getSpanStart(span) == 0 && spanned.getSpanEnd(span) == text.length()) {
                    span.updateDrawState(paint);
                }
            }
        }
        return paint;
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    private static ColorStateList checkedTint(int checked, int unchecked) {
        return new ColorStateList(new int[][] { { android.R.attr.state_checked }, {} },
                new int[] { checked, unchecked });
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Math.min(Color.alpha(color), alpha), Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int blend(int base, int tint, int tintPercent) {
        int basePercent = PERCENT - tintPercent;
        return Color.rgb((Color.red(base) * basePercent + Color.red(tint) * tintPercent) / PERCENT,
                (Color.green(base) * basePercent + Color.green(tint) * tintPercent) / PERCENT,
                (Color.blue(base) * basePercent + Color.blue(tint) * tintPercent) / PERCENT);
    }

    private static int brightness(int color) {
        return Color.red(color) + Color.green(color) + Color.blue(color);
    }

    private static View settingsPage(TextView template) {
        View page = template;
        while (page.getParent() instanceof View) {
            if (page instanceof ScrollView) {
                return (View) page.getParent();
            }
            page = (View) page.getParent();
        }
        return page;
    }

    private static int pageBackground(View page) {
        View ancestor = page;
        while (true) {
            Drawable background = ancestor.getBackground();
            if (background != null) {
                int color = backgroundColor(background);
                if (Color.alpha(color) == OPAQUE_ALPHA) {
                    return color;
                }
            }
            if (!(ancestor.getParent() instanceof View)) {
                return ancestor.getSolidColor();
            }
            ancestor = (View) ancestor.getParent();
        }
    }

    private static int backgroundColor(Drawable background) {
        if (background instanceof ColorDrawable) {
            return ((ColorDrawable) background).getColor();
        }
        Bitmap pixel = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(pixel);
        canvas.translate(-background.getBounds().centerX(), -background.getBounds().centerY());
        background.draw(canvas);
        int color = pixel.getPixel(0, 0);
        pixel.recycle();
        return color;
    }

}
