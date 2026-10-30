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
package app.hxreborn.extension.bereal.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import app.hxreborn.extension.bereal.download.DownloadFolder;

@TargetApi(Build.VERSION_CODES.S_V2)
public final class MemorySaver {

    private static final String TAG = "MemorySaver";

    // Shared by every saver, so overlapping saves never pick the same name
    private static final Object LOCK = new Object();

    private final Context context;

    private final MediaDownloader downloader;

    private final ZoneId zone;

    // Names in the picked folder, listed once per save and kept current on create
    private Set<String> treeNames;

    private boolean treeListed;

    public MemorySaver(final Context context, final MediaDownloader downloader, final ZoneId zone) {
        this.context = context.getApplicationContext();
        this.downloader = downloader;
        this.zone = zone;
    }

    public Result save(final MemoryPost post) {
        synchronized (LOCK) {
            // Another saver may have written to the folder since the last listing
            this.treeNames = null;
            this.treeListed = false;
            return saveLocked(post);
        }
    }

    private Result saveLocked(final MemoryPost post) {
        if (post.back == null || post.front == null) {
            return Result.FAILED;
        }
        final ContentResolver resolver = this.context.getContentResolver();
        final long time = MemoryFiles.time(post.takenAt, post.postedAt);
        final String backExt = MemoryFiles.extension(post.back.url, "webp");
        final String frontExt = MemoryFiles.extension(post.front.url, "webp");
        final String firstBase = MemoryFiles.base(post.takenAt, post.postedAt, post.id, this.zone);
        String base = firstBase;
        final Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        for (int attempt = 2; exists(base + "_back." + backExt, images); attempt++) {
            final Sidecar sidecar = sidecar(DownloadFolder.readText(resolver, base + ".json"), post.id);
            if (sidecar != Sidecar.OTHER) {
                return Result.SKIPPED;
            }
            base = MemoryFiles.suffixed(firstBase, attempt);
        }
        final String backName = base + "_back." + backExt;
        final String frontName = base + "_front." + frontExt;
        final List<String> saved = new ArrayList<>();
        final List<Uri> written = new ArrayList<>();
        try {
            written.add(savePhoto(post, time, post.front.url, frontName, frontExt));
            saved.add(frontName);
            saved.add(backName);
            if (post.btsUrl != null && !post.btsUrl.isEmpty()) {
                final String btsName = base + "_bts.mp4";
                try (InputStream video = this.downloader.open(post.btsUrl)) {
                    written.add(write(time, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, btsName, "video/mp4", video));
                }
                saved.add(btsName);
            }
            final byte[] json = MemoriesJson.sidecar(post, saved).toString(2).getBytes(StandardCharsets.UTF_8);
            written.add(write(time, MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    base + ".json", "application/json", new ByteArrayInputStream(json)));
            // Back goes last: its existence marks the post as saved (eg. a failed bts leaves no marker)
            written.add(savePhoto(post, time, post.back.url, backName, backExt));
            return Result.SAVED;
        } catch (IOException | JSONException | RuntimeException | OutOfMemoryError ex) {
            Log.e(TAG, "Post not saved: " + post.id, ex);
            for (final Uri file : written) {
                DownloadFolder.discard(resolver, file);
            }
            return Result.FAILED;
        }
    }

    private boolean exists(final String name, final Uri collection) {
        final ContentResolver resolver = this.context.getContentResolver();
        if (!this.treeListed) {
            this.treeNames = DownloadFolder.treeChildNames(resolver);
            this.treeListed = true;
        }
        return (this.treeNames != null && this.treeNames.contains(name))
                || DownloadFolder.existsInMediaStore(resolver, collection, name);
    }

    static Sidecar sidecar(final String text, final String postId) {
        if (text == null) {
            return Sidecar.MISSING;
        }
        try {
            return postId.equals(new JSONObject(text).optString("id", postId)) ? Sidecar.SAME : Sidecar.OTHER;
        } catch (JSONException ex) {
            // An unparsable sidecar is kept as it is
            return Sidecar.SAME;
        }
    }

    private Uri savePhoto(final MemoryPost post, final long time, final String url, final String name,
            final String ext) throws IOException {
        final byte[] original = readAll(url);
        return write(time, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, name, MemoryFiles.mimeType(ext),
                new ByteArrayInputStream(withExif(post, time, original, ext)));
    }

    private Uri write(final long time, final Uri collection, final String name, final String mime,
            final InputStream in) throws IOException {
        final ContentResolver resolver = this.context.getContentResolver();
        final Uri file = DownloadFolder.create(resolver, collection, name, mime, time);
        if (file == null) {
            throw new IOException("No file for " + name);
        }
        try {
            try (OutputStream out = DownloadFolder.open(resolver, file)) {
                final byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                }
            }
            DownloadFolder.finish(resolver, file);
        } catch (IOException | RuntimeException ex) {
            DownloadFolder.discard(resolver, file);
            throw ex;
        }
        if (this.treeNames != null) {
            this.treeNames.add(name);
        }
        return file;
    }

    private byte[] readAll(final String url) throws IOException {
        try (InputStream in = this.downloader.open(url)) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private byte[] withExif(final MemoryPost post, final long time, final byte[] original, final String ext) {
        File temp = null;
        try {
            temp = File.createTempFile("memory", "." + ext, this.context.getCacheDir());
            try (OutputStream out = new FileOutputStream(temp)) {
                out.write(original);
            }
            final ExifInterface exif = new ExifInterface(temp.getAbsolutePath());
            if (time > 0) {
                final String dateTime = ExifValues.dateTime(time, this.zone);
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateTime);
                exif.setAttribute(ExifInterface.TAG_DATETIME, dateTime);
                // TAG_OFFSET_TIME_ORIGINAL only exists from API 31
                if (Build.VERSION.SDK_INT >= 31) {
                    exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, ExifValues.offset(time, this.zone));
                }
            }
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "BeReal");
            if (post.location != null) {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, ExifValues.rational(post.location.latitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, ExifValues.latitudeRef(post.location.latitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, ExifValues.rational(post.location.longitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,
                        ExifValues.longitudeRef(post.location.longitude));
            }
            final String description = ExifValues.description(post.caption);
            if (description != null) {
                exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description);
            }
            exif.saveAttributes();
            return Files.readAllBytes(temp.toPath());
        } catch (IOException | RuntimeException | OutOfMemoryError ex) {
            Log.e(TAG, "EXIF not written, keeping the original bytes", ex);
            return original;
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    enum Sidecar {
        SAME, MISSING, OTHER
    }

    public enum Result {
        SAVED, SKIPPED, FAILED
    }
}
