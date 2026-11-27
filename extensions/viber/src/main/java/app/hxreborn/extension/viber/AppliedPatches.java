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

@SuppressWarnings("unused")
public final class AppliedPatches {

    private AppliedPatches() {
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

    public static boolean disableAiFeatures() {
        return isApplied("disableAiFeatures");
    }

    public static String disableAiFeaturesName() {
        return name("disableAiFeatures");
    }

    public static String disableAiFeaturesDescription() {
        return description("disableAiFeatures");
    }

    public static boolean removeChatPayments() {
        return isApplied("removeChatPayments");
    }

    public static String removeChatPaymentsName() {
        return name("removeChatPayments");
    }

    public static String removeChatPaymentsDescription() {
        return description("removeChatPayments");
    }

    public static boolean hideAds() {
        return isApplied("hideAds");
    }

    public static String hideAdsName() {
        return name("hideAds");
    }

    public static String hideAdsDescription() {
        return description("hideAds");
    }

    public static boolean hideMoreItems() {
        return isApplied("hideMoreItems");
    }

    public static String hideMoreItemsName() {
        return name("hideMoreItems");
    }

    public static String hideMoreItemsDescription() {
        return description("hideMoreItems");
    }

    public static boolean hideNavigationButtons() {
        return isApplied("hideNavigationButtons");
    }

    public static String hideNavigationButtonsName() {
        return name("hideNavigationButtons");
    }

    public static String hideNavigationButtonsDescription() {
        return description("hideNavigationButtons");
    }

    public static boolean startFolder() {
        return isApplied("startFolder");
    }

    public static String startFolderName() {
        return name("startFolder");
    }

    public static String startFolderDescription() {
        return description("startFolder");
    }

    public static boolean unfiledChats() {
        return isApplied("unfiledChats");
    }

    public static String unfiledChatsName() {
        return name("unfiledChats");
    }

    public static String unfiledChatsDescription() {
        return description("unfiledChats");
    }

    public static boolean raiseFolderLimit() {
        return isApplied("raiseFolderLimit");
    }

    public static String raiseFolderLimitName() {
        return name("raiseFolderLimit");
    }

    public static String raiseFolderLimitDescription() {
        return description("raiseFolderLimit");
    }

    public static boolean filterBusinessChats() {
        return isApplied("filterBusinessChats");
    }

    public static String filterBusinessChatsName() {
        return name("filterBusinessChats");
    }

    public static String filterBusinessChatsDescription() {
        return description("filterBusinessChats");
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

    public static boolean ignoreAccentsInContactSearch() {
        return isApplied("ignoreAccentsInContactSearch");
    }

    public static String ignoreAccentsInContactSearchName() {
        return name("ignoreAccentsInContactSearch");
    }

    public static String ignoreAccentsInContactSearchDescription() {
        return description("ignoreAccentsInContactSearch");
    }

    public static boolean removeOnlineStatusLimit() {
        return isApplied("removeOnlineStatusLimit");
    }

    public static String removeOnlineStatusLimitName() {
        return name("removeOnlineStatusLimit");
    }

    public static String removeOnlineStatusLimitDescription() {
        return description("removeOnlineStatusLimit");
    }

    public static boolean disappearingMediaPrivacy() {
        return isApplied("disappearingMediaPrivacy");
    }

    public static String disappearingMediaPrivacyName() {
        return name("disappearingMediaPrivacy");
    }

    public static String disappearingMediaPrivacyDescription() {
        return description("disappearingMediaPrivacy");
    }

    public static boolean saveDisappearingMedia() {
        return isApplied("saveDisappearingMedia");
    }

    public static String saveDisappearingMediaName() {
        return name("saveDisappearingMedia");
    }

    public static String saveDisappearingMediaDescription() {
        return description("saveDisappearingMedia");
    }

    public static boolean hidePromotions() {
        return isApplied("hidePromotions");
    }

    public static String hidePromotionsName() {
        return name("hidePromotions");
    }

    public static String hidePromotionsDescription() {
        return description("hidePromotions");
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

    public static boolean linkAsSecondaryDevice() {
        return isApplied("linkAsSecondaryDevice");
    }

    public static String linkAsSecondaryDeviceName() {
        return name("linkAsSecondaryDevice");
    }

    public static String linkAsSecondaryDeviceDescription() {
        return description("linkAsSecondaryDevice");
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

    public static boolean gmsCoreSupport() {
        return isApplied("gmsCoreSupport");
    }

    public static String gmsCoreSupportName() {
        return name("gmsCoreSupport");
    }

    public static String gmsCoreSupportDescription() {
        return description("gmsCoreSupport");
    }

    public static boolean unlockViberPlus() {
        return isApplied("unlockViberPlus");
    }

    public static String unlockViberPlusName() {
        return name("unlockViberPlus");
    }

    public static String unlockViberPlusDescription() {
        return description("unlockViberPlus");
    }

    public static boolean removeListGaps() {
        return isApplied("removeListGaps");
    }

    public static String removeListGapsName() {
        return name("removeListGaps");
    }

    public static String removeListGapsDescription() {
        return description("removeListGaps");
    }

    public static boolean removeTypingPrivacyBanner() {
        return isApplied("removeTypingPrivacyBanner");
    }

    public static String removeTypingPrivacyBannerName() {
        return name("removeTypingPrivacyBanner");
    }

    public static String removeTypingPrivacyBannerDescription() {
        return description("removeTypingPrivacyBanner");
    }

    public static boolean removeDeletedMessageNotices() {
        return isApplied("removeDeletedMessageNotices");
    }

    public static String removeDeletedMessageNoticesName() {
        return name("removeDeletedMessageNotices");
    }

    public static String removeDeletedMessageNoticesDescription() {
        return description("removeDeletedMessageNotices");
    }

    public static boolean hideImageRemix() {
        return isApplied("hideImageRemix");
    }

    public static String hideImageRemixName() {
        return name("hideImageRemix");
    }

    public static String hideImageRemixDescription() {
        return description("hideImageRemix");
    }

    public static boolean explainDriveErrors() {
        return isApplied("explainDriveErrors");
    }

    public static String explainDriveErrorsName() {
        return name("explainDriveErrors");
    }

    public static String explainDriveErrorsDescription() {
        return description("explainDriveErrors");
    }

    static List<AppliedPatch> applied() {
        List<AppliedPatch> applied = new ArrayList<>();
        add(applied, removeListGaps(), removeListGapsName(), removeListGapsDescription());
        add(applied, removeTypingPrivacyBanner(), removeTypingPrivacyBannerName(),
                removeTypingPrivacyBannerDescription());
        add(applied, disableAiFeatures(), disableAiFeaturesName(), disableAiFeaturesDescription());
        add(applied, removeChatPayments(), removeChatPaymentsName(), removeChatPaymentsDescription());
        add(applied, removeDeletedMessageNotices(), removeDeletedMessageNoticesName(),
                removeDeletedMessageNoticesDescription());
        add(applied, hideImageRemix(), hideImageRemixName(), hideImageRemixDescription());
        add(applied, hideAds(), hideAdsName(), hideAdsDescription());
        add(applied, hidePromotions(), hidePromotionsName(), hidePromotionsDescription());
        add(applied, hideMoreItems(), hideMoreItemsName(), hideMoreItemsDescription());
        add(applied, hideNavigationButtons(), hideNavigationButtonsName(), hideNavigationButtonsDescription());
        add(applied, startFolder(), startFolderName(), startFolderDescription());
        add(applied, unfiledChats(), unfiledChatsName(), unfiledChatsDescription());
        add(applied, raiseFolderLimit(), raiseFolderLimitName(), raiseFolderLimitDescription());
        add(applied, disappearingMediaPrivacy(), disappearingMediaPrivacyName(), disappearingMediaPrivacyDescription());
        add(applied, saveDisappearingMedia(), saveDisappearingMediaName(), saveDisappearingMediaDescription());
        add(applied, filterBusinessChats(), filterBusinessChatsName(), filterBusinessChatsDescription());
        add(applied, ignoreAccentsInContactSearch(), ignoreAccentsInContactSearchName(),
                ignoreAccentsInContactSearchDescription());
        add(applied, removeOnlineStatusLimit(), removeOnlineStatusLimitName(), removeOnlineStatusLimitDescription());
        add(applied, disableAnalytics(), disableAnalyticsName(), disableAnalyticsDescription());
        add(applied, disableUpdatePrompts(), disableUpdatePromptsName(), disableUpdatePromptsDescription());
        add(applied, linkAsSecondaryDevice(), linkAsSecondaryDeviceName(), linkAsSecondaryDeviceDescription());
        add(applied, spoofSignature(), spoofSignatureName(), spoofSignatureDescription());
        add(applied, gmsCoreSupport(), gmsCoreSupportName(), gmsCoreSupportDescription());
        add(applied, explainDriveErrors(), explainDriveErrorsName(), explainDriveErrorsDescription());
        add(applied, unlockViberPlus(), unlockViberPlusName(), unlockViberPlusDescription());
        Collections.sort(applied, (first, second) -> first.name.compareToIgnoreCase(second.name));
        return applied;
    }

    private static void add(List<AppliedPatch> applied, boolean patchApplied, String name, String description) {
        if (patchApplied) {
            applied.add(new AppliedPatch(name, description));
        }
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
