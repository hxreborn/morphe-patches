/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.hxreborn.extension.BuildConfig;

final class SettingsPages {

    private final Activity activity;

    private final SettingsStyle style;

    private final SettingsRows rows;

    private final PatchCatalog catalog;

    private final PageHost host;

    private final String hostVersion;

    private TextView status;

    SettingsPages(Activity activity, SettingsStyle style, SettingsRows rows, PatchCatalog catalog, PageHost host,
            String hostVersion) {
        this.activity = activity;
        this.style = style;
        this.rows = rows;
        this.catalog = catalog;
        this.host = host;
        this.hostVersion = hostVersion;
    }

    void page(LinearLayout content) {
        this.status = statusLine();
        refreshStatus();
        content.addView(this.status, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        for (PatchGroup group : this.catalog.groups) {
            content.addView(this.rows.sectionHeader(group.title));
            final LinearLayout card = this.rows.card();
            addCard(content, card);
            for (PatchGroup.Row spec : group.rows) {
                addRow(card, spec);
            }
        }
        content.addView(this.rows.sectionHeader("Patches"));
        final LinearLayout card = this.rows.card();
        this.rows.actionRow(card, "ic_arrow_counterclockwise", "Reset to defaults",
                "Every switch goes back to its default.",
                () -> ConfirmDialog.show(this.activity, this.style, "Reset to defaults?",
                        "Every switch goes back to its default.", "Cancel", "Reset", this.host::resetToDefaults));
        this.rows.valueRow(card, "ic_info_circle", "Version",
                "Patches " + BuildConfig.BUNDLE_VERSION + " \u00b7 BeReal " + this.hostVersion);
        addCard(content, card);
    }

    private TextView statusLine() {
        final TextView view = this.style.text("", SettingsStyle.SECTION_SP, SettingsStyle.SECONDARY_TEXT,
                this.style.fonts.regular);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, this.style.dp(4), 0, this.style.dp(4));
        return view;
    }

    private void refreshStatus() {
        final String status = PatchSummary.status(this.catalog.on(), this.catalog.off(), this.catalog.applied());
        final SpannableString spanned = new SpannableString(status);
        int index = 0;
        while (index < status.length()) {
            if (Character.isDigit(status.charAt(index))) {
                int end = index;
                while (end < status.length() && Character.isDigit(status.charAt(end))) {
                    end++;
                }
                spanned.setSpan(new ForegroundColorSpan(SettingsStyle.PRIMARY_TEXT), index, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = end;
            } else {
                index++;
            }
        }
        this.status.setText(spanned);
    }

    private void addRow(LinearLayout card, PatchGroup.Row spec) {
        switch (spec.kind) {
            case LOCATION:
                locationRow(card, spec);
                break;
            case FOLDER:
                folderRow(card, spec);
                break;
            case ALWAYS_ON:
                this.rows.alwaysOnRow(card, spec.iconName, spec.title, spec.description);
                break;
            default:
                this.rows.switchRow(card, spec.iconName, spec.title, spec.description, spec.chip, spec.setting, () -> {
                    refreshStatus();
                    if (spec.restart) {
                        ConfirmDialog.showRestart(this.activity, this.style);
                    }
                });
                break;
        }
    }

    private void locationRow(LinearLayout card, PatchGroup.Row spec) {
        final LocationRows location = new LocationRows(this.activity, this.style, this.rows);
        final SettingsRows.Row row = this.rows.switchRow(card, spec.iconName, spec.title, spec.description, spec.chip,
                spec.setting, () -> {
                    refreshStatus();
                    location.refresh();
                });
        card.addView(location.build(row));
        card.post(() -> card.setLayoutTransition(new LayoutTransition()));
    }

    private void folderRow(LinearLayout card, PatchGroup.Row spec) {
        this.rows.textRow(card, spec.iconName, spec.title, spec.description);
        card.addView(new FolderMenu(this.activity, this.style, this.host).build());
    }

    private void addCard(LinearLayout content, LinearLayout card) {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = this.style.dp(12);
        content.addView(card, params);
    }

}
