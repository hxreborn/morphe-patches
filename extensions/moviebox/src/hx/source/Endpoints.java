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
package hx.source;

public final class Endpoints {

    public static final String PLAY_INFO_SEGMENT = "/subject-api/play-info/";

    public static final String RESOURCE_LIST_SEGMENT = "/subject-api/resource/";

    public static final String USER_PROFILE = "wefeed-mobile-bff/user-api/profile";

    public static final String MEMBER_DETAIL = "wefeed-mobile-bff/vip/member/detail";

    public static final String APP_TAB = "wefeed-mobile-bff/subject-api/bottom-tab";

    static final String PLAY_INFO = "wefeed-mobile-bff/subject-api/play-info/v2";

    static final String RESOURCE_POSITION = "wefeed-mobile-bff/subject-api/resource-position/v2";

    static final String RESOURCE_LIST = "wefeed-mobile-bff/subject-api/resource/v2";

    private Endpoints() {
    }

}
