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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MemoryPost {

    public final String id;
    public final String momentId;
    public final long takenAt;
    public final long postedAt;
    public final boolean isMain;
    public final boolean isLate;
    public final Media back;
    public final Media front;
    public final String btsUrl;
    public final String caption;
    public final Location location;
    public final String music;
    public final List<String> visibility;
    public final List<RealMoji> realmojis;
    public final List<Comment> comments;

    public MemoryPost(
            final String id,
            final String momentId,
            final long takenAt,
            final long postedAt,
            final boolean isMain,
            final boolean isLate,
            final Media back,
            final Media front,
            final String btsUrl,
            final String caption,
            final Location location,
            final String music,
            final List<String> visibility,
            final List<RealMoji> realmojis,
            final List<Comment> comments) {
        this.id = id;
        this.momentId = momentId;
        this.takenAt = takenAt;
        this.postedAt = postedAt;
        this.isMain = isMain;
        this.isLate = isLate;
        this.back = back;
        this.front = front;
        this.btsUrl = btsUrl;
        this.caption = caption;
        this.location = location;
        this.music = music;
        this.visibility = Collections.unmodifiableList(new ArrayList<>(visibility));
        this.realmojis = Collections.unmodifiableList(new ArrayList<>(realmojis));
        this.comments = Collections.unmodifiableList(new ArrayList<>(comments));
    }

    public static final class Media {

        public final String url;
        public final int width;
        public final int height;

        public Media(final String url, final int width, final int height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }
    }

    public static final class Location {

        public final double latitude;
        public final double longitude;

        public Location(final double latitude, final double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static final class RealMoji {

        public final String username;
        public final String emoji;
        public final String url;
        public final long postedAt;

        public RealMoji(
                final String username, final String emoji, final String url, final long postedAt) {
            this.username = username;
            this.emoji = emoji;
            this.url = url;
            this.postedAt = postedAt;
        }
    }

    public static final class Comment {

        public final String username;
        public final String text;
        public final long postedAt;

        public Comment(final String username, final String text, final long postedAt) {
            this.username = username;
            this.text = text;
            this.postedAt = postedAt;
        }
    }
}
