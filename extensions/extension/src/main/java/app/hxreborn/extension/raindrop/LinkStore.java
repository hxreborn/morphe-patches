/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class LinkStore extends SQLiteOpenHelper {

    private static final int VERSION = 3;

    LinkStore(Context context, long account) {
        super(context.getApplicationContext(), "hx_raindrop_links_" + account + ".db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE links (link TEXT PRIMARY KEY, broken INTEGER NOT NULL DEFAULT 0, "
                + "archived INTEGER NOT NULL DEFAULT 0, health_due_at INTEGER NOT NULL DEFAULT 0, "
                + "archive_due_at INTEGER NOT NULL DEFAULT 0)");
        database.execSQL("CREATE VIRTUAL TABLE page_text USING fts4(link, body, tokenize=unicode61)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS links");
        database.execSQL("DROP TABLE IF EXISTS page_text");
        onCreate(database);
    }

    boolean isHealthCheckDue(String link) {
        return isDue(link, "health_due_at");
    }

    boolean isArchiveCheckDue(String link) {
        return isDue(link, "archive_due_at");
    }

    String resultStamp() {
        try (Cursor cursor = getReadableDatabase()
            .rawQuery("SELECT COALESCE(SUM(broken), 0), COALESCE(SUM(archived), 0) FROM links", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) + "." + cursor.getInt(1) : "";
        }
    }

    boolean isBroken(String link) {
        return flag(link, "broken");
    }

    boolean isArchived(String link) {
        return flag(link, "archived");
    }

    void recordHealth(String link, Boolean broken, long dueAt, String text) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            ensureRow(database, link);
            ContentValues values = new ContentValues();
            values.put("health_due_at", dueAt);
            if (broken != null) {
                values.put("broken", broken ? 1 : 0);
            }
            database.update("links", values, "link = ?", new String[] { link });
            if (text != null) {
                database.delete("page_text", "link = ?", new String[] { link });
                ContentValues page = new ContentValues();
                page.put("link", link);
                page.put("body", text);
                database.insert("page_text", null, page);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void recordArchive(String link, boolean archived, long dueAt) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            ensureRow(database, link);
            ContentValues values = new ContentValues();
            values.put("archive_due_at", dueAt);
            if (archived) {
                values.put("archived", 1);
            }
            database.update("links", values, "link = ?", new String[] { link });
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void retainOnly(Collection<String> links) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            database.execSQL("CREATE TEMP TABLE IF NOT EXISTS kept (link TEXT PRIMARY KEY)");
            database.delete("kept", null, null);
            ContentValues values = new ContentValues();
            for (String link : links) {
                values.put("link", link);
                database.insertWithOnConflict("kept", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
            database.delete("links", "link NOT IN (SELECT link FROM kept)", null);
            database.delete("page_text", "link NOT IN (SELECT link FROM kept)", null);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    Set<String> linksContaining(String query) {
        Set<String> links = new HashSet<>();
        String match = toMatchExpression(query);
        if (match.isEmpty()) {
            return links;
        }
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT link FROM page_text WHERE body MATCH ?",
                new String[] { match })) {
            while (cursor.moveToNext()) {
                links.add(cursor.getString(0));
            }
        }
        return links;
    }

    private boolean isDue(String link, String column) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT " + column + " FROM links WHERE link = ?",
                new String[] { link })) {
            return !cursor.moveToFirst() || cursor.getLong(0) <= System.currentTimeMillis();
        }
    }

    private boolean flag(String link, String column) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT " + column + " FROM links WHERE link = ?",
                new String[] { link })) {
            return cursor.moveToFirst() && cursor.getInt(0) == 1;
        }
    }

    private static void ensureRow(SQLiteDatabase database, String link) {
        ContentValues values = new ContentValues();
        values.put("link", link);
        database.insertWithOnConflict("links", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static String toMatchExpression(String query) {
        StringBuilder match = new StringBuilder();
        for (String word : query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (!word.isEmpty()) {
                match.append((match.length() == 0) ? "" : " ").append(word).append('*');
            }
        }
        return match.toString();
    }

}
