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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public final class FeatureFlags {

    private static final Set<String> UPGRADE_PROMOTION_FLAGS = new HashSet<>(
            Arrays.asList("SOCIAL_PROOF_ENABLED", "FAN_TO_FAN_GIFTING_ENABLED", "ANDROID_EXPLORE_GIFTS_ENABLED",
                    "LAUNCHER_GIFTING_DISCOVERY_ENABLED", "FCE_IN_FEED_IMPRESSION_SURVEY_ENABLED"));

    private static final Set<String> APP_PROMPT_FLAGS = new HashSet<>(
            Arrays.asList("APP_REVIEW_PROMPT_ENABLED", "IN_APP_UPGRADE_UI_ENABLED"));

    private static final Set<String> TRACKING_FLAGS = Collections.singleton("AUDIO_MUX_STATS_COLLECTION_ENABLED");

    private FeatureFlags() {
    }

    public static boolean isForcedOff(Enum<?> flag) {
        String name = flag.name();
        if (UPGRADE_PROMOTION_FLAGS.contains(name)) {
            return UpgradePromotions.isPatched() && UpgradePromotions.isHidden();
        }
        if (APP_PROMPT_FLAGS.contains(name)) {
            return AppPrompts.isPatched() && AppPrompts.isRemoved();
        }
        if (TRACKING_FLAGS.contains(name)) {
            return Tracking.isPatched() && Tracking.isDisabled();
        }
        return false;
    }

}
