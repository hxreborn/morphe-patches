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
package app.hxreborn.extension.netanalyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

final class ExportDelivery {

    private static final String TAG = "hxNetAnalyzer";

    private static final String CHOOSER_TITLE = "Export";

    private static final String TEXT_MIME = "text/plain";

    private static final String SUBJECT_PREFIX = "Network Analyzer: ";

    private static final String SUBJECT_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private static final int MAX_TEXT_LENGTH = 350000;

    private static final String TRUNCATED_TITLE = "Export string too large";

    private static final String TRUNCATED_MESSAGE = "The export string has been truncated to 350KB as larger strings "
            + "cannot be passed between apps.";

    private ExportDelivery() {
    }

    static void shareText(Activity activity, String subject, String text) {
        if (text == null) {
            return;
        }
        String body = text;
        if (body.length() > MAX_TEXT_LENGTH) {
            body = body.substring(0, MAX_TEXT_LENGTH);
            new AlertDialog.Builder(activity).setTitle(TRUNCATED_TITLE)
                .setMessage(TRUNCATED_MESSAGE)
                .setPositiveButton("OK", null)
                .show();
        }
        final String timestamp = new SimpleDateFormat(SUBJECT_TIME_PATTERN, Locale.US).format(new Date());
        final Intent send = new Intent(Intent.ACTION_SEND).setType(TEXT_MIME)
            .putExtra(Intent.EXTRA_SUBJECT, SUBJECT_PREFIX + subject + " (" + timestamp + ")")
            .putExtra(Intent.EXTRA_TEXT, body);
        activity.startActivity(Intent.createChooser(send, CHOOSER_TITLE));
    }

    static void shareCsv(Activity activity, String csv) {
        final File file = ExportFileProvider.file(activity);
        final File directory = file.getParentFile();
        try {
            if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new IOException("Failed to create export directory " + directory);
            }
            activity.revokeUriPermission(ExportFileProvider.uri(activity), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try (OutputStream output = new FileOutputStream(file)) {
                output.write(csv.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            Log.e(TAG, "Cannot write the CSV export", ex);
            Toast.makeText(activity, ex.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        final Intent view = new Intent(Intent.ACTION_VIEW)
            .setDataAndType(ExportFileProvider.uri(activity), ExportFileProvider.CSV_MIME)
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(view, CHOOSER_TITLE));
    }

}
