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
package app.hxreborn.extension.candycrushsaga;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;

final class JsonText {

    private static final int MAX_DEPTH = 64;

    private static final int MAX_NODES = 100000;

    private static final int SHALLOW_FIELDS_DEPTH = 2;

    private static final int UNICODE_ESCAPE_DIGITS = 4;

    private static final Pattern NUMBER = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");

    final String text;

    final int start;

    int end;

    final Map<String, JsonText> fields = new LinkedHashMap<>();

    final List<JsonText> values = new ArrayList<>();

    private JsonText(String text, int start) {
        this.text = text;
        this.start = start;
    }

    static JsonText parse(String text) throws JSONException {
        return parse(text, MAX_DEPTH);
    }

    static JsonText parseShallow(String text) throws JSONException {
        return parse(text, SHALLOW_FIELDS_DEPTH);
    }

    static JsonText expand(JsonText value) throws JSONException {
        JsonText expanded = read(value.text, value.start, 0, new int[] { MAX_NODES }, MAX_DEPTH);
        if (expanded.end != value.end) {
            throw new JSONException("JSON span rejected");
        }
        return expanded;
    }

    private static JsonText parse(String text, int expandedDepth) throws JSONException {
        JsonText root = read(text, whitespace(text, 0), 0, new int[] { MAX_NODES }, expandedDepth);
        if (whitespace(text, root.end) != text.length()) {
            throw new JSONException("Trailing JSON data");
        }
        return root;
    }

    String raw() {
        return this.text.substring(this.start, this.end);
    }

    String string() throws JSONException {
        if (this.text.charAt(this.start) != '"') {
            throw new JSONException("JSON string expected");
        }
        return new JSONArray("[" + raw() + "]").getString(0);
    }

    private static int whitespace(String text, int position) {
        while (position < text.length() && " \t\r\n".indexOf(text.charAt(position)) >= 0) {
            position++;
        }
        return position;
    }

    private static JsonText read(String text, int start, int depth, int[] remaining, int expandedDepth)
            throws JSONException {
        if (depth >= MAX_DEPTH || start >= text.length() || remaining[0]-- <= 0) {
            throw new JSONException("JSON bounds rejected");
        }
        JsonText node = new JsonText(text, start);
        if (depth == expandedDepth) {
            node.end = skip(text, start, depth);
            return node;
        }
        char first = text.charAt(start);
        if (first == '"') {
            node.end = stringEnd(text, start);
            return node;
        }
        int position = start + 1;
        if (first == '{' || first == '[') {
            char close = (first == '{') ? '}' : ']';
            position = whitespace(text, position);
            if (position < text.length() && text.charAt(position) == close) {
                node.end = position + 1;
                return node;
            }
            while (position < text.length()) {
                String key = null;
                if (first == '{') {
                    if (text.charAt(position) != '"') {
                        throw new JSONException("JSON key rejected");
                    }
                    JsonText name = read(text, position, depth + 1, remaining, expandedDepth);
                    key = name.string();
                    position = whitespace(text, name.end);
                    if (position >= text.length() || text.charAt(position++) != ':') {
                        throw new JSONException("JSON separator rejected");
                    }
                    position = whitespace(text, position);
                }
                JsonText value = read(text, position, depth + 1, remaining, expandedDepth);
                node.values.add(value);
                if (key != null && node.fields.put(key, value) != null) {
                    throw new JSONException("Duplicate JSON key");
                }
                position = whitespace(text, value.end);
                if (position >= text.length()) {
                    break;
                }
                char separator = text.charAt(position++);
                if (separator == close) {
                    node.end = position;
                    return node;
                }
                if (separator != ',') {
                    throw new JSONException("JSON separator rejected");
                }
                position = whitespace(text, position);
            }
            throw new JSONException("Unterminated JSON collection");
        }
        node.end = literalEnd(text, start);
        return node;
    }

    private static int skip(String text, int start, int depth) throws JSONException {
        if (depth >= MAX_DEPTH || start >= text.length()) {
            throw new JSONException("JSON bounds rejected");
        }
        char first = text.charAt(start);
        if (first == '"') {
            return stringEnd(text, start);
        }
        if (first != '{' && first != '[') {
            return literalEnd(text, start);
        }
        char close = (first == '{') ? '}' : ']';
        int position = whitespace(text, start + 1);
        if (position < text.length() && text.charAt(position) == close) {
            return position + 1;
        }
        while (position < text.length()) {
            if (first == '{') {
                if (text.charAt(position) != '"') {
                    throw new JSONException("JSON key rejected");
                }
                position = whitespace(text, stringEnd(text, position));
                if (position >= text.length() || text.charAt(position++) != ':') {
                    throw new JSONException("JSON separator rejected");
                }
                position = whitespace(text, position);
            }
            position = whitespace(text, skip(text, position, depth + 1));
            if (position >= text.length()) {
                break;
            }
            char separator = text.charAt(position++);
            if (separator == close) {
                return position;
            }
            if (separator != ',') {
                throw new JSONException("JSON separator rejected");
            }
            position = whitespace(text, position);
        }
        throw new JSONException("Unterminated JSON collection");
    }

    private static int stringEnd(String text, int start) throws JSONException {
        int position = start + 1;
        while (position < text.length()) {
            char current = text.charAt(position++);
            if (current == '"') {
                return position;
            }
            if (current < ' ') {
                throw new JSONException("JSON string rejected");
            }
            if (current != '\\') {
                continue;
            }
            if (position >= text.length()) {
                break;
            }
            char escape = text.charAt(position++);
            if (escape == 'u') {
                if (UNICODE_ESCAPE_DIGITS > text.length() - position) {
                    throw new JSONException("JSON unicode escape rejected");
                }
                for (int index = 0; index < UNICODE_ESCAPE_DIGITS; index++) {
                    if ("0123456789abcdefABCDEF".indexOf(text.charAt(position++)) < 0) {
                        throw new JSONException("JSON unicode escape rejected");
                    }
                }
            } else if (escape != '"' && escape != '\\' && escape != '/' && "bfnrt".indexOf(escape) < 0) {
                throw new JSONException("JSON escape rejected");
            }
        }
        throw new JSONException("Unterminated JSON string");
    }

    private static int literalEnd(String text, int start) throws JSONException {
        int position = start;
        while (position < text.length() && ",]} \t\r\n".indexOf(text.charAt(position)) < 0) {
            position++;
        }
        String literal = text.substring(start, position);
        if (!"null".equals(literal) && !"true".equals(literal) && !"false".equals(literal)
                && !NUMBER.matcher(literal).matches()) {
            throw new JSONException("JSON literal rejected");
        }
        return position;
    }

}
