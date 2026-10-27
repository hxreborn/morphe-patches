/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.app.Activity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import app.hxreborn.extension.bereal.download.DownloadFolder;

final class FolderMenu {

    private static final int ITEM_HEIGHT_DP = 56;

    private static final int MENU_ELEVATION_DP = 8;

    private final Activity activity;

    private final SettingsStyle style;

    private final PageHost host;

    FolderMenu(Activity activity, SettingsStyle style, PageHost host) {
        this.activity = activity;
        this.style = style;
        this.host = host;
    }

    View build() {
        final boolean customAvailable = DownloadFolder.isCustomAvailable(this.activity.getContentResolver());
        final String title = customAvailable ? DownloadFolder.label() : DownloadFolder.presetFolder();
        final String volume = customAvailable ? DownloadFolder.volume() : DownloadFolder.INTERNAL_STORAGE;
        final LinearLayout body = new LinearLayout(this.activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(this.style.dp(SettingsStyle.SCREEN_INSET_DP), 0, this.style.dp(SettingsStyle.SCREEN_INSET_DP),
                this.style.dp(16));
        final LinearLayout field = new LinearLayout(this.activity);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setMinimumHeight(this.style.dp(ITEM_HEIGHT_DP));
        field.setPadding(this.style.dp(16), this.style.dp(10), this.style.dp(12), this.style.dp(10));
        field.setBackground(this.style.ripple(SettingsStyle.FIELD_SURFACE, SettingsStyle.FIELD_RADIUS_DP));
        field.addView(labels(title, volume), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final ImageView chevron = this.style.hostIconView("ic_chevron_down");
        if (chevron != null) {
            chevron.setColorFilter(SettingsStyle.SECONDARY_TEXT);
            final LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(
                    this.style.dp(SettingsStyle.ICON_DP), this.style.dp(SettingsStyle.ICON_DP));
            chevronParams.leftMargin = this.style.dp(12);
            field.addView(chevron, chevronParams);
        }
        field.setContentDescription("Download folder, " + title + ", " + volume);
        field.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(view, info);
                info.setClassName(Button.class.getName());
            }
        });
        field.setClickable(true);
        field.setOnClickListener((view) -> show(field, customAvailable));
        body.addView(field, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        if (!customAvailable && DownloadFolder.isCustom()) {
            final TextView unavailable = this.style.text(
                    DownloadFolder.label() + " is unavailable. Choose it again or pick another folder.",
                    SettingsStyle.BODY_SP, SettingsStyle.CAUTION, this.style.fonts.regular);
            unavailable.setPadding(this.style.dp(4), this.style.dp(8), this.style.dp(4), 0);
            body.addView(unavailable);
        }
        return body;
    }

    private void show(View anchor, boolean customAvailable) {
        final LinearLayout column = new LinearLayout(this.activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, this.style.dp(8), 0, this.style.dp(8));
        final PopupWindow popup = new PopupWindow(column, anchor.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        final String preset = DownloadFolder.presetFolder();
        for (String folder : new String[] { DownloadFolder.PICTURES_FOLDER, DownloadFolder.DCIM_FOLDER }) {
            column.addView(item(popup, folder, DownloadFolder.INTERNAL_STORAGE,
                    !customAvailable && folder.equals(preset), () -> this.host.useDownloadPreset(folder)));
        }
        if (customAvailable) {
            column.addView(item(popup, DownloadFolder.label(), DownloadFolder.volume(), true, null));
        }
        column.addView(item(popup, "Choose folder…", null, false, this.host::chooseDownloadFolder));
        popup.setBackgroundDrawable(this.style.rounded(SettingsStyle.FIELD_SURFACE, SettingsStyle.FIELD_RADIUS_DP));
        popup.setElevation(this.style.dp(MENU_ELEVATION_DP));
        column.measure(View.MeasureSpec.makeMeasureSpec(anchor.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        popup.setHeight(column.getMeasuredHeight());
        popup.showAsDropDown(anchor, 0, this.style.dp(4));
    }

    private View item(PopupWindow popup, String title, String volume, boolean selected, Runnable action) {
        final boolean checkable = volume != null;
        final LinearLayout row = new LinearLayout(this.activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(this.style.dp(ITEM_HEIGHT_DP));
        row.setPadding(this.style.dp(16), this.style.dp(8), this.style.dp(16), this.style.dp(8));
        row.setBackground(this.style.ripple(SettingsStyle.FIELD_SURFACE, 0));
        row.addView(labels(title, volume), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (selected) {
            final ImageView check = this.style.hostIconView("ic_checkmark");
            if (check != null) {
                check.setColorFilter(SettingsStyle.ACCENT);
                final LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                        this.style.dp(SettingsStyle.ICON_DP), this.style.dp(SettingsStyle.ICON_DP));
                checkParams.leftMargin = this.style.dp(12);
                row.addView(check, checkParams);
            }
        }
        row.setContentDescription(checkable ? title + ", " + volume : title);
        row.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(view, info);
                info.setClassName(checkable ? CheckedTextView.class.getName() : Button.class.getName());
                info.setCheckable(checkable);
                info.setChecked(selected);
            }
        });
        row.setClickable(true);
        row.setOnClickListener((view) -> {
            popup.dismiss();
            if (action != null) {
                action.run();
            }
        });
        return row;
    }

    private LinearLayout labels(String title, String volume) {
        final LinearLayout labels = new LinearLayout(this.activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        final int color = (volume != null) ? SettingsStyle.PRIMARY_TEXT : SettingsStyle.ACCENT;
        final TextView name = this.style.text(title, SettingsStyle.ROW_TITLE_SP, color, this.style.fonts.medium);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        labels.addView(name);
        if (volume != null) {
            final TextView volumeView = this.style.text(volume, SettingsStyle.BODY_SP, SettingsStyle.SECONDARY_TEXT,
                    this.style.fonts.regular);
            volumeView.setSingleLine(true);
            volumeView.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(volumeView);
        }
        return labels;
    }

}
