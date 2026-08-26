/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import android.app.Activity;
import android.os.Build;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class RequestLog {

    private static final float FEATURE_SP = 15f;
    private static final float PATH_SP = 11.5f;
    private static final float VALUE_SP = 10.5f;
    private static final float LABEL_SP = 9.5f;
    private static final float ROW_SP = 10.5f;

    private static final int CARD_PAD_DP = 14;
    private static final int CARD_GAP_DP = 10;
    private static final int BLOCK_GAP_DP = 16;
    private static final int LABEL_COL_DP = 62;
    private static final int TABLE_PAD_DP = 8;
    private static final int TABLE_RADIUS_DP = 8;
    private static final int TOUCH_TARGET_DP = 48;
    private static final int ATTEMPT_HEAD = 3;

    private static final String NO_RESPONSE = "no response";
    private static final String COLLAPSED = ", collapsed";
    private static final String EXPANDED = ", expanded";

    private static final ThreadLocal<SimpleDateFormat> CLOCK = new ThreadLocal<>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        }
    };

    private RequestLog() {
    }

    static String summary() {
        if (!AiTrace.sessions().isEmpty()) {
            return AiTrace.headline();
        }
        if (!ProgressTrace.stages().isEmpty()) {
            return ProgressTrace.headline();
        }
        return AiTrace.headline();
    }

    static void show(Activity activity) {
        LinearLayout body = PatchPanel.logBody(activity);
        List<AiTrace.Session> sessions = AiTrace.sessions();

        if (!sessions.isEmpty()) {
            for (int i = sessions.size() - 1; i >= 0; i--) {
                body.addView(sessionCard(activity, sessions.get(i)));
            }
            PatchPanel.showLog(activity, "AI requests", body);
            return;
        }

        LinearLayout card = card(activity);
        List<String> stages = ProgressTrace.stages();
        if (!stages.isEmpty()) {
            for (String stage : stages) {
                card.addView(PatchPanel.logLine(activity, stage, PatchPanel.FG));
            }
        } else {
            card.addView(PatchPanel.logLine(activity, summary(), PatchPanel.DIM));
        }
        body.addView(card);
        PatchPanel.showLog(activity, "AI requests", body);
    }

    private static LinearLayout card(Activity activity) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(PatchPanel.card(activity));
        int pad = PatchPanel.dp(activity, CARD_PAD_DP);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = PatchPanel.dp(activity, CARD_GAP_DP);
        card.setLayoutParams(params);
        return card;
    }

    private static View sessionCard(Activity activity, AiTrace.Session session) {
        LinearLayout card = card(activity);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView feature = ui(activity, session.feature, PatchPanel.FG, FEATURE_SP);
        feature.setTypeface(Typeface.DEFAULT_BOLD);
        markHeading(feature);
        feature.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(feature);

        int outcome = session.outcomeCode();
        if (outcome != HttpStatus.NONE) {
            header.addView(PatchPanel.chip(activity, statusText(outcome),
                    PatchPanel.statusColour(outcome)));
        }
        card.addView(header);
        card.addView(gap(activity, 8));

        card.addView(field(activity, "started", AiTrace.iso(session.startedAtMs)));
        card.addView(field(activity, "ended", clock(session.endedAtMs())));
        card.addView(field(activity, "elapsed", AiTrace.duration(session.durationMs())));
        card.addView(field(activity, "requests", String.valueOf(session.requestCount())));
        if (!session.postObserved()) {
            card.addView(field(activity, "submit", "not observed"));
        }

        for (AiTrace.Exchange exchange : session.exchanges()) {
            if (exchange.last() == null) {
                continue;
            }
            card.addView(gap(activity, BLOCK_GAP_DP));
            card.addView(exchangeBlock(activity, exchange));
        }
        return card;
    }

    private static View exchangeBlock(Activity activity, AiTrace.Exchange exchange) {
        LinearLayout block = new LinearLayout(activity);
        block.setOrientation(LinearLayout.VERTICAL);

        LinearLayout line = new LinearLayout(activity);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(PatchPanel.chip(activity, exchange.method, PatchPanel.methodColour(exchange.method)));

        TextView path = mono(activity, exchange.path(), PatchPanel.FG, PATH_SP);
        path.setSingleLine(true);
        path.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pathParams.leftMargin = PatchPanel.dp(activity, 8);
        path.setLayoutParams(pathParams);
        line.addView(path);
        block.addView(line);
        block.addView(gap(activity, 6));

        List<AiTrace.Attempt> attempts = exchange.attempts();
        AiTrace.Attempt last = exchange.last();

        if (exchange.dispatchOnly()) {
            block.addView(field(activity, "sent", clock(last.startedAtMs)));
        } else {
            block.addView(field(activity, "attempts", String.valueOf(attempts.size())));
            block.addView(field(activity, "elapsed", AiTrace.duration(exchange.durationMs())));
            block.addView(field(activity, "outcome", tally(attempts)));
            block.addView(gap(activity, 8));
            block.addView(attemptTable(activity, attempts));
        }

        LinearLayout details = detailBlock(activity, exchange);
        details.setVisibility(View.GONE);
        block.addView(disclosure(activity, "Details", details));
        block.addView(details);
        return block;
    }

    private static String tally(List<AiTrace.Attempt> attempts) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < attempts.size() - 1; i++) {
            String status = attempts.get(i).status();
            String key = status.isEmpty() ? NO_RESPONSE : status;
            Integer seen = counts.get(key);
            counts.put(key, seen == null ? 1 : seen + 1);
        }
        StringBuilder line = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (line.length() > 0) {
                line.append(", ");
            }
            line.append(entry.getKey()).append(" \u00d7").append(entry.getValue());
        }
        String closing = attempts.get(attempts.size() - 1).status();
        if (closing.isEmpty()) {
            closing = NO_RESPONSE;
        }
        if (line.length() == 0) {
            return closing;
        }
        return line + " then " + closing;
    }

    private static View attemptTable(Activity activity, List<AiTrace.Attempt> attempts) {
        LinearLayout table = new LinearLayout(activity);
        table.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(PatchPanel.SURFACE);
        surface.setCornerRadius(PatchPanel.dp(activity, TABLE_RADIUS_DP));
        table.setBackground(surface);
        int pad = PatchPanel.dp(activity, TABLE_PAD_DP);
        table.setPadding(pad, pad, pad, pad);

        int total = attempts.size();
        if (total <= ATTEMPT_HEAD + 1) {
            for (int i = 0; i < total; i++) {
                table.addView(attemptRow(activity, i + 1, attempts.get(i)));
            }
            return table;
        }

        for (int i = 0; i < ATTEMPT_HEAD; i++) {
            table.addView(attemptRow(activity, i + 1, attempts.get(i)));
        }

        LinearLayout folded = new LinearLayout(activity);
        folded.setOrientation(LinearLayout.VERTICAL);
        for (int i = ATTEMPT_HEAD; i < total - 1; i++) {
            folded.addView(attemptRow(activity, i + 1, attempts.get(i)));
        }
        folded.setVisibility(View.GONE);
        table.addView(foldRow(activity, total - 1 - ATTEMPT_HEAD, folded));
        table.addView(folded);
        table.addView(attemptRow(activity, total, attempts.get(total - 1)));
        return table;
    }

    private static View attemptRow(Activity activity, int index, AiTrace.Attempt attempt) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padV = PatchPanel.dp(activity, 4);
        row.setPadding(0, padV, 0, padV);

        TextView number = mono(activity, String.valueOf(index), PatchPanel.MUTED, ROW_SP);
        number.setGravity(Gravity.END);
        number.setLayoutParams(new LinearLayout.LayoutParams(
                PatchPanel.dp(activity, 18), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(number);

        TextView at = mono(activity, clock(attempt.startedAtMs), PatchPanel.DIM, ROW_SP);
        at.setSingleLine(true);
        LinearLayout.LayoutParams atParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        atParams.leftMargin = PatchPanel.dp(activity, 10);
        at.setLayoutParams(atParams);
        row.addView(at);

        View chip = PatchPanel.chip(activity,
                attempt.dispatchOnly() ? "—" : attempt.status(),
                PatchPanel.statusColour(attempt.statusCode()));
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipParams.leftMargin = PatchPanel.dp(activity, 10);
        chip.setLayoutParams(chipParams);
        row.addView(chip);

        TextView latency = mono(activity, AiTrace.duration(attempt.durationMs()),
                PatchPanel.MUTED, ROW_SP);
        latency.setGravity(Gravity.END);
        latency.setSingleLine(true);
        latency.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(latency);

        row.setContentDescription("Attempt " + index + ", " + clock(attempt.startedAtMs) + ", "
                + statusText(attempt.statusCode()) + ", " + AiTrace.duration(attempt.durationMs()));
        for (int i = 0; i < row.getChildCount(); i++) {
            row.getChildAt(i).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        return row;
    }

    private static View foldRow(Activity activity, int hidden, View target) {
        String label = hidden + " hidden";
        TextView view = ui(activity, label, PatchPanel.MUTED, VALUE_SP);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(PatchPanel.dp(activity, TOUCH_TARGET_DP));
        view.setBackground(PatchPanel.ripple(activity));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setContentDescription(label + COLLAPSED);
        view.setOnClickListener(v -> {
            boolean open = target.getVisibility() == View.VISIBLE;
            target.setVisibility(open ? View.GONE : View.VISIBLE);
            view.setText(open ? label : "collapse");
            view.setContentDescription(label + (open ? COLLAPSED : EXPANDED));
        });
        return view;
    }

    private static LinearLayout detailBlock(Activity activity, AiTrace.Exchange exchange) {
        LinearLayout details = new LinearLayout(activity);
        details.setOrientation(LinearLayout.VERTICAL);
        AiTrace.Attempt last = exchange.last();

        section(activity, details, "URL");
        details.addView(value(activity, AiTrace.safeUrl(exchange.url)));

        if (!exchange.payload().isEmpty()) {
            section(activity, details, "Payload");
            for (String[] pair : exchange.payload()) {
                details.addView(field(activity, pair[0], pair[1]));
            }
        }

        if (!last.requestHeaders.isEmpty()) {
            section(activity, details, "Request headers");
            for (String[] header : last.requestHeaders) {
                details.addView(field(activity, header[0], header[1]));
            }
        }

        if (!last.dispatchOnly()) {
            section(activity, details, "Response");
            details.addView(field(activity, "status", (last.protocol.isEmpty()
                    ? "" : last.protocol + " ") + last.statusLine()));
            details.addView(field(activity, "received", AiTrace.iso(last.endedAtMs)));
            if (!last.contentType.isEmpty()) {
                details.addView(field(activity, "type", last.contentType));
            }
            if (last.contentLength >= 0) {
                details.addView(field(activity, "bytes", String.valueOf(last.contentLength)));
            }

            if (!last.responseHeaders.isEmpty()) {
                section(activity, details, "Response headers");
                for (String[] header : last.responseHeaders) {
                    details.addView(field(activity, header[0], header[1]));
                }
            }
        }
        return details;
    }

    private static View field(Activity activity, String label, String value) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, PatchPanel.dp(activity, 2), 0, PatchPanel.dp(activity, 2));

        TextView name = ui(activity, label, PatchPanel.MUTED, LABEL_SP);
        name.setLayoutParams(new LinearLayout.LayoutParams(
                PatchPanel.dp(activity, LABEL_COL_DP), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(name);

        TextView text = mono(activity, value, PatchPanel.DIM, VALUE_SP);
        text.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text);

        row.setContentDescription(label + " " + value);
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return row;
    }

    private static View disclosure(Activity activity, String label, View target) {
        TextView view = ui(activity, label.toUpperCase(Locale.US), PatchPanel.DIM, LABEL_SP);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setLetterSpacing(0.1f);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setMinHeight(PatchPanel.dp(activity, TOUCH_TARGET_DP));
        view.setBackground(PatchPanel.ripple(activity));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setContentDescription(label + COLLAPSED);
        view.setOnClickListener(v -> {
            boolean open = target.getVisibility() == View.VISIBLE;
            target.setVisibility(open ? View.GONE : View.VISIBLE);
            view.setText((open ? label : "hide " + label).toUpperCase(Locale.US));
            view.setContentDescription(label + (open ? COLLAPSED : EXPANDED));
        });
        return view;
    }

    private static void section(Activity activity, LinearLayout parent, String label) {
        TextView view = ui(activity, label.toUpperCase(Locale.US), PatchPanel.MUTED, LABEL_SP);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setLetterSpacing(0.1f);
        markHeading(view);
        view.setPadding(0, PatchPanel.dp(activity, 10), 0, PatchPanel.dp(activity, 3));
        parent.addView(view);
    }

    private static void markHeading(TextView view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    private static TextView value(Activity activity, String text) {
        TextView view = mono(activity, text, PatchPanel.DIM, VALUE_SP);
        view.setLineSpacing(PatchPanel.dp(activity, 2), 1f);
        return view;
    }

    private static TextView ui(Activity activity, String text, int colour, float sp) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(colour);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        return view;
    }

    private static TextView mono(Activity activity, String text, int colour, float sp) {
        TextView view = ui(activity, text, colour, sp);
        view.setTypeface(Typeface.MONOSPACE);
        return view;
    }

    private static View gap(Activity activity, int heightDp) {
        View view = new View(activity);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, PatchPanel.dp(activity, heightDp)));
        return view;
    }

    private static String clock(long epochMs) {
        return CLOCK.get().format(new Date(epochMs));
    }

    private static String statusText(int code) {
        if (code == HttpStatus.NONE) {
            return NO_RESPONSE;
        }
        String reason = HttpStatus.reason(code);
        return reason.isEmpty() ? String.valueOf(code) : code + " " + reason;
    }

}
