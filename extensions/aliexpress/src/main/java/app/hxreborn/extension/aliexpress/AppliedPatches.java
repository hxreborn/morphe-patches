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
package app.hxreborn.extension.aliexpress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String DISABLE_ANALYTICS = "Disable analytics";

    static final String DISABLE_UPDATE_PROMPTS = "Disable update prompts";

    static final String ENABLE_IMAGE_SAVING = "Enable image saving";

    static final String REMOVE_PROMOTIONAL_POPUPS = "Remove promotional popups";

    static final String REMOVE_SPONSORED_ITEMS = "Remove sponsored items";

    static final String SKIP_SPLASH_PROMOTIONS = "Skip splash promotions";

    static final List<String> SWITCH_KEYS = Collections
        .unmodifiableList(Arrays.asList(REMOVE_SPONSORED_ITEMS, DISABLE_ANALYTICS, DISABLE_UPDATE_PROMPTS,
                ENABLE_IMAGE_SAVING, REMOVE_PROMOTIONAL_POPUPS, SKIP_SPLASH_PROMOTIONS));

    private AppliedPatches() {
    }

    static List<AppliedPatch> applied() {
        List<AppliedPatch> applied = new ArrayList<>();
        add(applied, disableAnalytics(), disableAnalyticsName(), disableAnalyticsDescription());
        add(applied, disableUpdatePrompts(), disableUpdatePromptsName(), disableUpdatePromptsDescription());
        add(applied, enableImageSaving(), enableImageSavingName(), enableImageSavingDescription());
        add(applied, darkTheme(), darkThemeName(), darkThemeDescription());
        add(applied, gmsCoreSupport(), gmsCoreSupportName(), gmsCoreSupportDescription());
        add(applied, removePromotionalPopups(), removePromotionalPopupsName(), removePromotionalPopupsDescription());
        add(applied, removeSponsoredItems(), removeSponsoredItemsName(), removeSponsoredItemsDescription());
        add(applied, skipSplashPromotions(), skipSplashPromotionsName(), skipSplashPromotionsDescription());
        add(applied, spoofSignature(), spoofSignatureName(), spoofSignatureDescription());
        Collections.sort(applied, (first, second) -> first.name.compareToIgnoreCase(second.name));
        return applied;
    }

    private static void add(List<AppliedPatch> applied, boolean patchApplied, String name, String description) {
        if (patchApplied) {
            applied.add(new AppliedPatch(name, description));
        }
    }

    private static String metadata(String key, boolean description) {
        return null;
    }

    static boolean isApplied(String key) {
        return name(key) != null;
    }

    static String name(String key) {
        return metadata(key, false);
    }

    static String description(String key) {
        return metadata(key, true);
    }

    public static boolean disableAnalytics() {
        return isApplied("disableAnalytics");
    }

    public static String disableAnalyticsName() {
        return name("disableAnalytics");
    }

    public static String disableAnalyticsDescription() {
        return description("disableAnalytics");
    }

    public static boolean disableUpdatePrompts() {
        return isApplied("disableUpdatePrompts");
    }

    public static String disableUpdatePromptsName() {
        return name("disableUpdatePrompts");
    }

    public static String disableUpdatePromptsDescription() {
        return description("disableUpdatePrompts");
    }

    public static boolean enableImageSaving() {
        return isApplied("enableImageSaving");
    }

    public static String enableImageSavingName() {
        return name("enableImageSaving");
    }

    public static String enableImageSavingDescription() {
        return description("enableImageSaving");
    }

    public static boolean darkTheme() {
        return isApplied("darkTheme");
    }

    public static String darkThemeName() {
        return name("darkTheme");
    }

    public static String darkThemeDescription() {
        return description("darkTheme");
    }

    public static boolean gmsCoreSupport() {
        return isApplied("gmsCoreSupport");
    }

    public static String gmsCoreSupportName() {
        return name("gmsCoreSupport");
    }

    public static String gmsCoreSupportDescription() {
        return description("gmsCoreSupport");
    }

    public static boolean removePromotionalPopups() {
        return isApplied("removePromotionalPopups");
    }

    public static String removePromotionalPopupsName() {
        return name("removePromotionalPopups");
    }

    public static String removePromotionalPopupsDescription() {
        return description("removePromotionalPopups");
    }

    public static boolean removeSponsoredItems() {
        return isApplied("removeSponsoredItems");
    }

    public static String removeSponsoredItemsName() {
        return name("removeSponsoredItems");
    }

    public static String removeSponsoredItemsDescription() {
        return description("removeSponsoredItems");
    }

    public static boolean skipSplashPromotions() {
        return isApplied("skipSplashPromotions");
    }

    public static String skipSplashPromotionsName() {
        return name("skipSplashPromotions");
    }

    public static String skipSplashPromotionsDescription() {
        return description("skipSplashPromotions");
    }

    public static boolean spoofSignature() {
        return isApplied("spoofSignature");
    }

    public static String spoofSignatureName() {
        return name("spoofSignature");
    }

    public static String spoofSignatureDescription() {
        return description("spoofSignature");
    }

    static final class AppliedPatch {

        final String name;

        final String description;

        AppliedPatch(String name, String description) {
            this.name = name;
            this.description = description;
        }

    }

}
