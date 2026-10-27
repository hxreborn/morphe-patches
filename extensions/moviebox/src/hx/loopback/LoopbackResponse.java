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
package hx.loopback;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class LoopbackResponse {

    private static final String STATUS_OK = "200 OK";

    private static final String STATUS_PARTIAL = "206 Partial Content";

    private static final String STATUS_FOUND = "302 Found";

    private static final String STATUS_NOT_FOUND = "404 Not Found";

    private static final String STATUS_UNSATISFIABLE = "416 Range Not Satisfiable";

    private static final String STATUS_UNAVAILABLE = "503 Service Unavailable";

    private LoopbackResponse() {
    }

    static void writeRedirect(OutputStream out, String location) throws IOException {
        write(out,
                statusHeader(STATUS_FOUND).append("Location: ")
                    .append(location)
                    .append("\r\n")
                    .append("Content-Length: 0\r\n\r\n"));
    }

    static void writeNotFound(OutputStream out) throws IOException {
        writeEmpty(out, STATUS_NOT_FOUND, null);
    }

    static void writeUnavailable(OutputStream out) throws IOException {
        writeEmpty(out, STATUS_UNAVAILABLE, null);
    }

    static void writeUnsatisfiable(OutputStream out, long length) throws IOException {
        writeEmpty(out, STATUS_UNSATISFIABLE, "bytes */" + length);
    }

    static void writeHeaders(OutputStream out, ByteRange range, long length, boolean partial) throws IOException {
        StringBuilder head = statusHeader(partial ? STATUS_PARTIAL : STATUS_OK).append("Content-Length: ")
            .append(range.size())
            .append("\r\n");
        if (partial) {
            head.append("Content-Range: bytes ")
                .append(range.start)
                .append('-')
                .append(range.end)
                .append('/')
                .append(length)
                .append("\r\n");
        }
        write(out, head.append("\r\n"));
    }

    private static void writeEmpty(OutputStream out, String status, String contentRange) throws IOException {
        StringBuilder head = statusHeader(status).append("Content-Length: 0\r\n");
        if (contentRange != null) {
            head.append("Content-Range: ").append(contentRange).append("\r\n");
        }
        write(out, head.append("\r\n"));
    }

    private static StringBuilder statusHeader(String status) {
        return new StringBuilder("HTTP/1.1 ").append(status)
            .append("\r\n")
            .append("Connection: close\r\nAccept-Ranges: bytes\r\nContent-Type: video/mp4\r\n");
    }

    private static void write(OutputStream out, StringBuilder head) throws IOException {
        out.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

}
