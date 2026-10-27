/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.download;

import static org.junit.Assert.assertEquals;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import app.hxreborn.extension.bereal.PatchSettings;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class DownloadFolderTest {

    private static final String DOCUMENTS = "com.android.externalstorage.documents";

    private static final String OTHER_DOCUMENTS = "com.example.documents";

    private static final String TREE = "content://" + DOCUMENTS + "/tree/";

    private static final Uri CREATED = Uri.parse("content://" + DOCUMENTS + "/document/created");

    @Before
    public void setContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefault() {
        PatchSettings.DOWNLOAD_FOLDER.resetToDefault();
        FolderProvider.creates = true;
    }

    @Test
    public void savesToThePicturesFolderWithoutAGrant() {
        // given
        PatchSettings.DOWNLOAD_FOLDER.save(TREE + "primary%3APictures%2FBeReal");
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/BeReal");

        // when
        DownloadFolder.insert(RuntimeEnvironment.getApplication().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // then
        assertEquals("Pictures/BeReal", values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
    }

    @Test
    public void savesToThePicturesFolderByDefault() {
        // given
        final ContentValues values = givenBeRealDownload();

        // when
        DownloadFolder.insert(resolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // then
        assertEquals("Pictures/BeReal", values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
    }

    @Test
    public void savesToTheDcimFolderWhenPicked() {
        // given
        DownloadFolder.usePreset(DownloadFolder.DCIM_FOLDER);
        final ContentValues values = givenBeRealDownload();

        // when
        DownloadFolder.insert(resolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // then
        assertEquals("DCIM/BeReal", values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
    }

    @Test
    public void labelsThePicturesFolderWithoutAPick() {
        assertEquals("Pictures/BeReal", DownloadFolder.label());
    }

    @Test
    public void labelsAPickedFolderByItsPath() {
        // given
        PatchSettings.DOWNLOAD_FOLDER.save(TREE + "primary%3APictures%2FBeReal");

        // when
        final String label = DownloadFolder.label();

        // then
        assertEquals("Pictures/BeReal", label);
    }

    @Test
    public void labelsTheInternalStorageRoot() {
        // given
        PatchSettings.DOWNLOAD_FOLDER.save(TREE + "primary%3A");

        // when
        final String label = DownloadFolder.label();

        // then
        assertEquals("Internal storage", label);
    }

    @Test
    public void labelsAMemoryCardRootByItsVolume() {
        // given
        PatchSettings.DOWNLOAD_FOLDER.save(TREE + "1A2B-3C4D%3A");

        // when
        final String label = DownloadFolder.label();

        // then
        assertEquals("1A2B-3C4D", label);
    }

    @Test
    public void savesToThePickedFolderOnInternalStorage() {
        // given
        givenPickedFolder("primary%3APictures%2FBeReal");
        final ContentValues values = givenBeRealDownload();

        // when
        DownloadFolder.insert(resolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // then
        assertEquals("Pictures/BeReal", values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
    }

    @Test
    public void createsTheDownloadInAFolderTheGalleryRejects() {
        // given
        givenPickedFolder("primary%3ADownload%2FBeReal");
        Robolectric.setupContentProvider(RejectingMediaProvider.class, MediaStore.AUTHORITY);
        final ContentValues values = givenBeRealDownload();

        // when
        final Uri saved = DownloadFolder.insert(resolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // then
        assertEquals(CREATED, saved);
    }

    @Test
    public void createsTheDownloadWhenTheGalleryReturnsNoRow() {
        // given
        givenPickedFolder("primary%3APictures%2FBeReal");
        Robolectric.setupContentProvider(EmptyMediaProvider.class, MediaStore.AUTHORITY);

        // when
        final Uri saved = DownloadFolder.insert(resolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                givenBeRealDownload());

        // then
        assertEquals(CREATED, saved);
    }

    @Test
    public void releasesAFolderTheSettingsNoLongerUse() {
        // given
        givenPickedFolder("primary%3APictures%2FBeReal");
        PatchSettings.DOWNLOAD_FOLDER.resetToDefault();

        // when
        DownloadFolder.releaseGrantIfUnused(resolver(), TREE + "primary%3APictures%2FBeReal");

        // then
        assertEquals(0, resolver().getPersistedUriPermissions().size());
    }

    @Test
    public void keepsTheFolderTheSettingsUse() {
        // given
        givenPickedFolder("primary%3APictures%2FBeReal");

        // when
        DownloadFolder.releaseGrantIfUnused(resolver(), TREE + "primary%3APictures%2FBeReal");

        // then
        assertEquals(1, resolver().getPersistedUriPermissions().size());
    }

    @Test
    public void createsTheDownloadInAnotherProvidersFolder() {
        // given
        Robolectric.setupContentProvider(FolderProvider.class, OTHER_DOCUMENTS);
        DownloadFolder.choose(resolver(), Uri.parse("content://" + OTHER_DOCUMENTS + "/tree/primary%3ABeReal"));

        // when
        final Uri saved = DownloadFolder.insert(resolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                givenBeRealDownload());

        // then
        assertEquals(CREATED, saved);
    }

    @Test
    public void createsTheDownloadOnAMemoryCard() {
        // given
        givenPickedFolder("1A2B-3C4D%3ABeReal");

        // when
        final Uri saved = DownloadFolder.insert(resolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                givenBeRealDownload());

        // then
        assertEquals(CREATED, saved);
    }

    @Test
    public void savesToTheGalleryWhenTheMemoryCardRefusesTheFile() {
        // given
        givenPickedFolder("1A2B-3C4D%3ABeReal");
        FolderProvider.creates = false;

        // when
        final Uri saved = DownloadFolder.insert(resolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                givenBeRealDownload());

        // then
        assertEquals(MediaStore.AUTHORITY, saved.getAuthority());
    }

    private static void givenPickedFolder(String documentId) {
        Robolectric.setupContentProvider(FolderProvider.class, DOCUMENTS);
        DownloadFolder.choose(resolver(), Uri.parse(TREE + documentId));
    }

    private static ContentValues givenBeRealDownload() {
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "bereal.jpeg");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/BeReal");
        return values;
    }

    private static ContentResolver resolver() {
        return RuntimeEnvironment.getApplication().getContentResolver();
    }

    public static final class FolderProvider extends ContentProvider {

        static boolean creates = true;

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            final MatrixCursor cursor = new MatrixCursor(projection);
            cursor.addRow(new Object[] { DocumentsContract.getDocumentId(uri) });
            return cursor;
        }

        @Override
        public Bundle call(String method, String arg, Bundle extras) {
            final Bundle result = new Bundle();
            if (creates) {
                result.putParcelable("uri", CREATED);
            }
            return result;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }

    }

    public static class EmptyMediaProvider extends ContentProvider {

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            return null;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }

    }

    public static final class RejectingMediaProvider extends EmptyMediaProvider {

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            throw new IllegalArgumentException("Primary directory Download not allowed");
        }

    }

}
