/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
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
