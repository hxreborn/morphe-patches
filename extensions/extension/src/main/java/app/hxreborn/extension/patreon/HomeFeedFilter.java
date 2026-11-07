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
package app.hxreborn.extension.patreon;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

@SuppressWarnings("unused")
public final class HomeFeedFilter {

    static final String HIDE_RECOMMENDED_CREATORS = "hide_recommended_creators";

    private static final String EXPLORE_SECTION_TYPE = "chunked_explore";

    private static final String DATABASE_PREFIX = "room_primary_patreon_database_";

    private static final String MEMBER_CAMPAIGNS_QUERY = "SELECT campaign_id FROM member_table"
            + " WHERE campaign_id IS NOT NULL AND (is_free_member OR patron_status COLLATE NOCASE IN"
            + " ('active_patron', 'declined_patron', 'free_member', 'free_trial'))"
            + " UNION SELECT campaign_id FROM pledge_table WHERE campaign_id IS NOT NULL";

    private HomeFeedFilter() {
    }

    public static boolean isPatched() {
        return false;
    }

    static Object postCardOf(Object item) {
        return null;
    }

    static Object postOf(Object postCard) {
        return null;
    }

    static Object stringIdOf(Object post) {
        return null;
    }

    static Object recsplanationDataOf(Object item) {
        return null;
    }

    static Object recommendedCreatorCardOf(Object item) {
        return null;
    }

    static Object quickPostCardOf(Object item) {
        return null;
    }

    static Object sectionTypeIdOf(Object section) {
        return null;
    }

    static boolean isEnabled() {
        return PatchSettings.isEnabled(HIDE_RECOMMENDED_CREATORS, true);
    }

    public static List<Object> removeNonMemberItems(List<Object> items) {
        if (items == null || items.isEmpty() || !isEnabled()) {
            return items;
        }
        Set<String> nonMemberPosts = nonMemberPostIds(items);
        List<Object> kept = new ArrayList<>(items.size());
        for (Object item : items) {
            if (!isRecommendation(item) && !nonMemberPosts.contains(postId(item))) {
                kept.add(item);
            }
        }
        return (kept.size() == items.size()) ? items : kept;
    }

    public static List<Object> removeQuipCards(List<Object> items) {
        if (items == null || items.isEmpty() || !QuipsAndAutoplay.isPatched() || !QuipsAndAutoplay.isDisabled()) {
            return items;
        }
        List<Object> kept = new ArrayList<>(items.size());
        for (Object item : items) {
            if (quickPostCardOf(item) == null) {
                kept.add(item);
            }
        }
        return (kept.size() == items.size()) ? items : kept;
    }

    public static List<Object> removeExploreSections(List<Object> sections) {
        if (sections == null || sections.isEmpty() || !isEnabled()) {
            return sections;
        }
        List<Object> kept = new ArrayList<>(sections.size());
        for (Object section : sections) {
            if (!EXPLORE_SECTION_TYPE.equals(sectionTypeIdOf(section))) {
                kept.add(section);
            }
        }
        return (kept.size() == sections.size()) ? sections : kept;
    }

    private static Set<String> nonMemberPostIds(List<Object> items) {
        List<String> postIds = new ArrayList<>(items.size());
        for (Object item : items) {
            String postId = postId(item);
            if (postId != null) {
                postIds.add(postId);
            }
        }
        File databaseFile = roomDatabaseFile();
        if (postIds.isEmpty() || databaseFile == null) {
            return Collections.emptySet();
        }
        String query = "SELECT server_post_id, server_campaign_id IN (" + MEMBER_CAMPAIGNS_QUERY
                + ") FROM post_campaign_cross_ref_table WHERE server_post_id IN ("
                + TextUtils.join(",", Collections.nCopies(postIds.size(), "?")) + ")";
        Set<String> nonMemberPosts = new HashSet<>();
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(databaseFile.getPath(), null,
                SQLiteDatabase.OPEN_READONLY); Cursor cursor = db.rawQuery(query, postIds.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                if (cursor.getInt(1) == 0) {
                    nonMemberPosts.add(cursor.getString(0));
                }
            }
        } catch (RuntimeException ex) {
            Log.w(PatchSettings.LOG_TAG, "Membership query failed on " + databaseFile.getName(), ex);
            return Collections.emptySet();
        }
        return nonMemberPosts;
    }

    private static File roomDatabaseFile() {
        Context context = PatchSettings.application();
        File[] files = (context != null) ? context.getDatabasePath(DATABASE_PREFIX).getParentFile().listFiles() : null;
        File latest = null;
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith(DATABASE_PREFIX) && name.indexOf('-') < 0
                        && (latest == null || file.lastModified() > latest.lastModified())) {
                    latest = file;
                }
            }
        }
        return latest;
    }

    private static String postId(Object item) {
        Object postId = stringIdOf(postOf(postCardOf(item)));
        return (postId instanceof String) ? (String) postId : null;
    }

    private static boolean isRecommendation(Object item) {
        if (item == null) {
            return false;
        }
        return recsplanationDataOf(item) != null || recommendedCreatorCardOf(item) != null;
    }

    public static String patchName() {
        return null;
    }

    public static String patchDescription() {
        return null;
    }

}
