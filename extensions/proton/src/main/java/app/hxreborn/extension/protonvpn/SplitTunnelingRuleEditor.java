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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import app.hxreborn.extension.proton.AccentColor;
import app.hxreborn.extension.proton.PatchesDialog;
import app.hxreborn.extension.proton.PatchesTheme;
import app.hxreborn.extension.proton.SwitchStyle;

@SuppressLint("SetTextI18n")
@SuppressWarnings("deprecation")
final class SplitTunnelingRuleEditor {

    static final String SETTINGS_ROW_TITLE = "Split tunneling rules";

    static final String SUMMARY_SEPARATOR = " \u00b7 ";

    private static final int TOUCH_TARGET_DP = 48;

    private static final int SECTION_SPACING_DP = 16;

    private static final int BODY_TEXT_SP = 14;

    private static final int KEYWORD_MAX_LINES = 3;

    private static final int MAX_NAMED_RULES = 2;

    private final Activity activity;

    private final Runnable onSettingChanged;

    private final Map<SplitTunnelingCategory, Switch> categorySwitches = new EnumMap<>(SplitTunnelingCategory.class);

    private final EditText keywordInput;

    private final TextView matchSummary;

    private final View viewAppsButton;

    private final int accentColor;

    private final int textWeakColor;

    private Dialog dialog;

    private List<SplitTunnelingApp> installedApps;

    private boolean installedAppsUnreadable;

    private List<SplitTunnelingApp> matchedApps = Collections.emptyList();

    private SplitTunnelingRuleEditor(Activity activity, Runnable onSettingChanged) {
        this.activity = activity;
        this.onSettingChanged = onSettingChanged;
        this.accentColor = AccentColor.getAccentColor(PatchesTheme.isDark(PatchesTheme.backgroundSecondary(activity)));
        this.textWeakColor = PatchesTheme.textWeak(activity);
        this.keywordInput = new EditText(activity);
        this.matchSummary = bodyText("", this.textWeakColor);
        this.viewAppsButton = PatchesDialog.configureActionButton(activity,
                PatchesDialog.createTextView(activity, "View apps", BODY_TEXT_SP, this.accentColor),
                this::showMatchedApps);
    }

    static String summary() {
        final List<String> names = new ArrayList<>();
        for (SplitTunnelingCategory category : SplitTunnelingRuleSettings.categories()) {
            names.add(category.label);
        }
        names.addAll(SplitTunnelingRuleSettings.keywords());
        if (names.isEmpty()) {
            return "Off";
        }

        final String rules = (names.size() <= MAX_NAMED_RULES) ? TextUtils.join(", ", names) : names.size() + " rules";
        final int count = SplitTunnelingRuleSettings.matchCount();
        if (count == SplitTunnelingRuleSettings.UNKNOWN_MATCH_COUNT) {
            return rules;
        }
        return rules + SUMMARY_SEPARATOR + count + ((count == 1) ? " app" : " apps");
    }

    static void show(Activity activity, Runnable onSettingChanged) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        new SplitTunnelingRuleEditor(activity, onSettingChanged).show();
    }

    private void show() {
        this.dialog = PatchesDialog.createDialog(this.activity, buildSurface());
        final Window window = this.dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        refreshPreview();
        this.dialog.show();
        readInstalledApps();
    }

    private LinearLayout buildSurface() {
        final LinearLayout surface = PatchesDialog.createContentLayout(this.activity);
        surface.addView(PatchesDialog.createTitle(this.activity, SETTINGS_ROW_TITLE));
        surface.addView(categoryScroll(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        surface.addView(keywordSection(), matchWidth());
        surface.addView(previewRow(), matchWidth());
        surface.addView(actionRow(), matchWidth());
        return surface;
    }

    private ScrollView categoryScroll() {
        final LinearLayout content = new LinearLayout(this.activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setFocusableInTouchMode(true);
        content.requestFocus();
        content.addView(bodyText("Apps that match follow the mode set in Split tunneling, next to the apps you pick. "
                + "Reconnect to apply a change.", this.textWeakColor), matchWidth());

        final Set<SplitTunnelingCategory> savedCategories = SplitTunnelingRuleSettings.categories();
        for (SplitTunnelingCategory category : SplitTunnelingCategory.values()) {
            final Switch categorySwitch = new Switch(this.activity);
            categorySwitch.setText(category.label);
            categorySwitch.setGravity(Gravity.CENTER_VERTICAL);
            categorySwitch.setMinimumHeight(dp(TOUCH_TARGET_DP));
            categorySwitch.setChecked(savedCategories.contains(category));
            SwitchStyle.apply(categorySwitch, this.accentColor);
            categorySwitch.setTextColor(PatchesTheme.textNorm(this.activity));
            categorySwitch.setOnCheckedChangeListener((button, checked) -> refreshPreview());
            this.categorySwitches.put(category, categorySwitch);
            content.addView(categorySwitch, matchWidth());
        }

        final ScrollView scroll = new ScrollView(this.activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(content);
        return scroll;
    }

    private LinearLayout keywordSection() {
        final LinearLayout section = new LinearLayout(this.activity);
        section.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout.LayoutParams labelParams = matchWidth();
        labelParams.topMargin = dp(SECTION_SPACING_DP);
        section.addView(bodyText("A word in the name starts with", this.textWeakColor), labelParams);

        this.keywordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        this.keywordInput.setMaxLines(KEYWORD_MAX_LINES);
        this.keywordInput.setHint("bank, wallet, pay");
        this.keywordInput.setText(SplitTunnelingRuleSettings.keywordText());
        this.keywordInput.setContentDescription("Keywords matched against the start of each word in app names");
        this.keywordInput.setMinimumHeight(dp(TOUCH_TARGET_DP));
        this.keywordInput.setTextColor(PatchesTheme.textNorm(this.activity));
        this.keywordInput.setHintTextColor(this.textWeakColor);
        PatchesTheme.tintTextInput(this.keywordInput, this.accentColor);
        this.keywordInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                refreshPreview();
            }

        });
        section.addView(this.keywordInput, matchWidth());
        return section;
    }

    private LinearLayout previewRow() {
        final LinearLayout row = new LinearLayout(this.activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(this.matchSummary, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(this.viewAppsButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private LinearLayout actionRow() {
        final LinearLayout actions = PatchesDialog.createButtonRow(this.activity);
        actions.addView(PatchesDialog.configureActionButton(this.activity,
                PatchesDialog.createTextView(this.activity, "Cancel", BODY_TEXT_SP, this.textWeakColor),
                this::dismissDialog));
        actions.addView(PatchesDialog.configureActionButton(this.activity,
                PatchesDialog.createTextView(this.activity, "Save", BODY_TEXT_SP, this.accentColor), this::save));
        return actions;
    }

    private void readInstalledApps() {
        final Context context = this.activity.getApplicationContext();
        new Thread(() -> {
            List<SplitTunnelingApp> apps = null;
            try {
                apps = SplitTunnelingRules.installedApps(context);
            } catch (RuntimeException ex) {
                Log.w(VpnSettings.LOG_TAG, "Installed app lookup failed, the preview has no result", ex);
            }
            final List<SplitTunnelingApp> readApps = apps;
            this.activity.runOnUiThread(() -> {
                if (this.dialog.isShowing()) {
                    this.installedApps = readApps;
                    this.installedAppsUnreadable = readApps == null;
                    refreshPreview();
                }
            });
        }).start();
    }

    private void refreshPreview() {
        final Set<SplitTunnelingCategory> selectedCategories = selectedCategories();
        final List<String> keywords = SplitTunnelingRuleSettings.split(this.keywordInput.getText().toString());
        this.matchedApps = Collections.emptyList();
        if (selectedCategories.isEmpty() && keywords.isEmpty()) {
            showPreview("No rule set.");
            return;
        }
        if (this.installedApps == null) {
            showPreview(this.installedAppsUnreadable ? "Could not read installed apps." : "Reading installed apps...");
            return;
        }

        this.matchedApps = new SplitTunnelingRuleMatcher(selectedCategories, keywords).select(this.installedApps);
        if (this.matchedApps.isEmpty()) {
            showPreview("No installed app matches.");
        } else if (this.matchedApps.size() == 1) {
            showPreview("1 app matches");
        } else {
            showPreview(this.matchedApps.size() + " apps match");
        }
    }

    private void showPreview(String text) {
        this.matchSummary.setText(text);
        this.viewAppsButton.setVisibility(this.matchedApps.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showMatchedApps() {
        SplitTunnelingAppList.show(this.activity, this.matchedApps);
    }

    private void save() {
        final Set<SplitTunnelingCategory> categories = selectedCategories();
        final String keywordText = this.keywordInput.getText().toString();
        SplitTunnelingRuleSettings.saveCategories(categories);
        SplitTunnelingRuleSettings.saveKeywordText(keywordText);
        if (this.installedApps == null) {
            SplitTunnelingRuleSettings.saveMatchCount(SplitTunnelingRuleSettings.UNKNOWN_MATCH_COUNT);
        } else {
            SplitTunnelingRuleSettings.saveMatches(categories, SplitTunnelingRuleSettings.split(keywordText),
                    this.matchedApps);
        }
        Toast.makeText(this.activity, "Saved. Reconnect to apply.", Toast.LENGTH_SHORT).show();
        dismissDialog();
        this.onSettingChanged.run();
    }

    private void dismissDialog() {
        this.dialog.dismiss();
    }

    private Set<SplitTunnelingCategory> selectedCategories() {
        final Set<SplitTunnelingCategory> selected = EnumSet.noneOf(SplitTunnelingCategory.class);
        for (Map.Entry<SplitTunnelingCategory, Switch> entry : this.categorySwitches.entrySet()) {
            if (entry.getValue().isChecked()) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    private TextView bodyText(String text, int color) {
        final TextView view = new TextView(this.activity);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, BODY_TEXT_SP);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return PatchesTheme.dpToPx(this.activity, value);
    }

    private static LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

}
