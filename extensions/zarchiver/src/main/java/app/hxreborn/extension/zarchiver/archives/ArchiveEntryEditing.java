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
package app.hxreborn.extension.zarchiver.archives;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class ArchiveEntryEditing {

    private static final int COPY_BUFFER_BYTES = 64 * 1024;

    private static final ConcurrentHashMap<String, Boolean> OPEN_ARCHIVES = new ConcurrentHashMap<>();

    private static Handler writer;

    private final Context context;

    private final File archive;

    private final String format;

    private final String password;

    private final int entryIndex;

    private final StructStat originalStat;

    private final long originalModified;

    private ArchiveEntryEditing(Context context, File archive, String format, String password, int entryIndex)
            throws ErrnoException {
        this.context = context.getApplicationContext();
        this.archive = archive;
        this.format = format;
        this.password = password;
        this.entryIndex = entryIndex;
        this.originalStat = Os.stat(archive.getPath());
        this.originalModified = archive.lastModified();
    }

    public static ParcelFileDescriptor openForWrite(ContentProvider provider, String mode, String path, String type,
            String password, int entryIndex) throws FileNotFoundException {
        Context context = provider.getContext();
        if (context == null || path == null || !path.startsWith("/") || entryIndex < 0) {
            throw new FileNotFoundException("Archive entry unavailable");
        }
        try {
            File archive = new File(path).getCanonicalFile();
            String format = (type != null) ? type : archiveFormat(archive.getName());
            if (!supportsEditing(format) || !archive.isFile() || !archive.canWrite()) {
                throw new FileNotFoundException("Archive cannot be edited");
            }
            return new ArchiveEntryEditing(context, archive, format, password, entryIndex).open(mode);
        } catch (IOException | ErrnoException | IllegalArgumentException | SecurityException ex) {
            FileNotFoundException failure = new FileNotFoundException("Could not open archive entry for editing");
            failure.initCause(ex);
            throw failure;
        }
    }

    public static void grantWriteAccess(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null && "content".equals(uri.getScheme()) && archiveAuthority().equals(uri.getAuthority())) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
    }

    private static String archiveAuthority() {
        return null;
    }

    private ParcelFileDescriptor open(String mode) throws IOException {
        String path = this.archive.getPath();
        if (OPEN_ARCHIVES.putIfAbsent(path, Boolean.TRUE) != null) {
            throw new IOException("Archive already has an open editor");
        }
        File draft = null;
        boolean handedToEditor = false;
        try {
            draft = File.createTempFile("archive-entry-", ".tmp", this.context.getCacheDir());
            int flags = ParcelFileDescriptor.parseMode(mode);
            boolean preserveContent = (flags & ParcelFileDescriptor.MODE_TRUNCATE) == 0;
            if (preserveContent) {
                try (Closeable handle = openArchive(path, this.format, this.password)) {
                    if (handle == null) {
                        throw new IOException("Archive unavailable");
                    }
                    try (InputStream data = openEntry(handle, this.entryIndex, path)) {
                        if (data == null) {
                            throw new IOException("Archive entry unavailable");
                        }
                        copy(data, draft);
                    }
                }
            }
            File editedFile = draft;
            byte[] originalDigest = preserveContent ? digest(draft) : null;
            ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(draft, flags, writer(),
                    (error) -> this.finishEdit(editedFile, originalDigest, error));
            if (descriptor == null) {
                throw new IOException("Archive entry descriptor unavailable");
            }
            handedToEditor = true;
            return descriptor;
        } finally {
            if (!handedToEditor) {
                if (draft != null) {
                    draft.delete();
                }
                OPEN_ARCHIVES.remove(path);
            }
        }
    }

    private void finishEdit(File draft, byte[] originalDigest, IOException editorError) {
        File staged = null;
        try {
            if (editorError != null || (originalDigest != null && Arrays.equals(originalDigest, digest(draft)))) {
                return;
            }
            this.checkOriginal();
            staged = File.createTempFile(".archive-edit-", "." + this.format, this.archive.getParentFile());
            try (InputStream source = new FileInputStream(this.archive)) {
                copy(source, staged);
            }
            try (Closeable handle = openArchive(staged.getPath(), this.format, this.password)) {
                if (handle == null || replaceEntry(handle, this.entryIndex, draft.getPath()) != 0) {
                    throw new IOException("Archive entry update failed");
                }
            }
            this.checkOriginal();
            Os.rename(staged.getPath(), this.archive.getPath());
        } catch (IOException | ErrnoException | SecurityException ex) {
            new Handler(Looper.getMainLooper())
                .post(() -> Toast.makeText(this.context, "Could not save archive changes", Toast.LENGTH_LONG).show());
        } finally {
            draft.delete();
            if (staged != null) {
                staged.delete();
            }
            OPEN_ARCHIVES.remove(this.archive.getPath());
        }
    }

    private void checkOriginal() throws IOException, ErrnoException {
        StructStat current = Os.stat(this.archive.getPath());
        if (current.st_dev != this.originalStat.st_dev || current.st_ino != this.originalStat.st_ino
                || current.st_size != this.originalStat.st_size
                || this.archive.lastModified() != this.originalModified) {
            throw new IOException("Archive changed while editing");
        }
    }

    private static byte[] digest(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[COPY_BUFFER_BYTES];
            int count;
            while ((count = input.read(bytes)) != -1) {
                digest.update(bytes, 0, count);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("Could not compare archive entry contents", ex);
        }
    }

    private static void copy(InputStream source, File target) throws IOException {
        try (FileOutputStream output = new FileOutputStream(target)) {
            byte[] bytes = new byte[COPY_BUFFER_BYTES];
            int count;
            while ((count = source.read(bytes)) != -1) {
                output.write(bytes, 0, count);
            }
            output.getFD().sync();
        }
    }

    private static String archiveFormat(String name) {
        int extension = name.lastIndexOf('.');
        return (extension < 0) ? "" : name.substring(extension + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean supportsEditing(String format) {
        return switch (format) {
            case "zip", "7z", "tar" -> true;
            default -> false;
        };
    }

    private static synchronized Handler writer() {
        if (writer == null) {
            HandlerThread thread = new HandlerThread("ZArchiver archive edits");
            thread.start();
            writer = new Handler(thread.getLooper());
        }
        return writer;
    }

    private static Closeable openArchive(String path, String format, String password) throws IOException {
        return null;
    }

    private static InputStream openEntry(Closeable archive, int index, String archivePath) throws IOException {
        return null;
    }

    private static int replaceEntry(Closeable archive, int index, String editedFile) throws IOException {
        return -1;
    }

}
