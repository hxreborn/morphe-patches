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
package app.hxreborn.extension.redreader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class FilterRules {

    enum Field {

        TITLE, SUBREDDIT, FLAIR

    }

    private final List<Pattern> titles;

    private final List<Pattern> subreddits;

    private final List<Pattern> flairs;

    FilterRules(String titles, String subreddits, String flairs) {
        this.titles = compile(titles, Field.TITLE);
        this.subreddits = compile(subreddits, Field.SUBREDDIT);
        this.flairs = compile(flairs, Field.FLAIR);
    }

    boolean matches(String title, String subreddit, String flair) {
        return matches(this.titles, title) || matches(this.subreddits, subreddit) || matches(this.flairs, flair);
    }

    private static boolean matches(List<Pattern> patterns, String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    private static List<Pattern> compile(String text, Field field) {
        List<Pattern> patterns = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (int index = 0; index < lines.length; index++) {
            String rule = lines[index].trim();
            if (rule.isEmpty()) {
                continue;
            }
            boolean isRegex = rule.startsWith("/") && rule.endsWith("/") && rule.length() > 1;
            String expression = isRegex ? rule.substring(1, rule.length() - 1) : Pattern.quote(rule);
            if (expression.isEmpty()) {
                throw invalidRule(field, index, "enter a non-empty expression");
            }
            if (!isRegex && field == Field.SUBREDDIT) {
                expression = "^" + expression + "$";
            }
            try {
                patterns.add(Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            } catch (PatternSyntaxException ex) {
                throw invalidRule(field, index, "invalid regular expression");
            }
        }
        return patterns;
    }

    private static IllegalArgumentException invalidRule(Field field, int index, String reason) {
        return new IllegalArgumentException(
                field.name().toLowerCase(Locale.ROOT) + " line " + (index + 1) + ": " + reason);
    }

}
