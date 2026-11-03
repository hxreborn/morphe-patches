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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public final class FeatureFlags {

    private static final Set<String> UPGRADE_PROMOTION_FLAGS = new HashSet<>(
            Arrays.asList("SOCIAL_PROOF_ENABLED", "FAN_TO_FAN_GIFTING_ENABLED", "ANDROID_EXPLORE_GIFTS_ENABLED",
                    "LAUNCHER_GIFTING_DISCOVERY_ENABLED", "FCE_IN_FEED_IMPRESSION_SURVEY_ENABLED"));

    private FeatureFlags() {
    }

    public static boolean isForcedOff(Enum<?> flag) {
        String name = flag.name();
        if (UPGRADE_PROMOTION_FLAGS.contains(name)) {
            return UpgradePromotions.isPatched() && UpgradePromotions.isHidden();
        }
        return false;
    }

}
