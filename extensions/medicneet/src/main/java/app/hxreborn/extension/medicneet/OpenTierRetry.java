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
package app.hxreborn.extension.medicneet;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public final class OpenTierRetry {

    private static final String QUESTIONS_COLLECTION = "/questions";

    private static final String SEGMENTS_PATH = "/segments/";

    private static final String TIER_OPERATOR = "in";

    private static final List<Long> OPEN_TIERS = Arrays.asList(1L, 2L);

    private OpenTierRetry() {

    }

    public static boolean shouldRetry(Throwable failure, String path, List<List<Object>> where, Object tierField) {
        if (path == null || where == null || !path.endsWith(QUESTIONS_COLLECTION) || !isPermissionDenied(failure)) {
            return false;
        }

        for (List<Object> clause : where) {
            if (!clause.isEmpty() && tierField.equals(clause.get(0))) {
                return false;
            }
        }

        try {
            where.add(Arrays.<Object>asList(tierField, TIER_OPERATOR, OPEN_TIERS));
        } catch (UnsupportedOperationException ex) {
            return false;
        }
        return true;
    }

    public static boolean isOpenSegmentRead(Throwable failure, String path) {
        if (path == null) {
            return false;
        }
        int segments = path.lastIndexOf(SEGMENTS_PATH);
        return segments >= 0 && path.indexOf('/', segments + SEGMENTS_PATH.length()) < 0 && isPermissionDenied(failure);
    }

    private static boolean isPermissionDenied(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null
                    && (message.contains("PERMISSION_DENIED") || message.contains("insufficient permissions"))) {
                return true;
            }
        }
        return false;
    }

}
