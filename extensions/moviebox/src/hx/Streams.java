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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class Streams {

    private static final int BUFFER_BYTES = 8192;

    private Streams() {
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_BYTES];
        int count = in.read(buffer);
        while (count != -1) {
            bytes.write(buffer, 0, count);
            count = in.read(buffer);
        }
        return bytes.toByteArray();
    }

}
