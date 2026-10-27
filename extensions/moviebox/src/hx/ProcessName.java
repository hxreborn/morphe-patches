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
package hx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import android.util.Log;
import hx.util.Logs;
import hx.util.Streams;

final class ProcessName {

    private static final String COMMAND_LINE = "/proc/self/cmdline";

    private ProcessName() {
    }

    static String current() {
        return read(new File(COMMAND_LINE));
    }

    static String read(File commandLine) {
        try (InputStream in = new FileInputStream(commandLine)) {
            String content = new String(Streams.readAll(in), StandardCharsets.UTF_8);
            int end = content.indexOf('\0');
            String name = (end >= 0) ? content.substring(0, end) : content;
            return name.isEmpty() ? null : name;
        } catch (IOException ex) {
            Log.w(Logs.TAG, "process name unreadable", ex);
            return null;
        }
    }

    static boolean isMain(String processName) {
        return processName != null && !processName.contains(":");
    }

}
