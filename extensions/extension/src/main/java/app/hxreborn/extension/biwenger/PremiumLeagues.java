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
package app.hxreborn.extension.biwenger;

import android.webkit.WebView;

@SuppressWarnings("unused")
public final class PremiumLeagues {

    private static final String ROUTE = "/\\/api\\/v\\d+\\/(account|league(\\/[^/?]+)?)(\\?|$)/";

    private static final String REWRITE = """
            function (body) {
              var data = body.data || {};
              [].concat(data, data.leagues || []).forEach(function (league) {
                if (league.type === 'normal') {
                  league.type = 'premium';
                }
              });
            }
            """;

    private PremiumLeagues() {
    }

    public static void install(WebView view) {
        FetchRewrite.install(view, "premiumLeagues", ROUTE, REWRITE);
    }

}
