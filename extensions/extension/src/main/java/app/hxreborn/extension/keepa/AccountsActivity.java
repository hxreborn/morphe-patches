/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("SetTextI18n")
@SuppressWarnings("unused")
public final class AccountsActivity extends Activity {

    private static final int GUTTER_DP = 8;
    private static final int CARD_GAP_DP = 16;
    private static final int CARD_RADIUS_DP = 12;
    private static final int CARD_PAD_DP = 12;
    private static final int ROW_MIN_HEIGHT_DP = 56;
    private static final int ROW_PAD_V_DP = 10;
    private static final int PROGRESS_HEIGHT_DP = 6;
    private static final int DIVIDER_DP = 1;
    private static final int BUTTON_HEIGHT_DP = 48;
    private static final int HEADER_HEIGHT_DP = 48;
    private static final int HEADER_TITLE_SP = 18;
    private static final int ICON_SIZE_DP = 24;
    private static final int TITLE_SP = 16;
    private static final int BODY_SP = 14;
    private static final int BADGE_SP = 12;
    private static final int BADGE_PAD_H_DP = 8;
    private static final int BADGE_PAD_V_DP = 4;
    private static final int PILL_RADIUS_DP = 100;
    private static final int LINE_GAP_DP = 2;
    private static final int PROGRESS_GAP_DP = 10;
    private static final int STATUS_BAR_FALLBACK_DP = 24;
    private static final int COLOR_ALPHA_MASK = 0xFF000000;
    private static final int COLOR_ALPHA_SHIFT = 24;
    private static final int RIPPLE_ALPHA = 0x40;
    private static final int BADGE_ALPHA = 0x33;
    private static final long MINUTE_MS = 60_000L;

    private static final int DARK_BACKGROUND = 0xFF0F172A;
    private static final int DARK_CARD = 0xFF1E293B;
    private static final int DARK_TEXT = 0xFFF8FAFC;
    private static final int DARK_MUTED = 0xFF94A3B8;
    private static final int DARK_ACCENT = 0xFF60A5FA;
    private static final int DARK_DANGER = 0xFFF87171;
    private static final int DARK_BORDER = 0xFF334155;
    private static final int LIGHT_BACKGROUND = 0xFFF1F5F9;
    private static final int LIGHT_CARD = 0xFFFFFFFF;
    private static final int LIGHT_TEXT = 0xFF1E293B;
    private static final int LIGHT_MUTED = 0xFF64748B;
    private static final int LIGHT_ACCENT = 0xFF3B82F6;
    private static final int LIGHT_DANGER = 0xFFDC2626;
    private static final int LIGHT_BORDER = 0xFFE2E8F0;
    private static final String ICON_FONT_ASSET = "app/fonts/MaterialIcons-Regular.ttf";
    private static final String ICON_ARROW_BACK = "";

    private static final String[] TAG_PLACEMENTS = {Accounts.TAG_BELOW, Accounts.TAG_FLAG, Accounts.TAG_OFF};
    private static final String[] TAG_PLACEMENT_LABELS = {"Below the image", "Beside the flag", "Hidden"};

    private boolean dark;
    private int backgroundColor;
    private int cardColor;
    private int textColor;
    private int mutedColor;
    private int accentColor;
    private int dangerColor;
    private int trackColor;
    private int dividerColor;
    private int rippleColor;
    private int badgeBackgroundColor;
    private Typeface mediumTypeface;
    private ScrollView scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        backgroundColor = dark ? DARK_BACKGROUND : LIGHT_BACKGROUND;
        cardColor = dark ? DARK_CARD : LIGHT_CARD;
        textColor = dark ? DARK_TEXT : LIGHT_TEXT;
        mutedColor = dark ? DARK_MUTED : LIGHT_MUTED;
        accentColor = dark ? DARK_ACCENT : LIGHT_ACCENT;
        dangerColor = dark ? DARK_DANGER : LIGHT_DANGER;
        trackColor = dark ? DARK_BORDER : LIGHT_BORDER;
        dividerColor = trackColor;
        rippleColor = withAlpha(accentColor, RIPPLE_ALPHA);
        badgeBackgroundColor = withAlpha(accentColor, BADGE_ALPHA);
        mediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        final LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(backgroundColor);
        screen.setPadding(0, systemBarHeight("status_bar_height", STATUS_BAR_FALLBACK_DP), 0,
                systemBarHeight("navigation_bar_height", 0));
        screen.addView(header());

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        screen.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        final View add = addButton();
        final LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(BUTTON_HEIGHT_DP));
        addParams.setMargins(dp(GUTTER_DP), dp(GUTTER_DP), dp(GUTTER_DP), dp(GUTTER_DP));
        screen.addView(add, addParams);
        setContentView(screen);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private View header() {
        final LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(CARD_PAD_DP), 0, dp(CARD_PAD_DP), 0);
        bar.setMinimumHeight(dp(HEADER_HEIGHT_DP));
        bar.setBackground(roundedRipple(cardColor, CARD_RADIUS_DP));
        bar.setClickable(true);
        bar.setFocusable(true);
        bar.setContentDescription("Back");
        bar.setOnClickListener(ignored -> finish());

        final TextView arrow = new TextView(this);
        arrow.setTextColor(accentColor);
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, ICON_SIZE_DP);
        arrow.setGravity(Gravity.CENTER);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        try {
            arrow.setTypeface(Typeface.createFromAsset(getAssets(), ICON_FONT_ASSET));
            arrow.setText(ICON_ARROW_BACK);
        } catch (RuntimeException missingFont) {
            arrow.setText("←");
        }
        bar.addView(arrow, new LinearLayout.LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP)));

        final TextView title = text("Accounts", textColor, HEADER_TITLE_SP);
        title.setTypeface(mediumTypeface);
        title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.setMarginStart(dp(CARD_PAD_DP));
        bar.addView(title, titleParams);

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(GUTTER_DP), dp(GUTTER_DP), dp(GUTTER_DP), 0);
        bar.setLayoutParams(params);
        return bar;
    }

    private void render() {
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(GUTTER_DP), dp(CARD_GAP_DP), dp(GUTTER_DP), 0);

        final Accounts accounts;
        try {
            accounts = AccountStore.of(this).load();
        } catch (JSONException exception) {
            column.addView(paragraph("Stored accounts could not be read. Reset removes them from this device. "
                    + "Add each account again afterwards."));
            column.addView(button("Reset accounts", dangerColor, ignored -> {
                AccountStore.of(this).reset();
                render();
            }));
            scroll.removeAllViews();
            scroll.addView(column);
            return;
        }
        if (accounts.all().isEmpty()) {
            column.addView(paragraph("No accounts. Each account tracks up to "
                    + Account.FREE_TRACKING_LIMIT + " products."));
        } else {
            column.addView(summaryCard(accounts));
            for (Account account : primaryFirst(accounts)) {
                column.addView(accountCard(account, account.id.equals(accounts.primaryId())));
            }
        }
        scroll.removeAllViews();
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private List<Account> primaryFirst(Accounts accounts) {
        final List<Account> ordered = new ArrayList<>();
        for (Account account : accounts.all()) {
            if (account.id.equals(accounts.primaryId())) ordered.add(0, account);
            else ordered.add(account);
        }
        return ordered;
    }

    private View paragraph(String content) {
        final TextView message = text(content, mutedColor, BODY_SP);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(CARD_PAD_DP), 0, dp(CARD_PAD_DP), dp(CARD_GAP_DP));
        message.setLayoutParams(params);
        return message;
    }

    private View summaryCard(Accounts accounts) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundedFill(cardColor, CARD_RADIUS_DP));
        card.setLayoutParams(cardParams());

        final LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(CARD_PAD_DP), dp(CARD_PAD_DP), dp(CARD_PAD_DP), dp(CARD_PAD_DP));
        final TextView title = text(accounts.summary(), textColor, TITLE_SP);
        title.setTypeface(mediumTypeface);
        info.addView(title);
        info.addView(progress(accounts.trackedTotal(), accounts.limitTotal()));
        info.addView(secondaryLine(accounts.all().size() == 1
                ? "Add an account for " + Account.FREE_TRACKING_LIMIT + " more trackings."
                : "Primary: the account shown in Keepa settings. Each account keeps its own session."));
        card.addView(info);

        if (accounts.all().size() > 1) {
            card.addView(divider());
            card.addView(settingRow("Account for new trackings", allocationSummary(accounts), rectangularRipple(),
                    ignored -> showAllocationPicker(accounts)));
            card.addView(divider());
            card.addView(settingRow("Account on each product", tagPlacementLabel(accounts.tagPlacement()),
                    bottomRoundedRipple(), ignored -> showTagPlacementPicker(accounts)));
        }
        return card;
    }

    private View settingRow(String title, String summary, Drawable background, View.OnClickListener onClick) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(CARD_PAD_DP), dp(ROW_PAD_V_DP), dp(CARD_PAD_DP), dp(ROW_PAD_V_DP));
        row.setMinimumHeight(dp(ROW_MIN_HEIGHT_DP));
        row.setBackground(background);
        row.setClickable(true);
        row.setFocusable(true);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final TextView titleView = text(title, textColor, TITLE_SP);
        titleView.setTypeface(mediumTypeface);
        row.addView(titleView);
        row.addView(secondaryLine(summary));
        row.setContentDescription(title + ". " + summary);
        row.setOnClickListener(onClick);
        return row;
    }

    private String allocationSummary(Accounts accounts) {
        final Account pinned = accounts.find(accounts.allocationId());
        if (pinned == null) return "Automatic: primary until full, then the next";
        return pinned.username;
    }

    private String tagPlacementLabel(String placement) {
        return TAG_PLACEMENT_LABELS[Math.max(0, Arrays.asList(TAG_PLACEMENTS).indexOf(placement))];
    }

    private void showAllocationPicker(Accounts accounts) {
        final List<String> labels = new ArrayList<>();
        final List<String> ids = new ArrayList<>();
        labels.add("Automatic");
        ids.add(Accounts.AUTOMATIC);
        for (Account account : accounts.all()) {
            labels.add(account.username);
            ids.add(account.id);
        }
        showChoice("Account for new trackings", labels, Math.max(0, ids.indexOf(accounts.allocationId())),
                which -> AccountBridge.update(this, stored -> stored.setAllocation(ids.get(which))));
    }

    private void showTagPlacementPicker(Accounts accounts) {
        final int checked = Math.max(0, Arrays.asList(TAG_PLACEMENTS).indexOf(accounts.tagPlacement()));
        showChoice("Account on each product", Arrays.asList(TAG_PLACEMENT_LABELS), checked,
                which -> AccountBridge.update(this, stored -> stored.setTagPlacement(TAG_PLACEMENTS[which])));
    }

    private void showChoice(String title, List<String> labels, int checked, ChoiceListener choice) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels.toArray(new CharSequence[0]), checked, (dialog, which) -> {
                    dialog.dismiss();
                    try {
                        choice.chosen(which);
                        render();
                    } catch (JSONException exception) {
                        showSaveError();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private View accountCard(final Account account, boolean primary) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(CARD_PAD_DP), dp(CARD_PAD_DP), dp(CARD_PAD_DP), dp(CARD_PAD_DP));
        card.setBackground(roundedRipple(cardColor, CARD_RADIUS_DP));
        card.setClickable(true);
        card.setFocusable(true);
        card.setLayoutParams(cardParams());

        final LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        final TextView name = text(account.username, textColor, TITLE_SP);
        name.setTypeface(mediumTypeface);
        name.setMaxLines(1);
        name.setEllipsize(TextUtils.TruncateAt.END);
        headerRow.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (primary) {
            final LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeParams.setMarginStart(dp(CARD_PAD_DP));
            headerRow.addView(primaryBadge(), badgeParams);
        }
        card.addView(headerRow);

        if (!account.email.isEmpty()) card.addView(secondaryLine(account.email));
        final String state = stateLine(account);
        final TextView stateView = secondaryLine(state);
        if (needsAttention(account)) stateView.setTextColor(dangerColor);
        card.addView(stateView);

        card.setContentDescription(account.username + (primary ? ", primary" : "") + ". " + state);
        card.setOnClickListener(ignored -> showActions(account, primary));
        return card;
    }

    private View primaryBadge() {
        final TextView badge = new TextView(this);
        badge.setText("Primary");
        badge.setTextColor(accentColor);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, BADGE_SP);
        badge.setTypeface(mediumTypeface);
        badge.setIncludeFontPadding(false);
        badge.setPadding(dp(BADGE_PAD_H_DP), dp(BADGE_PAD_V_DP), dp(BADGE_PAD_H_DP), dp(BADGE_PAD_V_DP));
        badge.setBackground(roundedFill(badgeBackgroundColor, PILL_RADIUS_DP));
        return badge;
    }

    private boolean needsAttention(Account account) {
        return account.state == AccountState.INVALID || (account.limit > 0 && account.tracked >= account.limit);
    }

    private String stateLine(Account account) {
        final long now = System.currentTimeMillis();
        if (account.state == AccountState.INVALID) return "Signed out. Sign in again.";
        if (account.isThrottled(now)) {
            final long minutes = Math.max(1, (account.throttledUntil - now + MINUTE_MS - 1) / MINUTE_MS);
            return "Rate limited, " + minutes + " min left";
        }
        if (account.limit > 0 && account.tracked >= account.limit) {
            return "Full, " + account.tracked + " / " + account.limit + " tracked";
        }
        return account.tracked + " / " + account.limitOrDefault() + " tracked";
    }

    private void showActions(final Account account, boolean primary) {
        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();
        if (account.state == AccountState.INVALID) {
            labels.add("Sign in again");
            actions.add(() -> writePending("reauth", account.id));
        } else if (!primary) {
            labels.add("Set as primary");
            actions.add(() -> writePending("switch", account.id));
        }
        labels.add("Remove…");
        actions.add(() -> confirmRemove(account));
        new AlertDialog.Builder(this)
                .setTitle(account.username)
                .setItems(labels.toArray(new CharSequence[0]), (dialog, which) -> actions.get(which).run())
                .show();
    }

    private void confirmRemove(final Account account) {
        final String products = account.tracked == 1 ? "1 tracked product" : account.tracked + " tracked products";
        new AlertDialog.Builder(this)
                .setTitle("Remove " + account.username + "?")
                .setMessage("keepa.com keeps its " + products + ". This device stops showing "
                        + (account.tracked == 1 ? "it" : "them") + " and stops their push notifications.")
                .setPositiveButton("Remove", (dialog, which) -> writePending("remove", account.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private View addButton() {
        return button("Add account", accentColor, ignored -> {
            Toast.makeText(this, "Sign in to the account to add.", Toast.LENGTH_LONG).show();
            writePending("add", null);
        });
    }

    private View button(String label, int color, View.OnClickListener onClick) {
        final Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(dark ? DARK_BACKGROUND : LIGHT_CARD);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setStateListAnimator(null);
        button.setBackground(roundedRipple(color, CARD_RADIUS_DP));
        button.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(BUTTON_HEIGHT_DP)));
        button.setOnClickListener(onClick);
        return button;
    }

    private void writePending(String operation, String id) {
        try {
            final JSONObject pending = new JSONObject().put("op", operation);
            if (id != null) pending.put("id", id);
            AccountBridge.requestOperation(this, pending);
            finish();
        } catch (JSONException exception) {
            showSaveError();
        }
    }

    private void showSaveError() {
        Toast.makeText(this, "Change not saved. Try again.", Toast.LENGTH_LONG).show();
    }

    private LinearLayout.LayoutParams cardParams() {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(CARD_GAP_DP);
        return params;
    }

    private TextView text(CharSequence content, int color, int sizeSp) {
        final TextView view = new TextView(this);
        view.setText(content);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        return view;
    }

    private TextView secondaryLine(String content) {
        final TextView view = text(content, mutedColor, BODY_SP);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(LINE_GAP_DP);
        view.setLayoutParams(params);
        return view;
    }

    private View divider() {
        final View line = new View(this);
        line.setBackgroundColor(dividerColor);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(DIVIDER_DP));
        params.setMarginStart(dp(CARD_PAD_DP));
        params.setMarginEnd(dp(CARD_PAD_DP));
        line.setLayoutParams(params);
        return line;
    }

    private View progress(int value, int max) {
        final ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setProgressDrawable(roundedProgress(value >= max ? dangerColor : accentColor));
        bar.setMax(Math.max(max, 1));
        bar.setProgress(Math.max(0, Math.min(value, max)));
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(PROGRESS_HEIGHT_DP));
        params.topMargin = dp(PROGRESS_GAP_DP);
        bar.setLayoutParams(params);
        return bar;
    }

    private Drawable roundedProgress(int fillColor) {
        final float radius = dp(PROGRESS_HEIGHT_DP) / 2f;
        final GradientDrawable track = new GradientDrawable();
        track.setColor(trackColor);
        track.setCornerRadius(radius);
        final GradientDrawable fill = new GradientDrawable();
        fill.setColor(fillColor);
        fill.setCornerRadius(radius);
        final ClipDrawable clip = new ClipDrawable(fill, Gravity.START, ClipDrawable.HORIZONTAL);
        final LayerDrawable layers = new LayerDrawable(new Drawable[]{track, clip});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        return layers;
    }

    private GradientDrawable roundedFill(int color, int radiusDp) {
        final GradientDrawable fill = new GradientDrawable();
        fill.setColor(color);
        fill.setCornerRadius(dp(radiusDp));
        return fill;
    }

    private Drawable roundedRipple(int color, int radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor),
                roundedFill(color, radiusDp), roundedFill(Color.WHITE, radiusDp));
    }

    private Drawable bottomRoundedRipple() {
        final float radius = dp(CARD_RADIUS_DP);
        final GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadii(new float[]{0, 0, 0, 0, radius, radius, radius, radius});
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), null, mask);
    }

    private Drawable rectangularRipple() {
        final GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), null, mask);
    }

    private int systemBarHeight(String name, int fallbackDp) {
        final int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(fallbackDp);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & ~COLOR_ALPHA_MASK) | (alpha << COLOR_ALPHA_SHIFT);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
