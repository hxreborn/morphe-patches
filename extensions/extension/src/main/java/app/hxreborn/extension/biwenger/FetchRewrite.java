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

public final class FetchRewrite {

    private static final String SCRIPT = """
            (function () {
              if (!/(^|\\.)biwenger\\.(as\\.)?com$/.test(location.hostname)) {
                return;
              }
              var installed = window.hxRewrites = window.hxRewrites || {};
              if (installed['%1$s']) {
                return;
              }
              installed['%1$s'] = true;
              var route = %2$s;
              var rewrite = %3$s;
              var nativeFetch = window.fetch;
              window.fetch = function (input) {
                var pending = nativeFetch.apply(this, arguments);
                var url = String((input && input.url) || input);
                if (!route.test(url)) {
                  return pending;
                }
                return pending.then(function (response) {
                  return response.clone().json().then(function (body) {
                    rewrite(body);
                    return new Response(JSON.stringify(body), {
                      status: response.status,
                      statusText: response.statusText,
                      headers: response.headers
                    });
                  }).catch(function () {
                    return response;
                  });
                });
              };
            })();
            """;

    private FetchRewrite() {
    }

    static void install(WebView view, String name, String routePattern, String rewriteFunction) {
        view.evaluateJavascript(String.format(SCRIPT, name, routePattern, rewriteFunction), null);
    }

}
