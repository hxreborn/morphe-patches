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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import android.content.res.Resources;

@SuppressWarnings("unused")
public final class StartFolder {

    private static final int ALL_FOLDER_TYPE = 0;

    private static final int UNREAD_FOLDER_TYPE = 3;

    private static final int AI_ASSISTANTS_FOLDER_TYPE = 4;

    private static final Set<Object> initializedCollectors = Collections.newSetFromMap(new WeakHashMap<>());

    private static List<Object> folders = Collections.emptyList();

    private StartFolder() {
    }

    public static int initialIndex(List<Object> tabs, int original, Object collector) {
        folders = new ArrayList<>(tabs);
        if (initializedCollectors.add(collector)) {
            return savedFolderIndexOrAll();
        }
        return original;
    }

    public static int resetIndex(int original) {
        return folders.isEmpty() ? original : savedFolderIndexOrAll();
    }

    private static int indexOfSavedFolder() {
        String selected = PatchSettings.startFolder();
        if (selected.isEmpty()) {
            return 0;
        }
        for (int index = 0; index < folders.size(); index++) {
            if (selected.equals(folderId(folders.get(index)))) {
                return index;
            }
        }
        return -1;
    }

    static int savedFolderIndexOrAll() {
        return Math.max(0, indexOfSavedFolder());
    }

    static List<Object> folders() {
        return new ArrayList<>(folders);
    }

    static String folderId(Object tab) {
        return null;
    }

    static String folderTitle(Object tab, Resources resources) {
        return switch (folderType(tab)) {
            case ALL_FOLDER_TYPE -> resources.getString(allTitleId());
            case UNREAD_FOLDER_TYPE -> resources.getString(unreadTitleId());
            case AI_ASSISTANTS_FOLDER_TYPE -> resources.getString(aiAssistantsTitleId());
            default -> folderName(tab);
        };
    }

    private static int allTitleId() {
        return 0;
    }

    private static int unreadTitleId() {
        return 0;
    }

    private static int aiAssistantsTitleId() {
        return 0;
    }

    private static int folderType(Object tab) {
        return -1;
    }

    private static String folderName(Object tab) {
        return null;
    }

}
