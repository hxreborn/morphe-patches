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
package hx.testing;

import java.nio.charset.StandardCharsets;

import android.util.Base64;

public final class PlayInfoFixtures {

    private PlayInfoFixtures() {
    }

    public static String cloudFrontCookie(String resource) {
        String json = "{\"Statement\":[{\"Resource\":\"" + resource + "\"}]}";
        String policy = Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)
            .replace('+', '-')
            .replace('=', '_')
            .replace('/', '~');
        return "CloudFront-Policy=" + policy + ";CloudFront-Signature=sig;CloudFront-Key-Pair-Id=key";
    }

    public static String edgeCacheCookie(String prefix) {
        String encoded = Base64.encodeToString(prefix.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP);
        return "Edge-Cache-Cookie=urlprefix=" + encoded + ":expires=1:hmac=abc;path=/";
    }

    public static String playInfo(String... cookies) {
        StringBuilder streams = new StringBuilder();
        for (String cookie : cookies) {
            if (streams.length() > 0) {
                streams.append(',');
            }
            streams.append("{\"url\":\"https://origin.example.com/f.mp4\",\"signCookie\":\"")
                .append(cookie)
                .append("\"}");
        }
        return "{\"data\":{\"streams\":[" + streams + "]}}";
    }

}
