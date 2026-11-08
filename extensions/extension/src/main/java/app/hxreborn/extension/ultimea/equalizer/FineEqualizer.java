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
package app.hxreborn.extension.ultimea.equalizer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

import app.hxreborn.extension.shared.ActivityCallbacksAdapter;
import app.hxreborn.extension.ultimea.AppliedPatches;
import app.hxreborn.extension.ultimea.EqualizerStyle;

@SuppressWarnings("unused")
public final class FineEqualizer extends ActivityCallbacksAdapter {

    private static final String[] BANDS = { "31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz", "1 kHz", "2 kHz", "4 kHz",
            "8 kHz", "16 kHz" };

    private static final int ROW_HEIGHT_DP = 48;

    private static final int INSET_DP = 16;

    private static final int TEXT_SP = 14;

    private static final int TITLE_SP = 20;

    private static final int RIPPLE = 0x22541597;

    private static final int DISABLED_ALPHA = 0x61000000;

    private static final int RGB_MASK = 0x00ffffff;

    private static final int MAX_NAVIGATION_BYTES = 8192;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static WeakReference<FineEqualizer> current = new WeakReference<>(null);

    private final Activity activity;

    private final EqualizerStyle style;

    private final Button entry;

    private Dialog dialog;

    private Button close;

    private EqualizerHistory history;

    private SoundState edited;

    private Object connection;

    private EqualizerPreset stagedPreset;

    private Button remoteEntry;

    private boolean grouped;

    private TextView status;

    private LinearLayout bands;

    private Button apply;

    private Button undo;

    private Button presets;

    private boolean busy;

    private boolean readFailed;

    private int generation;

    private FineEqualizer(Activity activity, Button entry) {
        this.activity = activity;
        this.style = new EqualizerStyle(activity);
        this.entry = entry;
        entry.setOnClickListener((view) -> open(false));
    }

    public static void attach(Activity activity) {
        FineEqualizer previous = current.get();
        if (previous != null && previous.activity == activity) {
            return;
        }
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() != 1) {
            return;
        }
        View host = content.getChildAt(0);
        ViewGroup.LayoutParams original = host.getLayoutParams();
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        Button entry = new Button(activity);
        entry.setText("Fine equalizer");
        entry.setAllCaps(false);
        EqualizerStyle style = new EqualizerStyle(activity);
        entry.setTextColor(style.buttonText);
        entry.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP);
        entry.setBackground(
                new RippleDrawable(ColorStateList.valueOf(RIPPLE), new ColorDrawable(style.background), null));
        entry.setVisibility(View.GONE);
        content.removeView(host);
        wrapper.addView(host, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout entries = new LinearLayout(activity);
        entries.addView(entry, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button remote = new Button(activity);
        remote.setText("Remote controls");
        remote.setAllCaps(false);
        remote.setTextColor(style.buttonText);
        remote.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP);
        remote.setBackground(
                new RippleDrawable(ColorStateList.valueOf(RIPPLE), new ColorDrawable(style.background), null));
        remote.setVisibility(View.GONE);
        entries.addView(remote, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        wrapper.addView(entries, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(wrapper, original);
        FineEqualizer controller = new FineEqualizer(activity, entry);
        controller.remoteEntry = remote;
        remote.setOnClickListener((view) -> controller.open(true));
        current = new WeakReference<>(controller);
        activity.getApplication().registerActivityLifecycleCallbacks(controller);
    }

    public static void navigation(String channel, ByteBuffer message) {
        if (!"flutter/navigation".equals(channel) || message == null || message.remaining() > MAX_NAVIGATION_BYTES) {
            return;
        }
        ByteBuffer copy = message.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        try {
            JSONObject envelope = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if (!"routeInformationUpdated".equals(envelope.optString("method"))) {
                return;
            }
            JSONObject arguments = envelope.optJSONObject("args");
            boolean visible = arguments != null && "/device-detail".equals(arguments.optString("uri"));
            MAIN.post(() -> {
                FineEqualizer controller = current.get();
                if (controller != null) {
                    controller.entry.setVisibility(visible ? View.VISIBLE : View.GONE);
                    controller.remoteEntry.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            });
        } catch (JSONException ex) {
            return;
        }
    }

    private Button button(String title) {
        Button button = new Button(this.activity);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextColor(new ColorStateList(new int[][] { new int[] { -android.R.attr.state_enabled }, new int[0] },
                new int[] { (this.style.buttonText & RGB_MASK) | DISABLED_ALPHA, this.style.buttonText }));
        button.setBackground(
                new RippleDrawable(ColorStateList.valueOf(RIPPLE), new ColorDrawable(this.style.background), null));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP);
        button.setMinHeight(dp(ROW_HEIGHT_DP));
        return button;
    }

    private void open(boolean grouped) {
        if (this.dialog != null || this.activity.isFinishing()) {
            return;
        }
        EqualizerBases.configure(this.activity);
        this.grouped = grouped;
        this.presets = null;
        this.stagedPreset = null;
        final int opening = ++this.generation;
        this.connection = EqualizerConnection.connection();
        this.readFailed = false;
        LinearLayout content = new LinearLayout(this.activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(INSET_DP), 0, dp(INSET_DP), 0);
        content.setBackgroundColor(this.style.background);
        String title = grouped ? "Remote controls" : "Fine equalizer";
        TextView heading = new TextView(this.activity);
        heading.setText(title);
        heading.setTextColor(this.style.text);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP);
        heading.setPadding(0, dp(INSET_DP), 0, dp(INSET_DP));
        content.addView(heading);
        this.status = new TextView(this.activity);
        this.status.setTextColor(this.style.text);
        this.status.setText("Reading equalizer…");
        content.addView(this.status);
        this.bands = new LinearLayout(this.activity);
        this.bands.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this.activity);
        scroll.addView(this.bands);
        content.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout actions = new LinearLayout(this.activity);
        this.apply = button("Apply");
        this.undo = button("Undo");
        this.apply.setEnabled(false);
        this.undo.setEnabled(false);
        this.apply.setOnClickListener((view) -> change(this.edited));
        this.undo.setOnClickListener((view) -> change(this.history.undoTarget()));
        actions.addView(this.apply, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(this.undo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(actions);
        if (!grouped && AppliedPatches.localEqualizerPresets()) {
            this.presets = button("Presets");
            this.presets.setEnabled(false);
            this.presets.setOnClickListener((view) -> LocalPresets.open(this.activity, this.edited.curve, (preset) -> {
                if (this.dialog == null || opening != this.generation) {
                    return;
                }
                this.stagedPreset = preset;
                SoundState state = EqualizerBases.playable(preset.state);
                this.edited = (state.xupmix < 0) ? state.withXupmix(this.history.current().xupmix) : state;
                render();
                this.status.setText("Preset staged. Apply to confirm.");
            }));
            content.addView(this.presets);
        }
        Dialog editor = new Dialog(this.activity, this.style.dialogTheme);
        editor.requestWindowFeature(Window.FEATURE_NO_TITLE);
        editor.setTitle(title);
        this.close = button("Close");
        this.close.setMinimumWidth(dp(ROW_HEIGHT_DP));
        this.close.setOnClickListener((view) -> editor.dismiss());
        LinearLayout footer = new LinearLayout(this.activity);
        footer.setGravity(Gravity.END);
        footer.addView(this.close);
        content.addView(footer);
        editor.setContentView(content);
        this.dialog = editor;
        this.dialog.setOnDismissListener((ignored) -> {
            this.dialog = null;
            this.generation++;
            LocalPresets.close(this.activity);
        });
        this.dialog.show();
        Window window = this.dialog.getWindow();
        Drawable background = window.getDecorView().getBackground();
        if (background == null) {
            background = new ColorDrawable(this.style.background);
        } else {
            background = background.mutate();
            background.setTint(this.style.background);
        }
        window.setBackgroundDrawable(background);
        window.getDecorView().setClipToOutline(true);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                grouped ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT);
        EqualizerConnection.readState((state) -> {
            if (this.dialog == null || opening != this.generation) {
                return;
            }
            if (state == null || this.connection != EqualizerConnection.connection()) {
                this.status.setText("Equalizer read failed. Close and try again.");
                return;
            }
            this.history = new EqualizerHistory(state);
            this.edited = state;
            render();
        });
    }

    private void render() {
        this.bands.removeAllViews();
        if (this.grouped) {
            for (EqualizerGroup group : EqualizerGroup.values()) {
                LinearLayout row = new LinearLayout(this.activity);
                row.setGravity(Gravity.CENTER_VERTICAL);
                TextView label = new TextView(this.activity);
                label.setText(group.values(this.edited.curve));
                label.setTextColor(this.style.text);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP);
                row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                Button minus = button("−");
                Button plus = button("+");
                minus.setContentDescription(group.title + " decrease 0.1 dB");
                plus.setContentDescription(group.title + " increase 0.1 dB");
                minus.setEnabled(!this.busy && !this.readFailed && group.canAdjust(this.edited.curve, -1));
                plus.setEnabled(!this.busy && !this.readFailed && group.canAdjust(this.edited.curve, 1));
                minus.setOnClickListener((view) -> editGroup(group, -1));
                plus.setOnClickListener((view) -> editGroup(group, 1));
                row.addView(minus, new LinearLayout.LayoutParams(dp(ROW_HEIGHT_DP), dp(ROW_HEIGHT_DP)));
                row.addView(plus, new LinearLayout.LayoutParams(dp(ROW_HEIGHT_DP), dp(ROW_HEIGHT_DP)));
                this.bands.addView(row);
            }
        } else {
            for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
                final int index = band;
                LinearLayout row = new LinearLayout(this.activity);
                row.setGravity(Gravity.CENTER_VERTICAL);
                TextView label = new TextView(this.activity);
                label.setText(BANDS[band] + "  "
                        + String.format(Locale.ROOT, "%+.1f dB", this.edited.curve.gain(band) / 10.0));
                label.setTextColor(this.style.text);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP);
                row.addView(label, new LinearLayout.LayoutParams(0, dp(ROW_HEIGHT_DP), 1f));
                Button minus = button("−");
                Button plus = button("+");
                minus.setContentDescription(BANDS[band] + " decrease 0.1 dB");
                plus.setContentDescription(BANDS[band] + " increase 0.1 dB");
                minus.setEnabled(!this.busy && !this.readFailed
                        && this.edited.curve.gain(band) > EqualizerCurve.MIN_GAIN_TENTHS);
                plus.setEnabled(!this.busy && !this.readFailed
                        && this.edited.curve.gain(band) < EqualizerCurve.MAX_GAIN_TENTHS);
                minus.setOnClickListener((view) -> edit(index, -1));
                plus.setOnClickListener((view) -> edit(index, 1));
                row.addView(minus, new LinearLayout.LayoutParams(dp(ROW_HEIGHT_DP), dp(ROW_HEIGHT_DP)));
                row.addView(plus, new LinearLayout.LayoutParams(dp(ROW_HEIGHT_DP), dp(ROW_HEIGHT_DP)));
                this.bands.addView(row);
            }
        }
        if (this.presets != null) {
            this.presets.setEnabled(!this.busy && !this.readFailed);
        }
        this.apply.setEnabled(!this.busy && !this.readFailed
                && (this.stagedPreset != null || !this.edited.equals(this.history.current())));
        this.undo.setEnabled(!this.busy && !this.readFailed && this.history.canUndo());
        this.status.setText(this.busy ? "Waiting for equalizer readback…" : "0.1 dB steps (−6 to +6 dB)");
    }

    private void editGroup(EqualizerGroup group, int direction) {
        this.edited = this.edited.withCurve(group.adjust(this.edited.curve, direction));
        render();
    }

    private void edit(int band, int direction) {
        this.edited = this.edited.withCurve(this.edited.curve.withGain(band, this.edited.curve.gain(band) + direction));
        render();
    }

    private void change(SoundState state) {
        this.busy = true;
        this.history.request(state);
        render();
        this.dialog.setCancelable(false);
        this.close.setEnabled(false);
        final int changing = this.generation;
        EqualizerConnection.readState((before) -> {
            if (this.activity.isDestroyed() || changing != this.generation || this.dialog == null) {
                this.busy = false;
                return;
            }
            synchronized (EqualizerConnection.class) {
                if (before == null || this.connection != EqualizerConnection.connection()
                        || !before.equals(this.history.current())) {
                    completeChange(null);
                    return;
                }
                EqualizerConnection.applyState(before, state, (readback) -> completeChange(
                        (this.connection == EqualizerConnection.connection()) ? readback : null));
            }
        });
    }

    private void completeChange(SoundState readback) {
        this.busy = false;
        boolean confirmed = readback != null && this.history.confirm(readback);
        if (!confirmed) {
            this.readFailed = true;
            this.history.failed();
        } else {
            this.edited = readback;
        }
        boolean selectionSaved = true;
        if (confirmed && this.stagedPreset != null) {
            try {
                EqualizerPreset preset = this.stagedPreset;
                boolean matches = preset.state.curve.equals(readback.curve)
                        && (preset.state.xupmix < 0 || preset.state.xupmix == readback.xupmix);
                new EqualizerPresetStore(this.activity).setCurrent(matches ? preset.id : null);
                this.stagedPreset = null;
            } catch (IOException | JSONException ex) {
                selectionSaved = false;
            }
        }
        if (this.dialog != null) {
            render();
            this.dialog.setCancelable(true);
            this.close.setEnabled(true);
            this.status
                .setText(confirmed ? "Equalizer confirmed" : "Change not confirmed. Close and read equalizer again.");
            if (confirmed && !selectionSaved) {
                this.status.setText("Equalizer confirmed. Preset selection could not be saved.");
            }
            if (!confirmed) {
                this.apply.setEnabled(false);
                this.undo.setEnabled(false);
                this.bands.setEnabled(false);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * this.activity.getResources().getDisplayMetrics().density);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity == this.activity && this.dialog != null && !this.busy && !LocalPresets.isOpen(activity)) {
            this.dialog.dismiss();
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity == this.activity) {
            this.generation++;
            if (this.dialog != null) {
                this.dialog.dismiss();
            }
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
            if (current.get() == this) {
                current = new WeakReference<>(null);
            }
        }
    }

}
