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
package app.hxreborn.extension.imo;

import java.util.ArrayList;
import java.util.List;

import android.text.InputFilter;
import android.widget.EditText;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class LongTextMessages {

    private static final int MESSAGE_LENGTH_LIMIT = 2000;

    private static final int INPUT_LENGTH_LIMIT = 10000;

    private static final int WHITESPACE_SEARCH_LENGTH = 200;

    private static final String REPLY_KEY = "replyTo";

    private static boolean sendIntervalSkipped;

    private LongTextMessages() {

    }

    public static void raiseInputLimit(EditText input) {
        if (input == null) {
            return;
        }
        final InputFilter[] current = input.getFilters();
        final InputFilter[] raised = new InputFilter[current.length + 1];
        raised[0] = new InputFilter.LengthFilter(INPUT_LENGTH_LIMIT);
        System.arraycopy(current, 0, raised, 1, current.length);
        input.setFilters(raised);
    }

    public static String[] splitIfOverLimit(String text) {
        if (text == null || text.length() <= MESSAGE_LENGTH_LIMIT) {
            return null;
        }
        final List<String> parts = new ArrayList<>();
        int start = 0;
        while (text.length() - start > MESSAGE_LENGTH_LIMIT) {
            final int end = partEnd(text, start);
            parts.add(text.substring(start, end));
            start = end;
        }
        parts.add(text.substring(start));
        return parts.toArray(new String[0]);
    }

    public static JSONObject[] imdataPerPart(JSONObject imdata, int partCount) {
        final JSONObject[] perPart = new JSONObject[partCount];
        if (imdata == null) {
            return perPart;
        }
        perPart[0] = imdata;
        for (int i = 1; i < partCount; i++) {
            perPart[i] = copyWithoutReply(imdata);
        }
        return perPart;
    }

    public static void skipNextSendInterval() {
        sendIntervalSkipped = true;
    }

    public static boolean consumeSkippedSendInterval() {
        final boolean skipped = sendIntervalSkipped;
        sendIntervalSkipped = false;
        return skipped;
    }

    private static JSONObject copyWithoutReply(JSONObject imdata) {
        try {
            final JSONObject copy = new JSONObject(imdata.toString());
            copy.remove(REPLY_KEY);
            return copy;
        } catch (JSONException ex) {
            throw new IllegalStateException("Cannot copy imdata " + imdata, ex);
        }
    }

    private static int partEnd(String text, int start) {
        final int limit = start + MESSAGE_LENGTH_LIMIT;
        final int searchFloor = limit - WHITESPACE_SEARCH_LENGTH;
        for (int end = limit; end > searchFloor; end--) {
            if (Character.isWhitespace(text.charAt(end - 1))) {
                return end;
            }
        }
        return Character.isHighSurrogate(text.charAt(limit - 1)) ? limit - 1 : limit;
    }

}
