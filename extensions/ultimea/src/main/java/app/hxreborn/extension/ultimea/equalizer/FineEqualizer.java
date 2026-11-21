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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

import app.hxreborn.extension.shared.ActivityCallbacksAdapter;
import app.hxreborn.extension.ultimea.AppliedPatches;
import app.hxreborn.extension.ultimea.EqualizerStyle;
import app.hxreborn.extension.ultimea.HostStyle;

@SuppressWarnings("unused")
public final class FineEqualizer extends ActivityCallbacksAdapter {

    private static final int MAX_NAVIGATION_BYTES = 8192;

    private static final int MODE_COLUMNS = 3;

    private static final int PRESET_COLUMNS = 2;

    private static final int MODE_HEIGHT_DP = 96;

    private static final int KILOHERTZ = 1000;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static WeakReference<FineEqualizer> current = new WeakReference<>(null);

    private static EqualizerSession savingSession;

    private static WeakReference<FineEqualizer> waitingScreen = new WeakReference<>(null);

    private static boolean waitingRemote;

    private static boolean reopenOnAttach;

    private static boolean reopenRemote;

    private static EqualizerCurve reopenCurve;

    private final Activity activity;

    private final HostViews views;

    private final LinearLayout remoteEntry;

    private Dialog dialog;

    private EqualizerSession session;

    private EqualizerWrites writes;

    private EqualizerGraph graph;

    private LinearLayout body;

    private LinearLayout presets;

    private TextView status;

    private TextView readout;

    private Button undo;

    private Button more;

    private Button defaultTab;

    private Button customTab;

    private Button xupmix;

    private Button customize;

    private Button decrease;

    private Button increase;

    private boolean custom;

    private boolean remote;

    private boolean initialTabChosen;

    private boolean retryAfterRead;

    private String presetKey;

    private String pendingPresetId;

    private boolean pendingPresetMenu;

    private EqualizerCurve lastCurve;

    private AlertDialog childDialog;

    private PopupMenu activeMenu;

    private FineEqualizer(Activity activity, LinearLayout entry) {
        this.activity = activity;
        this.views = new HostViews(activity);
        this.remoteEntry = entry;
    }

    @SuppressWarnings("deprecation")
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
        HostViews views = new HostViews(activity);
        LinearLayout wrapper = views.column();
        wrapper.setBackgroundColor(views.style.background);
        LinearLayout entry = RemoteControls.card(views);
        entry.setVisibility(View.GONE);
        content.removeView(host);
        wrapper.addView(host, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout inset = views.column();
        inset.setVisibility(View.GONE);
        inset.setPadding(views.dp(HostStyle.INSET_DP), 0, views.dp(HostStyle.INSET_DP), views.dp(HostStyle.GAP_DP));
        wrapper.setOnApplyWindowInsetsListener((view, insets) -> {
            inset.setPadding(views.dp(HostStyle.INSET_DP) + insets.getSystemWindowInsetLeft(), 0,
                    views.dp(HostStyle.INSET_DP) + insets.getSystemWindowInsetRight(),
                    views.dp(HostStyle.GAP_DP) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        inset.addView(entry);
        wrapper.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int height = Math.round((right - left) * HostStyle.POWER_TILE_HEIGHT_RATIO);
            if (height > 0 && entry.getLayoutParams().height != height) {
                entry.post(() -> {
                    ViewGroup.LayoutParams layout = entry.getLayoutParams();
                    if (layout.height != height) {
                        layout.height = height;
                        entry.setLayoutParams(layout);
                    }
                });
            }
        });
        wrapper.addView(inset);
        content.addView(wrapper, original);
        wrapper.requestApplyInsets();
        FineEqualizer controller = new FineEqualizer(activity, entry);
        entry.setOnClickListener((view) -> controller.openScreen(true));
        current = new WeakReference<>(controller);
        if (reopenOnAttach) {
            reopenOnAttach = false;
            controller.lastCurve = reopenCurve;
            reopenCurve = null;
            MAIN.post(() -> controller.openScreen(reopenRemote));
        }
        activity.getApplication().registerActivityLifecycleCallbacks(controller);
    }

    public static void open(Activity activity) {
        attach(activity);
        FineEqualizer controller = current.get();
        if (controller != null && controller.activity == activity) {
            controller.openScreen(false);
        }
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
            JSONObject args = envelope.optJSONObject("args");
            if (args == null) {
                return;
            }
            String route = args.optString("uri", args.optString("location", args.optString("routeName")));
            boolean visible = "/device-detail".equals(android.net.Uri.parse(route).getPath());
            MAIN.post(() -> {
                FineEqualizer owner = current.get();
                if (owner != null) {
                    owner.remoteEntry.setVisibility(visible ? View.VISIBLE : View.GONE);
                    ((View) owner.remoteEntry.getParent()).setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            });
        } catch (JSONException ex) {
            android.util.Log.w("hxUltimea", "Equalizer route message could not be decoded");
        }
    }

    static void connectionChanged() {
        FineEqualizer owner = current.get();
        if (owner != null && owner.dialog != null && EqualizerConnection.connection() == null) {
            owner.writes.pause();
            owner.session.disconnected();
        }
    }

    static void refreshPresets(Activity activity) {
        FineEqualizer owner = current.get();
        if (owner != null && owner.activity == activity && owner.session != null) {
            owner.session.refreshPresets();
        }
    }

    @SuppressWarnings({ "deprecation", "DataFlowIssue" })
    private void openScreen(boolean remote) {
        if (this.dialog != null || this.activity.isFinishing() || this.activity.isDestroyed()) {
            return;
        }
        if (savingSession != null) {
            waitingScreen = new WeakReference<>(this);
            waitingRemote = remote;
            return;
        }
        if (waitingScreen.get() == this) {
            waitingScreen = new WeakReference<>(null);
        }
        this.readout = null;
        this.decrease = null;
        this.increase = null;
        this.xupmix = null;
        this.customize = null;
        this.defaultTab = null;
        this.customTab = null;
        this.remote = remote;
        this.initialTabChosen = false;
        this.custom = remote || this.lastCurve != null && this.lastCurve.mode() >= EqualizerCurve.CUSTOM_MODE;
        this.presetKey = null;
        this.pendingPresetId = null;
        this.session = new EqualizerSession(this.activity, AppliedPatches.localEqualizerPresets(), this::render);
        this.writes = new EqualizerWrites(this.session, () -> render(this.session.state()));
        LinearLayout screen = this.views.column();
        screen.setBackgroundColor(this.views.style.background);
        LinearLayout header = this.views.row();
        Button back = this.views.icon("Back", EqualizerIcon.Glyph.PREVIOUS);
        back.setOnClickListener((view) -> this.dialog.dismiss());
        header.addView(back, target());
        TextView title = this.views.heading(remote ? "Remote controls" : "Fine equalizer");
        title.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(title, new LinearLayout.LayoutParams(0, this.views.dp(HostStyle.TOUCH_DP), 1f));
        this.undo = this.views.icon("Undo", EqualizerIcon.Glyph.UNDO);
        this.undo.setOnClickListener((view) -> this.session.undo());
        header.addView(this.undo, target());
        this.more = this.views.icon("More options", EqualizerIcon.Glyph.MORE);
        this.more.setOnClickListener((view) -> showMenu());
        header.addView(this.more, target());
        screen.addView(header);
        ScrollView scroll = new ScrollView(this.activity);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        LinearLayout content = this.views.column();
        content.setPadding(this.views.dp(HostStyle.INSET_DP), this.views.dp(HostStyle.GAP_DP),
                this.views.dp(HostStyle.INSET_DP), this.views.dp(HostStyle.GAP_DP));
        scroll.addView(content,
                new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        if (!remote) {
            LinearLayout audio = this.views.card();
            TextView audioTitle = this.views.heading("Audio Settings");
            audioTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, HostStyle.CARD_TITLE_SP);
            audio.addView(audioTitle);
            this.xupmix = this.views.button("Xupmix");
            this.xupmix.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            this.xupmix.setPadding(this.views.dp(HostStyle.GAP_DP), 0, this.views.dp(HostStyle.GAP_DP), 0);
            this.xupmix.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            EqualizerIcon chevron = new EqualizerIcon(EqualizerIcon.Glyph.DOWN, this.views.style.accent);
            chevron.setBounds(0, 0, this.views.dp(HostStyle.TITLE_SP), this.views.dp(HostStyle.TITLE_SP));
            this.xupmix.setCompoundDrawablesRelative(null, null, chevron, null);
            this.xupmix.setBackground(this.views.surface(this.views.style.inactiveCard, HostStyle.CARD_RADIUS_DP));
            this.xupmix.setOnClickListener((view) -> showXupmix());
            audio.addView(this.xupmix);
            this.views.addCard(content, audio);
        }
        LinearLayout card = this.views.card();
        if (!remote) {
            LinearLayout tabs = this.views.row();
            this.defaultTab = this.views.button("Default");
            this.customTab = this.views.button("Custom");
            this.defaultTab.setOnClickListener((view) -> setCustom(false));
            this.customTab.setOnClickListener((view) -> setCustom(true));
            for (Button tab : new Button[] { this.defaultTab, this.customTab }) {
                tab.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, HostStyle.TAB_SP);
                tabs.addView(tab, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
            card.addView(tabs);
        }
        this.body = this.views.column();
        card.addView(this.body);
        this.views.addCard(content, card);
        rebuildBody();
        if (remote) {
            RemoteControls.addRows(content, this.views, (group, direction) -> this.writes.group(group, direction));
        }
        this.dialog = new Dialog(this.activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.dialog.setContentView(screen);
        this.dialog.setOnDismissListener((ignored) -> {
            this.pendingPresetId = null;
            if (this.activeMenu != null) {
                this.activeMenu.dismiss();
            }
            if (this.childDialog != null) {
                this.childDialog.dismiss();
            }
            this.writes.flush();
            this.writes.close();
            this.dialog = null;
            EqualizerSession closing = this.session;
            savingSession = closing;
            closing.closeAfterWrites(() -> {
                if (savingSession == closing) {
                    savingSession = null;
                }
                FineEqualizer waiting = waitingScreen.get();
                boolean remoteScreen = waitingRemote;
                if (waiting != null && !waiting.activity.isDestroyed()) {
                    MAIN.post(() -> {
                        if (waitingScreen.get() == waiting) {
                            waiting.openScreen(remoteScreen);
                        }
                    });
                } else {
                    waitingScreen = new WeakReference<>(null);
                }
            });
        });
        screen.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        this.dialog.show();
        this.dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.dialog.getWindow().setStatusBarColor(this.views.style.background);
        this.dialog.getWindow().setNavigationBarColor(this.views.style.background);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            this.dialog.getWindow().setDecorFitsSystemWindows(false);
        }
        int systemBars = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (!this.views.style.dark) {
            systemBars |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (!this.views.style.dark && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            systemBars |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        this.dialog.getWindow().getDecorView().setSystemUiVisibility(systemBars);
        this.session.read();
    }

    private LinearLayout.LayoutParams target() {
        return new LinearLayout.LayoutParams(this.views.dp(HostStyle.TOUCH_DP), this.views.dp(HostStyle.TOUCH_DP));
    }

    private void setCustom(boolean custom) {
        this.pendingPresetId = null;
        this.writes.flush();
        this.custom = custom;
        this.presetKey = null;
        rebuildBody();
        render(this.session.state());
    }

    @SuppressLint("DrawAllocation")
    private void rebuildBody() {
        this.body.removeAllViews();
        this.readout = null;
        this.decrease = null;
        this.increase = null;
        this.customize = null;
        this.status = this.views.text("Reading soundbar…", HostStyle.CAPTION_SP);
        this.status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        this.status.setPadding(0, this.views.dp(HostStyle.GAP_DP / 2f), 0, this.views.dp(HostStyle.GAP_DP / 2f));
        this.status.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        this.status.setTextColor(this.views.style.secondaryText);
        this.status.setOnClickListener((view) -> {
            this.retryAfterRead = this.writes.pending();
            this.session.retry();
        });
        this.body.addView(this.status);
        this.presets = this.views.column();
        if (!this.custom && !this.remote) {
            this.body.addView(this.presets);
        }
        this.graph = new EqualizerGraph(this.activity, this.views.style, new EqualizerBandListener() {
            @Override
            public void onSelected(int band) {
                renderBand();
            }

            @Override
            public void onStarted() {
                FineEqualizer.this.writes.begin();
            }

            @Override
            public void onChanged(int band, int gain) {
                FineEqualizer.this.writes.setGain(band, gain);
            }

            @Override
            public void onFinished() {
                FineEqualizer.this.writes.release();
            }
        });
        this.body.addView(this.graph, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                this.views.dp(HostStyle.GRAPH_HEIGHT_DP)));
        if (this.custom && !this.remote) {
            LinearLayout selection = this.views.row();
            Button previous = this.views.icon("Previous band", EqualizerIcon.Glyph.PREVIOUS);
            previous.setOnClickListener((view) -> this.graph.select(this.graph.selectedBand() - 1));
            selection.addView(previous, target());
            this.readout = this.views.text("", HostStyle.CAPTION_SP);
            this.readout.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            int valueWidth = (int) Math
                .ceil(android.text.Layout.getDesiredWidth(bandReadout("16000 Hz", "−6.0"), this.readout.getPaint()));
            selection.addView(this.readout,
                    new LinearLayout.LayoutParams(valueWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            Button next = this.views.icon("Next band", EqualizerIcon.Glyph.NEXT);
            next.setOnClickListener((view) -> this.graph.select(this.graph.selectedBand() + 1));
            selection.addView(next, target());
            LinearLayout adjustment = this.views.row();
            this.decrease = this.views.pill("−");
            this.increase = this.views.pill("+");
            this.views.repeat(this.decrease, () -> this.writes.step(this.graph.selectedBand(), -1));
            this.views.repeat(this.increase, () -> this.writes.step(this.graph.selectedBand(), 1));
            adjustment.addView(this.decrease, new LinearLayout.LayoutParams(0, this.views.dp(HostStyle.TOUCH_DP), 1f));
            LinearLayout.LayoutParams increaseTarget = new LinearLayout.LayoutParams(0,
                    this.views.dp(HostStyle.TOUCH_DP), 1f);
            increaseTarget.setMarginStart(this.views.dp(HostStyle.PILL_GAP_DP));
            adjustment.addView(this.increase, increaseTarget);
            int minimumWidth = valueWidth + this.views.dp(HostStyle.TOUCH_DP * 4 + HostStyle.PILL_GAP_DP);
            LinearLayout readout = new LinearLayout(this.activity) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    boolean stacked = MeasureSpec.getSize(widthMeasureSpec) < minimumWidth;
                    int orientation = stacked ? VERTICAL : HORIZONTAL;
                    if (getOrientation() != orientation) {
                        setOrientation(orientation);
                        adjustment.setLayoutParams(
                                new LinearLayout.LayoutParams(stacked ? ViewGroup.LayoutParams.MATCH_PARENT : 0,
                                        ViewGroup.LayoutParams.WRAP_CONTENT, stacked ? 0f : 1f));
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
            };
            readout.setGravity(Gravity.CENTER_VERTICAL);
            readout.addView(selection, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            readout.addView(adjustment, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            this.body.addView(readout);
            this.body.addView(this.presets);
        } else if (!this.remote) {
            this.customize = this.views.button("Customize current mode");
            this.customize.setOnClickListener((view) -> setCustom(true));
            this.body.addView(this.customize);
        }
    }

    private void render(EqualizerSession.State state) {
        if (this.graph == null) {
            return;
        }
        if (this.retryAfterRead && state.canEdit && !state.busy && !state.reading) {
            this.retryAfterRead = false;
            this.writes.flush();
            return;
        }
        if (!this.remote && !this.initialTabChosen && state.confirmed != null) {
            this.initialTabChosen = true;
            this.custom = state.confirmed.curve.mode() >= EqualizerCurve.CUSTOM_MODE;
            rebuildBody();
        }
        if (!this.remote && this.defaultTab != null) {
            this.views.tab(this.defaultTab, !this.custom);
            this.views.tab(this.customTab, this.custom);
        }
        EqualizerCurve curve = this.writes.curve();
        if (curve != null) {
            this.lastCurve = curve;
        }
        EqualizerCurve displayed = this.lastCurve;
        if (!this.custom && !this.remote) {
            EqualizerBases.Entry base = (curve != null) ? EqualizerBases.mode(curve.mode()) : null;
            displayed = (base != null) ? base.curve : null;
        }
        this.graph.setVisibility((displayed != null) ? View.VISIBLE : View.GONE);
        boolean presetRequired = state.presetsEnabled && state.selected == null;
        this.graph.show(displayed, state.canEdit && this.custom && !this.remote && !presetRequired);
        this.graph.setAlpha(state.canEdit ? 1f : HostStyle.DISABLED_OPACITY);
        if (this.customize != null) {
            HostViews.setEnabled(this.customize, displayed != null && state.canEdit && !state.busy);
        }
        String save = "Saved";
        if (state.error != null) {
            save = (EqualizerConnection.connection() == null) ? "Soundbar disconnected" : "Not saved. Tap to retry";
        } else if (state.reading || state.confirmed == null) {
            save = "Reading soundbar…";
        } else if (state.busy || this.writes.pending()) {
            save = "Saving…";
        }
        if (this.custom && !this.remote && presetRequired && !state.reading && !state.busy && !this.writes.pending()
                && state.error == null && state.confirmed != null) {
            save = "No preset selected. Select a preset or tap +.";
        }
        this.status.setText(save);
        this.status.setEnabled(state.error != null);
        this.status.setMinHeight((state.error != null) ? this.views.dp(HostStyle.TOUCH_DP) : 0);
        this.status.setVisibility((this.custom || this.remote || state.error != null || state.reading
                || state.confirmed == null || state.busy || this.writes.pending()) ? View.VISIBLE : View.GONE);
        this.undo.setEnabled(this.custom && state.canUndo && !this.writes.pending());
        this.undo.setAlpha(this.undo.isEnabled() ? 1f : HostStyle.DISABLED_OPACITY);
        if (this.xupmix != null) {
            this.xupmix.setText((state.desired != null && state.desired.xupmix == 1) ? "Xupmix ON" : "Xupmix OFF");
            HostViews.setEnabled(this.xupmix, state.canEdit && !state.busy);
        }
        if (this.remote) {
            RemoteControls.setEnabled(this.body.getRootView(), state.canEdit);
        }
        renderPresets(state);
        renderBand();
        finishPresetSelection(state);
    }

    private static android.text.SpannableString bandReadout(String frequency, String gain) {
        android.text.SpannableString value = new android.text.SpannableString(frequency + "\n" + gain + " dB");
        value.setSpan(new android.text.style.RelativeSizeSpan(HostStyle.TITLE_SP / (float) HostStyle.CAPTION_SP),
                frequency.length() + 1, value.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return value;
    }

    private void renderBand() {
        if (this.readout == null || this.writes == null || this.graph == null) {
            return;
        }
        EqualizerCurve curve = this.writes.curve();
        int band = this.graph.selectedBand();
        int frequency = EqualizerCurve.frequency(band);
        String hz = (frequency >= KILOHERTZ) ? frequency / KILOHERTZ + " kHz" : frequency + " Hz";
        this.readout.setText(bandReadout(hz, (curve != null) ? EqualizerGraph.gainLabel(curve.gain(band)) : "0.0"));
        EqualizerSession.State state = this.session.state();
        boolean enabled = state.canEdit && (!state.presetsEnabled || state.selected != null);
        HostViews.setEnabled(this.decrease, enabled);
        HostViews.setEnabled(this.increase, enabled);
        this.decrease.setContentDescription(frequency + " Hz decrease 0.1 dB");
        this.increase.setContentDescription(frequency + " Hz increase 0.1 dB");
    }

    @SuppressWarnings("NullableProblems")
    private void renderPresets(EqualizerSession.State state) {
        StringBuilder key = new StringBuilder().append(this.custom)
            .append(state.canEdit)
            .append(state.busy)
            .append(this.writes.pending());
        if (state.desired != null) {
            key.append(state.desired.curve.mode());
        }
        if (state.selected != null) {
            key.append(state.selected.id);
        }
        for (EqualizerPreset preset : state.presets) {
            key.append(preset.id).append(preset.name);
        }
        if (key.toString().equals(this.presetKey) || this.remote) {
            return;
        }
        this.presetKey = key.toString();
        this.presets.removeAllViews();
        List<Button> cards = new ArrayList<>();
        if (this.custom) {
            for (EqualizerPreset preset : state.presets) {
                Button button = this.views.button(preset.name);
                button.setSingleLine(true);
                button.setEllipsize(TextUtils.TruncateAt.END);
                button.setPadding(this.views.dp(HostStyle.GAP_DP), 0, this.views.dp(HostStyle.GAP_DP), 0);
                button.setTag(preset.id);
                button.setContentDescription(preset.name);
                button.setSelected(state.selected != null && preset.id.equals(state.selected.id));
                button.setOnClickListener((view) -> selectPreset(preset, false));
                button.setOnLongClickListener((view) -> {
                    selectPreset(preset, true);
                    return true;
                });
                button.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(View host,
                            android.view.accessibility.AccessibilityNodeInfo info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        info.removeAction(
                                android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
                        info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(
                                android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK, "Edit preset"));
                    }
                });
                cards.add(button);
            }
            Button add = this.views.button("+");
            add.setContentDescription("New preset from current curve");
            add.setOnClickListener((view) -> {
                if (canSelectPreset()) {
                    this.session.create();
                }
            });
            cards.add(add);
        } else if (EqualizerBases.available()) {
            for (DefaultMode mode : DefaultMode.values()) {
                EqualizerBases.Entry base = EqualizerBases.mode(mode.value);
                if (base == null) {
                    continue;
                }
                Button button = this.views.mode(base.name.toUpperCase(Locale.ROOT), mode.glyph);
                button.setSelected(state.desired != null && state.desired.curve.mode() == mode.value);
                button.setOnClickListener((view) -> this.session.selectBase(mode.value));
                cards.add(button);
            }
        }
        int columns = this.custom ? PRESET_COLUMNS : MODE_COLUMNS;
        LinearLayout row = null;
        for (int index = 0; index < cards.size(); index++) {
            if (index % columns == 0) {
                row = this.views.row();
                this.presets.addView(row);
            }
            Button card = cards.get(index);
            HostViews.setEnabled(card, state.canEdit && !state.busy && !this.writes.pending());
            card.setBackground(this.views.surface(
                    card.isSelected() ? this.views.style.selectedSurface : this.views.style.inactiveCard,
                    HostStyle.CARD_RADIUS_DP));
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f);
            layout.setMargins(this.views.dp(HostStyle.GAP_DP / 2f), this.views.dp(HostStyle.GAP_DP / 2f),
                    this.views.dp(HostStyle.GAP_DP / 2f), this.views.dp(HostStyle.GAP_DP / 2f));
            card.setTextColor((card.isSelected() && this.views.style.dark) ? android.graphics.Color.WHITE
                    : this.views.style.accent);
            card.setMinHeight(this.views.dp(this.custom ? HostStyle.PRESET_HEIGHT_DP : MODE_HEIGHT_DP));
            row.addView(card, layout);
        }
    }

    private void showXupmix() {
        PopupMenu menu = new PopupMenu(new ContextThemeWrapper(this.activity, this.views.style.dialogTheme),
                this.xupmix);
        for (String label : new String[] { "Xupmix ON", "Xupmix OFF" }) {
            menu.getMenu().add(label).setOnMenuItemClickListener((item) -> {
                this.session.setXupmix("Xupmix ON".equals(label));
                return true;
            });
        }
        this.activeMenu = menu;
        menu.show();
    }

    private void rename() {
        EqualizerPreset selected = this.session.state().selected;
        if (selected == null || !canSelectPreset()) {
            feedback("Select a preset before renaming it");
            return;
        }
        EditText input = new EditText(new ContextThemeWrapper(this.activity, this.views.style.dialogTheme));
        input.setSingleLine(true);
        input.setText(selected.name);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(EqualizerPreset.MAX_NAME_LENGTH) });
        EqualizerStyle style = new EqualizerStyle(this.activity);
        style.tint(input);
        LinearLayout content = this.views.column();
        content.setPadding(this.views.dp(HostStyle.DIALOG_INSET_DP), 0, this.views.dp(HostStyle.DIALOG_INSET_DP), 0);
        content.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(this.activity, this.views.style.dialogTheme).setTitle("Rename")
            .setView(content)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .show();
        style.tint(dialog);
        this.childDialog = dialog;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> {
            if (!canEditPreset(selected.id)) {
                return;
            }
            try {
                EqualizerPreset.validName(input.getText().toString());
                this.session.rename(input.getText().toString());
                dialog.dismiss();
            } catch (IllegalArgumentException ex) {
                input.setError(ex.getMessage());
            }
        });
    }

    private boolean canSelectPreset() {
        EqualizerSession.State state = this.session.state();
        String reason = null;
        if (state.closed) {
            reason = "Equalizer is closed. Open it again";
        } else if (!state.presetsEnabled) {
            reason = "Local presets are disabled";
        } else if (state.reading || state.busy || this.writes.pending()) {
            reason = "Wait for the soundbar update to finish";
        } else if (!state.canEdit) {
            reason = (state.error != null) ? state.error : "Read the soundbar before selecting a preset";
        }
        if (reason != null) {
            feedback(reason);
            return false;
        }
        return true;
    }

    private void selectPreset(EqualizerPreset preset, boolean menu) {
        if (!canSelectPreset()) {
            return;
        }
        EqualizerSession.State state = this.session.state();
        if (state.selected != null && preset.id.equals(state.selected.id)) {
            if (menu) {
                showPresetMenuAfterLayout(this.presets.findViewWithTag(preset.id), preset);
            } else {
                feedback("Selected " + preset.name);
                this.session.selectPreset(preset.id);
            }
            return;
        }
        this.pendingPresetId = preset.id;
        this.pendingPresetMenu = menu;
        this.session.selectPreset(preset.id);
    }

    private void finishPresetSelection(EqualizerSession.State state) {
        if (this.pendingPresetId == null || state.busy || state.reading) {
            return;
        }
        if (state.error != null) {
            this.pendingPresetId = null;
            feedback(state.error);
            return;
        }
        if (state.selected == null || !this.pendingPresetId.equals(state.selected.id)
                || !state.confirmed.equals(state.desired)) {
            return;
        }
        String id = this.pendingPresetId;
        this.pendingPresetId = null;
        if (this.pendingPresetMenu) {
            showPresetMenuAfterLayout(this.presets.findViewWithTag(id), state.selected);
        } else {
            feedback("Selected " + state.selected.name);
        }
    }

    private void feedback(String message) {
        android.widget.Toast.makeText(this.activity, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void showPresetMenuAfterLayout(Button anchor, EqualizerPreset preset) {
        if (anchor == null) {
            feedback("Preset no longer exists");
            return;
        }
        if (anchor.isLaidOut()) {
            showPresetMenu(anchor, preset);
            return;
        }
        anchor.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                    int oldRight, int oldBottom) {
                view.removeOnLayoutChangeListener(this);
                showPresetMenu(anchor, preset);
            }
        });
    }

    private boolean canEditPreset(String id) {
        if (!canSelectPreset()) {
            return false;
        }
        EqualizerPreset selected = this.session.state().selected;
        if (selected == null || !id.equals(selected.id)) {
            feedback("Preset selection changed. Open its menu again");
            return false;
        }
        return true;
    }

    private void showPresetMenu(Button anchor, EqualizerPreset preset) {
        if (this.dialog == null || !canSelectPreset()) {
            return;
        }
        EqualizerPreset selected = this.session.state().selected;
        if (selected == null || !preset.id.equals(selected.id)) {
            feedback("Select this preset before editing it");
            return;
        }
        PopupMenu menu = new PopupMenu(new ContextThemeWrapper(this.activity, this.views.style.dialogTheme), anchor);
        menu.getMenu().add("Rename").setOnMenuItemClickListener((item) -> {
            if (canEditPreset(selected.id)) {
                rename();
            }
            return true;
        });
        menu.getMenu().add("Share").setOnMenuItemClickListener((item) -> {
            if (!canEditPreset(selected.id)) {
                return true;
            }
            try {
                PresetShareProvider.share(this.activity, selected);
            } catch (IOException | JSONException ex) {
                feedback("Preset could not be shared");
            }
            return true;
        });
        menu.getMenu().add("Reset to " + selected.baseName).setOnMenuItemClickListener((item) -> {
            if (canEditPreset(selected.id)) {
                this.session.reset();
            }
            return true;
        });
        menu.getMenu().add("Delete").setOnMenuItemClickListener((item) -> {
            if (!canEditPreset(selected.id)) {
                return true;
            }
            AlertDialog confirm = new AlertDialog.Builder(this.activity, this.views.style.dialogTheme)
                .setTitle("Delete " + selected.name + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (canEditPreset(selected.id)) {
                        this.session.delete();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
            new EqualizerStyle(this.activity).tint(confirm);
            this.childDialog = confirm;
            return true;
        });
        this.activeMenu = menu;
        menu.show();
    }

    private void showMenu() {
        PopupMenu menu = new PopupMenu(new ContextThemeWrapper(this.activity, this.views.style.dialogTheme), this.more);
        if (this.session.state().presetsEnabled) {
            menu.getMenu().add("Import").setOnMenuItemClickListener((item) -> {
                LocalPresets.importPreset(this.activity);
                return true;
            });
        }
        this.activeMenu = menu;
        menu.show();
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity == this.activity) {
            boolean waiting = waitingScreen.get() == this;
            boolean reopen = (this.dialog != null || waiting) && !activity.isFinishing();
            boolean remoteScreen = waiting ? waitingRemote : this.remote;
            if (waiting) {
                waitingScreen = new WeakReference<>(null);
            }
            EqualizerCurve curve = (this.writes != null) ? this.writes.curve() : this.lastCurve;
            if (this.dialog != null) {
                this.dialog.dismiss();
            }
            if (reopen) {
                reopenOnAttach = true;
                reopenRemote = remoteScreen;
                reopenCurve = curve;
            }
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
            if (current.get() == this) {
                current = new WeakReference<>(null);
            }
        }
    }

    private enum DefaultMode {

        MOVIE(1, EqualizerIcon.Glyph.MOVIE), MUSIC(2, EqualizerIcon.Glyph.MUSIC), VOICE(3, EqualizerIcon.Glyph.VOICE),
        SPORT(4, EqualizerIcon.Glyph.SPORT), GAME(6, EqualizerIcon.Glyph.GAME), NIGHT(5, EqualizerIcon.Glyph.NIGHT);

        private final int value;

        private final EqualizerIcon.Glyph glyph;

        DefaultMode(int value, EqualizerIcon.Glyph glyph) {
            this.value = value;
            this.glyph = glyph;
        }

    }

}
