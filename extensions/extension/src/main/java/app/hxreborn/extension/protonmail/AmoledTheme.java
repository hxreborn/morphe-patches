/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import static java.nio.charset.StandardCharsets.US_ASCII;

import android.webkit.WebView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import app.hxreborn.extension.WebAssets;

public final class AmoledTheme {
    private static final String PROTON_DARK_BACKGROUND = "#191927";
    private static final String AMOLED_BACKGROUND = "#000000";
    private static final byte[] PROTON_DARK_BACKGROUND_BYTES =
            PROTON_DARK_BACKGROUND.getBytes(US_ASCII);
    private static final byte[] AMOLED_BACKGROUND_BYTES =
            AMOLED_BACKGROUND.getBytes(US_ASCII);

    private AmoledTheme() {}

    private static final String KEY = "amoled_dark_theme";
    private static final long BLACK = 0xFF000000L;
    private static final long PACKED_BLACK = BLACK << 32;
    private static final long PACKED_SURFACE = 0xFF2B2B38L << 32;
    private static final int DARK_CHANNEL_SUM = 3 * 0x80;

    public static boolean isPatched() {
        return false;
    }

    public static boolean isEnabled() {
        return PatchSettings.isFeatureEnabled(isPatched(), KEY);
    }

    static void setEnabled(boolean enabled) {
        PatchSettings.setEnabled(KEY, enabled);
    }

    public static long background(long original) {
        return isEnabled() ? BLACK : original;
    }

    public static long packedBackground(long original) {
        return isEnabled() && isDark(original) ? PACKED_BLACK : original;
    }

    public static long packedSurface(long original) {
        return isEnabled() && isDark(original) ? PACKED_SURFACE : original;
    }

    private static boolean isDark(long packedColor) {
        final int argb = (int) (packedColor >>> 32);
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF) < DARK_CHANNEL_SUM;
    }

    static void injectSettingsWebViewStyle(WebView view) {
        if (!isEnabled()) return;
        view.evaluateJavascript(WebAssets.AMOLED_WEBVIEW, null);
    }

    public static String replaceBackground(String html) {
        if (!isEnabled()) return html;
        return html == null
                ? null
                : html.replace(PROTON_DARK_BACKGROUND, AMOLED_BACKGROUND);
    }

    public static InputStream replaceBackground(InputStream input) {
        if (!isEnabled()) return input;
        return input == null ? null : new BackgroundReplacingInputStream(input);
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];

        for (int bytesRead; (bytesRead = input.read(buffer)) != -1; ) {
            output.write(buffer, 0, bytesRead);
        }

        return output.toByteArray();
    }

    private static void replaceInPlace(byte[] content, byte[] target, byte[] replacement) {
        if (target.length != replacement.length) {
            throw new IllegalArgumentException(
                    "In-place replacement requires equal-length values"
            );
        }

        for (int offset = 0; offset <= content.length - target.length; ) {
            if (matchesAt(content, target, offset)) {
                System.arraycopy(replacement, 0, content, offset, replacement.length);
                offset += target.length;
            } else {
                offset++;
            }
        }
    }

    private static boolean matchesAt(byte[] content, byte[] target, int offset) {
        for (int index = 0; index < target.length; index++) {
            if (content[offset + index] != target[index]) {
                return false;
            }
        }

        return true;
    }

    private static final class BackgroundReplacingInputStream extends InputStream {
        private final InputStream source;
        private ByteArrayInputStream replacement;

        private BackgroundReplacingInputStream(InputStream source) {
            this.source = source;
        }

        @Override
        public int read() throws IOException {
            return replacement().read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return replacement().read(buffer, offset, length);
        }

        @Override
        public long skip(long byteCount) throws IOException {
            return replacement().skip(byteCount);
        }

        @Override
        public int available() throws IOException {
            return replacement().available();
        }

        @Override
        public void close() throws IOException {
            source.close();
        }

        private ByteArrayInputStream replacement() throws IOException {
            if (replacement == null) {
                byte[] content = AmoledTheme.readAllBytes(source);
                replaceInPlace(
                        content,
                        PROTON_DARK_BACKGROUND_BYTES,
                        AMOLED_BACKGROUND_BYTES
                );
                replacement = new ByteArrayInputStream(content);
            }

            return replacement;
        }
    }
}
