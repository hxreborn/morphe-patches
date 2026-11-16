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
package app.hxreborn.extension.quranify;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class ApiResponse {

    final int status;

    final String contentType;

    final byte[] body;

    final Map<String, String> headers;

    ApiResponse(int status, String contentType, byte[] body, Map<String, String> headers) {
        this.status = status;
        this.contentType = contentType;
        this.body = body;
        this.headers = headers;
    }

    void writeTo(OutputStream output) throws IOException {
        StringBuilder head = new StringBuilder("HTTP/1.1 ").append(this.status)
            .append(" Result\r\nContent-Type: ")
            .append(this.contentType)
            .append("\r\nContent-Length: ")
            .append(this.body.length)
            .append("\r\nConnection: close\r\n");
        for (Map.Entry<String, String> header : this.headers.entrySet()) {
            String value = header.getValue();
            if (value.indexOf('\r') < 0 && value.indexOf('\n') < 0) {
                head.append(header.getKey()).append(": ").append(value).append("\r\n");
            }
        }
        head.append("\r\n");
        output.write(head.toString().getBytes(StandardCharsets.US_ASCII));
        output.write(this.body);
        output.flush();
    }

}
