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
package app.hxreborn.extension.readera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SpeechTextFilter {

    static final String DEFAULT_CONFIGURATION = "【\n】\n〖\n〗\n〈\n〉\n《\n》\n「\n」\n『\n』";

    private final List<Pattern> entries = new ArrayList<>();

    SpeechTextFilter(String configuration) {
        for (String line : configuration.split("\\r?\\n")) {
            String entry = line.trim();
            if (entry.isEmpty()) {
                continue;
            }
            String start = Character.isLetterOrDigit(entry.codePointAt(0)) ? "(?<![\\p{L}\\p{M}\\p{N}_])" : "";
            String end = Character.isLetterOrDigit(entry.codePointBefore(entry.length())) ? "(?![\\p{L}\\p{M}\\p{N}_])"
                    : "";
            this.entries.add(Pattern.compile(start + Pattern.quote(entry) + end,
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }
    }

    String filter(String text) {
        if (text == null || text.isEmpty() || this.entries.isEmpty()) {
            return text;
        }
        char[] spoken = text.toCharArray();
        for (Pattern entry : this.entries) {
            Matcher matcher = entry.matcher(text);
            int searchStart = 0;
            while (matcher.find(searchStart)) {
                Arrays.fill(spoken, matcher.start(), matcher.end(), ' ');
                searchStart = text.offsetByCodePoints(matcher.start(), 1);
            }
        }
        return new String(spoken);
    }

}
