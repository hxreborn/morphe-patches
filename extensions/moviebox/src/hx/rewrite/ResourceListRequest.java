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
package hx.rewrite;

import android.net.Uri;
import hx.source.Endpoints;

final class ResourceListRequest {

    private static final String VERSION_SUFFIX = "/v2";

    private static final String MEMBERSHIP_PARAMETER = "isVip";

    private ResourceListRequest() {
    }

    static String listingUrl(String url) {
        Uri requested = Uri.parse(url);
        String path = requested.getEncodedPath();
        if (path == null || !path.endsWith(Endpoints.RESOURCE_LIST_V2_SEGMENT)) {
            return null;
        }
        Uri.Builder listing = requested.buildUpon()
            .encodedPath(path.substring(0, path.length() - VERSION_SUFFIX.length()))
            .clearQuery();
        for (String name : requested.getQueryParameterNames()) {
            if (!MEMBERSHIP_PARAMETER.equals(name)) {
                for (String value : requested.getQueryParameters(name)) {
                    listing.appendQueryParameter(name, value);
                }
            }
        }
        return listing.build().toString();
    }

}
