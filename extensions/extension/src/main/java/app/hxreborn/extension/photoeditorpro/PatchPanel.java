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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import app.hxreborn.extension.BuildConfig;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.EnumSetting;
import app.morphe.extension.shared.settings.Setting;

public final class PatchPanel {

    private static final String TITLE = "hxreborn Patches";
    private static final String BUNDLE_VERSION = BuildConfig.BUNDLE_VERSION;

    private static final int BG = Color.parseColor("#0B0B0D");
    static final int FG = Color.parseColor("#FFFFFF");
    static final int DIM = Color.parseColor("#8E8E93");
    static final int ACCENT = Color.parseColor("#FA2A80");
    static final int HAIRLINE = Color.parseColor("#26262A");
    static final int OK = Color.parseColor("#7EE787");
    static final int PENDING = Color.parseColor("#E3B341");
    static final int FAILED = Color.parseColor("#FF7B72");
    static final int NEUTRAL = Color.parseColor("#2C2C30");
    static final int SURFACE = Color.parseColor("#1E1E22");
    static final int MUTED = Color.parseColor("#6E6E73");
    private static final int CARD_BG = Color.parseColor("#17171A");

    private static final float TITLE_SP = 17f;
    private static final float SUMMARY_SP = 14f;
    private static final float HEADER_SP = 15f;
    private static final int ROW_HEIGHT_DP = 73;
    private static final int SIDE_PAD_DP = 25;
    private static final float CHEVRON_SP = 20f;
    private static final int CHEVRON_MARGIN_DP = 8;

    private static final int CARD_RADIUS_DP = 14;
    private static final int CHIP_RADIUS_DP = 8;
    private static final int CHIP_PAD_H_DP = 8;
    private static final int CHIP_PAD_V_DP = 3;
    private static final float CHIP_SP = 12f;
    private static final int ROW_PAD_V_DP = 4;
    private static final int ROW_TIME_WIDTH_DP = 46;
    private static final int ROW_CHIP_MARGIN_DP = 10;
    private static final float ROW_SP = 13f;
    private static final float DISABLED_ALPHA = 0.38f;

    private static final String GENERAL = "General";
    private static final String AI_TOOLS = "AI tools";
    private static final String DIAGNOSTICS = "Diagnostics";

    private static final Set<String> INSTALLED = new HashSet<>();
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final Set<String> HTTP_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE", "CONNECT");
    private static final float EQUAL_CONTRAST_LUMINANCE =
            (float) (Math.sqrt(1.05 * 0.05) - 0.05);
    private static final List<Entry> ENTRIES = List.of(
            new Entry(PatchSettings.HIDE_ADS, GENERAL, "Hide ads",
                    "Hides banner, interstitial, app-open and rewarded ads"),
            new Entry(PatchSettings.UNLOCK_PREMIUM, GENERAL, "Unlock premium",
                    "Unlocks the pro tools and removes the export watermark"),
            new Entry(PatchSettings.SHOW_AI_PROGRESS, AI_TOOLS,
                    "Show AI progress",
                    "Reads the stage off the real network activity instead of InShot's "
                            + "fake progress bar"),
            new Entry(PatchSettings.LOG_ENDPOINTS, DIAGNOSTICS,
                    "Inspect AI requests",
                    "Shows the network calls an AI tool makes (GET, POST, Firebase uploads) "
                            + "and keeps a log in memory"),
            new Entry(PatchSettings.OVERLAY_POSITION, DIAGNOSTICS,
                    "Live overlay position",
                    "Screen position of the live call list",
                    PatchSettings.LOG_ENDPOINTS),
            new Entry(PatchSettings.OVERLAY_LINGER, DIAGNOSTICS,
                    "Live overlay hold time",
                    "Delay before the overlay disappears",
                    PatchSettings.LOG_ENDPOINTS));

    static boolean installed(Setting<?> setting) {
        return INSTALLED.contains(setting.key);
    }

    public static void markInstalled(String key) {
        INSTALLED.add(key);
    }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    private PatchPanel() {
    }

    static int topInset(WindowInsets insets) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? insets.getInsets(WindowInsets.Type.systemBars()).top
                : insets.getSystemWindowInsetTop();
    }

    static int bottomInset(WindowInsets insets) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? insets.getInsets(WindowInsets.Type.systemBars()).bottom
                : insets.getSystemWindowInsetBottom();
    }

    private static void fitSystemBars(View root) {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, topInset(insets), 0, bottomInset(insets));
            return insets;
        });
    }

    public static void attach(Activity activity) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                ListView list = findListView(activity.findViewById(android.R.id.content));
                if (list == null) {
                    Log.w(TITLE, "attach: no ListView found in " + activity.getClass().getName());
                    return;
                }
                ViewGroup parent = (ViewGroup) list.getParent();
                if (parent == null) {
                    Log.w(TITLE, "attach: ListView has no parent in " + activity.getClass().getName());
                    return;
                }
                if (TITLE.equals(parent.getTag())) {
                    return;
                }
                stackAbove(activity, list, buildEntry(activity));
            } catch (Exception ex) {
                Log.e(TITLE, "attach failed", ex);
            }
        });
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

    private static ListView findListView(View view) {
        if (view == null) {
            return null;
        }
        if (view instanceof ListView listView) {
            return listView;
        }
        if (view instanceof ViewGroup group) {
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
        row.addView(labels(activity, TITLE, "Applied with Morphe"));
        row.addView(versionBadge(activity));
        row.addView(chevron(activity));
        row.setContentDescription(TITLE + ", bundle " + BUNDLE_VERSION);
        row.setOnClickListener(v -> show(activity));
        return row;
    }

    private static View chevron(Activity activity) {
        TextView chevron = new TextView(activity);
        chevron.setText("›");
        chevron.setTextColor(DIM);
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, CHEVRON_SP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(activity, CHEVRON_MARGIN_DP);
        chevron.setLayoutParams(params);
        return chevron;
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

    public static void show(Activity activity) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        fitSystemBars(root);
        root.addView(toolbar(activity, dialog, TITLE));

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(activity, 24));

        List<Runnable> dependants = new ArrayList<>();
        Runnable syncDependants = () -> {
            for (Runnable dependant : dependants) {
                dependant.run();
            }
        };

        String previous = null;
        for (Entry entry : ENTRIES) {
            if (!installed(entry.setting)) {
                continue;
            }
            View row;
            if (entry.setting instanceof BooleanSetting) {
                row = booleanRow(activity, entry, syncDependants);
            } else if (entry.setting instanceof EnumSetting) {
                row = choiceRow(activity, entry);
            } else {
                continue;
            }
            if (!entry.category.equals(previous)) {
                content.addView(sectionHeader(activity, entry.category));
                previous = entry.category;
            }
            content.addView(divider(activity));
            content.addView(row);
            if (entry.parent != null) {
                dependants.add(() -> setRowEnabled(row, entry.parent.get()));
            }
        }
        syncDependants.run();

        if (installed(PatchSettings.SHOW_AI_PROGRESS) || installed(PatchSettings.LOG_ENDPOINTS)) {
            content.addView(divider(activity));
            content.addView(traceRow(activity, "AI requests", RequestLog.summary(),
                    () -> RequestLog.show(activity)));
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

    private static View toolbar(Activity activity, Dialog dialog, String heading) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(BG);
        bar.setPadding(dp(activity, 10), dp(activity, 14),
                dp(activity, SIDE_PAD_DP), dp(activity, 14));

        TextView back = new TextView(activity);
        back.setText("‹");
        back.setTextColor(FG);
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        back.setGravity(Gravity.CENTER);
        back.setBackground(ripple(activity));
        int size = dp(activity, 48);
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

    private static View booleanRow(Activity activity, Entry entry, Runnable onChange) {
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
            onChange.run();
        });
        return row;
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

    @SuppressWarnings("unchecked")
    private static View choiceRow(Activity activity, Entry entry) {
        EnumSetting<Enum<?>> setting = (EnumSetting<Enum<?>>) entry.setting;
        Enum<?>[] choices = setting.get().getDeclaringClass().getEnumConstants();

        LinearLayout row = flatRow(activity);
        row.addView(labels(activity, entry.title, entry.summary));

        TextView value = new TextView(activity);
        value.setTextColor(ACCENT);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP);
        value.setText(setting.get().toString());
        row.addView(value);

        String[] items = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            items[i] = choices[i].toString();
        }
        row.setContentDescription(entry.title + ". " + entry.summary);
        row.setOnClickListener(v -> new AlertDialog.Builder(
                activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(entry.title)
                .setItems(items, (dialog, which) -> {
                    setting.save(choices[which]);
                    value.setText(items[which]);
                })
                .show());
        return row;
    }

    private static void setRowEnabled(View row, boolean enabled) {
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : DISABLED_ALPHA);
    }

    private static View traceRow(Activity activity, String titleText, String summaryText,
                                 Runnable onOpen) {
        LinearLayout row = flatRow(activity);
        row.setPadding(dp(activity, SIDE_PAD_DP), dp(activity, 16),
                dp(activity, SIDE_PAD_DP), dp(activity, 16));
        row.addView(labels(activity, titleText, summaryText));
        row.addView(chevron(activity));
        row.setContentDescription(titleText + ". " + summaryText);
        row.setOnClickListener(v -> onOpen.run());
        return row;
    }

    private static View staticRow(Activity activity, String titleText, String summaryText) {
        LinearLayout row = flatRow(activity);
        row.setPadding(dp(activity, SIDE_PAD_DP), dp(activity, 16),
                dp(activity, SIDE_PAD_DP), dp(activity, 16));
        row.addView(labels(activity, titleText, summaryText));
        return row;
    }

    static LinearLayout logBody(Activity activity) {
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(activity, 16), dp(activity, 8),
                dp(activity, 16), dp(activity, 24));
        return body;
    }

    static TextView logLine(Activity activity, String text, int colour) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(colour);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setLineSpacing(dp(activity, 4), 1f);
        return view;
    }

    static int statusColour(int code) {
        if (code == HttpStatus.NONE) {
            return NEUTRAL;
        }
        if (HttpStatus.isSuccess(code)) {
            return OK;
        }
        return HttpStatus.isPending(code) ? PENDING : FAILED;
    }

    static Drawable card(Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(CARD_BG);
        shape.setCornerRadius(dp(context, CARD_RADIUS_DP));
        return shape;
    }

    static View chip(Context context, String text, int background) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(chipTextColour(background));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, CHIP_SP);
        view.setPadding(dp(context, CHIP_PAD_H_DP), dp(context, CHIP_PAD_V_DP),
                dp(context, CHIP_PAD_H_DP), dp(context, CHIP_PAD_V_DP));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(background);
        pill.setCornerRadius(dp(context, CHIP_RADIUS_DP));
        view.setBackground(pill);
        return view;
    }

    static int chipTextColour(int background) {
        return luminance(background) > EQUAL_CONTRAST_LUMINANCE ? BG : FG;
    }

    private static float luminance(int colour) {
        return 0.2126f * linear(Color.red(colour))
                + 0.7152f * linear(Color.green(colour))
                + 0.0722f * linear(Color.blue(colour));
    }

    private static float linear(int channel) {
        float value = channel / 255f;
        return value < 0.03928f
                ? value / 12.92f
                : (float) Math.pow((value + 0.055f) / 1.055f, 2.4);
    }

    static boolean isHttpMethod(String method) {
        return HTTP_METHODS.contains(method.toUpperCase(Locale.US));
    }

    static int methodColour(String method) {
        return SAFE_METHODS.contains(method.toUpperCase(Locale.US)) ? NEUTRAL : ACCENT;
    }

    static View logRow(Activity activity, String time, String status, int statusColour,
                       String detail) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(activity, ROW_PAD_V_DP), 0, dp(activity, ROW_PAD_V_DP));

        TextView timeView = new TextView(activity);
        timeView.setText(time);
        timeView.setTextColor(DIM);
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SP);
        timeView.setLayoutParams(new LinearLayout.LayoutParams(
                dp(activity, ROW_TIME_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(timeView);

        View chip = chip(activity, status, statusColour);
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipParams.rightMargin = dp(activity, ROW_CHIP_MARGIN_DP);
        chip.setLayoutParams(chipParams);
        row.addView(chip);

        if (detail != null) {
            TextView detailView = new TextView(activity);
            detailView.setText(detail);
            detailView.setTextColor(DIM);
            detailView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SP);
            row.addView(detailView);
        }
        return row;
    }

    static void showLog(Activity activity, String heading, View body) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        fitSystemBars(root);
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

    static Drawable ripple(Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        Drawable drawable = value.resourceId != 0 ? context.getDrawable(value.resourceId) : null;
        return drawable != null ? drawable : new ColorDrawable(Color.TRANSPARENT);
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

    private static final class Entry {
        final Setting<?> setting;
        final String category;
        final String title;
        final String summary;
        final BooleanSetting parent;

        Entry(Setting<?> setting, String category, String title, String summary) {
            this(setting, category, title, summary, null);
        }

        Entry(Setting<?> setting, String category, String title, String summary,
              BooleanSetting parent) {
            this.setting = setting;
            this.category = category;
            this.title = title;
            this.summary = summary;
            this.parent = parent;
        }
    }
}
