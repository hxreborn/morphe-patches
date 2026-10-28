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
package app.hxreborn.extension.protonmail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import app.hxreborn.extension.proton.AmoledTheme;
import app.hxreborn.extension.shared.Streams;

@SuppressWarnings("unused")
public final class WebContentBackground {

    private static final String PROTON_DARK_BACKGROUND = "#191927";

    private static final String AMOLED_BACKGROUND = "#000000";

    private static final byte[] PROTON_DARK_BACKGROUND_BYTES = PROTON_DARK_BACKGROUND
        .getBytes(StandardCharsets.US_ASCII);

    private static final byte[] AMOLED_BACKGROUND_BYTES = AMOLED_BACKGROUND.getBytes(StandardCharsets.US_ASCII);

    private WebContentBackground() {

    }

    public static String replaceBackground(String html) {
        if (!AmoledTheme.isEnabled()) {
            return html;
        }
        return (html != null) ? html.replace(PROTON_DARK_BACKGROUND, AMOLED_BACKGROUND) : null;
    }

    public static InputStream replaceBackground(InputStream input) {
        if (!AmoledTheme.isEnabled()) {
            return input;
        }
        return (input != null) ? new BackgroundReplacingInputStream(input) : null;
    }

    private static void replaceInPlace(byte[] content, byte[] target, byte[] replacement) {
        if (target.length != replacement.length) {
            throw new IllegalArgumentException("In-place replacement requires equal-length values");
        }

        for (int offset = 0; offset <= content.length - target.length;) {
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
            this.source.close();
        }

        private ByteArrayInputStream replacement() throws IOException {
            if (this.replacement == null) {
                byte[] content = Streams.readAll(this.source);
                replaceInPlace(content, PROTON_DARK_BACKGROUND_BYTES, AMOLED_BACKGROUND_BYTES);
                this.replacement = new ByteArrayInputStream(content);
            }

            return this.replacement;
        }

    }

}
