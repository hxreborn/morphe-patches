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

    REMOVE_CHAT_PAYMENTS("Chats", "Remove chat payment actions", "Removes send-money actions.", true),
    DISABLE_AI_FEATURES("Chats", "Disable AI features", "Removes AI replies and summaries.", true),
    SAVE_DISAPPEARING_MEDIA("Chats", "saveDisappearingMedia", "Allows saving disappearing media.", false),
    FILTER_BUSINESS_CHATS("Chats", "filterBusinessChats", "Removes selected chats once read.", true),
    UNFILED_CHATS("Folders", "unfiledChats", "Adds a tab for chats outside your folders.", true),
    RAISE_FOLDER_LIMIT("Folders", "raiseFolderLimit", "Allows up to 10 folders.", false),
    HIDE_ADS("Ads", "Hide ads", "Removes ads across the app.", false),
    HIDE_PROMOTIONS("Ads", "hidePromotions", "Removes promos and suggestions.", true),
    HIDE_NAVIGATION_BUTTONS("Navigation", "Hide navigation buttons", "Removes selected tabs.", true),
    HIDE_MORE_ITEMS("Navigation", "Hide More screen items", "Removes selected entries.", false),
    REMOVE_ONLINE_STATUS_LIMIT("Privacy", "removeOnlineStatusLimit", "Allows online status changes any time.", false),
    DISABLE_ANALYTICS("Privacy", "Disable analytics", "Stops usage tracking.", true),
    DISABLE_UPDATE_PROMPTS("App", "disableUpdatePrompts", "Stops optional update dialogs.", false),
    UNLOCK_VIBER_PLUS("App", "Unlock Viber Plus", "Unlocks icons and typing privacy.", true);

    final String section;

    final String key;

    final String description;

    final boolean restart;

    PatchSetting(String section, String key, String description, boolean restart) {
        this.section = section;
        this.key = key;
        this.description = description;
        this.restart = restart;
    }

    String title() {
        return switch (this) {
            case REMOVE_CHAT_PAYMENTS -> AppliedPatches.removeChatPaymentsName();
            case DISABLE_AI_FEATURES -> AppliedPatches.disableAiFeaturesName();
            case UNFILED_CHATS -> AppliedPatches.unfiledChatsName();
            case RAISE_FOLDER_LIMIT -> AppliedPatches.raiseFolderLimitName();
            case SAVE_DISAPPEARING_MEDIA -> AppliedPatches.saveDisappearingMediaName();
            case HIDE_ADS -> AppliedPatches.hideAdsName();
            case HIDE_PROMOTIONS -> AppliedPatches.hidePromotionsName();
            case HIDE_MORE_ITEMS -> AppliedPatches.hideMoreItemsName();
            case HIDE_NAVIGATION_BUTTONS -> AppliedPatches.hideNavigationButtonsName();
            case FILTER_BUSINESS_CHATS -> AppliedPatches.filterBusinessChatsName();
            case REMOVE_ONLINE_STATUS_LIMIT -> AppliedPatches.removeOnlineStatusLimitName();
            case DISABLE_ANALYTICS -> AppliedPatches.disableAnalyticsName();
            case DISABLE_UPDATE_PROMPTS -> AppliedPatches.disableUpdatePromptsName();
            case UNLOCK_VIBER_PLUS -> AppliedPatches.unlockViberPlusName();
        };
    }

    boolean applied() {
        return switch (this) {
            case REMOVE_CHAT_PAYMENTS -> AppliedPatches.removeChatPayments();
            case DISABLE_AI_FEATURES -> AppliedPatches.disableAiFeatures();
            case UNFILED_CHATS -> AppliedPatches.unfiledChats();
            case RAISE_FOLDER_LIMIT -> AppliedPatches.raiseFolderLimit();
            case SAVE_DISAPPEARING_MEDIA -> AppliedPatches.saveDisappearingMedia();
            case HIDE_ADS -> AppliedPatches.hideAds();
            case HIDE_PROMOTIONS -> AppliedPatches.hidePromotions();
            case HIDE_MORE_ITEMS -> AppliedPatches.hideMoreItems();
            case HIDE_NAVIGATION_BUTTONS -> AppliedPatches.hideNavigationButtons();
            case FILTER_BUSINESS_CHATS -> AppliedPatches.filterBusinessChats();
            case REMOVE_ONLINE_STATUS_LIMIT -> AppliedPatches.removeOnlineStatusLimit();
            case DISABLE_ANALYTICS -> AppliedPatches.disableAnalytics();
            case DISABLE_UPDATE_PROMPTS -> AppliedPatches.disableUpdatePrompts();
            case UNLOCK_VIBER_PLUS -> AppliedPatches.unlockViberPlus();
        };
    }

    String icon() {
        return "hx_viber_ic_" + name().toLowerCase(Locale.ROOT);
    }

}
