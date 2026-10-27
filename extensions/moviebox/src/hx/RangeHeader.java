/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RangeHeader {

    private static final Pattern PATTERN = Pattern.compile("^bytes=(\\d{1,18})-(\\d{0,18})$");

    final long first;

    private final String last;

    private RangeHeader(long first, String last) {
        this.first = first;
        this.last = last;
    }

    static RangeHeader parse(String header) {
        Matcher matcher = (header != null) ? PATTERN.matcher(header) : null;
        if (matcher == null || !matcher.matches()) {
            return null;
        }
        return new RangeHeader(Long.parseLong(matcher.group(1)), matcher.group(2));
    }

    ByteRange within(long length) {
        long end = this.last.isEmpty() ? length - 1 : Math.min(length - 1, Long.parseLong(this.last));
        return new ByteRange(this.first, end);
    }

}
