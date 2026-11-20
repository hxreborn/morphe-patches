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
package app.hxreborn.extension.proton;

import java.util.ArrayList;
import java.util.List;

import android.content.res.ColorStateList;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.RequiresApi;

@SuppressWarnings("unused")
public final class NavigationBarRows {

    private final PatchesSettingsActivity activity;

    private NavigationBarRows(PatchesSettingsActivity activity) {
        this.activity = activity;
    }

    public static void add(PatchesSettingsActivity activity, List<View> rows) {
        rows.addAll(new NavigationBarRows(activity).navigationRows());
    }

    private List<View> navigationRows() {
        final List<View> rows = new ArrayList<>();
        final boolean floating = FloatingNavigationBar.isEnabled();
        rows.add(navigationSwitchRow(AppliedPatches.floatingNavigationBarName(),
                "Uses Proton Mail's floating toolbar style.", FloatingNavigationBar.KEY, true));
        if (!NavigationBarBackdrop.isSupported()) {
            rows.add(navigationSwitchRow("Glass navigation bar", "Requires Android 12 or later.",
                    NavigationBarBackdrop.GLASS_KEY, false));
            final LinearLayout explanation = navigationColumn();
            explanation.addView(navigationLabel("Blur strength", PatchesSettingsActivity.ROW_TITLE_SP,
                    this.activity.textNormColor()));
            explanation.addView(navigationLabel("Requires Android 12 or later.", PatchesSettingsActivity.ROW_SUMMARY_SP,
                    this.activity.textWeakColor()));
            explanation.setEnabled(false);
            explanation.setAlpha(PatchesSettingsActivity.DISABLED_ROW_ALPHA);
            rows.add(explanation);
        } else {
            addNavigationAppearance(rows, floating);
        }
        addNavigationRestart(rows);
        return rows;
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private void addNavigationAppearance(List<View> rows, boolean floating) {
        rows.add(navigationSwitchRow("Glass navigation bar",
                floating ? "Adds blur, translucency and highlights." : "Requires floating navigation bar.",
                NavigationBarBackdrop.GLASS_KEY, floating));
        final NavigationBarPreview preview = new NavigationBarPreview(this.activity);
        final LinearLayout strengthRow = navigationColumn();
        final LinearLayout labelRow = new LinearLayout(this.activity);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        labelRow.addView(
                navigationLabel("Blur strength", PatchesSettingsActivity.ROW_TITLE_SP, this.activity.textNormColor()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView amount = navigationLabel(blurStrengthLabel(NavigationBarBackdrop.blurStrengthDp()),
                PatchesSettingsActivity.ROW_SUMMARY_SP, this.activity.textWeakColor());
        labelRow.addView(amount);
        strengthRow.addView(labelRow);
        final SeekBar strength = new SeekBar(this.activity);
        strength.setMin(PatchSettings.isEnabled(NavigationBarBackdrop.GLASS_KEY, false)
                ? NavigationBarBackdrop.MIN_GLASS_BLUR_STRENGTH_DP : 0);
        strength.setMax(NavigationBarBackdrop.MAX_BLUR_STRENGTH_DP);
        strength.setProgress(NavigationBarBackdrop.blurStrengthDp());
        strength.setMinimumHeight(this.activity.dp(PatchesSettingsActivity.BACK_TOUCH_TARGET_DP));
        strength.setContentDescription("Blur strength. " + amount.getText());
        final ColorStateList accent = ColorStateList.valueOf(AccentColor.getAccentColor(this.activity.isDarkTheme()));
        strength.setProgressTintList(accent);
        strength.setThumbTintList(accent);
        strength.setEnabled(floating);
        strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                amount.setText(blurStrengthLabel(progress));
                seekBar.setContentDescription("Blur strength. " + amount.getText());
                if (fromUser) {
                    NavigationBarBackdrop.setBlurStrengthDp(progress);
                    preview.updateAppearance();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        strengthRow.addView(strength, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                this.activity.dp(PatchesSettingsActivity.BACK_TOUCH_TARGET_DP)));
        strengthRow.setEnabled(floating);
        if (!floating) {
            strengthRow.setAlpha(PatchesSettingsActivity.DISABLED_ROW_ALPHA);
        }
        rows.add(strengthRow);
        final LinearLayout previewRow = navigationColumn();
        final LinearLayout previewHeading = new LinearLayout(this.activity);
        previewHeading.setGravity(Gravity.CENTER_VERTICAL);
        previewHeading.addView(
                navigationLabel("Preview", PatchesSettingsActivity.ROW_SUMMARY_SP, this.activity.textWeakColor()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final View scroll = PatchesDialog.configureActionButton(this.activity, navigationLabel("Scroll content",
                PatchesSettingsActivity.ROW_SUMMARY_SP, AccentColor.getAccentColor(this.activity.isDarkTheme())),
                preview::scrollContent);
        scroll.setEnabled(floating);
        preview.setEnabled(floating);
        if (!floating) {
            previewRow.setAlpha(PatchesSettingsActivity.DISABLED_ROW_ALPHA);
        }
        previewHeading.addView(scroll);
        previewRow.addView(previewHeading);
        previewRow.addView(preview,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, preview.getMinimumHeight()));
        rows.add(previewRow);
    }

    private void addNavigationRestart(List<View> rows) {
        final LinearLayout restart = new LinearLayout(this.activity);
        restart.setGravity(Gravity.CENTER_VERTICAL);
        restart.setPadding(this.activity.dp(PatchesSettingsActivity.CARD_PADDING_DP), 0,
                this.activity.dp(PatchesSettingsActivity.CARD_PADDING_DP), 0);
        restart.addView(
                navigationLabel("Restart to apply changes.", PatchesSettingsActivity.ROW_SUMMARY_SP,
                        this.activity.textWeakColor()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        restart
            .addView(
                    PatchesDialog.configureActionButton(this.activity,
                            navigationLabel("Restart", PatchesSettingsActivity.ROW_SUMMARY_SP,
                                    AccentColor.getAccentColor(this.activity.isDarkTheme())),
                            this.activity::restartApp));
        rows.add(restart);
    }

    private LinearLayout navigationColumn() {
        final LinearLayout column = new LinearLayout(this.activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(this.activity.dp(PatchesSettingsActivity.CARD_PADDING_DP),
                this.activity.dp(PatchesSettingsActivity.CARD_PADDING_DP),
                this.activity.dp(PatchesSettingsActivity.CARD_PADDING_DP),
                this.activity.dp(PatchesSettingsActivity.CARD_PADDING_DP));
        return column;
    }

    private TextView navigationLabel(String text, int sizeSp, int color) {
        final TextView label = new TextView(this.activity);
        label.setText(text);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        label.setTextColor(color);
        return label;
    }

    private View navigationSwitchRow(String title, String summary, String key, boolean enabled) {
        return this.activity.switchRow(title, summary, PatchSettings.isEnabled(key, false), (checked) -> {
            PatchSettings.setEnabled(key, checked);
            this.activity.recreateContent();
        }, enabled);
    }

    private static String blurStrengthLabel(int strength) {
        return (strength == 0) ? "Off" : strength + " / " + NavigationBarBackdrop.MAX_BLUR_STRENGTH_DP;
    }

}
