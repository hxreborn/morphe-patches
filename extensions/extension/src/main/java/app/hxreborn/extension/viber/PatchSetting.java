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

import java.util.Locale;

enum PatchSetting {

    REMOVE_CHAT_PAYMENTS("Chats", "Remove chat payment actions", true),
    REMOVE_TYPING_PRIVACY_BANNER("Chats", "removeTypingPrivacyBanner", true),
    REMOVE_LIST_GAPS("Chats", "removeListGaps", true), DISABLE_AI_FEATURES("Chats", "Disable AI features", true),
    SAVE_DISAPPEARING_MEDIA("Chats", "saveDisappearingMedia", false),
    FILTER_BUSINESS_CHATS("Chats", "filterBusinessChats", true), UNFILED_CHATS("Folders", "unfiledChats", true),
    START_FOLDER("Folders", "startFolder", true, true), RAISE_FOLDER_LIMIT("Folders", "raiseFolderLimit", false),
    HIDE_ADS("Ads", "Hide ads", false), HIDE_PROMOTIONS("Ads", "hidePromotions", true),
    HIDE_NAVIGATION_BUTTONS("Navigation", "Hide navigation buttons", true),
    HIDE_MORE_ITEMS("Navigation", "Hide More screen items", false),
    REMOVE_ONLINE_STATUS_LIMIT("Privacy", "removeOnlineStatusLimit", false),
    DISAPPEARING_MEDIA_PRIVACY("Privacy", "disappearingMediaPrivacy", false),
    DISABLE_ANALYTICS("Privacy", "Disable analytics", true),
    DISABLE_UPDATE_PROMPTS("App", "disableUpdatePrompts", false),
    LINK_AS_SECONDARY_DEVICE("App", "linkAsSecondaryDevice", true), UNLOCK_VIBER_PLUS("App", "Unlock Viber Plus", true);

    final String section;

    final String key;

    final boolean restart;

    final boolean choice;

    PatchSetting(String section, String key, boolean restart) {
        this(section, key, restart, false);
    }

    PatchSetting(String section, String key, boolean restart, boolean choice) {
        this.choice = choice;
        this.section = section;
        this.key = key;
        this.restart = restart;
    }

    String title() {
        return switch (this) {
            case REMOVE_LIST_GAPS -> AppliedPatches.removeListGapsName();
            case REMOVE_TYPING_PRIVACY_BANNER -> AppliedPatches.removeTypingPrivacyBannerName();
            case REMOVE_CHAT_PAYMENTS -> AppliedPatches.removeChatPaymentsName();
            case DISABLE_AI_FEATURES -> AppliedPatches.disableAiFeaturesName();
            case START_FOLDER -> AppliedPatches.startFolderName();
            case UNFILED_CHATS -> AppliedPatches.unfiledChatsName();
            case RAISE_FOLDER_LIMIT -> AppliedPatches.raiseFolderLimitName();
            case DISAPPEARING_MEDIA_PRIVACY -> AppliedPatches.disappearingMediaPrivacyName();
            case SAVE_DISAPPEARING_MEDIA -> AppliedPatches.saveDisappearingMediaName();
            case HIDE_ADS -> AppliedPatches.hideAdsName();
            case HIDE_PROMOTIONS -> AppliedPatches.hidePromotionsName();
            case HIDE_MORE_ITEMS -> AppliedPatches.hideMoreItemsName();
            case HIDE_NAVIGATION_BUTTONS -> AppliedPatches.hideNavigationButtonsName();
            case FILTER_BUSINESS_CHATS -> AppliedPatches.filterBusinessChatsName();
            case REMOVE_ONLINE_STATUS_LIMIT -> AppliedPatches.removeOnlineStatusLimitName();
            case DISABLE_ANALYTICS -> AppliedPatches.disableAnalyticsName();
            case DISABLE_UPDATE_PROMPTS -> AppliedPatches.disableUpdatePromptsName();
            case LINK_AS_SECONDARY_DEVICE -> AppliedPatches.linkAsSecondaryDeviceName();
            case UNLOCK_VIBER_PLUS -> AppliedPatches.unlockViberPlusName();
        };
    }

    String description() {
        return switch (this) {
            case REMOVE_LIST_GAPS -> AppliedPatches.removeListGapsDescription();
            case REMOVE_TYPING_PRIVACY_BANNER -> AppliedPatches.removeTypingPrivacyBannerDescription();
            case REMOVE_CHAT_PAYMENTS -> AppliedPatches.removeChatPaymentsDescription();
            case DISABLE_AI_FEATURES -> AppliedPatches.disableAiFeaturesDescription();
            case DISAPPEARING_MEDIA_PRIVACY -> {
                String description = AppliedPatches.disappearingMediaPrivacyDescription();
                int sentenceEnd = description.indexOf('.');
                yield (sentenceEnd < 0) ? description : description.substring(0, sentenceEnd + 1);
            }
            case SAVE_DISAPPEARING_MEDIA -> AppliedPatches.saveDisappearingMediaDescription();
            case FILTER_BUSINESS_CHATS -> AppliedPatches.filterBusinessChatsDescription();
            case START_FOLDER -> AppliedPatches.startFolderDescription();
            case UNFILED_CHATS -> AppliedPatches.unfiledChatsDescription();
            case RAISE_FOLDER_LIMIT -> AppliedPatches.raiseFolderLimitDescription();
            case HIDE_ADS -> AppliedPatches.hideAdsDescription();
            case HIDE_PROMOTIONS -> AppliedPatches.hidePromotionsDescription();
            case HIDE_NAVIGATION_BUTTONS -> AppliedPatches.hideNavigationButtonsDescription();
            case HIDE_MORE_ITEMS -> AppliedPatches.hideMoreItemsDescription();
            case REMOVE_ONLINE_STATUS_LIMIT -> AppliedPatches.removeOnlineStatusLimitDescription();
            case DISABLE_ANALYTICS -> AppliedPatches.disableAnalyticsDescription();
            case DISABLE_UPDATE_PROMPTS -> AppliedPatches.disableUpdatePromptsDescription();
            case LINK_AS_SECONDARY_DEVICE -> AppliedPatches.linkAsSecondaryDeviceDescription();
            case UNLOCK_VIBER_PLUS -> AppliedPatches.unlockViberPlusDescription();
        };
    }

    boolean applied() {
        return switch (this) {
            case REMOVE_LIST_GAPS -> AppliedPatches.removeListGaps();
            case REMOVE_TYPING_PRIVACY_BANNER -> AppliedPatches.removeTypingPrivacyBanner();
            case REMOVE_CHAT_PAYMENTS -> AppliedPatches.removeChatPayments();
            case DISABLE_AI_FEATURES -> AppliedPatches.disableAiFeatures();
            case START_FOLDER -> AppliedPatches.startFolder();
            case UNFILED_CHATS -> AppliedPatches.unfiledChats();
            case RAISE_FOLDER_LIMIT -> AppliedPatches.raiseFolderLimit();
            case DISAPPEARING_MEDIA_PRIVACY -> AppliedPatches.disappearingMediaPrivacy();
            case SAVE_DISAPPEARING_MEDIA -> AppliedPatches.saveDisappearingMedia();
            case HIDE_ADS -> AppliedPatches.hideAds();
            case HIDE_PROMOTIONS -> AppliedPatches.hidePromotions();
            case HIDE_MORE_ITEMS -> AppliedPatches.hideMoreItems();
            case HIDE_NAVIGATION_BUTTONS -> AppliedPatches.hideNavigationButtons();
            case FILTER_BUSINESS_CHATS -> AppliedPatches.filterBusinessChats();
            case REMOVE_ONLINE_STATUS_LIMIT -> AppliedPatches.removeOnlineStatusLimit();
            case DISABLE_ANALYTICS -> AppliedPatches.disableAnalytics();
            case DISABLE_UPDATE_PROMPTS -> AppliedPatches.disableUpdatePrompts();
            case LINK_AS_SECONDARY_DEVICE -> AppliedPatches.linkAsSecondaryDevice();
            case UNLOCK_VIBER_PLUS -> AppliedPatches.unlockViberPlus();
        };
    }

    String icon() {
        return "hx_viber_ic_" + name().toLowerCase(Locale.ROOT);
    }

}
