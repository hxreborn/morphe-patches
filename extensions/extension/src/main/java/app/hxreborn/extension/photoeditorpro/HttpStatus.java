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
package app.hxreborn.extension.photoeditorpro;

import java.net.HttpURLConnection;

final class HttpStatus {

    static final int NONE = -1;

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private HttpStatus() {
    }

    static boolean isSuccess(int code) {
        return code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE;
    }

    static boolean isPending(int code) {
        return code == HttpURLConnection.HTTP_NOT_FOUND;
    }

    static String reason(int code) {
        return switch (code) {
            case HttpURLConnection.HTTP_OK -> "OK";
            case HttpURLConnection.HTTP_CREATED -> "Created";
            case HttpURLConnection.HTTP_ACCEPTED -> "Accepted";
            case HttpURLConnection.HTTP_NO_CONTENT -> "No Content";
            case HttpURLConnection.HTTP_MOVED_PERM -> "Moved Permanently";
            case HttpURLConnection.HTTP_MOVED_TEMP -> "Found";
            case HttpURLConnection.HTTP_NOT_MODIFIED -> "Not Modified";
            case HttpURLConnection.HTTP_BAD_REQUEST -> "Bad Request";
            case HttpURLConnection.HTTP_UNAUTHORIZED -> "Unauthorized";
            case HttpURLConnection.HTTP_FORBIDDEN -> "Forbidden";
            case HttpURLConnection.HTTP_NOT_FOUND -> "Not Found";
            case HttpURLConnection.HTTP_CLIENT_TIMEOUT -> "Request Timeout";
            case HttpURLConnection.HTTP_CONFLICT -> "Conflict";
            case HTTP_TOO_MANY_REQUESTS -> "Too Many Requests";
            case HttpURLConnection.HTTP_INTERNAL_ERROR -> "Internal Server Error";
            case HttpURLConnection.HTTP_BAD_GATEWAY -> "Bad Gateway";
            case HttpURLConnection.HTTP_UNAVAILABLE -> "Service Unavailable";
            case HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> "Gateway Timeout";
            default -> "";
        };
    }

}
