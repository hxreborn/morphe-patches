/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import app.hxreborn.extension.BuildConfig;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.IntegerSetting;
import app.morphe.extension.shared.settings.Setting;

public final class PatchPanel {

    private static final String TITLE = "hxreborn Patches";
    private static final String BUNDLE_VERSION = BuildConfig.BUNDLE_VERSION;

    private static final int BG = Color.parseColor("#0B0B0D");
    private static final int FG = Color.parseColor("#FFFFFF");
    private static final int DIM = Color.parseColor("#8E8E93");
    private static final int ACCENT = Color.parseColor("#FA2A80");
    private static final int HAIRLINE = Color.parseColor("#26262A");
    private static final int SURFACE = Color.parseColor("#18181C");
    private static final int OK = Color.parseColor("#7EE787");
    private static final int PENDING = Color.parseColor("#E3B341");
    private static final int FAILED = Color.parseColor("#FF7B72");

    private static final float TITLE_SP = 17f;
    private static final float SUMMARY_SP = 14f;
    private static final float HEADER_SP = 15f;
    private static final int ROW_HEIGHT_DP = 73;
    private static final int SIDE_PAD_DP = 25;

    private static final Set<String> INSTALLED = new HashSet<>();

    private static final class Entry {
        final Setting<?> setting;
        final String category;
        final String title;
        final String summary;
        final int min;
        final int max;

        Entry(Setting<?> setting, String category, String title, String summary) {
            this(setting, category, title, summary, 0, 0);
        }

        Entry(Setting<?> setting, String category, String title, String summary,
              int min, int max) {
            this.setting = setting;
            this.category = category;
            this.title = title;
            this.summary = summary;
            this.min = min;
            this.max = max;
        }
    }

    private static final List<Entry> ENTRIES = Collections.emptyList();

    public static void markInstalled(String key) {
        INSTALLED.add(key);
    }

    private static boolean installed(Setting<?> setting) {
        return INSTALLED.contains(setting.key);
    }

    private PatchPanel() {
    }

    private static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    private static int statusBarHeight(Context c) {
        int id = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? c.getResources().getDimensionPixelSize(id) : dp(c, 24);
    }

    public static void install(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                if (activity.getClass().getName().endsWith(".SettingActivity")) {
                    attach(activity);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    public static void attach(Activity activity) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                ListView list = findListView(activity.findViewById(android.R.id.content));
                if (list == null) {
                    return;
                }
                ViewGroup parent = (ViewGroup) list.getParent();
                if (parent == null || TITLE.equals(parent.getTag())) {
                    return;
                }
                stackAbove(activity, list, buildEntry(activity));
            } catch (Exception ex) {
                Log.e(TITLE, "attach failed", ex);
            }
        });
    }

    private static ListView findListView(View view) {
        if (view == null) {
            return null;
        }
        if (view instanceof ListView) {
            return (ListView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ListView found = findListView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static View buildEntry(Activity activity) {
        LinearLayout row = flatRow(activity);
        row.addView(labels(activity, TITLE, "Options added by patches"));
        row.addView(versionBadge(activity));
        row.setContentDescription(TITLE + ", bundle " + BUNDLE_VERSION);
        row.setOnClickListener(v -> show(activity));
        return row;
    }

    private static View versionBadge(Activity activity) {
        TextView badge = new TextView(activity);
        badge.setText(BUNDLE_VERSION);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setPadding(dp(activity, 8), dp(activity, 3), dp(activity, 8), dp(activity, 4));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(ACCENT);
        pill.setCornerRadius(dp(activity, 10));
        badge.setBackground(pill);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(activity, 12);
        badge.setLayoutParams(params);
        return badge;
    }

    private static void stackAbove(Activity activity, ListView list, View row) {
        ViewGroup parent = (ViewGroup) list.getParent();
        if (parent == null) {
            return;
        }
        int index = parent.indexOfChild(list);
        ViewGroup.LayoutParams original = list.getLayoutParams();
        parent.removeViewAt(index);

        LinearLayout holder = new LinearLayout(activity);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setTag(TITLE);
        holder.addView(sectionHeader(activity, "Patches"));
        holder.addView(divider(activity));
        holder.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, ROW_HEIGHT_DP)));
        holder.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        parent.addView(holder, index, original);
    }

    public static void show(Activity activity) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.addView(toolbar(activity, dialog, TITLE));

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(activity, 40));

        String previous = null;
        for (Entry entry : ENTRIES) {
            if (!installed(entry.setting)) {
                continue;
            }
            if (!entry.category.equals(previous)) {
                content.addView(sectionHeader(activity, entry.category));
                previous = entry.category;
            }
            content.addView(divider(activity));
            if (entry.setting instanceof BooleanSetting) {
                content.addView(booleanRow(activity, entry));
            } else if (entry.setting instanceof IntegerSetting) {
                content.addView(integerRow(activity, entry));
            }
        }

        content.addView(sectionHeader(activity, "About"));
        content.addView(divider(activity));
        content.addView(staticRow(activity, "App version", appVersion(activity)));

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(BG));
            window.setStatusBarColor(BG);
            window.setNavigationBarColor(BG);
        }
        dialog.show();
    }

    private static View toolbar(Activity activity, Dialog dialog, String heading) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(BG);
        // Dialog draws behind the status bar
        bar.setPadding(dp(activity, 10), statusBarHeight(activity) + dp(activity, 14),
                dp(activity, SIDE_PAD_DP), dp(activity, 14));

        TextView back = new TextView(activity);
        back.setText("‹");
        back.setTextColor(FG);
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        back.setGravity(Gravity.CENTER);
        back.setBackground(ripple(activity));
        int size = dp(activity, 44);
        back.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        back.setOnClickListener(v -> dialog.dismiss());
        bar.addView(back);

        TextView title = new TextView(activity);
        title.setText(heading);
        title.setTextColor(FG);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(activity, 12);
        title.setLayoutParams(params);
        bar.addView(title);

        return bar;
    }

    private static TextView title(Activity activity, String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(FG);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP);
        return view;
    }

    private static TextView summary(Activity activity, String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(DIM);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, SUMMARY_SP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(activity, 5);
        view.setLayoutParams(params);
        return view;
    }

    private static View sectionHeader(Activity activity, String text) {
        TextView header = new TextView(activity);
        header.setText(text.toUpperCase(Locale.getDefault()));
        header.setTextColor(DIM);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_SP);
        header.setPadding(dp(activity, SIDE_PAD_DP), dp(activity, 26),
                dp(activity, SIDE_PAD_DP), dp(activity, 14));
        return header;
    }

    private static View divider(Activity activity) {
        View line = new View(activity);
        line.setBackgroundColor(HAIRLINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(activity, 0.5f)));
        params.leftMargin = dp(activity, 14);
        line.setLayoutParams(params);
        return line;
    }

    private static LinearLayout flatRow(Activity activity) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(ripple(activity));
        row.setPadding(dp(activity, SIDE_PAD_DP), 0, dp(activity, SIDE_PAD_DP), 0);
        row.setMinimumHeight(dp(activity, ROW_HEIGHT_DP));
        return row;
    }

    private static LinearLayout labels(Activity activity, String titleText, String summaryText) {
        LinearLayout block = new LinearLayout(activity);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        block.addView(title(activity, titleText));
        if (!summaryText.isEmpty()) {
            block.addView(summary(activity, summaryText));
        }
        return block;
    }

    private static View booleanRow(Activity activity, Entry entry) {
        BooleanSetting setting = (BooleanSetting) entry.setting;
        LinearLayout row = flatRow(activity);
        row.addView(labels(activity, entry.title, entry.summary));

        Switch toggle = new Switch(activity);
        toggle.setChecked(setting.get());
        toggle.setClickable(false);
        toggle.setFocusable(false);
        tint(toggle);
        row.addView(toggle);

        row.setContentDescription(entry.title + ". " + entry.summary);
        row.setOnClickListener(v -> {
            boolean next = !toggle.isChecked();
            toggle.setChecked(next);
            setting.save(next);
        });
        return row;
    }

    private static View integerRow(Activity activity, Entry entry) {
        IntegerSetting setting = (IntegerSetting) entry.setting;
        LinearLayout row = flatRow(activity);
        LinearLayout block = labels(activity, entry.title, entry.summary);
        row.addView(block);

        TextView value = new TextView(activity);
        value.setTextColor(ACCENT);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP);
        value.setText(String.valueOf(setting.get()));
        row.addView(value);

        row.setContentDescription(entry.title + ". " + entry.summary);
        row.setOnClickListener(v -> promptForInteger(activity, entry, value));
        return row;
    }

    private static void promptForInteger(Activity activity, Entry entry, TextView value) {
        IntegerSetting setting = (IntegerSetting) entry.setting;

        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(setting.get()));
        input.setTextColor(FG);
        input.setHintTextColor(DIM);
        input.setHint(entry.min + " to " + entry.max);
        int pad = dp(activity, SIDE_PAD_DP);
        input.setPadding(pad, dp(activity, 12), pad, dp(activity, 12));

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(entry.title)
                .setMessage(entry.summary)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int parsed;
                    try {
                        parsed = Integer.parseInt(input.getText().toString().trim());
                    } catch (NumberFormatException ex) {
                        return;
                    }
                    int clamped = Math.max(entry.min, Math.min(entry.max, parsed));
                    setting.save(clamped);
                    value.setText(String.valueOf(clamped));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static View staticRow(Activity activity, String titleText, String summaryText) {
        LinearLayout row = flatRow(activity);
        row.setPadding(dp(activity, SIDE_PAD_DP), dp(activity, 16),
                dp(activity, SIDE_PAD_DP), dp(activity, 16));
        row.addView(labels(activity, titleText, summaryText));
        return row;
    }

    private static View traceRow(Activity activity, String titleText, String summaryText,
                                 Runnable onOpen) {
        LinearLayout row = flatRow(activity);
        row.setPadding(dp(activity, SIDE_PAD_DP), dp(activity, 16),
                dp(activity, SIDE_PAD_DP), dp(activity, 16));
        row.addView(labels(activity, titleText, summaryText));
        row.setContentDescription(titleText + ". " + summaryText);
        row.setOnClickListener(v -> onOpen.run());
        return row;
    }



    private static View requestCard(Activity activity, String[] event) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 16), dp(activity, 13),
                dp(activity, 16), dp(activity, 13));
        GradientDrawable background = new GradientDrawable();
        background.setColor(SURFACE);
        background.setCornerRadius(dp(activity, 12));
        card.setBackground(background);

        LinearLayout request = new LinearLayout(activity);
        request.setOrientation(LinearLayout.HORIZONTAL);
        request.setGravity(Gravity.CENTER_VERTICAL);

        TextView method = requestText(activity, event[1], ACCENT, 13, true);
        LinearLayout.LayoutParams methodParams = new LinearLayout.LayoutParams(
                dp(activity, 48), ViewGroup.LayoutParams.WRAP_CONTENT);
        method.setLayoutParams(methodParams);
        request.addView(method);

        TextView endpoint = requestText(activity, event[2], FG, 15, false);
        endpoint.setTypeface(Typeface.MONOSPACE);
        request.addView(endpoint, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(request);

        LinearLayout details = new LinearLayout(activity);
        details.setOrientation(LinearLayout.HORIZONTAL);
        details.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailsParams.topMargin = dp(activity, 10);
        details.setLayoutParams(detailsParams);

        details.addView(requestText(activity, event[0].trim(), DIM, 13, false));
        View spacer = new View(activity);
        details.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));
        if (!event[3].isEmpty()) {
            details.addView(statusBadge(activity, event[3]));
        }
        if (!event[4].isEmpty()) {
            TextView latency = requestText(activity, event[4], DIM, 13, false);
            LinearLayout.LayoutParams latencyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            latencyParams.leftMargin = dp(activity, 12);
            latency.setLayoutParams(latencyParams);
            details.addView(latency);
        }
        card.addView(details);
        card.setContentDescription(String.format(Locale.US, "%s %s, %s, %s, %s",
                event[1], event[2], event[0].trim(), statusLabel(event[3]), event[4]));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(activity, 10);
        card.setLayoutParams(cardParams);
        return card;
    }

    private static TextView requestText(Activity activity, String text, int colour,
                                        float size, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(colour);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private static View statusBadge(Activity activity, String code) {
        int colour = statusColour(code);
        TextView badge = requestText(activity, statusLabel(code), colour, 12, true);
        badge.setPadding(dp(activity, 8), dp(activity, 3),
                dp(activity, 8), dp(activity, 3));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(30, Color.red(colour),
                Color.green(colour), Color.blue(colour)));
        background.setStroke(dp(activity, 1), colour);
        background.setCornerRadius(dp(activity, 12));
        badge.setBackground(background);
        return badge;
    }

    private static String statusLabel(String code) {
        if (code.isEmpty()) {
            return "no response";
        }
        if (code.startsWith("2")) {
            return code + " ready";
        }
        return "404".equals(code) ? code + " waiting" : code;
    }

    // Polling returns 404 until the result is ready
    private static int statusColour(String code) {
        if (code.startsWith("2")) {
            return OK;
        }
        return "404".equals(code) ? PENDING : FAILED;
    }

    private static LinearLayout logBody(Activity activity) {
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(activity, 16), dp(activity, 8),
                dp(activity, 16), dp(activity, 24));
        return body;
    }

    private static TextView logLine(Activity activity, String text, int colour) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(colour);
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setLineSpacing(dp(activity, 4), 1f);
        return view;
    }

    private static void showLog(Activity activity, String heading, View body) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.addView(toolbar(activity, dialog, heading));

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(BG));
            window.setStatusBarColor(BG);
            window.setNavigationBarColor(BG);
        }
        dialog.show();
    }

    private static String appVersion(Activity activity) {
        try {
            return activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private static void tint(Switch toggle) {
        int[][] states = {{android.R.attr.state_checked}, {}};
        toggle.setThumbTintList(new android.content.res.ColorStateList(
                states, new int[]{ACCENT, Color.parseColor("#B0B0B6")}));
        toggle.setTrackTintList(new android.content.res.ColorStateList(
                states, new int[]{
                        Color.argb(130, Color.red(ACCENT), Color.green(ACCENT), Color.blue(ACCENT)),
                        Color.parseColor("#4A4A50"),
                }));
    }

    private static Drawable ripple(Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        Drawable drawable = value.resourceId != 0 ? context.getDrawable(value.resourceId) : null;
        return drawable != null ? drawable : new ColorDrawable(Color.TRANSPARENT);
    }
}
