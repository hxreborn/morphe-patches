/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;

final class AccountStore {

    static final String FILE_NAME = "hx_keepa_accounts.json";

    private static final String KEY_ACCOUNTS = "accounts";

    private static final String KEY_OWNERS = "owners";

    private static final String KEY_PENDING = "pending";

    private final File file;

    AccountStore(File file) {
        this.file = file;
    }

    static AccountStore of(Context context) {
        return new AccountStore(new File(context.getNoBackupFilesDir(), FILE_NAME));
    }

    Accounts load() throws JSONException {
        final JSONObject document = read();
        return Accounts.parse(document.optString(KEY_ACCOUNTS, "{}"), document.optString(KEY_OWNERS, "{}"));
    }

    void save(Accounts accounts) throws JSONException {
        write(read().put(KEY_ACCOUNTS, accounts.toJson().toString()).put(KEY_OWNERS, accounts.owners().toString()));
    }

    String pending() throws JSONException {
        return read().optString(KEY_PENDING, "");
    }

    void writePending(String json) throws JSONException {
        write(read().put(KEY_PENDING, json));
    }

    void clearPending() throws JSONException {
        final JSONObject document = read();
        document.remove(KEY_PENDING);
        write(document);
    }

    void reset() {
        if (this.file.exists() && !this.file.delete()) {
            throw new IllegalStateException("Unable to delete " + this.file);
        }
    }

    private JSONObject read() throws JSONException {
        if (!this.file.exists()) {
            return new JSONObject();
        }
        try {
            final InputStream stream = new FileInputStream(this.file);
            try {
                final byte[] bytes = new byte[(int) this.file.length()];
                int offset = 0;
                while (offset < bytes.length) {
                    final int count = stream.read(bytes, offset, bytes.length - offset);
                    if (count < 0) {
                        break;
                    }
                    offset += count;
                }
                return new JSONObject(new String(bytes, 0, offset, StandardCharsets.UTF_8));
            } finally {
                stream.close();
            }
        } catch (IOException exception) {
            throw new JSONException("Unable to read " + this.file + ": " + exception);
        }
    }

    private void write(JSONObject document) throws JSONException {
        final File replacement = new File(this.file.getPath() + ".tmp");
        try {
            final OutputStream stream = new FileOutputStream(replacement);
            try {
                stream.write(document.toString().getBytes(StandardCharsets.UTF_8));
            } finally {
                stream.close();
            }
            if (!replacement.renameTo(this.file)) {
                throw new IOException("rename failed");
            }
        } catch (IOException exception) {
            throw new JSONException("Unable to write " + this.file + ": " + exception);
        }
    }

}
