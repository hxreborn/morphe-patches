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
package app.hxreborn.extension.viber;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

final class BusinessChatSelection {

    private static final String DATABASE = "viber_messages";

    private static final String CHAT_QUERY = "SELECT conversations._id, "
            + "COALESCE(NULLIF(conversations.name, ''), NULLIF(participants_info.contact_name, ''), "
            + "NULLIF(participants_info.viber_name, ''), CAST(conversations._id AS TEXT)) "
            + "FROM conversations LEFT JOIN participants_info "
            + "ON participants_info._id = conversations.participant_id_1 " + "WHERE conversations.deleted = 0 AND "
            + BusinessChats.CHAT_PREDICATE + " ORDER BY 2 COLLATE NOCASE, 1";

    private final PatchesSettingsActivity activity;

    private final Runnable onSaved;

    private boolean showing;

    BusinessChatSelection(PatchesSettingsActivity activity, Runnable onSaved) {
        this.activity = activity;
        this.onSaved = onSaved;
    }

    void show() {
        if (this.showing) {
            return;
        }
        this.showing = true;
        new Thread(this::loadChats, "hxViber-business-chats").start();
    }

    private void loadChats() {
        File database = this.activity.getDatabasePath(DATABASE);
        List<Chat> chats = new ArrayList<>();
        if (database.isFile()) {
            try (SQLiteDatabase connection = SQLiteDatabase.openDatabase(database.getPath(), null,
                    SQLiteDatabase.OPEN_READONLY); Cursor cursor = connection.rawQuery(CHAT_QUERY, null)) {
                while (cursor.moveToNext()) {
                    chats.add(new Chat(Long.toString(cursor.getLong(0)), cursor.getString(1)));
                }
            } catch (SQLiteException ex) {
                Log.w(PatchSettings.LOG_TAG, "Business chat list lookup failed");
                this.activity.runOnUiThread(this::showError);
                return;
            }
        }
        this.activity.runOnUiThread(() -> showSelection(chats));
    }

    private void showError() {
        if (this.activity.isFinishing() || this.activity.isDestroyed()) {
            this.showing = false;
            return;
        }
        showDialog(new AlertDialog.Builder(this.activity, this.activity.dialogTheme()).setTitle("Business chats")
            .setMessage("Could not load business chats. Try again after opening Chats.")
            .setPositiveButton("OK", null));
    }

    private void showSelection(List<Chat> chats) {
        if (this.activity.isFinishing() || this.activity.isDestroyed()) {
            this.showing = false;
            return;
        }
        if (chats.isEmpty()) {
            showDialog(new AlertDialog.Builder(this.activity, this.activity.dialogTheme()).setTitle("Business chats")
                .setMessage("No business chats yet.")
                .setPositiveButton("OK", null));
            return;
        }
        Set<String> selected = PatchSettings.selectedBusinessChats();
        String[] names = new String[chats.size()];
        boolean[] checked = new boolean[chats.size()];
        for (int index = 0; index < chats.size(); index++) {
            Chat chat = chats.get(index);
            names[index] = chat.name;
            checked[index] = selected.contains(chat.id);
        }
        showDialog(new AlertDialog.Builder(this.activity, this.activity.dialogTheme())
            .setTitle("Filter read business chats")
            .setMultiChoiceItems(names, checked, (dialog, index, enabled) -> {
                String id = chats.get(index).id;
                if (enabled) {
                    selected.add(id);
                } else {
                    selected.remove(id);
                }
            })
            .setPositiveButton("Save", (dialog, which) -> {
                PatchSettings.setSelectedBusinessChats(selected);
                this.onSaved.run();
            })
            .setNegativeButton("Cancel", null));
    }

    private void showDialog(AlertDialog.Builder builder) {
        this.activity.showDialog(builder).setOnDismissListener((dialog) -> this.showing = false);
    }

    private static final class Chat {

        private final String id;

        private final String name;

        private Chat(String id, String name) {
            this.id = id;
            this.name = name;
        }

    }

}
