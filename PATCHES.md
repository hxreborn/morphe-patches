# Patches

> Generated from `patches-list.json` for **v1.88.0-dev.4** (`dev`)&nbsp;&nbsp;&middot;&nbsp;&nbsp;**616 patches** across **194 apps**&nbsp;&nbsp;&middot;&nbsp;&nbsp;back to [README](README.md)

---

## 1Weather (com.handmark.expressweather)

<img src="https://publicmetadata.co.uk/com.handmark.expressweather" width="48" height="48" align="top" alt="">

**Supported versions:**

| 13.1.0 | 12.9.3 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="1weather-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/oneweather/misc/telemetry/DisableTrackingPatch.kt) | Stops installs, sessions and in-app events from reaching AppsFlyer. |
| <a id="1weather-hide-shorts"></a>[**Hide Shorts**](patches/src/main/kotlin/app/morphe/patches/oneweather/misc/shorts/HideShortsPatch.kt) | Hides the 1Weather Shorts card from the Today screen. |
| <a id="1weather-hide-skyla"></a>[**Hide Skyla**](patches/src/main/kotlin/app/morphe/patches/oneweather/misc/skyla/HideSkylaPatch.kt) | Hides the Skyla AI assistant, its prompts and the Summarize buttons. |
| <a id="1weather-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/oneweather/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium and removes ads. |

---

## 3way (app.threesome.dating)

<img src="https://publicmetadata.co.uk/app.threesome.dating" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.1.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="3way-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/threeway/misc/premium/UnlockPremiumPatch.kt) | Unlocks Gold: unblurred photos of the people who liked or visited you, the Gold crown, and the advanced search filters. |

---

## aCalendar (org.withouthat.acalendar)

<img src="https://publicmetadata.co.uk/org.withouthat.acalendar" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.9.5 | 2.9.6 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="acalendar-unlock-acalendar"></a>[**Unlock aCalendar+**](patches/src/main/kotlin/app/morphe/patches/acalendar/misc/premium/UnlockPlusPatch.kt) | Unlocks public holidays, color schemes, business features and tasks, and removes ads. |

---

## Albo (com.thesortdapp.sortd)

<img src="https://publicmetadata.co.uk/com.thesortdapp.sortd" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.28.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="albo-enable-guest-sign-in"></a>[**Enable guest sign-in**](patches/src/main/kotlin/app/morphe/patches/albo/misc/guest/EnableGuestSignInPatch.kt) | Enables guest sign-in without a Google account. |
| <a id="albo-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="albo-remove-bulk-import-limit"></a>[**Remove bulk import limit**](patches/src/main/kotlin/app/morphe/patches/albo/misc/imports/RemoveBulkImportLimitPatch.kt) | Removes the credit limit on bulk imports and the weekly limit on link imports. |
| <a id="albo-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/albo/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro and Max features. Import limits are not included. |

---

## AliExpress (com.alibaba.aliexpresshd)

<img src="https://publicmetadata.co.uk/com.alibaba.aliexpresshd" width="48" height="48" align="top" alt="">

**Supported versions:**

| 8.180.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="aliexpress-disable-analytics"></a>[**Disable analytics**](patches/src/main/kotlin/app/morphe/patches/aliexpress/misc/analytics/DisableAnalyticsPatch.kt) | Stops usage statistics and affiliate link reports. |
| <a id="aliexpress-disable-update-prompts"></a>[**Disable update prompts**](patches/src/main/kotlin/app/morphe/patches/aliexpress/misc/updates/DisableUpdatePromptsPatch.kt) | Removes automatic update dialogs. The Version row in Settings still checks manually. |
| <a id="aliexpress-enable-image-saving"></a>[**Enable image saving**](patches/src/main/kotlin/app/morphe/patches/aliexpress/misc/images/EnableImageSavingPatch.kt) | Shows a save button on full-screen product and review photos. |
| <a id="aliexpress-fix-dark-mode"></a>[**Fix dark mode**](patches/src/main/kotlin/app/morphe/patches/aliexpress/misc/theme/DarkThemePatch.kt) | Fixes the screens and web pages the built-in Dark mode leaves white. Requires Android 10 or later. |
| <a id="aliexpress-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="aliexpress-remove-promotional-popups"></a>[**Remove promotional popups**](patches/src/main/kotlin/app/morphe/patches/aliexpress/misc/promotions/RemovePromotionalPopupsPatch.kt) | Stops coupon offers opening over Home and search results. |
| <a id="aliexpress-remove-sponsored-items"></a>[**Remove sponsored items**](patches/src/main/kotlin/app/morphe/patches/aliexpress/ads/RemoveSponsoredItemsPatch.kt) | Removes paid placements from search results and product feeds. |
| <a id="aliexpress-skip-splash-promotions"></a>[**Skip splash promotions**](patches/src/main/kotlin/app/morphe/patches/aliexpress/misc/splash/SkipSplashPromotionsPatch.kt) | Skips the full-screen promotion shown at launch. |
| <a id="aliexpress-spoof-signature"></a>[**Spoof signature**](patches/src/main/kotlin/app/morphe/patches/aliexpress/misc/signin/SpoofSignaturePatch.kt) | Restores product loading on patched installs. |

---

## All Video Player App (com.allformatplayer.streamvideoplayer)

<img src="https://publicmetadata.co.uk/com.allformatplayer.streamvideoplayer" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.4 |
| :---: |

| Patch | Details |
|---|---|
| <a id="all-video-player-app-allow-offline-use"></a>[**Allow offline use**](patches/src/main/kotlin/app/morphe/patches/allvideoplayer/offline/AllowOfflineUsePatch.kt) | Opens the app without an internet connection. |
| <a id="all-video-player-app-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/allvideoplayer/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics, Facebook and OneSignal from collecting usage data. |
| <a id="all-video-player-app-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/allvideoplayer/ads/HideAdsPatch.kt) | Removes app open, interstitial and native ads, and the promoted apps list. |
| <a id="all-video-player-app-remove-rating-prompts"></a>[**Remove rating prompts**](patches/src/main/kotlin/app/morphe/patches/allvideoplayer/rate/RemoveRatingPromptsPatch.kt) | Removes the prompts asking for a rating. |
| <a id="all-video-player-app-resume-after-screen-lock"></a>[**Resume after screen lock**](patches/src/main/kotlin/app/morphe/patches/allvideoplayer/resume/ResumeAfterScreenLockPatch.kt) | Resumes videos where playback stopped after the screen locks, including videos the library has not listed yet. |
| <a id="all-video-player-app-resume-videos-opened-from-other-apps"></a>[**Resume videos opened from other apps**](patches/src/main/kotlin/app/morphe/patches/allvideoplayer/resume/ResumeExternalVideosPatch.kt) | Resumes videos opened from a file manager or gallery where playback stopped. |
| <a id="all-video-player-app-skip-splash-delay"></a>[**Skip splash delay**](patches/src/main/kotlin/app/morphe/patches/allvideoplayer/splash/SkipSplashDelayPatch.kt) | Opens the app without waiting on the splash screen. |

---

## All-In-One Calculator (all.in.one.calculator)

<img src="https://publicmetadata.co.uk/all.in.one.calculator" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.4.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="all-in-one-calculator-gmscore-support"></a>[**GmsCore support**](patches/src/main/kotlin/app/morphe/patches/allinonecalculator/misc/gms/GmsCoreSupportPatch.kt) | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="all-in-one-calculator-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/allinonecalculator/misc/premium/UnlockPremiumPatch.kt) | Unlocks the Pro tools and removes ads. |

---

## Aloa (com.getaloa.app)

<img src="https://publicmetadata.co.uk/com.getaloa.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.3.3 | 1.3.4 | 1.3.5 |
| :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="aloa-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="aloa-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/aloa/misc/premium/UnlockPremiumPatch.kt) | Unlocks conversations, tutor lessons and roadmaps. |

---

## AlpineQuest (psyberia.alpinequest.free)

<img src="https://publicmetadata.co.uk/psyberia.alpinequest.free" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.4.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="alpinequest-unlock-full-version"></a>[**Unlock full version**](patches/src/main/kotlin/app/morphe/patches/alpinequest/misc/activation/UnlockFullVersionPatch.kt) | Unlocks the Off-Road Explorer features. |

---

## AnyTracker (com.shervinkoushan.anyTracker)

<img src="https://publicmetadata.co.uk/com.shervinkoushan.anyTracker" width="48" height="48" align="top" alt="">

**Supported versions:**

| 7.5.4 | 7.5.6 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="anytracker-unlock-platinum"></a>[**Unlock Platinum**](patches/src/main/kotlin/app/morphe/patches/anytracker/misc/premium/UnlockPlatinumPatch.kt) | Unlocks the Platinum plan with unlimited tracked items, every-minute updates, widgets, watchlists and backups. |

---

## APNA Tunnel Lite (com.apnatunnel.lite)

<img src="https://publicmetadata.co.uk/com.apnatunnel.lite" width="48" height="48" align="top" alt="">

**Supported versions:**

| APNA |
| :---: |

| Patch | Details |
|---|---|
| <a id="apna-tunnel-lite-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/apnatunnel/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics and Firebase Sessions from collecting usage data. |
| <a id="apna-tunnel-lite-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/apnatunnel/misc/ads/RemoveAdsPatch.kt) | Removes app open, banner and interstitial ads and the ad consent form. |

---

## Arc Player (com.videoplayer.arcplayer)

<img src="https://publicmetadata.co.uk/com.videoplayer.arcplayer" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.4.2.141 | 1.5.0.145 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="arc-player-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/arcplayer/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics and Firebase Sessions from collecting usage data. |
| <a id="arc-player-remove-rating-prompts"></a>[**Remove rating prompts**](patches/src/main/kotlin/app/morphe/patches/arcplayer/misc/rate/RemoveRatingPromptsPatch.kt) | Removes the feedback and rating popup that appears after watching videos. |
| <a id="arc-player-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/arcplayer/misc/premium/UnlockPremiumPatch.kt) | Unlocks the pure black theme and custom subtitle fonts, and removes the premium prompts. |

---

## ArduinoDroid (name.antonsmirnov.android.arduinodroid2)

<img src="https://publicmetadata.co.uk/name.antonsmirnov.android.arduinodroid2" width="48" height="48" align="top" alt="">

**Supported versions:**

| 9.7.6 |
| :---: |

| Patch | Details |
|---|---|
| <a id="arduinodroid-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/arduinodroid/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium features, removes ads and removes flashing limits. |

---

## Athan (com.athan)

<img src="https://publicmetadata.co.uk/com.athan" width="48" height="48" align="top" alt="">

**Supported versions:**

| 11.0.8 |
| :---: |

| Patch | Details |
|---|---|
| <a id="athan-dark-theme"></a>[**Dark theme**](patches/src/main/kotlin/app/morphe/patches/athan/misc/theme/DarkThemePatch.kt) | Shows the app in dark colors when the system dark theme is on. |
| <a id="athan-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/athan/misc/tracking/DisableTrackingPatch.kt) | Stops usage events from reaching Firebase and Facebook, and disables Crashlytics and Firebase performance monitoring. |
| <a id="athan-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="athan-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/athan/ads/RemoveAdsPatch.kt) | Removes banner, native, app open and interstitial ads, including the ad shown before sharing. |
| <a id="athan-remove-promotions"></a>[**Remove promotions**](patches/src/main/kotlin/app/morphe/patches/athan/misc/promotion/RemovePromotionsPatch.kt) | Removes the Athan Academy banner in the Quran tab, and the Academy popup and teacher feedback bar in the chapter reader. |
| <a id="athan-save-images-to-gallery"></a>[**Save images to gallery**](patches/src/main/kotlin/app/morphe/patches/athan/misc/share/SaveImagesToGalleryPatch.kt) | Adds a Save to gallery option to the share sheet of verse, dua and greeting card images. Requires Android 10 or newer. |
| <a id="athan-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/athan/misc/premium/UnlockPremiumPatch.kt) | Unlocks Athan Premium, including the premium Quran themes and athans, and removes ads. |

---

## AtloMaps (com.atlogis.atlomaps)

<img src="https://publicmetadata.co.uk/com.atlogis.atlomaps" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.0.2 | 2.0.3 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="atlomaps-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/atlomaps/misc/premium/UnlockPremiumPatch.kt) | Unlocks the custom map sources, navigation settings and backup restore. Premium map packages are not included. |

---

## atvTools (dev.vodik7.atvtools)

<img src="https://publicmetadata.co.uk/dev.vodik7.atvtools" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.3.2 |
| :---: |

| Patch | Details |
|---|---|
| <a id="atvtools-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/atvtools/misc/premium/UnlockPremiumPatch.kt) | Unlocks all features and removes the ads. |

---

## Audible (com.audible.application)

<img src="https://publicmetadata.co.uk/com.audible.application" width="48" height="48" align="top" alt="">

**Supported versions:**

| 26.30.05 | 26.38.08 | 26.41.06 | 26.41.07 | 26.43.17 | 26.45.08 |
| :---: | :---: | :---: | :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="audible-hide-membership-upselling"></a>[**Hide membership upselling**](patches/src/main/kotlin/app/morphe/patches/audible/misc/upselling/HideMembershipUpsellingPatch.kt) | Hides the membership promotion on the Home screen and the free trial bottom sheet. |
| <a id="audible-open-library-on-launch"></a>[**Open Library on launch**](patches/src/main/kotlin/app/morphe/patches/audible/startup/OpenLibraryOnLaunchPatch.kt) | Opens the Library tab instead of Home on launch. Applies only while signed in. |

---

## Audiobook Unlimited for Kindle (com.oneletters.audiobooksai)

<img src="https://publicmetadata.co.uk/com.oneletters.audiobooksai" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.7.30 |
| :---: |

| Patch | Details |
|---|---|
| <a id="audiobook-unlimited-for-kindle-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="audiobook-unlimited-for-kindle-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/audiobookunlimited/misc/premium/UnlockPremiumPatch.kt) | Unlocks Unlimited Books and skips the subscription offer shown after onboarding. Disables OTA updates. |

---

## AudioLab (com.hitrolab.audioeditor)

<img src="https://publicmetadata.co.uk/com.hitrolab.audioeditor" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.3.33 |
| :---: |

| Patch | Details |
|---|---|
| <a id="audiolab-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/audiolab/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro tools and removes ads, reward videos and upgrade prompts. The AI tools are not included. |

---

## Audiomack (com.audiomack)

<img src="https://publicmetadata.co.uk/com.audiomack" width="48" height="48" align="top" alt="">

**Supported versions:**

| 8.11.4 |
| :---: |

| Patch | Details |
|---|---|
| <a id="audiomack-export-downloaded-songs"></a>[**Export downloaded songs**](patches/src/main/kotlin/app/morphe/patches/audiomack/misc/downloads/ExportDownloadedSongsPatch.kt) | Adds Save to device and Share file to the menu of downloaded songs. Saved songs go to Music/Audiomack, named by artist and title and tagged with title, artist and album. Requires Android 10 or later. |
| <a id="audiomack-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="audiomack-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/audiomack/misc/premium/UnlockPremiumPatch.kt) | Unlocks Plus features. Region-limited songs are still restricted. |

---

## Bangla Calendar (com.outscar.bd.bongcal)

<img src="https://publicmetadata.co.uk/com.outscar.bd.bongcal" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.0.01q |
| :---: |

| Patch | Details |
|---|---|
| <a id="bangla-calendar-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/banglacalendar/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics and Crashlytics from collecting usage data. |
| <a id="bangla-calendar-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/banglacalendar/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium and removes ads. |

---

## BeHisabi (com.neonbhoot.behisabi)

<img src="https://publicmetadata.co.uk/com.neonbhoot.behisabi" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.0.7 |
| :---: |

| Patch | Details |
|---|---|
| <a id="behisabi-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/behisabi/misc/premium/UnlockPremiumPatch.kt) | Unlocks BeHisabi Pro features. AI features require your own API key. |

---

## BeReal (com.bereal.ft)

<img src="https://publicmetadata.co.uk/com.bereal.ft" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.98.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="bereal-back-up-memories"></a>[**Back up memories**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/backup/BackupMemoriesPatch.kt) | Adds Back up memories to hxreborn patches. Saves every memory as separate back and front photos with the time, place and caption, plus the BTS video. Writes JSON sidecars with location, friends' usernames and comments to Download/BeReal unless a folder is picked. On a mount install, keep BeReal open until it finishes. |
| <a id="bereal-change-download-location"></a>[**Change download location**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/download/ChangeDownloadLocationPatch.kt) | Saves downloaded BeReals to Pictures/BeReal, DCIM/BeReal or a folder you pick. Requires Android 10 or newer. |
| <a id="bereal-custom-befake-header"></a>[**Custom BeFake header**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/header/CustomBeFakeHeaderPatch.kt) | Replaces the BeReal wordmark in the top bar and posting header with BeFake. |
| <a id="bereal-custom-location"></a>[**Custom location**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/post/CustomLocationPatch.kt) | Sets the location shown on your BeReal to a place you choose on a map or by name. |
| <a id="bereal-custom-retake-count"></a>[**Custom retake count**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/post/CustomRetakeCountPatch.kt) | Asks before each BeReal how many retakes your friends see on it. |
| <a id="bereal-disable-data-collection"></a>[**Disable data collection**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/privacy/DisableDataCollectionPatch.kt) | Turns off ad tracking, Firebase crash reports and sessions, Amplitude and Datadog analytics, Adjust attribution and the Shake bug reporter. |
| <a id="bereal-disable-typing-indicator"></a>[**Disable typing indicator**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/typing/DisableTypingIndicatorPatch.kt) | Stops showing friends that you're typing in a chat. You still see when they type. |
| <a id="bereal-disable-view-receipts"></a>[**Disable view receipts**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/seen/DisableViewReceiptsPatch.kt) | Stops sending which BeReals you viewed and which chats you opened. |
| <a id="bereal-post-from-gallery"></a>[**Post from gallery**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/gallery/GalleryPhotosPatch.kt) | Adds a gallery button to the camera for picking the back and front photo of your BeReal. |
| <a id="bereal-remove-friends-of-friends-prompts"></a>[**Remove Friends of Friends prompts**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/fof/RemoveFriendsOfFriendsPromptsPatch.kt) | Removes the "Share with Friends of Friends" card and the end-of-feed messages. |
| <a id="bereal-remove-friends-of-friends-tab"></a>[**Remove Friends of Friends tab**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/fof/RemoveFriendsOfFriendsTabPatch.kt) | Removes the Friends of Friends tab from the home screen. |
| <a id="bereal-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/ads/RemoveAdsPatch.kt) | Removes the full-screen ad at launch and sponsored BeReals in both feeds. Stops the ad networks from loading. |
| <a id="bereal-remove-contacts-permission"></a>[**Remove contacts permission**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/privacy/RemoveContactsPermissionPatch.kt) | Stops BeReal from asking for or reading your phone's contacts. |
| <a id="bereal-remove-friend-suggestions"></a>[**Remove friend suggestions**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/friends/RemoveFriendSuggestionsPatch.kt) | Removes suggested friends, Contacts on BeReal and popular accounts from the Friends screen and the feed. |
| <a id="bereal-remove-late-flag"></a>[**Remove late flag**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/post/RemoveLateFlagPatch.kt) | Sends BeReals you take after the moment's window as on time. |
| <a id="bereal-remove-late-label"></a>[**Remove late label**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/late/RemoveLateLabelPatch.kt) | Replaces the late label on friends' BeReals with how long ago they posted. |
| <a id="bereal-remove-promotions"></a>[**Remove promotions**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/promos/RemovePromotionsPatch.kt) | Removes Recap cards, the "How's BeReal?" review prompt, the profile completion card, the notifications banner and the music tooltip. |
| <a id="bereal-remove-streak-prompts"></a>[**Remove streak prompts**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/streak/RemoveStreakPromptsPatch.kt) | Removes the offers to recover your streak and the Save your streak sheet. Your streak count doesn't change. |
| <a id="bereal-remove-update-prompts"></a>[**Remove update prompts**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/update/RemoveUpdatePromptsPatch.kt) | Removes BeReal's required and suggested update dialogs and the Play Store update prompt. |
| <a id="bereal-remove-watermark"></a>[**Remove watermark**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/download/RemoveWatermarkPatch.kt) | Saves downloaded BeReals at full size without the BeReal logo. |
| <a id="bereal-save-friends-bereals"></a>[**Save friends' BeReals**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/download/SaveFriendsBeRealsPatch.kt) | Adds Download to the 3-dot menu of friends' BeReals in the feed and on the RealMoji screen. |
| <a id="bereal-show-friends-posts-before-posting"></a>[**Show friends' posts before posting**](patches/src/main/kotlin/app/morphe/patches/bereal/misc/blur/ShowFriendsPostsPatch.kt) | Shows friends' BeReals in My Friends and Friends of Friends before you post your own. |

---

## BetterSleep (ipnossoft.rma.free)

<img src="https://publicmetadata.co.uk/ipnossoft.rma.free" width="48" height="48" align="top" alt="">

**Supported versions:**

| 26.20 |
| :---: |

| Patch | Details |
|---|---|
| <a id="bettersleep-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/bettersleep/misc/premium/UnlockPremiumPatch.kt) | Unlocks all premium content and skips the free trial screen. |

---

## Biobird Flashcards (com.biobird.exvee)

<img src="https://publicmetadata.co.uk/com.biobird.exvee" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.0.2 | 3.0.0 | 4.0.0 |
| :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="biobird-flashcards-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="biobird-flashcards-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/biobird/misc/premium/UnlockPremiumPatch.kt) | Unlocks all chapters and previous-year papers. Signing in requires GmsCore support. |

---

## BitTorrent (com.bittorrent.client)

<img src="https://publicmetadata.co.uk/com.bittorrent.client" width="48" height="48" align="top" alt="">

**Supported versions:**

| 8.4.3 |
| :---: |

| Patch | Details |
|---|---|
| <a id="bittorrent-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/bittorrent/misc/theme/AmoledThemePatch.kt) | Adds a setting that makes dialogs, snackbars, buttons and cards pure black in dark mode. |
| <a id="bittorrent-fix-screen-insets"></a>[**Fix screen insets**](patches/src/main/kotlin/app/morphe/patches/bittorrent/layout/insets/FixScreenInsetsPatch.kt) | Keeps main screens and settings clear of system bars and display cutouts on Android 17. |
| <a id="bittorrent-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/bittorrent/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro settings and removes ads. |

---

## Biwenger (com.biwenger.app)

<img src="https://publicmetadata.co.uk/com.biwenger.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.0.4 | 4.0.5 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="biwenger-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="biwenger-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/biwenger/ads/RemoveAdsPatch.kt) | Removes banner, interstitial, rewarded, native and app open ads. |
| <a id="biwenger-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/biwenger/premium/UnlockPremiumPatch.kt) | Unlocks the Premium league settings that the server accepts. The server refuses custom scoring, release clauses, substitutes and player tracking. Actions that cost credits still charge you, with no confirmation. |

---

## blockit (com.hypenet.focused)

<img src="https://publicmetadata.co.uk/com.hypenet.focused" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.2.9 |
| :---: |

| Patch | Details |
|---|---|
| <a id="blockit-free-parachutes"></a>[**Free parachutes**](patches/src/main/kotlin/app/morphe/patches/blockit/misc/parachutes/FreeParachutesPatch.kt) | Adds parachute packs without a purchase. |
| <a id="blockit-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/blockit/misc/premium/UnlockPremiumPatch.kt) | Unlocks Premium, the big suitcase and the sound pack. |

---

## Blossom Blast Saga (com.king.blossomblast)

<img src="https://publicmetadata.co.uk/com.king.blossomblast" width="48" height="48" align="top" alt="">

**Supported versions:**

| 100.234.0.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="blossom-blast-saga-add-boosters"></a>[**Add boosters**](patches/src/main/kotlin/app/morphe/patches/blossomblast/misc/boosters/AddBoostersPatch.kt) | Tops up each booster to 999 every time the game starts. |
| <a id="blossom-blast-saga-add-extra-moves"></a>[**Add extra moves**](patches/src/main/kotlin/app/morphe/patches/blossomblast/misc/moves/AddExtraMovesPatch.kt) | Starts every level with five extra moves. |
| <a id="blossom-blast-saga-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/ads/HideAdsPatch.kt) | Stops ad networks from starting and blocks rewarded and interstitial ads. |
| <a id="blossom-blast-saga-unlimited-lives"></a>[**Unlimited lives**](patches/src/main/kotlin/app/morphe/patches/blossomblast/misc/lives/UnlimitedLivesPatch.kt) | Gives unlimited lives for 24 hours every time the game starts. |
| <a id="blossom-blast-saga-unlock-booster-slots"></a>[**Unlock booster slots**](patches/src/main/kotlin/app/morphe/patches/blossomblast/misc/slots/UnlockBoosterSlotsPatch.kt) | Opens all booster slots from level 2. |

---

## BlurWall (apps.automan.blurwallpaper)

<img src="https://publicmetadata.co.uk/apps.automan.blurwallpaper" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.9.8 | 3.0.0 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="blurwall-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/blurwall/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black. |
| <a id="blurwall-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/blurwall/misc/tracking/DisableTrackingPatch.kt) | Stops the Google Mobile Ads SDK from starting and reading the advertising ID. |
| <a id="blurwall-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/blurwall/misc/premium/UnlockPremiumPatch.kt) | Unlocks the HalfBlur effect. |

---

## botim (im.thebot.messenger)

<img src="https://publicmetadata.co.uk/im.thebot.messenger" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.20.1 | 4.21.0 | 4.21.1 |
| :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="botim-disable-background-logging"></a>[**Disable background logging**](patches/src/main/kotlin/app/morphe/patches/botim/misc/logging/DisableBackgroundLoggingPatch.kt) | Stops the logging service and the extra background process the app starts at launch. |
| <a id="botim-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/botim/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics, Adjust and MoEngage from collecting usage data. |
| <a id="botim-fix-sign-in"></a>[**Fix sign-in**](patches/src/main/kotlin/app/morphe/patches/botim/misc/signin/FixSignInPatch.kt) | Fixes sending the verification code when signing in with a phone number. |
| <a id="botim-hide-all-tab"></a>[**Hide All tab**](patches/src/main/kotlin/app/morphe/patches/botim/layout/navigation/HideAllTabPatch.kt) | Hides the All tab from the bottom bar. |
| <a id="botim-hide-home-tab"></a>[**Hide Home tab**](patches/src/main/kotlin/app/morphe/patches/botim/layout/navigation/HideHomeTabPatch.kt) | Hides the Home tab from the bottom bar and opens the app on Calls. |
| <a id="botim-remove-home-sections"></a>[**Remove Home sections**](patches/src/main/kotlin/app/morphe/patches/botim/layout/home/RemoveHomeSectionsPatch.kt) | Removes the Services grid, Discover more banners and the search bar from Home. |
| <a id="botim-remove-botim-ai"></a>[**Remove botim AI**](patches/src/main/kotlin/app/morphe/patches/botim/layout/ai/RemoveBotimAiPatch.kt) | Removes Chat with botim AI from New chat and New call. |
| <a id="botim-spoof-signature"></a>[**Spoof signature**](patches/src/main/kotlin/app/morphe/patches/botim/misc/signin/SpoofSignaturePatch.kt) | Lets a new phone number receive the verification code and sign in. |
| <a id="botim-unlock-vip"></a>[**Unlock VIP**](patches/src/main/kotlin/app/morphe/patches/botim/misc/premium/UnlockVipPatch.kt) | Removes ads and upgrade prompts. Shows the VIP badge in the header. |

---

## CalcNote (com.burton999.notecal)

<img src="https://publicmetadata.co.uk/com.burton999.notecal" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.25.111 |
| :---: |

| Patch | Details |
|---|---|
| <a id="calcnote-unlock-pro"></a>[**Unlock pro**](patches/src/main/kotlin/app/morphe/patches/calcnote/misc/premium/UnlockProPatch.kt) | Unlocks Pro features and removes ads. |

---

## Candy Crush Saga (com.king.candycrushsaga)

<img src="https://publicmetadata.co.uk/com.king.candycrushsaga" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.340.0.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="candy-crush-saga-add-boosters"></a>[**Add boosters**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/misc/boosters/AddBoostersPatch.kt) | Adds 1,000 of each booster every time the game starts. A booster is usable from its usual level, or from the first level with Unlock booster slots. |
| <a id="candy-crush-saga-add-extra-moves"></a>[**Add extra moves**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/misc/moves/AddExtraMovesPatch.kt) | Starts every level with five extra moves. |
| <a id="candy-crush-saga-disable-events"></a>[**Disable events**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/misc/events/DisableEventsPatch.kt) | Turns off limited-time events such as races, quests and leagues. |
| <a id="candy-crush-saga-disable-notifications"></a>[**Disable notifications**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/misc/notifications/DisableNotificationsPatch.kt) | Stops game reminders and the notification permission prompt. |
| <a id="candy-crush-saga-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/misc/tracking/DisableTrackingPatch.kt) | Stops AppsFlyer, Braze, Firebase Analytics and Google measurement from collecting data, and removes the advertising ID and install referrer permissions. Braze push and in-app messages stop too. |
| <a id="candy-crush-saga-hide-ads"></a>**Hide ads** | Stops ad networks from starting and blocks rewarded and interstitial ads. |
| <a id="candy-crush-saga-parked-app-for-android-auto"></a>[**Parked app for Android Auto**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/misc/androidauto/ParkedAppPatch.kt) | Lists the game on the Android Auto car screen while parked. Requires Android 15 or newer. Android Auto needs Unknown sources turned on in its developer settings. |
| <a id="candy-crush-saga-remove-ad-offers"></a>[**Remove ad offers**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/ads/offers/RemoveAdOffersPatch.kt) | Removes offers to watch an ad for extra moves, lives or boosters. |
| <a id="candy-crush-saga-remove-offer-pop-ups"></a>[**Remove offer pop-ups**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/layout/popups/RemoveOfferPopupsPatch.kt) | Removes pop-ups selling bundles, passes and limited-time offers. |
| <a id="candy-crush-saga-remove-rating-prompts"></a>[**Remove rating prompts**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/misc/rating/RemoveRatingPromptsPatch.kt) | Removes prompts asking to rate the game after a level. |
| <a id="candy-crush-saga-unlimited-lives"></a>[**Unlimited lives**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/misc/lives/UnlimitedLivesPatch.kt) | Adds 24 hours of unlimited lives every time the game starts. |
| <a id="candy-crush-saga-unlock-booster-slots"></a>[**Unlock booster slots**](patches/src/main/kotlin/app/morphe/patches/candycrushsaga/misc/slots/UnlockBoosterSlotsPatch.kt) | Unlocks all booster slots from the first level. |

---

## Candy Crush Soda Saga (com.king.candycrushsodasaga)

<img src="https://publicmetadata.co.uk/com.king.candycrushsodasaga" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.331.3 |
| :---: |

| Patch | Details |
|---|---|
| <a id="candy-crush-soda-saga-add-boosters"></a>**Add boosters** | Adds 1,000 of each booster every time the game starts. A booster is usable from its usual level, or from the first level with Unlock booster slots. |
| <a id="candy-crush-soda-saga-add-extra-moves"></a>**Add extra moves** | Starts every level with five extra moves. |
| <a id="candy-crush-soda-saga-disable-events"></a>**Disable events** | Turns off limited-time events such as races, quests and leagues. |
| <a id="candy-crush-soda-saga-disable-notifications"></a>**Disable notifications** | Stops game reminders and the notification permission prompt. |
| <a id="candy-crush-soda-saga-disable-tracking"></a>**Disable tracking** | Stops AppsFlyer, Braze, Firebase Analytics and Google measurement from collecting data, and removes the advertising ID and install referrer permissions. Braze push and in-app messages stop too. |
| <a id="candy-crush-soda-saga-hide-ads"></a>**Hide ads** | Stops ad networks from starting and blocks rewarded and interstitial ads. |
| <a id="candy-crush-soda-saga-parked-app-for-android-auto"></a>**Parked app for Android Auto** | Lists the game on the Android Auto car screen while parked. Requires Android 15 or newer. Android Auto needs Unknown sources turned on in its developer settings. |
| <a id="candy-crush-soda-saga-remove-ad-offers"></a>**Remove ad offers** | Removes offers to watch an ad for extra moves, lives or boosters. |
| <a id="candy-crush-soda-saga-remove-offer-pop-ups"></a>**Remove offer pop-ups** | Removes pop-ups selling bundles, passes and limited-time offers. |
| <a id="candy-crush-soda-saga-remove-rating-prompts"></a>**Remove rating prompts** | Removes prompts asking to rate the game after a level. |
| <a id="candy-crush-soda-saga-unlimited-lives"></a>**Unlimited lives** | Adds 24 hours of unlimited lives every time the game starts. |
| <a id="candy-crush-soda-saga-unlock-booster-slots"></a>**Unlock booster slots** | Unlocks all booster slots from the first level. |

---

## Cast Plug-in for Musicolet (in.krosbits.castplugin)

<img src="https://publicmetadata.co.uk/in.krosbits.castplugin" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.9 |
| :---: |

| Patch | Details |
|---|---|
| <a id="cast-plug-in-for-musicolet-remove-app-protection"></a>[**Remove app protection**](patches/src/main/kotlin/app/morphe/patches/musicoletcastplugin/misc/protection/RemoveAppProtectionPatch.kt) | Lets a patched build start. Cast in Musicolet requires both apps patched with the same signing key. |

---

## Castle (com.external.castle)

<img src="https://publicmetadata.co.uk/com.external.castle" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.1.1 | 2.1.3 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="castle-disable-update-prompt"></a>[**Disable update prompt**](patches/src/main/kotlin/app/morphe/patches/castle/misc/update/DisableUpdatePromptPatch.kt) | Disables the new version dialog and the required update. |
| <a id="castle-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/castle/ads/RemoveAdsPatch.kt) | Removes banner, video, interstitial and rewarded ads. Actions that waited on a rewarded ad run immediately. |
| <a id="castle-spoof-signature"></a>[**Spoof signature**](patches/src/main/kotlin/app/morphe/patches/castle/misc/fix/signature/SpoofSignaturePatch.kt) | Removes the modified app warning that stops video playback. |
| <a id="castle-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/castle/misc/premium/UnlockPremiumPatch.kt) | Unlocks member-only titles, shows the account as Premium and removes the premium bundle offer. Requires a signed-in account. |

---

## Castle for TV (com.gxnet.castle.indiatv)

<img src="https://publicmetadata.co.uk/com.gxnet.castle.indiatv" width="48" height="48" align="top" alt="">

**Supported versions:**

| [1.1.5](https://download.sxwon.com/CastleTV_v1.1.5_0929_19_30_official.apk "Download stock APK") |
| :---: |

Always verify the downloaded APK's SHA-256 against a trusted checksum before patching.

| Patch | Details |
|---|---|
| <a id="castle-for-tv-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/castletv/ads/RemoveAdsPatch.kt) | Removes the pre-roll and mid-roll ads from playback. |
| <a id="castle-for-tv-spoof-signature"></a>[**Spoof signature**](patches/src/main/kotlin/app/morphe/patches/castletv/misc/fix/signature/SpoofSignaturePatch.kt) | Removes the modified app warning that stops video playback. |
| <a id="castle-for-tv-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/castletv/misc/premium/UnlockPremiumPatch.kt) | Unlocks member-only titles and shows the account as Premium. Requires a signed-in account. |

---

## Catzy (com.nieruo.healthapp)

<img src="https://publicmetadata.co.uk/com.nieruo.healthapp" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.61.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="catzy-max-intimacy-level"></a>**Max intimacy level** | Raises pet intimacy to the highest level. |
| <a id="catzy-remove-usage-limits"></a>**Remove usage limits** | Removes daily caps on store refreshes, blind boxes, feeding, petting and the Book of Answers. Opens the Item Recycling Center every day and shortens pet exploration to three minutes. |
| <a id="catzy-unlimited-cat-coins"></a>**Unlimited cat coins** | Buys every store item without running out of cat coins. |
| <a id="catzy-unlock-premium"></a>**Unlock premium** | Unlocks premium goals, journeys, breathing exercises, focus timers, sounds and themes. |

---

## Chess Scanner (com.stervs.chessscanner)

<img src="https://publicmetadata.co.uk/com.stervs.chessscanner" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.8.33 | 1.8.36 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="chess-scanner-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/chessscanner/misc/premium/UnlockPremiumPatch.kt) | Unlocks Chess Scanner Pro. |

---

## ClassDojo (com.classdojo.android)

<img src="https://publicmetadata.co.uk/com.classdojo.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 9.12.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="classdojo-unlock-progress-reports"></a>[**Unlock progress reports**](patches/src/main/kotlin/app/morphe/patches/classdojo/misc/reports/UnlockProgressReportsPatch.kt) | Unlocks detailed progress statistics. Date and points filters keep their existing limits. Report limits can return after in-app updates. |

---

## Clink (ltd.anti.clink)

<img src="https://publicmetadata.co.uk/ltd.anti.clink" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.9.2 | 2.0.2 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="clink-unlock-pro"></a>[**Unlock Pro**](patches/src/main/kotlin/app/morphe/patches/clink/misc/premium/UnlockProPatch.kt) | Unlocks Clink Pro. |

---

## Clip Studio Paint (jp.co.celsys.clipstudiopaint.googleplay)

<img src="https://publicmetadata.co.uk/jp.co.celsys.clipstudiopaint.googleplay" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.1.4 |
| :---: |

| Patch | Details |
|---|---|
| <a id="clip-studio-paint-open-drawing-on-launch"></a>[**Open drawing on launch**](patches/src/main/kotlin/app/morphe/patches/clipstudiopaint/misc/launch/OpenDrawingOnLaunchPatch.kt) | Opens the selected Studio or Simple Mode workspace on launch. |
| <a id="clip-studio-paint-skip-plan-selection"></a>[**Skip plan selection**](patches/src/main/kotlin/app/morphe/patches/clipstudiopaint/misc/launch/SkipPlanSelectionPatch.kt) | Opens the app in doodle mode without showing the plan selection screen. Log in from the home screen to use a plan. |
| <a id="clip-studio-paint-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/clipstudiopaint/misc/premium/UnlockPremiumPatch.kt) | Removes the monthly drawing time limit. Unlocks saving and exporting in doodle mode and full-length timelapse export with an optional logo. |

---

## Clone App (com.pengyou.cloneapp)

<img src="https://publicmetadata.co.uk/com.pengyou.cloneapp" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.2.3 | 4.2.5 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="clone-app-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/cloneapp/misc/tracking/DisableTrackingPatch.kt) | Stops sending usage events, the cloned app list and the private browser's search and download history to the developer, and disables Firebase Analytics. |
| <a id="clone-app-fix-manage-google-account"></a>[**Fix Manage Google account**](patches/src/main/kotlin/app/morphe/patches/cloneapp/misc/googleaccount/FixManageGoogleAccountPatch.kt) | Opens the Google Account settings from Manage Google account. |
| <a id="clone-app-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/cloneapp/ads/RemoveAdsPatch.kt) | Removes splash, banner, native and interstitial ads, including the ad shown when a cloned app opens, and the ad consent form. |
| <a id="clone-app-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/cloneapp/misc/premium/UnlockPremiumPatch.kt) | Unlocks VIP: multiple copies of each app, batch cloning, bubble mode, custom themes and no ads. Removes the 50% off offer and the Free VIP, Renew VIP and Get F-Code entries. |

---

## Coddy (tech.coddy.coddyapp)

<img src="https://publicmetadata.co.uk/tech.coddy.coddyapp" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.3.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="coddy-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/coddy/misc/premium/UnlockPremiumPatch.kt) | Unlocks Coddy Plus with unlimited energy and no ads, and removes the daily AI tutor limit and the free project limit. |

---

## Connected2.me (com.connected2.ozzy.c2m)

<img src="https://publicmetadata.co.uk/com.connected2.ozzy.c2m" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.396 |
| :---: |

| Patch | Details |
|---|---|
| <a id="connected2-me-enable-chat-in-all-regions"></a>[**Enable chat in all regions**](patches/src/main/kotlin/app/morphe/patches/connected2/misc/region/EnableChatInAllRegionsPatch.kt) | Enables sign-in and chat in regions the server blocks. Creating an account still requires a supported region. |
| <a id="connected2-me-keep-deleted-messages"></a>[**Keep deleted messages**](patches/src/main/kotlin/app/morphe/patches/connected2/misc/deleted/KeepDeletedMessagesPatch.kt) | Keeps messages the other person deletes and marks deleted text messages with 🗑. |
| <a id="connected2-me-remove-app-protection"></a>[**Remove app protection**](patches/src/main/kotlin/app/morphe/patches/connected2/misc/protection/RemoveAppProtectionPatch.kt) | Lets a patched build start. |
| <a id="connected2-me-remove-screenshot-restriction"></a>[**Remove screenshot restriction**](patches/src/main/kotlin/app/morphe/patches/connected2/misc/screenshot/RemoveScreenshotRestrictionPatch.kt) | Allows screenshots of view-once photos and videos. |
| <a id="connected2-me-spoof-signature"></a>[**Spoof signature**](patches/src/main/kotlin/app/morphe/patches/connected2/misc/fix/signature/SpoofSignaturePatch.kt) | Enables sign-up and sign-in. |

---

## Cubasis LE 3 (com.steinberg.cubasisle3)

<img src="https://publicmetadata.co.uk/com.steinberg.cubasisle3" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.8.6 |
| :---: |

| Patch | Details |
|---|---|
| <a id="cubasis-le-3-disable-telemetry"></a>[**Disable telemetry**](patches/src/main/kotlin/app/morphe/patches/cubasisle3/misc/telemetry/DisableTelemetryPatch.kt) | Stops crash reports, sessions and configuration requests from reaching Bugsnag. |
| <a id="cubasis-le-3-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/cubasisle3/misc/premium/UnlockPremiumPatch.kt) | Unlocks the full Cubasis version and the in-app shop content. |

---

## Cx File Explorer (com.cxinventor.file.explorer)

<img src="https://publicmetadata.co.uk/com.cxinventor.file.explorer" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.7.8 | 2.7.9 | 2.8.1 |
| :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="cx-file-explorer-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/cx/misc/theme/AmoledThemePatch.kt) | Adds a pure black option to the dark theme. |
| <a id="cx-file-explorer-dark-theme"></a>[**Dark theme**](patches/src/main/kotlin/app/morphe/patches/cx/misc/theme/DarkThemePatch.kt) | Renders the app's dark theme and adds it to the settings. |
| <a id="cx-file-explorer-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/cx/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium and removes ads. |

---

## Cxxdroid (ru.iiec.cxxdroid)

<img src="https://publicmetadata.co.uk/ru.iiec.cxxdroid" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.6_arm64 | 6.0_arm64 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="cxxdroid-amoled-dark-theme"></a>**AMOLED dark theme** | Adds an AMOLED option to Settings &gt; Appearance &gt; Editor theme (dark). Applies only while the Dark theme is active. |
| <a id="cxxdroid-disable-tracking"></a>**Disable tracking** | Stops Firebase Analytics from collecting usage data. |
| <a id="cxxdroid-unlock-premium"></a>**Unlock premium** | Unlocks premium and removes ads. |

---

## DepthPix (com.androholic.wallpapersdepthpix)

<img src="https://publicmetadata.co.uk/com.androholic.wallpapersdepthpix" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.0.42 |
| :---: |

| Patch | Details |
|---|---|
| <a id="depthpix-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="depthpix-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/depthpix/ads/HideAdsPatch.kt) | Removes banner, native, interstitial and rewarded ads. |
| <a id="depthpix-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/depthpix/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro wallpapers, 3D motion, particles and clock presets. Unlimited photo designs are not included. |

---

## Desh Telugu Keyboard (com.telug.keyboard.for.android)

<img src="https://publicmetadata.co.uk/com.telug.keyboard.for.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 17.7.1 | 17.7.3 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="desh-telugu-keyboard-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/deshtelugu/misc/tracking/DisableTrackingPatch.kt) | Stops the keyboard sending typed text, voice input, usage events, device details, install attribution and crash reports to Desh Keyboard, Meta and Firebase. |
| <a id="desh-telugu-keyboard-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/deshtelugu/misc/premium/UnlockPremiumPatch.kt) | Unlocks Premium, which removes promoted app suggestions and the Desh Keyboard signature added to messages and shared stickers. |

---

## DevCheck (flar2.devcheck)

<img src="https://publicmetadata.co.uk/flar2.devcheck" width="48" height="48" align="top" alt="">

**Supported versions:**

| 6.63 |
| :---: |

| Patch | Details |
|---|---|
| <a id="devcheck-hide-menu-items"></a>[**Hide menu items**](patches/src/main/kotlin/app/morphe/patches/devcheck/layout/menu/HideMenuItemsPatch.kt) | Hides Manage subscription, Share and About from the main menu. |
| <a id="devcheck-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/devcheck/misc/premium/UnlockPremiumPatch.kt) | Unlocks the Pro tools, tests, themes and colors. |

---

## Duolingo (com.duolingo)

<img src="https://publicmetadata.co.uk/com.duolingo" width="48" height="48" align="top" alt="">

**Supported versions:**

| 6.106.6 |
| :---: |

| Patch | Details |
|---|---|
| <a id="duolingo-disable-dynamic-app-icon"></a>[**Disable dynamic app icon**](patches/src/main/kotlin/app/morphe/patches/duolingo/misc/icon/DisableDynamicAppIconPatch.kt) | Keeps the default launcher icon. |
| <a id="duolingo-enable-debug-mode"></a>[**Enable debug mode**](patches/src/main/kotlin/app/morphe/patches/duolingo/misc/debug/EnableDebugModePatch.kt) | Shows the debug menu and debug button. Turn it on in the patches menu. |
| <a id="duolingo-unlimited-rampup-time"></a>[**Unlimited RampUp time**](patches/src/main/kotlin/app/morphe/patches/duolingo/misc/rampup/UnlimitedRampUpTimePatch.kt) | Holds the RampUp timer at one hour. |
| <a id="duolingo-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/duolingo/misc/premium/UnlockPremiumPatch.kt) | Unlocks Super or Max and keeps energy full. Max Video Call does not work. |

---

## DWG FastView (com.gstarmc.android)

<img src="https://publicmetadata.co.uk/com.gstarmc.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.19.4 | 5.19.6 | 5.20.0 | 5.21.0 |
| :---: | :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="dwg-fastview-block-telemetry"></a>[**Block telemetry**](patches/src/main/kotlin/app/morphe/patches/gstarmc/misc/telemetry/BlockTelemetryPatch.kt) | Blocks the Umeng, ByteDance and ad network analytics endpoints. |
| <a id="dwg-fastview-hide-rating-dialog"></a>[**Hide rating dialog**](patches/src/main/kotlin/app/morphe/patches/gstarmc/misc/rating/HideRatingDialogPatch.kt) | Removes the prompt asking for a store review. |
| <a id="dwg-fastview-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/gstarmc/misc/premium/UnlockPremiumPatch.kt) | Unlocks the paid drawing, annotation and measurement tools, and removes ads. |

---

## Echo Equalizer (com.hapibits.soundlift)

<img src="https://publicmetadata.co.uk/com.hapibits.soundlift" width="48" height="48" align="top" alt="">

**Supported versions:**

| 9.7 | 10.0 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="echo-equalizer-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/echoequalizer/misc/premium/UnlockPremiumPatch.kt) | Unlocks Echo Pro, including the 15- and 31-band equalizers, compressor, limiter and Safe Hearing protect mode. |

---

## Echogram (com.liori.echogram)

<img src="https://publicmetadata.co.uk/com.liori.echogram" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.1.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="echogram-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/echogram/misc/premium/UnlockPremiumPatch.kt) | Unlocks all premium features. |

---

## Etsy (com.etsy.android)

<img src="https://publicmetadata.co.uk/com.etsy.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 8.3.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="etsy-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/etsy/ads/HideAdsPatch.kt) | Removes promoted listings and the "with Ads" label from search results. |

---

## Fast STL Viewer (com.performance.meshview)

<img src="https://publicmetadata.co.uk/com.performance.meshview" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.94 | 2.95 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="fast-stl-viewer-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/faststlviewer/misc/premium/UnlockPremiumPatch.kt) | Unlocks slice view, colors, lighting, normals, measurements, printability analysis and transform, and removes ads. |

---

## Fddb (com.fddb)

<img src="https://publicmetadata.co.uk/com.fddb" width="48" height="48" align="top" alt="">

**Supported versions:**

| v7.8.7-Build-1-gms-release |
| :---: |

| Patch | Details |
|---|---|
| <a id="fddb-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/fddb/misc/premium/UnlockPremiumPatch.kt) | Unlocks the weekly report, intermittent fasting, the calorie and nutrient planners, and custom nutrient targets. |

---

## Flights25 (de.app.flighttracker)

<img src="https://publicmetadata.co.uk/de.app.flighttracker" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.4.7 |
| :---: |

| Patch | Details |
|---|---|
| <a id="flights25-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="flights25-remove-calendar-import-limit"></a>[**Remove calendar import limit**](patches/src/main/kotlin/app/morphe/patches/flights25/misc/premium/CalendarImportPatch.kt) | Removes limit on single-flight calendar imports. |
| <a id="flights25-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/flights25/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium features. AI imports, Gmail scans and automatic calendar imports are not unlocked. |

---

## FlipaClip (com.vblast.flipaclip)

<img src="https://publicmetadata.co.uk/com.vblast.flipaclip" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.0.3 |
| :---: |

| Patch | Details |
|---|---|
| <a id="flipaclip-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/flipaclip/misc/ads/RemoveAdsPatch.kt) | Removes fullscreen ads when opening projects. |
| <a id="flipaclip-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/flipaclip/misc/premium/UnlockPremiumPatch.kt) | Unlocks paid editing tools and export options. Voice maker and Magic Cut are excluded. |

---

## Flipboard (flipboard.app)

<img src="https://publicmetadata.co.uk/flipboard.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.3.68 |
| :---: |

| Patch | Details |
|---|---|
| <a id="flipboard-disable-automatic-home-feeds"></a>[**Disable automatic Home feeds**](patches/src/main/kotlin/app/morphe/patches/flipboard/home/DisableAutomaticHomeFeedsPatch.kt) | Skips automatically added Home tabs. |
| <a id="flipboard-remove-sponsored-cards"></a>[**Remove sponsored cards**](patches/src/main/kotlin/app/morphe/patches/flipboard/ads/RemoveSponsoredCardsPatch.kt) | Removes sponsored cards from feeds and banners from the article reader. |

---

## FocusNow (com.ferdinandobermeier.focusmate)

<img src="https://publicmetadata.co.uk/com.ferdinandobermeier.focusmate" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.8.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="focusnow-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/focusnow/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics and Crashlytics from collecting usage data. |
| <a id="focusnow-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="focusnow-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/focusnow/misc/premium/UnlockPremiumPatch.kt) | Unlocks FocusNow Pro. |

---

## ForusApp (com.myvitale.forus)

<img src="https://publicmetadata.co.uk/com.myvitale.forus" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.0.15 | 3.0.18 | 3.0.20 |
| :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="forusapp-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/forus/misc/premium/UnlockPremiumPatch.kt) | Unlocks all premium features. |

---

## Freedom (to.freedom.android2)

<img src="https://publicmetadata.co.uk/to.freedom.android2" width="48" height="48" align="top" alt="">

**Supported versions:**

| 7.18.2 |
| :---: |

| Patch | Details |
|---|---|
| <a id="freedom-remove-upsells"></a>[**Remove upsells**](patches/src/main/kotlin/app/morphe/patches/freedom/misc/upsell/RemoveUpsellsPatch.kt) | Removes the sign-up trial offers and the Unlock Premium prompts on Today and in Settings. |
| <a id="freedom-unlock-locked-mode"></a>[**Unlock Locked Mode**](patches/src/main/kotlin/app/morphe/patches/freedom/misc/lockedmode/UnlockLockedModePatch.kt) | Unlocks Locked Mode on free accounts. |

---

## FV File Explorer (com.folderv.file)

<img src="https://publicmetadata.co.uk/com.folderv.file" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.28.6 |
| :---: |

| Patch | Details |
|---|---|
| <a id="fv-file-explorer-remove-banner-ads"></a>[**Remove banner ads**](patches/src/main/kotlin/app/morphe/patches/fvfileexplorer/ads/RemoveBannerAdsPatch.kt) | Removes banner ads from the home screen and archive viewer. |

---

## Glass Widgets (com.jndapp.glass.widgets)

<img src="https://publicmetadata.co.uk/com.jndapp.glass.widgets" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.0.10 | 1.0.2 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="glass-widgets-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/glasswidgets/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro widgets, glass styles and settings and removes ads. |

---

## Graph Messenger (ir.ilmili.telegraph)

<img src="https://publicmetadata.co.uk/ir.ilmili.telegraph" width="48" height="48" align="top" alt="">

**Supported versions:**

| 13.2.2.2 |
| :---: |

| Patch | Details |
|---|---|
| <a id="graph-messenger-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/graphmessenger/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics and AppMetrica from collecting and sending usage data. |
| <a id="graph-messenger-keep-timed-media"></a>[**Keep timed media**](patches/src/main/kotlin/app/morphe/patches/graphmessenger/misc/timedmedia/KeepTimedMediaPatch.kt) | Stops photos, videos and voice messages with a timer from self-destructing after you open them. Media Telegram's servers already deleted can't be restored. |
| <a id="graph-messenger-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/graphmessenger/misc/ads/RemoveAdsPatch.kt) | Removes banner, interstitial, native, app open and rewarded ads, Graph's own promotions, Telegram sponsored messages and the proxy sponsor channel. |
| <a id="graph-messenger-remove-forced-update"></a>[**Remove forced update**](patches/src/main/kotlin/app/morphe/patches/graphmessenger/misc/update/RemoveForcedUpdatePatch.kt) | Removes the dialog that blocks the app when Graph marks this version unsupported. |
| <a id="graph-messenger-remove-restrictions"></a>[**Remove restrictions**](patches/src/main/kotlin/app/morphe/patches/graphmessenger/misc/restrictions/RemoveRestrictionsPatch.kt) | Allows copying, forwarding, saving and screenshots in chats and channels that restrict them. |
| <a id="graph-messenger-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/graphmessenger/misc/premium/UnlockPremiumPatch.kt) | Unlocks Graph Premium and Telegram Premium features in the client. Features Telegram's servers meter still need a Premium account. |

---

## Ground News (com.checkitt)

<img src="https://publicmetadata.co.uk/com.checkitt" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.0.0 | 5.1.0 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="ground-news-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/groundnews/misc/tracking/DisableTrackingPatch.kt) | Stops usage analytics and crash reporting. |
| <a id="ground-news-remove-article-ads"></a>[**Remove article ads**](patches/src/main/kotlin/app/morphe/patches/groundnews/misc/ads/RemoveArticleAdsPatch.kt) | Blocks advertising hosts in the in-app article browser. |
| <a id="ground-news-remove-promotions"></a>[**Remove promotions**](patches/src/main/kotlin/app/morphe/patches/groundnews/misc/promotions/RemovePromotionsPatch.kt) | Removes subscription promotions from the main screen. |
| <a id="ground-news-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/groundnews/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium features. |

---

## Habitify (co.unstatic.habitify)

<img src="https://publicmetadata.co.uk/co.unstatic.habitify" width="48" height="48" align="top" alt="">

**Supported versions:**

| 52.1.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="habitify-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="habitify-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/habitify/misc/premium/UnlockPremiumPatch.kt) | Unlocks lifetime Pro features and unlimited habits. Requires a signed-in account. |

---

## Hindu Calendar (com.alokmandavgane.hinducalendar)

<img src="https://publicmetadata.co.uk/com.alokmandavgane.hinducalendar" width="48" height="48" align="top" alt="">

**Supported versions:**

| 9.4.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="hindu-calendar-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/hinducalendar/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics from collecting usage data. |
| <a id="hindu-calendar-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/hinducalendar/ads/HideAdsPatch.kt) | Removes banner and interstitial ads and the Remove Ads menu item. |

---

## Hypic (com.xt.retouchoversea)

<img src="https://publicmetadata.co.uk/com.xt.retouchoversea" width="48" height="48" align="top" alt="">

**Supported versions:**

| 10.1.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="hypic-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="hypic-hide-upgrade-promotions"></a>[**Hide upgrade promotions**](patches/src/main/kotlin/app/morphe/patches/hypic/misc/upselling/HideUpgradePromotionsPatch.kt) | Hides the Pro button on the home screen, the Pro banner and promotions on the Me tab, free trial tips in the editor and on export, and Plans &amp; billing in Settings. Closes subscription pages before they show. |
| <a id="hypic-remove-reinstall-prompt"></a>[**Remove reinstall prompt**](patches/src/main/kotlin/app/morphe/patches/hypic/misc/prompt/RemoveReinstallPromptPatch.kt) | Removes the prompt to download the official app. Keeps saved app data. |
| <a id="hypic-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/hypic/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro filters, effects, templates and tools. AI features that use server credits are not unlocked. |

---

## iLovePDF (com.ilovepdf.www)

<img src="https://publicmetadata.co.uk/com.ilovepdf.www" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.2.3 |
| :---: |

| Patch | Details |
|---|---|
| <a id="ilovepdf-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="ilovepdf-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/ilovepdf/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium PDF tools and cloud storage features. |

---

## imo (com.imo.android.imoim)

<img src="https://publicmetadata.co.uk/com.imo.android.imoim" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2026.10.1031 |
| :---: |

| Patch | Details |
|---|---|
| <a id="imo-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/imo/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black. |
| <a id="imo-allow-sending-apk-files"></a>[**Allow sending APK files**](patches/src/main/kotlin/app/morphe/patches/imo/misc/files/AllowSendingApkFilesPatch.kt) | Adds the Apps option to the chat file menu for sending installed apps as APK files. |
| <a id="imo-allow-story-recording-during-calls"></a>[**Allow story recording during calls**](patches/src/main/kotlin/app/morphe/patches/imo/misc/story/AllowStoryRecordingInCallPatch.kt) | Allows recording and posting stories while a call is active. |
| <a id="imo-disable-analytics"></a>[**Disable analytics**](patches/src/main/kotlin/app/morphe/patches/imo/misc/analytics/DisableAnalyticsPatch.kt) | Blocks usage statistics sent to imo, AppsFlyer, Firebase and Facebook, and the advertising ID upload. |
| <a id="imo-disable-update-check"></a>[**Disable update check**](patches/src/main/kotlin/app/morphe/patches/imo/misc/update/DisableUpdateCheckPatch.kt) | Removes the forced update screen and the update prompts. |
| <a id="imo-enable-burn-after-read-messages"></a>[**Enable burn-after-read messages**](patches/src/main/kotlin/app/morphe/patches/imo/misc/burn/EnableBurnAfterReadPatch.kt) | Adds the burn-after-read option to voice messages in private and group chats. |
| <a id="imo-enable-chat-search-results"></a>[**Enable chat search results**](patches/src/main/kotlin/app/morphe/patches/imo/misc/search/EnableChatSearchResultsPatch.kt) | Adds a results list to in-chat search. |
| <a id="imo-enable-jump-to-first-unread"></a>[**Enable jump to first unread**](patches/src/main/kotlin/app/morphe/patches/imo/misc/unread/EnableJumpToUnreadPatch.kt) | Jumps to the first unread chat when the Chats tab is double-tapped. |
| <a id="imo-enable-message-translation"></a>[**Enable message translation**](patches/src/main/kotlin/app/morphe/patches/imo/misc/translation/EnableMessageTranslationPatch.kt) | Enables translating received messages. |
| <a id="imo-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="imo-hide-voice-club-tab"></a>[**Hide Voice Club tab**](patches/src/main/kotlin/app/morphe/patches/imo/misc/tabs/HideVoiceClubTabPatch.kt) | Removes the Voice Club tab from the home screen. |
| <a id="imo-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/imo/ads/HideAdsPatch.kt) | Removes splash, chat list, story and end-of-call ads, and skips the ad consent form. |
| <a id="imo-hide-invite-prompts"></a>[**Hide invite prompts**](patches/src/main/kotlin/app/morphe/patches/imo/misc/invite/HideInvitePromptsPatch.kt) | Hides the invite friends dialog, the invite entry in the chat list, the call sharing banner and the Invite to Official imo dialog for unregistered numbers. |
| <a id="imo-keep-screen-on-while-recording"></a>[**Keep screen on while recording**](patches/src/main/kotlin/app/morphe/patches/imo/misc/recording/KeepScreenOnWhileRecordingPatch.kt) | Keeps the screen on while recording a voice message. |
| <a id="imo-prevent-screenshot-detection"></a>[**Prevent screenshot detection**](patches/src/main/kotlin/app/morphe/patches/imo/misc/screenshot/PreventScreenshotDetectionPatch.kt) | Stops chats and calls from reporting screenshots and screen recordings to the other person. |
| <a id="imo-raise-gif-size-limit"></a>[**Raise GIF size limit**](patches/src/main/kotlin/app/morphe/patches/imo/misc/limits/RaiseGifSizeLimitPatch.kt) | Raises the size limit on GIFs picked for custom stickers to 100 MB. |
| <a id="imo-raise-time-machine-erase-limit"></a>[**Raise Time Machine erase limit**](patches/src/main/kotlin/app/morphe/patches/imo/misc/limits/RaiseTimeMachineEraseLimitPatch.kt) | Raises the limit on messages erased at once with Time Machine to 1000. |
| <a id="imo-raise-channel-video-limit"></a>[**Raise channel video limit**](patches/src/main/kotlin/app/morphe/patches/imo/misc/channel/RaiseChannelVideoLimitPatch.kt) | Raises the length limit on videos posted to a channel to 24 hours. |
| <a id="imo-raise-media-sending-limit"></a>[**Raise media sending limit**](patches/src/main/kotlin/app/morphe/patches/imo/misc/limits/RaiseMediaSendingLimitPatch.kt) | Raises the limit on photos, videos and files sent at once to 100 and the size of each photo and video to 3 GB. |
| <a id="imo-remove-chat-protection"></a>[**Remove chat protection**](patches/src/main/kotlin/app/morphe/patches/imo/misc/privacy/RemoveChatProtectionPatch.kt) | Allows copying, sharing, forwarding and downloading messages in chats with chat privacy or Time Machine turned on. |
| <a id="imo-remove-contact-selection-limit"></a>[**Remove contact selection limit**](patches/src/main/kotlin/app/morphe/patches/imo/misc/limits/RemoveContactSelectionLimitPatch.kt) | Raises the limit on contacts selected for iBubble close friends, Family Guard invites and group calls to 999. Experimental. |
| <a id="imo-remove-forwarding-restrictions"></a>[**Remove forwarding restrictions**](patches/src/main/kotlin/app/morphe/patches/imo/misc/forwarding/RemoveForwardingRestrictionsPatch.kt) | Allows forwarding disappearing and view-once text messages. |
| <a id="imo-remove-premium-entry"></a>[**Remove premium entry**](patches/src/main/kotlin/app/morphe/patches/imo/misc/premium/RemovePremiumEntryPatch.kt) | Removes the imo Premium entry from the Me page. |
| <a id="imo-remove-screenshot-restriction"></a>[**Remove screenshot restriction**](patches/src/main/kotlin/app/morphe/patches/imo/misc/screenshot/RemoveScreenshotRestrictionPatch.kt) | Allows screenshots and screen recording in chats, calls, media and profiles. |
| <a id="imo-remove-story-notification-limit"></a>[**Remove story notification limit**](patches/src/main/kotlin/app/morphe/patches/imo/misc/story/RemoveStoryNotificationLimitPatch.kt) | Removes the 5 minute spacing between story notifications from 18:00 to 23:59. |
| <a id="imo-remove-story-video-limit"></a>[**Remove story video limit**](patches/src/main/kotlin/app/morphe/patches/imo/misc/story/RemoveStoryVideoLimitPatch.kt) | Removes the length limit on story videos picked from the gallery or recorded. |
| <a id="imo-send-long-messages"></a>[**Send long messages**](patches/src/main/kotlin/app/morphe/patches/imo/misc/messages/SendLongMessagesPatch.kt) | Sends text longer than 2000 characters as consecutive messages. Raises the chat input limit to 10000 characters. Privacy Chat messages are cut at 2000 characters. |
| <a id="imo-show-full-profile-photos"></a>[**Show full profile photos**](patches/src/main/kotlin/app/morphe/patches/imo/misc/privacy/ShowFullProfilePhotosPatch.kt) | Opens profile photos in full size when their owner restricts it. |
| <a id="imo-show-invisible-friends-in-contacts"></a>[**Show invisible friends in contacts**](patches/src/main/kotlin/app/morphe/patches/imo/misc/invisiblefriends/ShowInvisibleFriendsInContactsPatch.kt) | Adds the Invisible Friends entry to the contacts list. Requires at least one invisible friend. |
| <a id="imo-spoof-signature"></a>[**Spoof signature**](patches/src/main/kotlin/app/morphe/patches/imo/misc/signin/SpoofSignaturePatch.kt) | Restores phone verification by call when signing in and lets the app get its push token. |
| <a id="imo-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/imo/misc/premium/UnlockPremiumPatch.kt) | Unlocks imo Premium. |

---

## Internet Speed Meter Lite (com.internet.speed.meter.lite)

<img src="https://publicmetadata.co.uk/com.internet.speed.meter.lite" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.7.0-lite |
| :---: |

| Patch | Details |
|---|---|
| <a id="internet-speed-meter-lite-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/internetspeedmeter/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics and Sessions from collecting usage data. |
| <a id="internet-speed-meter-lite-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/internetspeedmeter/ads/RemoveAdsPatch.kt) | Removes ads and the prompts to allow them. |
| <a id="internet-speed-meter-lite-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/internetspeedmeter/premium/UnlockPremiumPatch.kt) | Unlocks the notification dialog, smart notifications, themes, notification accent and the starting hour of the day. On a mount install, the quick overview has no per-app usage. |

---

## IoToToy (com.hardcodedjoy.iototoy)

<img src="https://publicmetadata.co.uk/com.hardcodedjoy.iototoy" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.1.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="iototoy-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/iototoy/misc/tracking/DisableTrackingPatch.kt) | Stops the AppBrain SDK from starting, loading ads and reading the advertising ID. Disables ML Kit usage logging. |
| <a id="iototoy-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/iototoy/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium blocks, diagrams over 25 blocks and runs longer than 15 minutes. Removes ads. |

---

## iVoox (com.ivoox.app)

<img src="https://publicmetadata.co.uk/com.ivoox.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.4041 |
| :---: |

| Patch | Details |
|---|---|
| <a id="ivoox-remove-rating-prompts"></a>[**Remove rating prompts**](patches/src/main/kotlin/app/morphe/patches/ivoox/misc/rate/RemoveRatingPromptsPatch.kt) | Removes the prompts asking for a rating. |
| <a id="ivoox-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/ivoox/misc/premium/UnlockPremiumPatch.kt) | Removes audio, banner, full-screen and splash screen ads, Premium and Plus offers, the player subscribe banner and Home alerts, and enables car mode, volume boost and up to 20 smart lists. |

---

## Joyn (de.prosiebensat1digital.seventv)

<img src="https://publicmetadata.co.uk/de.prosiebensat1digital.seventv" width="48" height="48" align="top" alt="">

**Supported versions:**

| 6.9.2-AOS-609282180 |
| :---: |

| Patch | Details |
|---|---|
| <a id="joyn-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/joyn/ads/HideAdsPatch.kt) | Removes ads before and during videos. Live TV is unavailable from outside Joyn's supported countries. |

---

## Joyn Austria (at.zappn)

<img src="https://publicmetadata.co.uk/at.zappn" width="48" height="48" align="top" alt="">

**Supported versions:**

| 6.9.0-AOS-JOYN_AT-2212264 | 6.9.2-AOS-JOYN_AT-2282180 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="joyn-austria-hide-ads"></a>**Hide ads** | Removes ads before and during videos. Live TV is unavailable from outside Joyn's supported countries. |

---

## Jujo (com.dev2a.junk_journal)

<img src="https://publicmetadata.co.uk/com.dev2a.junk_journal" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.2.5 | 1.2.6 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="jujo-gmscore-support"></a>[**GmsCore support**](patches/src/main/kotlin/app/morphe/patches/shared/misc/gms/GetCredentialGmsCoreSupportPatch.kt) | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="jujo-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/jujo/misc/premium/UnlockPremiumPatch.kt) | Unlocks unlimited journals and widgets, premium templates, brushes, papers, stamp options, HD video export without a watermark, more pictures per page, longer PDF imports and sync for every journal. |

---

## Jvdroid (ru.iiec.jvdroid)

<img src="https://publicmetadata.co.uk/ru.iiec.jvdroid" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.8 |
| :---: |

| Patch | Details |
|---|---|
| <a id="jvdroid-amoled-dark-theme"></a>**AMOLED dark theme** | Adds an AMOLED option to Settings &gt; Appearance &gt; Editor theme (dark). Applies only while the Dark theme is active. |
| <a id="jvdroid-disable-tracking"></a>**Disable tracking** | Stops Firebase Analytics from collecting usage data. |
| <a id="jvdroid-unlock-premium"></a>**Unlock premium** | Unlocks premium and removes ads. |

---

## Keepa (com.keepa.mobile)

<img src="https://publicmetadata.co.uk/com.keepa.mobile" width="48" height="48" align="top" alt="">

**Supported versions:**

| 6.7.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="keepa-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/keepa/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics and Crashlytics from collecting usage data. |
| <a id="keepa-multiple-accounts"></a>[**Multiple accounts**](patches/src/main/kotlin/app/morphe/patches/keepa/misc/accounts/MultipleAccountsPatch.kt) | Signs in to several Keepa accounts at once and lists their price watches together. Accounts are added and removed in Settings &gt; Accounts. |
| <a id="keepa-show-offer-counts"></a>[**Show offer counts**](patches/src/main/kotlin/app/morphe/patches/keepa/misc/offercount/ShowOfferCountsPatch.kt) | Shows the new and used offer counts in the product overview. |
| <a id="keepa-unlock-price-increase-tracking"></a>[**Unlock price increase tracking**](patches/src/main/kotlin/app/morphe/patches/keepa/misc/priceincrease/UnlockPriceIncreaseTrackingPatch.kt) | Adds the rise option when creating or editing a price watch. |

---

## Kick (com.kick.mobile)

<img src="https://publicmetadata.co.uk/com.kick.mobile" width="48" height="48" align="top" alt="">

**Supported versions:**

| Any version |
| :---: |

| Patch | Details |
|---|---|
| <a id="kick-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/kick/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black. Disables over-the-air updates that would restore the original background. |

---

## Klassik Radio+ (de.klassikradio.app)

<img src="https://publicmetadata.co.uk/de.klassikradio.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| p5.12.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="klassik-radio-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/klassikradio/misc/premium/UnlockPremiumPatch.kt) | Unlocks the premium music channels, on-demand playback and unlimited track skipping, and hides the trial banner. Requires a signed-in account. |

---

## Lagguy Wallpapers (com.lagguy.wallpapers)

<img src="https://publicmetadata.co.uk/com.lagguy.wallpapers" width="48" height="48" align="top" alt="">

**Supported versions:**

| 40.0.44 |
| :---: |

| Patch | Details |
|---|---|
| <a id="lagguy-wallpapers-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="lagguy-wallpapers-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/lagguy/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro collections, wide walls, original-size downloads and unlimited saves. Requires a signed-in account. |

---

## LaunchBox (com.unbrokensoftware.launchbox)

<img src="https://www.launchbox-app.com/apple-touch-icon-120x120.png" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.23 |
| :---: |

| Patch | Details |
|---|---|
| <a id="launchbox-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/launchbox/misc/premium/UnlockPremiumPatch.kt) | Removes the 100-game limit and the license prompts. |

---

## LED Blinker (com.ledblinker)

<img src="https://publicmetadata.co.uk/com.ledblinker" width="48" height="48" align="top" alt="">

**Supported versions:**

| 26.01.08 |
| :---: |

| Patch | Details |
|---|---|
| <a id="led-blinker-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/ledblinker/misc/premium/UnlockPremiumPatch.kt) | Unlocks pocket mode, notification history and statistics, and removes ads. |

---

## Liftoff (com.gymbros.app)

<img src="https://publicmetadata.co.uk/com.gymbros.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.17.5 | 2.18.0 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="liftoff-unlock-customizations"></a>[**Unlock customizations**](patches/src/main/kotlin/app/morphe/patches/liftoff/misc/customizations/UnlockCustomizationsPatch.kt) | Unlocks themes, profile borders and profile banners. |
| <a id="liftoff-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/liftoff/misc/premium/UnlockPremiumPatch.kt) | Unlocks rank analysis, bodygraph, full chart history and nutrition tools. Requires a Liftoff account. |

---

## LingoDeer (com.lingodeer)

<img src="https://publicmetadata.co.uk/com.lingodeer" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.99.423 |
| :---: |

| Patch | Details |
|---|---|
| <a id="lingodeer-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="lingodeer-skip-sign-in-prompt"></a>[**Skip sign-in prompt**](patches/src/main/kotlin/app/morphe/patches/lingodeer/misc/signin/SkipSignInPromptPatch.kt) | Opens lessons past the second unit without an account. |
| <a id="lingodeer-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/lingodeer/misc/premium/UnlockPremiumPatch.kt) | Unlocks every course, lesson and practice mode. |

---

## Magic Earth (com.generalmagic.magicearth)

<img src="https://publicmetadata.co.uk/com.generalmagic.magicearth" width="48" height="48" align="top" alt="">

**Supported versions:**

| 7.1.26.26.21DB1F1B.3C81F7001 | 7.1.26.38.B4684D03.6207A61BA | 7.1.26.42.F9918E5E.7B9203C7B |
| :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="magic-earth-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/magicearth/misc/premium/UnlockPremiumPatch.kt) | Unlocks offline maps, real-time traffic and weather, elevation maps, activity recording, vehicle profiles, personalized voices and Android Auto. |

---

## MangaTime (net.mangatime.app)

<img src="https://publicmetadata.co.uk/net.mangatime.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.24.5 | 1.25.3 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="mangatime-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="mangatime-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/mangatime/misc/premium/UnlockPremiumPatch.kt) | Removes ads and chapter limits, and unlocks reading history, binge mode and section settings. AI recommendation refresh and custom explore views are not unlocked. |

---

## May: Beautiful Bullet Journal (com.azeravn.bulletjournal)

<img src="https://publicmetadata.co.uk/com.azeravn.bulletjournal" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.0.216 |
| :---: |

| Patch | Details |
|---|---|
| <a id="may-beautiful-bullet-journal-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/bulletjournal/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium features. |

---

## MBC Shahid (net.mbc.shahid)

<img src="https://publicmetadata.co.uk/net.mbc.shahid" width="48" height="48" align="top" alt="">

**Supported versions:**

| 8.11.0 | 8.12.2 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="mbc-shahid-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/shahid/misc/tracking/DisableTrackingPatch.kt) | Stops AppsFlyer, mParticle, CleverTap, NPAW, Bugsnag, Firebase Analytics and Facebook from collecting usage data. |
| <a id="mbc-shahid-fix-sign-in"></a>[**Fix sign-in**](patches/src/main/kotlin/app/morphe/patches/shahid/misc/signin/FixSignInPatch.kt) | Fixes signing in with an email, phone number or social account. |
| <a id="mbc-shahid-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="mbc-shahid-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/shahid/ads/RemoveAdsPatch.kt) | Removes video ads, pause ads and sponsored cards on the home, show and Shorts pages. |
| <a id="mbc-shahid-remove-root-detection"></a>[**Remove root detection**](patches/src/main/kotlin/app/morphe/patches/shahid/misc/root/RemoveRootDetectionPatch.kt) | Opens the app and plays videos on rooted devices. |
| <a id="mbc-shahid-unlock-full-hd"></a>[**Unlock Full HD**](patches/src/main/kotlin/app/morphe/patches/shahid/misc/premium/UnlockPremiumPatch.kt) | Unlocks Full HD playback. VIP titles require a subscription. |

---

## MedicNEET (com.halalfire.medicneet)

<img src="https://publicmetadata.co.uk/com.halalfire.medicneet" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.5.138 |
| :---: |

| Patch | Details |
|---|---|
| <a id="medicneet-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="medicneet-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/medicneet/misc/premium/UnlockPremiumPatch.kt) | Unlocks the AIMTS test series, the PYQ generator, video lectures and tier 1 and 2 questions in every topic. Tier 3 questions and flashcards are not included. |

---

## MemoNeet (com.adithya.memoneet)

<img src="https://publicmetadata.co.uk/com.adithya.memoneet" width="48" height="48" align="top" alt="">

**Supported versions:**

| 63.3 |
| :---: |

| Patch | Details |
|---|---|
| <a id="memoneet-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="memoneet-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/memoneet/misc/premium/UnlockPremiumPatch.kt) | Unlocks the premium question banks, notes, test series, previous-year papers and shop plans, with no energy cost or ads. Signing in requires GmsCore support. |

---

## MiniShare (com.omniashare.minishare)

<img src="https://publicmetadata.co.uk/com.omniashare.minishare" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.6.6 |
| :---: |

| Patch | Details |
|---|---|
| <a id="minishare-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/minishare/misc/theme/AmoledThemePatch.kt) | Replaces the light theme with a pure black dark theme. |

---

## Monet Launcher (com.klevico.monet)

<img src="https://publicmetadata.co.uk/com.klevico.monet" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.1.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="monet-launcher-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/monet/misc/premium/UnlockPremiumPatch.kt) | Unlocks Monet Premium. |

---

## Moon+ Reader (com.flyersoft.moonreader)

<img src="https://publicmetadata.co.uk/com.flyersoft.moonreader" width="48" height="48" align="top" alt="">

**Supported versions:**

| 10.7 | 10.8 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="moon-reader-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/moonreader/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics from collecting usage data and the Google Mobile Ads SDK from starting at launch. |
| <a id="moon-reader-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/moonreader/ads/HideAdsPatch.kt) | Hides full-screen ads and the Remove ads button in the side menu. |
| <a id="moon-reader-unlock-pro"></a>[**Unlock pro**](patches/src/main/kotlin/app/morphe/patches/moonreader/misc/premium/UnlockProPatch.kt) | Unlocks Pro features, including PDF annotations and form filling, WebDAV and FTP sync, text-to-speech controls, the home screen widget and extra toolbar and shelf options. On a mount install, the home screen widget is not available. |

---

## MovieBox (com.community.oneroom)

<img src="https://publicmetadata.co.uk/com.community.oneroom" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.0.03.1020.03 |
| :---: |

| Patch | Details |
|---|---|
| <a id="moviebox-disable-update-prompts"></a>[**Disable update prompts**](patches/src/main/kotlin/app/morphe/patches/moviebox/misc/update/DisableUpdatePromptsPatch.kt) | Removes update prompts and forced updates. |
| <a id="moviebox-hide-buzzbox"></a>[**Hide BuzzBox**](patches/src/main/kotlin/app/morphe/patches/moviebox/misc/buzzbox/HideBuzzBoxPatch.kt) | Removes the BuzzBox tabs from the bottom navigation bar and the home screen. |
| <a id="moviebox-hide-midnight-tab"></a>[**Hide Midnight tab**](patches/src/main/kotlin/app/morphe/patches/moviebox/misc/midnight/HideMidnightTabPatch.kt) | Removes the Midnight 18+ tab from the home screen. |
| <a id="moviebox-hide-permanent-notification"></a>[**Hide permanent notification**](patches/src/main/kotlin/app/morphe/patches/moviebox/misc/permanentnotification/HidePermanentNotificationPatch.kt) | Hides the permanent notification. |
| <a id="moviebox-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/moviebox/ads/RemoveAdsPatch.kt) | Removes ads. |
| <a id="moviebox-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/moviebox/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium, unlimited playback time, unlimited downloads, every resolution and the Laboratory menu, and removes membership offers. |

---

## MovieBox TV (com.community.mbox.tv)

<img src="https://publicmetadata.co.uk/com.community.mbox.tv" width="48" height="48" align="top" alt="">

**Supported versions:**

| [1.2.0.0923.web_socialmedia](https://apk.aoneroom.com/apk/moviebox/release/1.2.0.0923/com.community.mbox.tv-release-1.2.0.0923-ws-50040018-h.apk "Download stock APK") |
| :---: |

Always verify the downloaded APK's SHA-256 against a trusted checksum before patching.

| Patch | Details |
|---|---|
| <a id="moviebox-tv-disable-update-prompts"></a>[**Disable update prompts**](patches/src/main/kotlin/app/morphe/patches/movieboxtv/misc/update/DisableUpdatePromptsPatch.kt) | Removes update prompts and forced updates. Requires Android 10 or later. |
| <a id="moviebox-tv-fix-quality-selection"></a>[**Fix quality selection**](patches/src/main/kotlin/app/morphe/patches/movieboxtv/misc/quality/FixQualitySelectionPatch.kt) | Makes the quality choices in the player switch the stream to 480p, 720p or 1080p. Requires Android 10 or later. |
| <a id="moviebox-tv-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/movieboxtv/misc/ads/RemoveAdsPatch.kt) | Removes ads. Requires Android 10 or later. |
| <a id="moviebox-tv-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/movieboxtv/misc/premium/UnlockPremiumPatch.kt) | Unlocks full-length playback and removes region blocks. Requires Android 10 or later. |

---

## MuscleWiki (com.musclewiki.macro)

<img src="https://publicmetadata.co.uk/com.musclewiki.macro" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.2.7 |
| :---: |

| Patch | Details |
|---|---|
| <a id="musclewiki-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="musclewiki-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/musclewiki/misc/premium/UnlockPremiumPatch.kt) | Unlocks MuscleWiki Premium and removes ads. Requires a signed-in account. |

---

## Musicolet (in.krosbits.musicolet)

<img src="https://publicmetadata.co.uk/in.krosbits.musicolet" width="48" height="48" align="top" alt="">

**Supported versions:**

| 6.14.1 build540 |
| :---: |

| Patch | Details |
|---|---|
| <a id="musicolet-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/musicolet/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro features. |

---

## Musixmatch (com.musixmatch.android.lyrify)

<img src="https://publicmetadata.co.uk/com.musixmatch.android.lyrify" width="48" height="48" align="top" alt="">

**Supported versions:**

| 8.5.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="musixmatch-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/musixmatch/misc/premium/UnlockPremiumPatch.kt) | Unlocks offline lyrics, animated backgrounds and Android Auto lyrics. |

---

## Muviz Widgets (com.sparkine.widgets)

<img src="https://publicmetadata.co.uk/com.sparkine.widgets" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.1.5.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="muviz-widgets-fix-weather"></a>[**Fix weather**](patches/src/main/kotlin/app/morphe/patches/muvizwidgets/misc/weather/FixWeatherPatch.kt) | Loads weather and finds the current city in the weather widgets. Fetches weather from Open-Meteo and the current city from BigDataCloud. |
| <a id="muviz-widgets-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/muvizwidgets/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro widgets and styles and removes ads. |

---

## My APK (com.andatsoft.myapk.fwa)

<img src="https://publicmetadata.co.uk/com.andatsoft.myapk.fwa" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.1.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="my-apk-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/myapk/premium/UnlockPremiumPatch.kt) | Unlocks premium and removes ads. |

---

## MyMoveset (com.soulbreakers.mymoveset)

<img src="https://publicmetadata.co.uk/com.soulbreakers.mymoveset" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.4.0 | 1.5.0 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="mymoveset-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/mymoveset/misc/tracking/DisableTrackingPatch.kt) | Stops the install identifier from reaching Expo and usage events from reaching Google. Disables OTA updates. |
| <a id="mymoveset-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/mymoveset/misc/premium/UnlockPremiumPatch.kt) | Unlocks unlimited move views, goals and move cards, manual sync, earlier library updates and card customization. Disables OTA updates. |

---

## MyScript Math (com.myscript.math)

<img src="https://publicmetadata.co.uk/com.myscript.math" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.0.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="myscript-math-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/myscriptmath/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics, Firebase Sessions and Survicate surveys from collecting usage data. |
| <a id="myscript-math-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/myscriptmath/misc/premium/UnlockPremiumPatch.kt) | Unlocks all features after the free trial ends. |

---

## MyScript Notes (com.myscript.nebo)

<img src="https://publicmetadata.co.uk/com.myscript.nebo" width="48" height="48" align="top" alt="">

**Supported versions:**

| 7.4.9 |
| :---: |

| Patch | Details |
|---|---|
| <a id="myscript-notes-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/myscriptnotes/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics, Firebase Sessions and Survicate surveys from collecting usage data. |
| <a id="myscript-notes-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/myscriptnotes/misc/premium/UnlockPremiumPatch.kt) | Unlocks unlimited notes and pages, text conversion, shape and scratch-out gestures, PDF import and AI tools. |

---

## Nara Baby (com.naraorganics.nara)

<img src="https://publicmetadata.co.uk/com.naraorganics.nara" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.8.3 | 2.9.0 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="nara-baby-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/narababy/misc/tracking/DisableTrackingPatch.kt) | Stops Google Analytics, Crashlytics, Facebook App Events, Kochava and Klaviyo from collecting usage data. |
| <a id="nara-baby-remove-forced-update"></a>[**Remove forced update**](patches/src/main/kotlin/app/morphe/patches/narababy/misc/update/RemoveForcedUpdatePatch.kt) | Removes the update screen that blocks the app when a newer build is published. |
| <a id="nara-baby-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/narababy/misc/premium/UnlockPremiumPatch.kt) | Unlocks Nara Lifetime for every family the account belongs to. |

---

## NEETprep (com.lernr.app)

<img src="https://publicmetadata.co.uk/com.lernr.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| 15.0.93 |
| :---: |

| Patch | Details |
|---|---|
| <a id="neetprep-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="neetprep-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/neetprep/misc/premium/UnlockPremiumPatch.kt) | Unlocks question and test explanations, Ask AI and attempt deletion. |

---

## NEETprep Essential (com.gooded.essential)

<img src="https://publicmetadata.co.uk/com.gooded.essential" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.2.95 |
| :---: |

| Patch | Details |
|---|---|
| <a id="neetprep-essential-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="neetprep-essential-remove-screenshot-restriction"></a>[**Remove screenshot restriction**](patches/src/main/kotlin/app/morphe/patches/neetprepessential/misc/screenshot/RemoveScreenshotRestrictionPatch.kt) | Allows screenshots and screen recording on every screen. |
| <a id="neetprep-essential-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/neetprepessential/misc/premium/UnlockPremiumPatch.kt) | Unlocks all chapters, practice sets, flashcards, hardest questions, mock tests and PYQ Marked NCERT questions. |

---

## Network Analyzer (net.techet.netanalyzerlite.an)

<img src="https://publicmetadata.co.uk/net.techet.netanalyzerlite.an" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="network-analyzer-bonjour-browser"></a>[**Bonjour browser**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/bonjour/BonjourBrowserPatch.kt) | Lists the Bonjour services announced on the Wi-Fi network, grouped by type, with each service's host, address, port and TXT records. |
| <a id="network-analyzer-common-ports-list"></a>[**Common ports list**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/commonports/CommonPortsPatch.kt) | Adds an editor for the ports the Common port scan checks. |
| <a id="network-analyzer-custom-device-names"></a>[**Custom device names**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/devicenames/CustomDeviceNamesPatch.kt) | Adds custom names for Wi-Fi networks and LAN devices, shown in the Wi-Fi and LAN lists. |
| <a id="network-analyzer-export-results"></a>[**Export results**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/export/ExportResultsPatch.kt) | Adds an export menu to Ping, Traceroute, Port Scan, DNS, Whois, LAN Scan, Wi-Fi Signal and Information, with plain text and CSV output. |
| <a id="network-analyzer-external-ip-details"></a>[**External IP details**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/externalip/ExternalIpDetailsPatch.kt) | Adds a Details button to the External IP rows that shows the address's hostname, network operator and location. |
| <a id="network-analyzer-host-details"></a>[**Host details**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/hostdetails/HostDetailsPatch.kt) | Adds a details page for each ping reply and traceroute hop with its address, hostname, round-trip times, TTL, AS and location. |
| <a id="network-analyzer-lan-device-details"></a>[**LAN device details**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/lan/LanDeviceDetailsPatch.kt) | Adds a details page for each device in the LAN scan with its names, Wake on LAN, Bonjour and UPnP services and web interface, plus text and CSV export of the scan. |
| <a id="network-analyzer-lan-scan-history"></a>[**LAN scan history**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/lanhistory/LanScanHistoryPatch.kt) | Saves each finished LAN scan and its devices, lets you star, delete or clear saved scans, and scans a custom IP range. |
| <a id="network-analyzer-ping-statistics"></a>[**Ping statistics**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/pingstatistics/PingStatisticsPatch.kt) | Adds a bar chart of the current or last ping's round-trip times with min, median, max and lost packets. |
| <a id="network-analyzer-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/ads/RemoveAdsPatch.kt) | Removes the banner ads, the ad consent prompt and the Pro upgrade prompts. |
| <a id="network-analyzer-settings"></a>[**Settings**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/settings/SettingsPatch.kt) | Adds a Settings screen with the theme, Wi-Fi scan frequency and 6 GHz scan options. |
| <a id="network-analyzer-speed-test"></a>[**Speed test**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/speedtest/SpeedTestPatch.kt) | Adds an internet speed test with download and upload charts and a test history. |
| <a id="network-analyzer-tool-options"></a>[**Tool options**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/tooloptions/ToolOptionsPatch.kt) | Adds ping protocol, TCP ping, probe delay, payload size, don't fragment and audible ping, traceroute and port scan delays, whois and RDAP servers, and DNS flags. |
| <a id="network-analyzer-tools-history"></a>[**Tools history**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/toolshistory/ToolsHistoryPatch.kt) | Lists the hosts queried in Tools with their last options, and lets you star or delete them. Tapping an entry opens it in Tools. |
| <a id="network-analyzer-upnp-browser"></a>[**UPnP browser**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/upnp/UpnpBrowserPatch.kt) | Lists the UPnP devices on the Wi-Fi network with their model, manufacturer, embedded devices and services. |
| <a id="network-analyzer-visual-traceroute"></a>[**Visual traceroute**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/visualtraceroute/VisualTraceroutePatch.kt) | Shows the hops of the current or last traceroute on an OpenStreetMap map. Map tiles load from tile.openstreetmap.org. |
| <a id="network-analyzer-wi-fi-channel-usage"></a>[**Wi-Fi channel usage**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/channelusage/ChannelUsagePatch.kt) | Adds a chart of how busy each Wi-Fi channel is, stacking the networks that share it. |
| <a id="network-analyzer-wi-fi-network-details"></a>[**Wi-Fi network details**](patches/src/main/kotlin/app/morphe/patches/netanalyzer/misc/wifidetails/WifiDetailsPatch.kt) | Adds a details page for each network in the Signal list with its channel range, utilization, beamforming support, maximum speed and modulation. |

---

## NetworkFlow (com.evozi.network)

<img src="https://publicmetadata.co.uk/com.evozi.network" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.1.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="networkflow-unlock-pro"></a>[**Unlock pro**](patches/src/main/kotlin/app/morphe/patches/networkflow/misc/premium/UnlockProPatch.kt) | Unlocks the full app ranking, all usage periods and date ranges, the Total Speed notification layout, colourful Home cards and unlimited data plans. Removes ads. Requires an arm64 device. |

---

## Nextdoor (com.nextdoor)

<img src="https://publicmetadata.co.uk/com.nextdoor" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.38.1 | 5.39.2 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="nextdoor-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="nextdoor-remove-sponsored-posts"></a>[**Remove sponsored posts**](patches/src/main/kotlin/app/morphe/patches/nextdoor/ads/RemoveSponsoredPostsPatch.kt) | Removes sponsored posts from the feed and For Sale ads from listings and search results. |

---

## Notein (com.orion.notein.global)

<img src="https://publicmetadata.co.uk/com.orion.notein.global" width="48" height="48" align="top" alt="">

**Supported versions:**

| v2.1.37.0 | v2.1.49.0 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="notein-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/notein/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro features and removes the notebook limit. AI and cloud sync are not included. |

---

## Notesnook (com.streetwriters.notesnook)

<img src="https://publicmetadata.co.uk/com.streetwriters.notesnook" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.4.12 | 3.4.13 | 3.4.14 | 3.4.15 |
| :---: | :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="notesnook-material-you-theme"></a>[**Material You theme**](patches/src/main/kotlin/app/morphe/patches/notesnook/theme/MaterialYouThemePatch.kt) | Applies the system accent colours to the default light and dark themes. Colours refresh when the app restarts, and start from the second launch. Requires Android 12 or later. |
| <a id="notesnook-unlock-pro"></a>[**Unlock pro**](patches/src/main/kotlin/app/morphe/patches/notesnook/misc/premium/UnlockProPatch.kt) | Unlocks task lists, callouts, app lock, and the notebook, tag, colour and reminder limits. Requires a signed-in account. The server still enforces storage, attachment size, monographs and SMS 2FA. |

---

## OK (ru.ok.android)

<img src="https://publicmetadata.co.uk/ru.ok.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 26.10.15 |
| :---: |

| Patch | Details |
|---|---|
| <a id="ok-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/okru/ads/RemoveAdsPatch.kt) | Removes sponsored posts, video and clip ads, and ads in messages, notifications, discussions, games, music and Dzen. |

---

## One4Home Launcher (com.one4studio.one4home)

<img src="https://publicmetadata.co.uk/com.one4studio.one4home" width="48" height="48" align="top" alt="">

**Supported versions:**

| 0.5.145 | 0.5.190 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="one4home-launcher-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/one4home/misc/premium/UnlockPremiumPatch.kt) | Unlocks One4Home Pro and the collector Pals. |

---

## Opal (com.withopal.opal)

<img src="https://publicmetadata.co.uk/com.withopal.opal" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.17.0 | 4.19.1 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="opal-skip-sign-in"></a>[**Skip sign-in**](patches/src/main/kotlin/app/morphe/patches/opal/misc/signin/SkipSignInPatch.kt) | Skips sign-in and runs Opal without an account. Features that need the Opal server are unavailable. |
| <a id="opal-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/opal/misc/premium/UnlockPremiumPatch.kt) | Unlocks Opal Pro. |

---

## Outlook (com.microsoft.office.outlook)

<img src="https://publicmetadata.co.uk/com.microsoft.office.outlook" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.2645.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="outlook-add-custom-themes"></a>[**Add custom themes**](patches/src/main/kotlin/app/morphe/patches/outlook/misc/theme/CustomThemesPatch.kt) | Adds Custom color and Custom picture to Outlook's theme picker. Pick any color on a color wheel or any photo from the gallery. |
| <a id="outlook-add-unread-shortcut"></a>[**Add unread shortcut**](patches/src/main/kotlin/app/morphe/patches/outlook/misc/shortcut/AddUnreadShortcutPatch.kt) | Adds an Unread shortcut to the Outlook icon that opens unread mail from every account. |
| <a id="outlook-fix-sign-in"></a>[**Fix sign-in**](patches/src/main/kotlin/app/morphe/patches/outlook/misc/fix/signin/FixSignInPatch.kt) | Fixes signing in with Microsoft accounts. |
| <a id="outlook-gmscore-support"></a>[**GmsCore support**](patches/src/main/kotlin/app/morphe/patches/outlook/misc/gms/GmsCoreSupportPatch.kt) | Signs in to Google accounts through GmsCore instead of Google Play Services. Requires GmsCore. |
| <a id="outlook-hide-copilot"></a>[**Hide Copilot**](patches/src/main/kotlin/app/morphe/patches/outlook/misc/copilot/HideCopilotPatch.kt) | Hides Copilot chat, summaries, scheduling and writing help on all accounts, regardless of the Copilot setting in Outlook. |
| <a id="outlook-hide-navigation-buttons"></a>[**Hide navigation buttons**](patches/src/main/kotlin/app/morphe/patches/outlook/layout/navigation/HideNavigationButtonsPatch.kt) | Hides Calendar, Apps or the whole bottom bar. Choose which in the patches menu. |
| <a id="outlook-keep-messages-unread"></a>[**Keep messages unread**](patches/src/main/kotlin/app/morphe/patches/outlook/mail/unread/KeepMessagesUnreadPatch.kt) | Leaves messages unread after opening them, or flagging or moving them from the reading pane. |
| <a id="outlook-open-email-templates"></a>[**Open email templates**](patches/src/main/kotlin/app/morphe/patches/outlook/misc/templates/OpenEmailTemplatesPatch.kt) | Opens .oft template attachments as new drafts without a Microsoft 365 subscription. |
| <a id="outlook-remove-inbox-ads"></a>[**Remove inbox ads**](patches/src/main/kotlin/app/morphe/patches/outlook/ads/RemoveInboxAdsPatch.kt) | Removes sponsored messages from Focused and Other. |
| <a id="outlook-select-multiple-search-results"></a>[**Select multiple search results**](patches/src/main/kotlin/app/morphe/patches/outlook/search/bulkactions/SelectMultipleSearchResultsPatch.kt) | Long-press search results to select several, then mark, move or delete them together. Mail tab of search only. |

---

## Patreon (com.patreon.android)

<img src="https://publicmetadata.co.uk/com.patreon.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 126.46.0.14 |
| :---: |

| Patch | Details |
|---|---|
| <a id="patreon-disable-quips-and-autoplay"></a>[**Disable Quips and autoplay**](patches/src/main/kotlin/app/morphe/patches/patreon/layout/quips/DisableQuipsAndAutoplayPatch.kt) | Removes Quip cards and the Quip button, and stops feed videos from autoplaying or preloading. |
| <a id="patreon-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/patreon/misc/tracking/DisableTrackingPatch.kt) | Disables Amplitude, AppsFlyer, Sentry, Sift and Mux data collection. |
| <a id="patreon-hide-home-tab"></a>[**Hide Home tab**](patches/src/main/kotlin/app/morphe/patches/patreon/layout/launcher/HideHomeTabPatch.kt) | Removes the Home tab and starts on Memberships. Accounts without memberships keep the tab. |
| <a id="patreon-hide-navigation-buttons"></a>[**Hide navigation buttons**](patches/src/main/kotlin/app/morphe/patches/patreon/layout/navigation/HideNavigationButtonsPatch.kt) | Removes chosen tabs from the bottom bar.<br><sub>Options: Hide Feed, Hide Library, Hide Quip, +2 more</sub> |
| <a id="patreon-hide-recommended-creators"></a>[**Hide recommended creators**](patches/src/main/kotlin/app/morphe/patches/patreon/layout/home/HideRecommendedCreatorsPatch.kt) | Limits Home to creators you're a member of. |
| <a id="patreon-hide-search-suggestions"></a>[**Hide search suggestions**](patches/src/main/kotlin/app/morphe/patches/patreon/layout/search/HideSearchSuggestionsPatch.kt) | Removes suggested creators and topics from Explore. |
| <a id="patreon-hide-upgrade-promotions"></a>[**Hide upgrade promotions**](patches/src/main/kotlin/app/morphe/patches/patreon/layout/promotions/HideUpgradePromotionsPatch.kt) | Removes upgrade, rejoin and gift banners, upsell sheets, social proof and the in-feed survey. |
| <a id="patreon-remove-app-prompts"></a>[**Remove app prompts**](patches/src/main/kotlin/app/morphe/patches/patreon/misc/prompts/RemoveAppPromptsPatch.kt) | Removes the rating prompt and the in-app update prompt. |

---

## Period Tracker and Calendar (com.lbrc.PeriodCalendar)

<img src="https://publicmetadata.co.uk/com.lbrc.PeriodCalendar" width="48" height="48" align="top" alt="">

**Supported versions:**

| 13.6.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="period-tracker-and-calendar-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/periodcalendar/misc/premium/UnlockPremiumPatch.kt) | Unlocks Premium and the cycle synchronization workbook. Removes ads. |

---

## Perplexity (ai.perplexity.app.android)

<img src="https://publicmetadata.co.uk/ai.perplexity.app.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.104.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="perplexity-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/perplexity/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black. |
| <a id="perplexity-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/perplexity/misc/tracking/DisableTrackingPatch.kt) | Stops usage events, crash reports and performance data from reaching Perplexity, Singular and Firebase. |
| <a id="perplexity-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="perplexity-hide-upgrade-promotions"></a>[**Hide upgrade promotions**](patches/src/main/kotlin/app/morphe/patches/perplexity/misc/upselling/HideUpgradePromotionsPatch.kt) | Hides the subscription offer after signing in, the Upgrade plan banner, home screen promotions and upgrade cards in answers. Plan limits still apply. |

---

## Photo Editor Pro (photo.editor.photoeditor.photoeditorpro)

<img src="https://publicmetadata.co.uk/photo.editor.photoeditor.photoeditorpro" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.791.265 | 1.802.266 | 1.811.267 | 1.823.268 |
| :---: | :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="photo-editor-pro-disable-push-notifications"></a>[**Disable push notifications**](patches/src/main/kotlin/app/morphe/patches/photoeditorpro/misc/notifications/DisablePushNotificationsPatch.kt) | Blocks push notifications and the prompt asking to allow them. |
| <a id="photo-editor-pro-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/photoeditorpro/ads/HideAdsPatch.kt) | Removes banner, interstitial, app-open and rewarded ads. |
| <a id="photo-editor-pro-hide-share-options"></a>[**Hide share options**](patches/src/main/kotlin/app/morphe/patches/photoeditorpro/layout/HideShareOptionsPatch.kt) | Removes share buttons from the save screen and centers the saved photo. |
| <a id="photo-editor-pro-inspect-ai-requests"></a>[**Inspect AI requests**](patches/src/main/kotlin/app/morphe/patches/photoeditorpro/diagnostics/TraceAiRequestsPatch.kt) | Shows the network calls an AI tool makes, such as HTTP requests and Firebase uploads. Keeps a log of them. |
| <a id="photo-editor-pro-show-ai-progress"></a>[**Show AI progress**](patches/src/main/kotlin/app/morphe/patches/photoeditorpro/aitools/ShowAiProgressPatch.kt) | Shows the current stage of AI Remove and Enhance on their loading screens. |
| <a id="photo-editor-pro-speed-up-ai-tools"></a>[**Speed up AI tools**](patches/src/main/kotlin/app/morphe/patches/photoeditorpro/aitools/SpeedUpAiToolsPatch.kt) | Shortens the AI tool wait by polling for the result more often and uploading the photo in larger chunks. |
| <a id="photo-editor-pro-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/photoeditorpro/misc/premium/UnlockPremiumPatch.kt) | Unlocks pro tools and removes export watermarks and upgrade prompts. |

---

## Photone (io.lightray.photone)

<img src="https://publicmetadata.co.uk/io.lightray.photone" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.5.4 |
| :---: |

| Patch | Details |
|---|---|
| <a id="photone-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/photone/misc/premium/UnlockPremiumPatch.kt) | Unlocks all light sources, extended PAR, Pro guides, Pro settings and the full toolbox. Pro support is not included. |

---

## Pixlr (com.pixlr.express)

<img src="https://publicmetadata.co.uk/com.pixlr.express" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.9.3 | 3.9.4 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="pixlr-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/pixlr/misc/tracking/DisableTrackingPatch.kt) | Disables Firebase Analytics, Singular, Facebook App Events, Crashlytics and the app's own usage events. |
| <a id="pixlr-hide-home-sections"></a>[**Hide home sections**](patches/src/main/kotlin/app/morphe/patches/pixlr/layout/home/HideHomeSectionsPatch.kt) | Hides the selected sections from the home screen.<br><sub>Options: Hide featured carousel, Hide AI Create, Hide AI Tools, +1 more</sub> |
| <a id="pixlr-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/pixlr/ads/RemoveAdsPatch.kt) | Removes ads and the advertising consent prompt. |
| <a id="pixlr-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/pixlr/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium stickers and effects. Removes ads, subscription screens, the daily save limit and the video watermark. AI tools need account credits. |

---

## Pleco (com.pleco.chinesesystem)

<img src="https://publicmetadata.co.uk/com.pleco.chinesesystem" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.2.101 |
| :---: |

| Patch | Details |
|---|---|
| <a id="pleco-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/pleco/misc/tracking/DisableTrackingPatch.kt) | Stops device identifiers and usage counters from reaching Pleco's servers and usage events from reaching Google. |
| <a id="pleco-unlock-add-ons"></a>[**Unlock add-ons**](patches/src/main/kotlin/app/morphe/patches/pleco/misc/addons/UnlockAddOnsPatch.kt) | Unlocks the Flashcard System, Document Reader, Optical Character Recognizer and Enhanced Handwriting Recognizer. Excludes paid dictionaries, audio and voices. |

---

## Pocket Whip (com.greenstone.pocketwhip)

<img src="https://publicmetadata.co.uk/com.greenstone.pocketwhip" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.3 |
| :---: |

| Patch | Details |
|---|---|
| <a id="pocket-whip-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/pocketwhip/ads/HideAdsPatch.kt) | Hides the banner and stops ads from loading. |
| <a id="pocket-whip-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/pocketwhip/misc/premium/UnlockPremiumPatch.kt) | Unlocks all whips. |

---

## Podcast Republic (com.itunestoppodcastplayer.app)

<img src="https://publicmetadata.co.uk/com.itunestoppodcastplayer.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| 26.11.4R |
| :---: |

| Patch | Details |
|---|---|
| <a id="podcast-republic-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/podcastrepublic/misc/ads/RemoveAdsPatch.kt) | Removes in-app ads and frees up banner space. |

---

## Poe (com.poe.android)

<img src="https://publicmetadata.co.uk/com.poe.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| a3.80.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="poe-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/poe/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics, Sessions and Performance Monitoring from collecting usage data. |
| <a id="poe-spoof-signature"></a>[**Spoof signature**](patches/src/main/kotlin/app/morphe/patches/poe/misc/fix/signature/SpoofSignaturePatch.kt) | Enables push notifications. |

---

## Poweramp Equalizer (com.maxmpz.equalizer)

<img src="https://publicmetadata.co.uk/com.maxmpz.equalizer" width="48" height="48" align="top" alt="">

**Supported versions:**

| build-1035-bundle-play |
| :---: |

| Patch | Details |
|---|---|
| <a id="poweramp-equalizer-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/powerampequalizer/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium features and removes the time-limited trial. |

---

## Prayer Times and Qibla (com.reworewo.prayertimes)

<img src="https://publicmetadata.co.uk/com.reworewo.prayertimes" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.3.2 | 3.8.3 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="prayer-times-and-qibla-remove-app-protection"></a>[**Remove app protection**](patches/src/main/kotlin/app/morphe/patches/prayertimes/misc/protection/RemoveAppProtectionPatch.kt) | Lets a patched build of the Wear OS app start. Syncing with the phone app requires both apps patched with the same signing key. |
| <a id="prayer-times-and-qibla-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/prayertimes/misc/premium/UnlockPremiumPatch.kt) | Unlocks random Adhan, unlimited personal reminders, theme colors and other Pro settings. |

---

## Privacy Notes (app.privacynotes)

<img src="https://publicmetadata.co.uk/app.privacynotes" width="48" height="48" align="top" alt="">

**Supported versions:**

| 0.654.2 |
| :---: |

| Patch | Details |
|---|---|
| <a id="privacy-notes-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/privacynotes/premium/UnlockPremiumPatch.kt) | Unlocks Pro features, unlimited devices and larger file uploads. The server still blocks note history and caps storage at 50 MB. |

---

## Proton Authenticator (proton.android.authenticator)

<img src="https://publicmetadata.co.uk/proton.android.authenticator" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.3.10 |
| :---: |

| Patch | Details |
|---|---|
| <a id="proton-authenticator-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/protonauthenticator/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black and can dim menus, bars and buttons. |
| <a id="proton-authenticator-custom-accent-color"></a>[**Custom accent color**](patches/src/main/kotlin/app/morphe/patches/protonauthenticator/misc/theme/AccentColorPatch.kt) | Changes the accent color. Choose a color in the patches menu. |
| <a id="proton-authenticator-hide-layout-components"></a>[**Hide layout components**](patches/src/main/kotlin/app/morphe/patches/protonauthenticator/misc/layout/HideLayoutComponentsPatch.kt) | Adds settings to hide the Spread the word section, the Discover Proton section and the Proton Pass banner. |
| <a id="proton-authenticator-m3e-loading-indicator"></a>[**M3E loading indicator**](patches/src/main/kotlin/app/morphe/patches/protonauthenticator/misc/loadingindicator/LoadingIndicatorPatch.kt) | Shows the pull to refresh indicator as the Material 3 Expressive shape-morphing loading indicator. |
| <a id="proton-authenticator-material-3-switches"></a>[**Material 3 switches**](patches/src/main/kotlin/app/morphe/patches/protonauthenticator/misc/theme/MaterialSwitchesPatch.kt) | Adds check and close icons to switches. |

---

## Proton Lumo (me.proton.android.lumo)

<img src="https://publicmetadata.co.uk/me.proton.android.lumo" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.2.0-gms | 2.2.0-nogms |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="proton-lumo-material-3-switches"></a>[**Material 3 switches**](patches/src/main/kotlin/app/morphe/patches/lumo/misc/materialswitch/MaterialSwitchesPatch.kt) | Shows switches in the Material 3 style with check and close icons. |
| <a id="proton-lumo-remove-tap-highlight"></a>[**Remove tap highlight**](patches/src/main/kotlin/app/morphe/patches/lumo/misc/taphighlight/RemoveWebTapHighlightPatch.kt) | Removes the highlight flash on tapped buttons and links. |
| <a id="proton-lumo-store-encryption-key-in-tee"></a>[**Store encryption key in TEE**](patches/src/main/kotlin/app/morphe/patches/lumo/misc/strongbox/StoreKeyInTeePatch.kt) | Stores the local encryption key in the TEE instead of StrongBox. Prevents the Encryption error on devices whose StrongBox rejects the key. |
| <a id="proton-lumo-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/lumo/misc/premium/UnlockPremiumPatch.kt) | Unlocks unlimited projects and removes upgrade prompts. Usage limits for models, messages and images still apply. |

---

## Proton Mail (ch.protonmail.android)

<img src="https://publicmetadata.co.uk/ch.protonmail.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 7.12.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="proton-mail-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black and can dim menus, bars and buttons. |
| <a id="proton-mail-custom-accent-color"></a>[**Custom accent color**](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/theme/AccentColorPatch.kt) | Changes the accent color. Choose a color in the patches menu. |
| <a id="proton-mail-hide-layout-components"></a>[**Hide layout components**](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/layout/HideLayoutComponentsPatch.kt) | Adds settings to hide the Subscription and Report a problem sidebar entries. |
| <a id="proton-mail-hide-upgrade-promotions"></a>[**Hide upgrade promotions**](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/upselling/HideUpgradePromotionsPatch.kt) | Hides the top-bar upgrade button, promotional sidebar rows and the auto-delete upgrade banner in Trash and Spam, and skips the seasonal sale screens shown at launch. Keeps the Empty trash and Empty spam buttons. |
| <a id="proton-mail-m3e-loading-indicator"></a>[**M3E loading indicator**](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/loadingindicator/LoadingIndicatorPatch.kt) | Shows the pull to refresh indicator as the Material 3 Expressive shape-morphing loading indicator. |
| <a id="proton-mail-material-3-switches"></a>[**Material 3 switches**](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/materialswitch/MaterialSwitchesPatch.kt) | Shows switches in the Material 3 style with check and close icons. |
| <a id="proton-mail-remove-sent-from-signature"></a>[**Remove 'Sent from' signature**](patches/src/main/kotlin/app/morphe/patches/protonmail/signature/RemoveSentFromSignaturePatch.kt) | Removes the 'Sent from Proton Mail' signature and unlocks the mobile signature setting. |
| <a id="proton-mail-remove-free-accounts-limit"></a>[**Remove free accounts limit**](patches/src/main/kotlin/app/morphe/patches/protonmail/account/RemoveFreeAccountsLimitPatch.kt) | Removes the limit on signed-in free accounts. |
| <a id="proton-mail-scheduled-trash-and-spam-deletion"></a>[**Scheduled Trash and Spam deletion**](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/scheduleddeletion/ScheduledDeletionPatch.kt) | Deletes all messages in Trash and Spam on separate configurable schedules, also while the app is closed. Deleted messages cannot be recovered. |
| <a id="proton-mail-swipe-to-switch-accounts"></a>[**Swipe to switch accounts**](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/accountswipe/SwipeToSwitchAccountsPatch.kt) | Switches between signed-in accounts by swiping up or down on the account avatar. |
| <a id="proton-mail-unlock-custom-time-picker"></a>[**Unlock custom time picker**](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/scheduling/UnlockCustomTimePickerPatch.kt) | Enables picking a custom date and time when snoozing conversations and scheduling messages. |

---

## Proton Pass (proton.android.pass)

<img src="https://publicmetadata.co.uk/proton.android.pass" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.40.3 | 1.41.2 | 1.41.3 | 1.41.4 | 1.41.5 |
| :---: | :---: | :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="proton-pass-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/protonpass/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black and can dim menus, bars and buttons. |
| <a id="proton-pass-custom-accent-color"></a>[**Custom accent color**](patches/src/main/kotlin/app/morphe/patches/protonpass/misc/theme/AccentColorPatch.kt) | Changes the accent color. Choose a color in the patches menu. |
| <a id="proton-pass-floating-navigation-bar"></a>[**Floating navigation bar**](patches/src/main/kotlin/app/morphe/patches/protonpass/misc/navigationbar/FloatingNavigationBarPatch.kt) | Adds optional floating navigation with adjustable blur and glass. |
| <a id="proton-pass-hide-layout-components"></a>[**Hide layout components**](patches/src/main/kotlin/app/morphe/patches/protonpass/misc/layout/HideLayoutComponentsPatch.kt) | Adds settings to hide the Help center, Spread the word and About sections. |
| <a id="proton-pass-hide-navigation-bar-on-scroll"></a>[**Hide navigation bar on scroll**](patches/src/main/kotlin/app/morphe/patches/protonpass/misc/navigationbar/HideNavigationBarOnScrollPatch.kt) | Hides the bottom navigation bar on scroll down and shows it on scroll up. |
| <a id="proton-pass-hide-promotional-messages"></a>[**Hide promotional messages**](patches/src/main/kotlin/app/morphe/patches/protonpass/misc/inappmessages/HidePromotionalMessagesPatch.kt) | Hides promotional banners, offers and pop-up messages. |
| <a id="proton-pass-hide-upgrade-promotions"></a>[**Hide upgrade promotions**](patches/src/main/kotlin/app/morphe/patches/protonpass/misc/upselling/HideUpgradePromotionsPatch.kt) | Hides the Upgrade buttons, upgrade prompts and the welcome offer after signing in. Plan limits still apply. |
| <a id="proton-pass-m3e-loading-indicator"></a>[**M3E loading indicator**](patches/src/main/kotlin/app/morphe/patches/protonpass/misc/loadingindicator/LoadingIndicatorPatch.kt) | Shows the pull to refresh indicator as the Material 3 Expressive shape-morphing loading indicator. |
| <a id="proton-pass-material-3-switches"></a>[**Material 3 switches**](patches/src/main/kotlin/app/morphe/patches/protonpass/misc/theme/MaterialSwitchesPatch.kt) | Shows switches in the Material 3 style with check and close icons. |
| <a id="proton-pass-remove-free-accounts-limit"></a>[**Remove free accounts limit**](patches/src/main/kotlin/app/morphe/patches/protonpass/account/RemoveFreeAccountsLimitPatch.kt) | Removes the limit on signed-in free accounts. |

---

## Proton Pass (F-Droid) (proton.android.pass.fdroid)

<img src="https://publicmetadata.co.uk/proton.android.pass.fdroid" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.41.5 |
| :---: |

| Patch | Details |
|---|---|
| <a id="proton-pass-f-droid-amoled-dark-theme"></a>**AMOLED dark theme** | Replaces the dark theme background with pure black and can dim menus, bars and buttons. |
| <a id="proton-pass-f-droid-custom-accent-color"></a>**Custom accent color** | Changes the accent color. Choose a color in the patches menu. |
| <a id="proton-pass-f-droid-floating-navigation-bar"></a>**Floating navigation bar** | Adds optional floating navigation with adjustable blur and glass. |
| <a id="proton-pass-f-droid-hide-layout-components"></a>**Hide layout components** | Adds settings to hide the Help center, Spread the word and About sections. |
| <a id="proton-pass-f-droid-hide-navigation-bar-on-scroll"></a>**Hide navigation bar on scroll** | Hides the bottom navigation bar on scroll down and shows it on scroll up. |
| <a id="proton-pass-f-droid-hide-promotional-messages"></a>**Hide promotional messages** | Hides promotional banners, offers and pop-up messages. |
| <a id="proton-pass-f-droid-hide-upgrade-promotions"></a>**Hide upgrade promotions** | Hides the Upgrade buttons, upgrade prompts and the welcome offer after signing in. Plan limits still apply. |
| <a id="proton-pass-f-droid-m3e-loading-indicator"></a>**M3E loading indicator** | Shows the pull to refresh indicator as the Material 3 Expressive shape-morphing loading indicator. |
| <a id="proton-pass-f-droid-material-3-switches"></a>**Material 3 switches** | Shows switches in the Material 3 style with check and close icons. |
| <a id="proton-pass-f-droid-remove-free-accounts-limit"></a>**Remove free accounts limit** | Removes the limit on signed-in free accounts. |

---

## Proton VPN (ch.protonvpn.android)

<img src="https://publicmetadata.co.uk/ch.protonvpn.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.21.57.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="proton-vpn-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black and can dim menus, bars and buttons. |
| <a id="proton-vpn-custom-accent-color"></a>[**Custom accent color**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/theme/AccentColorPatch.kt) | Changes the accent color. Choose a color in the patches menu. |
| <a id="proton-vpn-disable-telemetry"></a>[**Disable telemetry**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/telemetry/DisableTelemetryPatch.kt) | Stops sending usage statistics and diagnostics to Proton. |
| <a id="proton-vpn-floating-navigation-bar"></a>[**Floating navigation bar**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/navigationbar/FloatingNavigationBarPatch.kt) | Adds optional floating navigation with adjustable blur and glass. |
| <a id="proton-vpn-hide-upgrade-promotions"></a>[**Hide upgrade promotions**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/upselling/HideUpgradePromotionsPatch.kt) | Hides settings that need a paid plan, upgrade banners, the Discover VPN Plus carousel and special offers. |
| <a id="proton-vpn-material-3-switches"></a>[**Material 3 switches**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/theme/MaterialSwitchesPatch.kt) | Adds check and close icons to switches. |
| <a id="proton-vpn-remove-server-change-delay"></a>[**Remove server change delay**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/delay/RemoveServerChangeDelayPatch.kt) | Removes the wait between server changes on free plans. |
| <a id="proton-vpn-show-free-server-locations"></a>[**Show free server locations**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/freeservers/ShowFreeServerLocationsPatch.kt) | Lists free server locations in Countries and Search and connects to the one you pick. Applies only to free plans. |
| <a id="proton-vpn-unlock-lan-connections"></a>[**Unlock LAN connections**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/lan/UnlockLanConnectionsPatch.kt) | Unlocks LAN connections on free plans. |
| <a id="proton-vpn-unlock-netshield"></a>[**Unlock NetShield**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/netshield/UnlockNetShieldPatch.kt) | Unlocks NetShield ad and tracker blocking on free plans. |
| <a id="proton-vpn-unlock-connection-preferences"></a>[**Unlock connection preferences**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/connectionpreferences/UnlockConnectionPreferencesPatch.kt) | Unlocks the default connection, recent connections and excluded locations on free plans. |
| <a id="proton-vpn-unlock-custom-dns"></a>[**Unlock custom DNS**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/customdns/UnlockCustomDnsPatch.kt) | Unlocks custom DNS on free plans. |
| <a id="proton-vpn-unlock-profiles"></a>[**Unlock profiles**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/profiles/UnlockProfilesPatch.kt) | Unlocks profiles on free plans and limits them to free locations. Profiles for other locations are hidden. |
| <a id="proton-vpn-unlock-split-tunneling"></a>[**Unlock split tunneling**](patches/src/main/kotlin/app/morphe/patches/protonvpn/misc/splittunneling/UnlockSplitTunnelingPatch.kt) | Unlocks split tunneling on free plans. |

---

## Proton VPN (F-Droid) (ch.protonvpn.android.reproducible)

<img src="https://publicmetadata.co.uk/ch.protonvpn.android.reproducible" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.21.57.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="proton-vpn-f-droid-amoled-dark-theme"></a>**AMOLED dark theme** | Replaces the dark theme background with pure black and can dim menus, bars and buttons. |
| <a id="proton-vpn-f-droid-custom-accent-color"></a>**Custom accent color** | Changes the accent color. Choose a color in the patches menu. |
| <a id="proton-vpn-f-droid-disable-telemetry"></a>**Disable telemetry** | Stops sending usage statistics and diagnostics to Proton. |
| <a id="proton-vpn-f-droid-floating-navigation-bar"></a>**Floating navigation bar** | Adds optional floating navigation with adjustable blur and glass. |
| <a id="proton-vpn-f-droid-hide-upgrade-promotions"></a>**Hide upgrade promotions** | Hides settings that need a paid plan, upgrade banners, the Discover VPN Plus carousel and special offers. |
| <a id="proton-vpn-f-droid-material-3-switches"></a>**Material 3 switches** | Adds check and close icons to switches. |
| <a id="proton-vpn-f-droid-remove-server-change-delay"></a>**Remove server change delay** | Removes the wait between server changes on free plans. |
| <a id="proton-vpn-f-droid-show-free-server-locations"></a>**Show free server locations** | Lists free server locations in Countries and Search and connects to the one you pick. Applies only to free plans. |
| <a id="proton-vpn-f-droid-unlock-lan-connections"></a>**Unlock LAN connections** | Unlocks LAN connections on free plans. |
| <a id="proton-vpn-f-droid-unlock-netshield"></a>**Unlock NetShield** | Unlocks NetShield ad and tracker blocking on free plans. |
| <a id="proton-vpn-f-droid-unlock-connection-preferences"></a>**Unlock connection preferences** | Unlocks the default connection, recent connections and excluded locations on free plans. |
| <a id="proton-vpn-f-droid-unlock-custom-dns"></a>**Unlock custom DNS** | Unlocks custom DNS on free plans. |
| <a id="proton-vpn-f-droid-unlock-profiles"></a>**Unlock profiles** | Unlocks profiles on free plans and limits them to free locations. Profiles for other locations are hidden. |
| <a id="proton-vpn-f-droid-unlock-split-tunneling"></a>**Unlock split tunneling** | Unlocks split tunneling on free plans. |

---

## Psiphon (com.psiphon3.subscription)

<img src="https://publicmetadata.co.uk/com.psiphon3.subscription" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.5.13 |
| :---: |

| Patch | Details |
|---|---|
| <a id="psiphon-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/psiphon/premium/UnlockPremiumPatch.kt) | Unlocks all regions. Removes ads and the upgrade button. |

---

## Public (com.cardfeed.video_public)

<img src="https://publicmetadata.co.uk/com.cardfeed.video_public" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.4.2 |
| :---: |

| Patch | Details |
|---|---|
| <a id="public-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/publicvideos/tracking/DisableTrackingPatch.kt) | Stops AppsFlyer, Flurry, comScore, Firebase Analytics and Facebook from collecting usage data. |
| <a id="public-disable-trailer-autoplay"></a>[**Disable trailer autoplay**](patches/src/main/kotlin/app/morphe/patches/publicvideos/misc/trailer/DisableTrailerAutoplayPatch.kt) | Stops the trailers that play on their own at the top of the Local Drama page. Only thumbnails load. |
| <a id="public-disable-video-preloading"></a>[**Disable video preloading**](patches/src/main/kotlin/app/morphe/patches/publicvideos/misc/preload/DisableVideoPreloadingPatch.kt) | Stops preloading upcoming videos in the feed and in dramas. |
| <a id="public-follow-system-theme"></a>[**Follow system theme**](patches/src/main/kotlin/app/morphe/patches/publicvideos/misc/theme/FollowSystemThemePatch.kt) | Switches the app between light and dark with the system theme. The app's Night Mode setting has no effect. |
| <a id="public-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="public-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/publicvideos/ads/HideAdsPatch.kt) | Removes ads from the feed, app open, interstitial and rewarded placements, and the sponsor banner on the launch screen. |
| <a id="public-load-videos-on-tap"></a>[**Load videos on tap**](patches/src/main/kotlin/app/morphe/patches/publicvideos/misc/thumbnails/LoadVideosOnTapPatch.kt) | Loads feed videos only after a tap when Autoplay is set to Off in the app's settings. YouTube videos are not affected. |
| <a id="public-remove-feed-cards"></a>[**Remove feed cards**](patches/src/main/kotlin/app/morphe/patches/publicvideos/feed/RemoveFeedCardsPatch.kt) | Removes follow suggestion cards and custom cards from the For You feed. |
| <a id="public-remove-promotions"></a>[**Remove promotions**](patches/src/main/kotlin/app/morphe/patches/publicvideos/promotions/RemovePromotionsPatch.kt) | Removes advertiser banners under posts, the VIP package and advertising offers on the profile, and the pop-ups over bottom bar tabs. |
| <a id="public-remove-screenshot-restriction"></a>[**Remove screenshot restriction**](patches/src/main/kotlin/app/morphe/patches/publicvideos/misc/screenshot/RemoveScreenshotRestrictionPatch.kt) | Allows screenshots and screen recording on every screen. |
| <a id="public-remove-startup-animation"></a>[**Remove startup animation**](patches/src/main/kotlin/app/morphe/patches/publicvideos/misc/splash/RemoveStartupAnimationPatch.kt) | Opens the app without playing the animated logo on launch. |

---

## Pydroid 3 (ru.iiec.pydroid3)

<img src="https://publicmetadata.co.uk/ru.iiec.pydroid3" width="48" height="48" align="top" alt="">

**Supported versions:**

| 8.6_arm64 |
| :---: |

| Patch | Details |
|---|---|
| <a id="pydroid-3-amoled-dark-theme"></a>**AMOLED dark theme** | Adds an AMOLED option to Settings &gt; Appearance &gt; Editor theme (dark). Applies only while the Dark theme is active. |
| <a id="pydroid-3-disable-tracking"></a>**Disable tracking** | Stops Firebase Analytics from collecting usage data. |
| <a id="pydroid-3-unlock-premium"></a>**Unlock premium** | Unlocks premium and removes ads. |

---

## QR & Barcode Scanner (com.gamma.scan)

<img src="https://publicmetadata.co.uk/com.gamma.scan" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.2.221 | 2.2.224 | 2.2.227 | 2.2.229 |
| :---: | :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="qr-barcode-scanner-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/gammascan/ads/HideAdsPatch.kt) | Disables banner, interstitial, and native ads. |

---

## Quick Cursor (com.quickcursor)

<img src="https://publicmetadata.co.uk/com.quickcursor" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.0.2 | 3.1.0 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="quick-cursor-unlock-pro"></a>[**Unlock pro**](patches/src/main/kotlin/app/morphe/patches/quickcursor/misc/premium/UnlockProPatch.kt) | Unlocks trigger, tracker and edge actions, floating tracker and cursor customization. |

---

## Quranify (com.mchutov.Quranify)

<img src="https://publicmetadata.co.uk/com.mchutov.Quranify" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.4.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="quranify-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/quranify/misc/premium/UnlockPremiumPatch.kt) | Unlocks downloading every surah, lyrics and tafsir, Android Auto, background playback controls, and insights. Removes the Free Premium challenge section from Settings. |

---

## Radio Javan (com.radiojavan.androidradio)

<img src="https://publicmetadata.co.uk/com.radiojavan.androidradio" width="48" height="48" align="top" alt="">

**Supported versions:**

| 11.20.1 | 11.23.0 | 11.24.0 |
| :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="radio-javan-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/radiojavan/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics and Firebase Sessions from collecting usage data. |
| <a id="radio-javan-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/radiojavan/misc/ads/HideAdsPatch.kt) | Removes banner, interstitial, app open and podcast audio ads, the launch promo video and the ad consent form. |
| <a id="radio-javan-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/radiojavan/misc/premium/UnlockPremiumPatch.kt) | Unlocks RJ Premium downloads, high quality audio, lyrics, unlimited skips and song selection, and hides ads. Requires a signed-in account. |

---

## Raindrop.io (io.raindrop.raindropio)

<img src="https://publicmetadata.co.uk/io.raindrop.raindropio" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.8.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="raindrop-io-accent-color"></a>[**Accent color**](patches/src/main/kotlin/app/morphe/patches/raindrop/theme/AccentColorPatch.kt) | Replaces the accent color with the wallpaper color on Android 12 and later, or with a chosen color. A new wallpaper color applies after the app restarts.<br><sub>Options: Material You, Accent color</sub> |
| <a id="raindrop-io-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="raindrop-io-hide-ai-assistant"></a>[**Hide AI assistant**](patches/src/main/kotlin/app/morphe/patches/raindrop/layout/ai/HideAiAssistantPatch.kt) | Hides the AI assistant button on the home screen, in collections and in the reader. |
| <a id="raindrop-io-hide-settings-links"></a>[**Hide Settings links**](patches/src/main/kotlin/app/morphe/patches/raindrop/layout/settings/HideSettingsLinksPatch.kt) | Hides the Web and Desktop app, Blog, Help and Support links in Settings. |
| <a id="raindrop-io-hide-upgrade-promotions"></a>[**Hide upgrade promotions**](patches/src/main/kotlin/app/morphe/patches/raindrop/misc/upselling/HideUpgradePromotionsPatch.kt) | Hides the Go Pro entry in Settings. |
| <a id="raindrop-io-unlock-pro"></a>[**Unlock pro**](patches/src/main/kotlin/app/morphe/patches/raindrop/misc/premium/UnlockProPatch.kt) | Unlocks reminders and highlight notes. Adds duplicate and broken link filters, collection and tag suggestions, full-text search of saved pages, Wayback Machine copies and a weekly bookmark export to Downloads. Requires a signed-in account. |

---

## RateGlance (com.sry.rateglance)

<img src="https://publicmetadata.co.uk/com.sry.rateglance" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.17.6 | 1.18.6 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="rateglance-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/rateglance/misc/premium/UnlockPremiumPatch.kt) | Unlocks all premium features. |

---

## ReadEra (org.readera)

<img src="https://publicmetadata.co.uk/org.readera" width="48" height="48" align="top" alt="">

**Supported versions:**

| 26.05.20+2300 | 26.11.06+2360 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="readera-accent-color"></a>[**Accent color**](patches/src/main/kotlin/app/morphe/patches/readera/misc/accent/AccentColorPatch.kt) | Replaces ReadEra's yellow with the wallpaper color on Android 12 and later, or green below 12. Another color can be picked in the patches menu. |
| <a id="readera-app-theme"></a>[**App theme**](patches/src/main/kotlin/app/morphe/patches/readera/misc/theme/AppThemePatch.kt) | Adds a black AMOLED theme for the library, menus and Settings, picked in the patches menu. Book pages keep their own colors. |
| <a id="readera-background-listening"></a>[**Background listening**](patches/src/main/kotlin/app/morphe/patches/readera/misc/background/BackgroundListeningPatch.kt) | Keeps Read aloud playing after switching apps or turning the screen off. |
| <a id="readera-colored-quotes"></a>[**Colored quotes**](patches/src/main/kotlin/app/morphe/patches/readera/misc/highlightcolors/ColoredQuotesPatch.kt) | Saves selected text as a quote in one of five colors, picked from the text selection menu. |
| <a id="readera-copy-with-source"></a>[**Copy with source**](patches/src/main/kotlin/app/morphe/patches/readera/misc/selection/CopyWithSourcePatch.kt) | Copies selected text with the book title and author. Found under More in the text selection menu. |
| <a id="readera-custom-fonts"></a>[**Custom fonts**](patches/src/main/kotlin/app/morphe/patches/readera/misc/fonts/CustomFontsPatch.kt) | Adds reading fonts from a folder on the device. The folder is picked on ReadEra's Fonts screen. |
| <a id="readera-dictionary-section"></a>[**Dictionary section**](patches/src/main/kotlin/app/morphe/patches/readera/misc/dictionary/DictionarySectionPatch.kt) | Adds a side menu page with the words saved from every book. |
| <a id="readera-dictionary-word-colors"></a>[**Dictionary word colors**](patches/src/main/kotlin/app/morphe/patches/readera/misc/highlightcolors/DictionaryWordColorsPatch.kt) | Saves a selected word to Dictionary in one of five colors. |
| <a id="readera-full-book-descriptions"></a>[**Full book descriptions**](patches/src/main/kotlin/app/morphe/patches/readera/misc/descriptions/FullBookDescriptionsPatch.kt) | Shows the whole book description in About document, including very short and very long ones. |
| <a id="readera-library-full-screen"></a>[**Library full screen**](patches/src/main/kotlin/app/morphe/patches/readera/misc/fullscreen/LibraryFullScreenPatch.kt) | Hides the status bar and navigation bar in the library, as full-screen mode does in the reader. Turned on in the patches menu. |
| <a id="readera-library-view-modes"></a>[**Library view modes**](patches/src/main/kotlin/app/morphe/patches/readera/misc/viewmode/LibraryViewModesPatch.kt) | Adds Full, Brief, Thumbnails and Grid layouts for book lists to the library menu. |
| <a id="readera-lock-screen-on-read-chapter-aloud"></a>**Lock screen on Read chapter aloud** | Locks the phone when Read chapter aloud starts, and the chapter keeps playing. Turned on in the patches menu. Needs device admin access. |
| <a id="readera-material-3-controls"></a>[**Material 3 controls**](patches/src/main/kotlin/app/morphe/patches/readera/misc/materialcontrols/MaterialControlsPatch.kt) | Gives switches, checkboxes and radio buttons in Settings and dialogs the Material 3 look. |
| <a id="readera-more-sleep-timer-choices"></a>[**More sleep timer choices**](patches/src/main/kotlin/app/morphe/patches/readera/misc/sleeptimer/MoreSleepTimerChoicesPatch.kt) | Adds 5 and 10 minute options to the Read aloud sleep timer. |
| <a id="readera-omit-words-and-symbols"></a>[**Omit words and symbols**](patches/src/main/kotlin/app/morphe/patches/readera/misc/speechfilter/OmitWordsAndSymbolsPatch.kt) | Makes Read aloud skip the words and symbols on a list, without changing the book. The list is edited in the patches menu. |
| <a id="readera-page-counter-in-long-chapters"></a>[**Page counter in long chapters**](patches/src/main/kotlin/app/morphe/patches/readera/misc/pagecounter/PageCounterInLongChaptersPatch.kt) | Keeps the page counter next to the chapter title in chapters of 1,000 pages or more. |
| <a id="readera-page-thumbnails"></a>[**Page thumbnails**](patches/src/main/kotlin/app/morphe/patches/readera/misc/minipages/PageThumbnailsPatch.kt) | Adds a button next to the page slider that shows the book's pages as thumbnails. |
| <a id="readera-quotes-and-notes-section"></a>[**Quotes and notes section**](patches/src/main/kotlin/app/morphe/patches/readera/misc/notes/QuotesAndNotesPatch.kt) | Adds a side menu page with the quotes, bookmarks and reviews from every book. Tapping one opens the book at that spot. |
| <a id="readera-read-aloud-from-selection"></a>[**Read aloud from selection**](patches/src/main/kotlin/app/morphe/patches/readera/misc/selection/ReadAloudFromSelectionPatch.kt) | Starts Read aloud from the selected text. Found under More in the text selection menu. |
| <a id="readera-read-aloud-notification-controls"></a>[**Read aloud notification controls**](patches/src/main/kotlin/app/morphe/patches/readera/misc/notification/ReadAloudNotificationControlsPatch.kt) | Adds Stop, Previous and Next buttons to the Read aloud notification. On Android 7 to 10 it also counts down the sleep timer. |
| <a id="readera-read-chapter-aloud"></a>[**Read chapter aloud**](patches/src/main/kotlin/app/morphe/patches/readera/misc/chapterspeech/ReadChapterAloudPatch.kt) | Reads one chapter aloud and stops at its end. Long-press the chapter in Contents to start. |
| <a id="readera-read-footnotes-aloud"></a>[**Read footnotes aloud**](patches/src/main/kotlin/app/morphe/patches/readera/misc/footnotes/ReadFootnotesAloudPatch.kt) | Reads each footnote aloud where the book refers to it, in place of its number. Works in EPUB, FB2 and MOBI books. |
| <a id="readera-remove-nags"></a>[**Remove nags**](patches/src/main/kotlin/app/morphe/patches/readera/misc/nags/RemoveNagsPatch.kt) | Removes the rate-the-app and promotion pop-ups and the Go to Premium button. |
| <a id="readera-search-selection-in-book"></a>[**Search selection in book**](patches/src/main/kotlin/app/morphe/patches/readera/misc/selection/SearchSelectionInBookPatch.kt) | Searches the book for the selected text and lists every match. Found under More in the text selection menu. |
| <a id="readera-skip-headers-and-footers"></a>[**Skip headers and footers**](patches/src/main/kotlin/app/morphe/patches/readera/misc/skipheaders/SkipHeadersPatch.kt) | Makes Read aloud skip page headers, footers and page numbers in PDF and DjVu files. |
| <a id="readera-speech-pitch-slider"></a>[**Speech pitch slider**](patches/src/main/kotlin/app/morphe/patches/readera/misc/speechpitch/SpeechPitchSliderPatch.kt) | Adds a voice pitch slider from 0.5x to 2x to Settings &gt; Text to speech (TTS). |
| <a id="readera-speech-rate-slider"></a>[**Speech rate slider**](patches/src/main/kotlin/app/morphe/patches/readera/misc/speechrate/SpeechRateSliderPatch.kt) | Sets Read aloud speed from 0.1x to 4x with a slider on the Speech rate screen. |

---

## Realme Link (com.realme.link)

<img src="https://publicmetadata.co.uk/com.realme.link" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.8.811.2814 |
| :---: |

| Patch | Details |
|---|---|
| <a id="realme-link-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/realmelink/theme/AmoledThemePatch.kt) | Replaces the light theme with a pure black dark theme. Requires Android 13 or later. |
| <a id="realme-link-bypass-session-expiry"></a>[**Bypass session expiry**](patches/src/main/kotlin/app/morphe/patches/realmelink/session/BypassSessionExpiryPatch.kt) | Stops the session expired prompt and toast. |

---

## RedReader (org.quantumbadger.redreader)

<img src="https://publicmetadata.co.uk/org.quantumbadger.redreader" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.27 |
| :---: |

| Patch | Details |
|---|---|
| <a id="redreader-filter-posts"></a>[**Filter posts**](patches/src/main/kotlin/app/morphe/patches/redreader/layout/filter/FilterPostsPatch.kt) | Filters posts by title keyword, subreddit name or post flair, with regular expression support. |
| <a id="redreader-fix-login"></a>[**Fix login**](patches/src/main/kotlin/app/morphe/patches/redreader/misc/login/FixLoginPatch.kt) | Enables Reddit login without entering a client ID. |

---

## RedReader Alpha (org.quantumbadger.redreaderalpha)

<img src="https://raw.githubusercontent.com/QuantumBadger/RedReader/master/src/main/res/mipmap-xxxhdpi/icon_inv.png" width="48" height="48" align="top" alt="">

**Supported versions:**

| Alpha 375 |
| :---: |

| Patch | Details |
|---|---|
| <a id="redreader-alpha-filter-posts"></a>**Filter posts** | Filters posts by title keyword, subreddit name or post flair, with regular expression support. |
| <a id="redreader-alpha-fix-login"></a>**Fix login** | Enables Reddit login without entering a client ID. |

---

## Resprite (com.fengeon.resprite_desktop)

<img src="https://publicmetadata.co.uk/com.fengeon.resprite_desktop" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.33.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="resprite-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/resprite/misc/premium/UnlockPremiumPatch.kt) | Unlocks every premium feature, including the export limit, symmetry, outline, filters, text, timelapse and custom brushes.<br><sub>Options: Hide Get Premium</sub> |

---

## Ridmik Keyboard (ridmik.keyboard)

<img src="https://publicmetadata.co.uk/ridmik.keyboard" width="48" height="48" align="top" alt="">

**Supported versions:**

| 20.0.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="ridmik-keyboard-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/ridmik/ads/RemoveAdsPatch.kt) | Removes banner, full-screen and app-open ads from the keyboard and the app, and sponsored results in GIF search. |

---

## Ringtone Maker (ringtonemaker.musiccutter.customringtones.freeringtonemaker)

<img src="https://publicmetadata.co.uk/ringtonemaker.musiccutter.customringtones.freeringtonemaker" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.02.03.1117 |
| :---: |

| Patch | Details |
|---|---|
| <a id="ringtone-maker-remove-rating-prompts"></a>[**Remove rating prompts**](patches/src/main/kotlin/app/morphe/patches/ringtonemaker/misc/rate/RemoveRatingPromptsPatch.kt) | Removes the prompts asking for a rating. |
| <a id="ringtone-maker-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/ringtonemaker/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium, removes ads and skips the upgrade screens. |

---

## RISE Sleep Tracker (com.risesci.nyx)

<img src="https://publicmetadata.co.uk/com.risesci.nyx" width="48" height="48" align="top" alt="">

**Supported versions:**

| Android V1.79.4 |
| :---: |

| Patch | Details |
|---|---|
| <a id="rise-sleep-tracker-disable-telemetry"></a>[**Disable telemetry**](patches/src/main/kotlin/app/morphe/patches/rise/misc/telemetry/DisableTelemetryPatch.kt) | Stops crash and error reports from reaching Sentry. |
| <a id="rise-sleep-tracker-disable-usage-tracking"></a>[**Disable usage tracking**](patches/src/main/kotlin/app/morphe/patches/rise/misc/telemetry/DisableUsageTrackingPatch.kt) | Stops app usage events from being uploaded. Local usage tracking remains enabled. |
| <a id="rise-sleep-tracker-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/rise/misc/premium/UnlockPremiumPatch.kt) | Unlocks the energy schedule, habit tools, smart alarm and progress insights. Requires a RISE account. |

---

## Rubber Bands (app.rubberbands.fit)

<img src="https://publicmetadata.co.uk/app.rubberbands.fit" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.19 |
| :---: |

| Patch | Details |
|---|---|
| <a id="rubber-bands-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/rubberbands/misc/premium/UnlockPremiumPatch.kt) | Unlocks running and logging workouts, progress tracking and personal records. AI workout generation is not included. |

---

## Serializd (com.serializdmobile)

<img src="https://publicmetadata.co.uk/com.serializdmobile" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.992 |
| :---: |

| Patch | Details |
|---|---|
| <a id="serializd-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/serializd/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics and the Playwire advertising SDK from collecting usage data and the advertising ID. |
| <a id="serializd-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/serializd/ads/RemoveAdsPatch.kt) | Removes ad placements and the advertising consent prompt. |
| <a id="serializd-sharpen-launcher-icon"></a>[**Sharpen launcher icon**](patches/src/main/kotlin/app/morphe/patches/serializd/misc/icon/SharpenLauncherIconPatch.kt) | Sharpens the default launcher icon. Alternate icons are unchanged. |

---

## Sheytoon (com.triovent.datingapp)

<img src="https://publicmetadata.co.uk/com.triovent.datingapp" width="48" height="48" align="top" alt="">

**Supported versions:**

| 6.8 |
| :---: |

| Patch | Details |
|---|---|
| <a id="sheytoon-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/sheytoon/misc/premium/UnlockPremiumPatch.kt) | Unlocks Sheytoon Select: unlimited likes, superlikes and quick notes, incognito mode, Second Look, and unblurred photos of the people who liked or viewed you. |

---

## Showly (com.michaldrabik.showly2)

<img src="https://publicmetadata.co.uk/com.michaldrabik.showly2" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.70.0 | 3.72.0 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="showly-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/showly/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black. |
| <a id="showly-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/showly/misc/premium/UnlockPremiumPatch.kt) | Unlocks ad removal, light theme, custom images, list view types, quick ratings, transparent widgets, and the News feed. |

---

## SHTTPS Marketless (com.phlox.simpleserver.marketless)

<img src="https://publicmetadata.co.uk/com.phlox.simpleserver.marketless" width="48" height="48" align="top" alt="">

**Supported versions:**

| 3.4.7 |
| :---: |

| Patch | Details |
|---|---|
| <a id="shttps-marketless-unlock-plus"></a>[**Unlock PLUS**](patches/src/main/kotlin/app/morphe/patches/shttps/misc/premium/UnlockPlusPatch.kt) | Unlocks remote screen control, control from external apps and auto-shutdown timeouts over 6 hours or off. |

---

## SQCOM (com.sqcom.shangqing)

<img src="https://publicmetadata.co.uk/com.sqcom.shangqing" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.1.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="sqcom-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="sqcom-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/sqcom/ads/HideAdsPatch.kt) | Removes splash, banner, native, interstitial and rewarded ads, and the ad consent prompt. |

---

## STEEZY (co.steezy.app)

<img src="https://publicmetadata.co.uk/co.steezy.app" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.23.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="steezy-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/steezy/misc/tracking/DisableTrackingPatch.kt) | Stops RudderStack, Branch, Firebase Sessions and Crashlytics from collecting usage data. |
| <a id="steezy-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="steezy-hide-upgrade-promotions"></a>[**Hide upgrade promotions**](patches/src/main/kotlin/app/morphe/patches/steezy/misc/upselling/HideUpgradePromotionsPatch.kt) | Skips the subscription offer shown after sign-up. |
| <a id="steezy-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/steezy/misc/premium/UnlockPremiumPatch.kt) | Unlocks all classes and programs. Requires a STEEZY account. |

---

## Storywise (com.storywise.storywise)

<img src="https://publicmetadata.co.uk/com.storywise.storywise" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.0.3 | 1.0.5 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="storywise-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="storywise-remove-forced-update"></a>[**Remove forced update**](patches/src/main/kotlin/app/morphe/patches/storywise/misc/update/RemoveForcedUpdatePatch.kt) | Removes the update screen that blocks the app when a newer build is published. |
| <a id="storywise-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/storywise/misc/premium/UnlockPremiumPatch.kt) | Unlocks locked lessons, quizzes and the full Report Card without a pass. The server caps the AI tutor at five questions a day and withholds lectures and live sessions. Signing in requires GmsCore support. |

---

## Sygic (com.sygic.aura)

<img src="https://publicmetadata.co.uk/com.sygic.aura" width="48" height="48" align="top" alt="">

**Supported versions:**

| 26.6.1-120585 | 26.6.2-121046 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="sygic-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="sygic-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/sygic/misc/premium/UnlockPremiumPatch.kt) | Unlocks Premium+ features, including live traffic, speed cameras and Motorbike mode. |

---

## Symfonium (app.symfonik.music.player)

<img src="https://publicmetadata.co.uk/app.symfonik.music.player" width="48" height="48" align="top" alt="">

**Supported versions:**

| 14.0.0 | 14.1.0 | 15.0.1 | 15.1.0 | [14.0.0 TV](https://www.apkmirror.com/apk/tolriq/symfonium-music-player-cast-android-tv/symfonium-music-player-cast-android-tv-14-0-0-tv-release/ "Download stock APK") | [15.1.0 TV](https://www.apkmirror.com/apk/tolriq/symfonium-music-player-cast-android-tv/symfonium-music-player-cast-android-tv-15-1-0-tv-release/ "Download stock APK") |
| :---: | :---: | :---: | :---: | :---: | :---: |

Always verify the downloaded APK's SHA-256 against a trusted checksum before patching.

| Patch | Details |
|---|---|
| <a id="symfonium-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/symfonium/misc/premium/UnlockPremiumPatch.kt) | Unlocks all premium features. |

---

## TeraBox (com.dubox.drive)

<img src="https://publicmetadata.co.uk/com.dubox.drive" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.29.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="terabox-clone-app"></a>[**Clone app**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/clone/CloneAppPatch.kt) | Installs TeraBox as a separate app alongside the original, with its own account. Each copy needs a different clone number.<br><sub>Options: Package name, App name</sub> |
| <a id="terabox-download-generated-files"></a>[**Download generated files**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/ai/DownloadAiFilesPatch.kt) | Downloads files generated in TeraBox chat to the Downloads folder. |
| <a id="terabox-fix-google-login"></a>[**Fix Google login**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/login/FixGoogleLoginPatch.kt) | Restores signing in with a Google account. |
| <a id="terabox-fix-transfers-over-vpn"></a>[**Fix transfers over VPN**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/fix/vpn/FixTransfersOverVpnPatch.kt) | Runs uploads, downloads and saves from shared links while a VPN is active. |
| <a id="terabox-gmscore-support"></a>[**GmsCore support**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/gms/GmsCoreSupportPatch.kt) | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="terabox-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/terabox/ads/HideAdsPatch.kt) | Removes feed, banner and video ads, plus interstitial and app-open ads. Rewarded ads still play when requested from Benefits. |
| <a id="terabox-hide-consent-form"></a>[**Hide consent form**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/consent/HideConsentFormPatch.kt) | Hides the ad consent form shown at startup. |
| <a id="terabox-hide-home-shortcuts"></a>[**Hide home shortcuts**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/shortcuts/HideHomeShortcutsPatch.kt) | Removes Trending and Short Drama from Home and More, and Translate where it appears. Options hide the Creative Generation tools, Signature, PDF Encryption and Add Watermark too.<br><sub>Options: Hide Creative Generation, Hide Translate, Hide Signature, +4 more</sub> |
| <a id="terabox-hide-promotions"></a>[**Hide promotions**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/promotions/HidePromotionsPatch.kt) | Removes Premium and sale offers, plus prize and campaign cards, speed-up prompts, floating invites and coupon popups. |
| <a id="terabox-hide-video-recommendations"></a>[**Hide video recommendations**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/recommendations/HideVideoRecommendationsPatch.kt) | Removes recommendations below the player. |
| <a id="terabox-remove-recent-panel"></a>[**Remove recent panel**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/recent/RemoveRecentPanelPatch.kt) | Removes Recent, Starred and Offline from Home. |
| <a id="terabox-show-backup-count"></a>[**Show backup count**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/backup/ShowBackupCountPatch.kt) | Shows remaining files on Home and Photos, including paused backups. |
| <a id="terabox-sort-by-original-date"></a>[**Sort by original date**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/sort/SortByOriginalDatePatch.kt) | Sorts and displays files by their original modification dates. |
| <a id="terabox-unlock-premium-plus"></a>[**Unlock Premium Plus**](patches/src/main/kotlin/app/morphe/patches/terabox/misc/premium/UnlockPremiumPatch.kt) | Unlocks HD, 3x speed and uploads, with HD up to original quality. HD buffers faster over parallel connections. |

---

## TradingView (com.tradingview.tradingviewapp)

<img src="https://publicmetadata.co.uk/com.tradingview.tradingviewapp" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.20.86.0.1002565 |
| :---: |

| Patch | Details |
|---|---|
| <a id="tradingview-disable-analytics"></a>[**Disable analytics**](patches/src/main/kotlin/app/morphe/patches/tradingview/misc/analytics/DisableAnalyticsPatch.kt) | Disables network telemetry and Snowplow analytics. |
| <a id="tradingview-fix-google-sign-in"></a>[**Fix Google sign-in**](patches/src/main/kotlin/app/morphe/patches/tradingview/misc/signin/FixGoogleSignInPatch.kt) | Signs in with Google through a web page. |
| <a id="tradingview-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="tradingview-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/tradingview/ads/HideAdsPatch.kt) | Hides the ads and upgrade cards in the Ideas and Community feeds. |
| <a id="tradingview-hide-upgrade-promotions"></a>[**Hide upgrade promotions**](patches/src/main/kotlin/app/morphe/patches/tradingview/misc/upselling/HideUpgradePromotionsPatch.kt) | Hides the Upgrade button, plan banners, subscription card and paywalls. Extra indicators, watchlists, flag colors, alerts, second and tick intervals, and intraday replay are not unlocked. |
| <a id="tradingview-skip-sign-up-screen"></a>[**Skip sign-up screen**](patches/src/main/kotlin/app/morphe/patches/tradingview/misc/onboarding/SkipSignUpPatch.kt) | Opens the app without signing in. Sign in later from the menu. |
| <a id="tradingview-unlock-chart-features"></a>[**Unlock chart features**](patches/src/main/kotlin/app/morphe/patches/tradingview/chart/UnlockChartFeaturesPatch.kt) | Unlocks layouts with up to 8 charts, more than 3 indicators per chart and the Renko, Kagi, Point &amp; Figure, Line break, Range, Volume candles and HLC area chart types. Alerts, watchlists, profile indicators and second or tick intervals follow your plan. |

---

## Trainline (com.thetrainline)

<img src="https://publicmetadata.co.uk/com.thetrainline" width="48" height="48" align="top" alt="">

**Supported versions:**

| 441.0.0.185329 |
| :---: |

| Patch | Details |
|---|---|
| <a id="trainline-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/trainline/ads/HideAdsPatch.kt) | Removes the adverts shown between search results. |

---

## Transparent clock & weather (com.droid27.transparentclockweather)

<img src="https://publicmetadata.co.uk/com.droid27.transparentclockweather" width="48" height="48" align="top" alt="">

**Supported versions:**

| 9.19.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="transparent-clock-weather-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/transparentclock/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics and Singular from collecting usage data, and disables Crashlytics. |
| <a id="transparent-clock-weather-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/transparentclock/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium features and removes ads. Premium themes and widgets downloaded from Google Play are not included. |

---

## TunnelDOOM VPN (com.tunneldoom.http)

<img src="https://publicmetadata.co.uk/com.tunneldoom.http" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.3.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="tunneldoom-vpn-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/tunneldoom/misc/ads/RemoveAdsPatch.kt) | Removes app open, banner, interstitial and rewarded ads and the ad consent form. |
| <a id="tunneldoom-vpn-remove-time-limit"></a>[**Remove time limit**](patches/src/main/kotlin/app/morphe/patches/tunneldoom/misc/time/RemoveTimeLimitPatch.kt) | Keeps the VPN connected without adding time by watching ads. |

---

## U Dictionary (com.youdao.hindict)

<img src="https://publicmetadata.co.uk/com.youdao.hindict" width="48" height="48" align="top" alt="">

**Supported versions:**

| 6.7.7 |
| :---: |

| Patch | Details |
|---|---|
| <a id="u-dictionary-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/udictionary/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro features and removes ads. |

---

## Ultimate Guitar (com.ultimateguitar.tabs)

<img src="https://publicmetadata.co.uk/com.ultimateguitar.tabs" width="48" height="48" align="top" alt="">

**Supported versions:**

| 7.3.38 |
| :---: |

| Patch | Details |
|---|---|
| <a id="ultimate-guitar-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="ultimate-guitar-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/ultimateguitar/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro, Sing and Practice features and removes ads. Paid course lessons and Songbooks are not unlocked. |

---

## ULTIMEA (com.dynanic.ultimea)

<img src="https://publicmetadata.co.uk/com.dynanic.ultimea" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.4.4 |
| :---: |

| Patch | Details |
|---|---|
| <a id="ultimea-accent-color"></a>[**Accent color**](patches/src/main/kotlin/app/morphe/patches/ultimea/misc/theme/AccentColorPatch.kt) | Changes accent colors with a chosen color or Material You. |
| <a id="ultimea-dark-theme"></a>[**Dark theme**](patches/src/main/kotlin/app/morphe/patches/ultimea/misc/theme/DarkThemePatch.kt) | Adds dark and AMOLED themes with light text and controls. |
| <a id="ultimea-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/ultimea/misc/tracking/DisableTrackingPatch.kt) | Disables Firebase Analytics, Crashlytics, Logan and usage event uploads. |
| <a id="ultimea-fine-equalizer"></a>[**Fine equalizer**](patches/src/main/kotlin/app/morphe/patches/ultimea/misc/equalizer/FineEqualizerPatch.kt) | Adds 0.1 dB bands, band values and Undo to the equalizer screen. On a mount install, presets are shared as text instead of a file. |
| <a id="ultimea-local-equalizer-presets"></a>[**Local equalizer presets**](patches/src/main/kotlin/app/morphe/patches/ultimea/misc/presets/LocalEqualizerPresetsPatch.kt) | Adds named local curves with duplicate, export and import actions. |
| <a id="ultimea-remove-distractions"></a>[**Remove distractions**](patches/src/main/kotlin/app/morphe/patches/ultimea/misc/distractions/RemoveDistractionsPatch.kt) | Removes community, message and support entries. |
| <a id="ultimea-remove-popups"></a>[**Remove popups**](patches/src/main/kotlin/app/morphe/patches/ultimea/misc/popups/RemovePopupsPatch.kt) | Removes promotional and rating popups. |
| <a id="ultimea-stop-firmware-reminders"></a>[**Stop firmware reminders**](patches/src/main/kotlin/app/morphe/patches/ultimea/misc/firmware/StopFirmwareRemindersPatch.kt) | Stops automatic firmware reminders. Keeps manual update checks. |
| <a id="ultimea-xupmix"></a>[**Xupmix**](patches/src/main/kotlin/app/morphe/patches/ultimea/misc/controls/XupmixPatch.kt) | Enables Xupmix control on Poseidon D80. |

---

## UPTCL (com.ufoneselfcare)

<img src="https://publicmetadata.co.uk/com.ufoneselfcare" width="48" height="48" align="top" alt="">

**Supported versions:**

| 11.0.41 |
| :---: |

| Patch | Details |
|---|---|
| <a id="uptcl-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/uptcl/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics, Firebase Sessions, Facebook, TikTok and the app's own event reporting from collecting usage data. |
| <a id="uptcl-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/uptcl/ads/HideAdsPatch.kt) | Hides banner, native and full-screen ads. |
| <a id="uptcl-hide-launch-promotion"></a>[**Hide launch promotion**](patches/src/main/kotlin/app/morphe/patches/uptcl/misc/promotion/HideLaunchPromotionPatch.kt) | Hides the promotional banner and video on the launch splash screen and the full-screen offer on the home screen. |
| <a id="uptcl-match-system-bar-color"></a>[**Match system bar color**](patches/src/main/kotlin/app/morphe/patches/uptcl/misc/systembars/MatchSystemBarColorPatch.kt) | Colors the status bar and navigation bar white to match the app's screens. |
| <a id="uptcl-remove-forced-update"></a>[**Remove forced update**](patches/src/main/kotlin/app/morphe/patches/uptcl/misc/update/RemoveForcedUpdatePatch.kt) | Removes the update prompts that block the app at launch. |

---

## USA VPN (vpn.usa_tap2free)

<img src="https://publicmetadata.co.uk/vpn.usa_tap2free" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.171 |
| :---: |

| Patch | Details |
|---|---|
| <a id="usa-vpn-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/usavpn/misc/premium/UnlockPremiumPatch.kt) | Unlocks PRO servers and removes ads and the connection time limit. |

---

## Viber (com.viber.voip)

<img src="https://publicmetadata.co.uk/com.viber.voip" width="48" height="48" align="top" alt="">

**Supported versions:**

| 29.3.1.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="viber-disable-ai-features"></a>[**Disable AI features**](patches/src/main/kotlin/app/morphe/patches/viber/layout/ai/DisableAiFeaturesPatch.kt) | Removes AI and inline Translate. |
| <a id="viber-disable-analytics"></a>[**Disable analytics**](patches/src/main/kotlin/app/morphe/patches/viber/misc/analytics/DisableAnalyticsPatch.kt) | Stops Mixpanel, Adjust, Braze, Firebase, Crashlytics and Snap Kit from collecting and sending usage data. |
| <a id="viber-disable-update-prompts"></a>[**Disable update prompts**](patches/src/main/kotlin/app/morphe/patches/viber/misc/updates/DisableUpdatePromptsPatch.kt) | Stops the Google Play update dialog, the suggested and required upgrade dialogs and the update request on links this version can't open. Viber's servers can still stop serving an old version. |
| <a id="viber-disappearing-media-privacy"></a>[**Disappearing media privacy**](patches/src/main/kotlin/app/morphe/patches/viber/layout/mediaprivacy/DisappearingMediaPrivacyPatch.kt) | Stops screenshot alerts and opening reports for disappearing messages. Opening reports resume once you read a newer message in that chat. |
| <a id="viber-enable-viber-plus-features"></a>[**Enable Viber Plus features**](patches/src/main/kotlin/app/morphe/patches/viber/misc/premium/UnlockViberPlusPatch.kt) | Enables Viber Plus app icons and the typing privacy switch. The Plus badge and Invisible Mode need a paid subscription. |
| <a id="viber-explain-google-drive-errors"></a>[**Explain Google Drive errors**](patches/src/main/kotlin/app/morphe/patches/viber/misc/drive/ExplainDriveErrorsPatch.kt) | Shows how to fix Google Drive backup and restore errors when GmsCore support is not applied. |
| <a id="viber-filter-business-chats"></a>[**Filter business chats**](patches/src/main/kotlin/app/morphe/patches/viber/layout/business/FilterBusinessChatsPatch.kt) | Filters selected business chats after reading and removes inbox discovery prompts. Shows unread chats and verification-code chats. |
| <a id="viber-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Google Drive backup and restore work only with this patch. Requires GmsCore to be installed. |
| <a id="viber-hide-more-screen-items"></a>[**Hide More screen items**](patches/src/main/kotlin/app/morphe/patches/viber/layout/more/HideMoreItemsPatch.kt) | Hides selected entries from the main screen and More screen.<br><sub>Options: Hide About and FAQ, Hide Business Account, Hide Caller ID, +11 more</sub> |
| <a id="viber-hide-viber-out-and-channel-promotions"></a>[**Hide Viber Out and channel promotions**](patches/src/main/kotlin/app/morphe/patches/viber/layout/promotions/HidePromotionsPatch.kt) | Hides the Viber Out banners and intro on the Calls tab and the channel and bot suggestions in the chat list. |
| <a id="viber-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/viber/ads/HideAdsPatch.kt) | Hides ads in the chat list, the More screen, Explore, search and after calls. |
| <a id="viber-hide-image-remix"></a>[**Hide image remix**](patches/src/main/kotlin/app/morphe/patches/viber/layout/remix/HideImageRemixPatch.kt) | Hides Remix actions and the Remix introduction popup on images. |
| <a id="viber-hide-navigation-buttons"></a>[**Hide navigation buttons**](patches/src/main/kotlin/app/morphe/patches/viber/layout/navigation/HideNavigationButtonsPatch.kt) | Hides the selected tabs from the bottom navigation bar.<br><sub>Options: Hide Calls, Hide ChatGPT, Hide Explore, +3 more</sub> |
| <a id="viber-link-as-secondary-device"></a>[**Link as secondary device**](patches/src/main/kotlin/app/morphe/patches/viber/misc/secondary/LinkAsSecondaryDevicePatch.kt) | Links to your main Viber phone. |
| <a id="viber-raise-folder-limit"></a>[**Raise folder limit**](patches/src/main/kotlin/app/morphe/patches/viber/layout/folders/RaiseFolderLimitPatch.kt) | Allows up to 10 chat folders. |
| <a id="viber-remove-caller-id-prompts"></a>[**Remove Caller ID prompts**](patches/src/main/kotlin/app/morphe/patches/viber/layout/callerid/RemoveCallerIdPromptsPatch.kt) | Removes Caller ID introduction sheets and the Calls tab banner. |
| <a id="viber-remove-chat-payment-actions"></a>[**Remove chat payment actions**](patches/src/main/kotlin/app/morphe/patches/viber/layout/payments/RemoveChatPaymentsPatch.kt) | Removes payment actions from chat menus. |
| <a id="viber-remove-deleted-message-notices"></a>[**Remove deleted message notices**](patches/src/main/kotlin/app/morphe/patches/viber/misc/deletedmessages/RemoveDeletedMessagesPatch.kt) | Makes Delete for everyone leave no notice on either side and clears deleted message notices from chats. Hides Viber's own Delete without a trace option. |
| <a id="viber-remove-list-gaps"></a>[**Remove list gaps**](patches/src/main/kotlin/app/morphe/patches/viber/layout/gaps/RemoveListGapsPatch.kt) | Removes list gaps and extra scrolling. |
| <a id="viber-remove-locked-plus-items"></a>[**Remove locked Plus items**](patches/src/main/kotlin/app/morphe/patches/viber/layout/lockedplus/RemoveLockedPlusItemsPatch.kt) | Removes Invisible status options, Plus badge settings and locked Plus entries. |
| <a id="viber-remove-online-status-limit"></a>[**Remove online status limit**](patches/src/main/kotlin/app/morphe/patches/viber/misc/onlinestatus/RemoveOnlineStatusLimitPatch.kt) | Removes the 24-hour wait between online status changes. |
| <a id="viber-remove-typing-privacy-banner"></a>[**Remove typing privacy banner**](patches/src/main/kotlin/app/morphe/patches/viber/layout/typingbanner/RemoveTypingPrivacyBannerPatch.kt) | Removes typing privacy promotions. |
| <a id="viber-save-disappearing-media"></a>[**Save disappearing media**](patches/src/main/kotlin/app/morphe/patches/viber/layout/media/SaveDisappearingMediaPatch.kt) | Shows Save to gallery for disappearing photos and videos. |
| <a id="viber-spoof-signature"></a>[**Spoof signature**](patches/src/main/kotlin/app/morphe/patches/viber/misc/fix/signature/SpoofSignaturePatch.kt) | Lets the app send and receive messages and get its push token. |
| <a id="viber-start-folder"></a>[**Start folder**](patches/src/main/kotlin/app/morphe/patches/viber/layout/startfolder/StartFolderPatch.kt) | Opens Chats in the selected folder. |
| <a id="viber-unfiled-chats"></a>[**Unfiled chats**](patches/src/main/kotlin/app/morphe/patches/viber/layout/unfiled/UnfiledChatsPatch.kt) | Adds a folder tab for chats outside your custom folders. |

**Google Drive backup and restore** need **GmsCore support**. Without it, Google refuses Drive access to the patched
Viber, and the backup and restore error dialogs say "Google Drive backup and restore need the GmsCore support patch."

1. Install a microG build that uses the `app.revanced.android.gms` package and sign in to the Google account that
   holds the backup. [MicroG RE](https://github.com/MorpheApp/MicroG-RE/releases/latest), Morphe's own build, is
   recommended.
2. In Morphe Manager, turn on Settings → Patching → Expert mode, enable **GmsCore support** in Viber's patch list,
   then patch and install over the current copy. Manager keeps this selection for later repatches while expert mode
   is on.
3. In Viber, open Settings → Account → Viber Backup, tap the account row and pick the account that holds the
   backup.

Viber offers restore right after activation on the primary device, the phone registered with the Viber number.

Questions about Drive backup and restore go to [Q&A #1052](https://github.com/hxreborn/morphe-patches/discussions/1052).

---

## Video Editor (videoeditor.videomaker.videoeditorforyoutube)

<img src="https://publicmetadata.co.uk/videoeditor.videomaker.videoeditorforyoutube" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.631.200.I |
| :---: |

| Patch | Details |
|---|---|
| <a id="video-editor-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/videoeditor/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro features. |

---

## VLLO (com.darinsoft.vimo)

<img src="https://publicmetadata.co.uk/com.darinsoft.vimo" width="48" height="48" align="top" alt="">

**Supported versions:**

| 13.9.0 | 13.9.6 | 13.9.18 |
| :---: | :---: | :---: |

| Patch | Details |
|---|---|
| <a id="vllo-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/vllo/misc/tracking/DisableTrackingPatch.kt) | Stops AppsFlyer, Firebase Analytics, and Facebook from collecting usage data. |
| <a id="vllo-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/vllo/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium editing features, removes ads and the export watermark, and skips the ad before importing audio. Hides the store button. The AI tools are not included. |

---

## VPN India (india.vpn_tap2free)

<img src="https://publicmetadata.co.uk/india.vpn_tap2free" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.175 |
| :---: |

| Patch | Details |
|---|---|
| <a id="vpn-india-unlock-premium"></a>**Unlock premium** | Unlocks PRO servers and removes ads and the connection time limit. |

---

## VPN Super Unlimited Proxy (com.free.vpn.super.hotspot.open)

<img src="https://publicmetadata.co.uk/com.free.vpn.super.hotspot.open" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.36.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="vpn-super-unlimited-proxy-disconnect-from-notification"></a>[**Disconnect from notification**](patches/src/main/kotlin/app/morphe/patches/vpnsuper/misc/notification/DisconnectFromNotificationPatch.kt) | Skips opening the app when Disconnect is tapped in the notification. Doesn't work on Android TV. |
| <a id="vpn-super-unlimited-proxy-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/vpnsuper/premium/UnlockPremiumPatch.kt) | Unlocks premium servers and removes ads, upgrade banners, the launch paywall and the Android TV sign-in screen. |

---

## vpnify (com.vpn.free.hotspot.secure.vpnify)

<img src="https://publicmetadata.co.uk/com.vpn.free.hotspot.secure.vpnify" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.3.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="vpnify-disable-rating-prompt"></a>[**Disable rating prompt**](patches/src/main/kotlin/app/morphe/patches/vpnify/misc/review/DisableRatingPromptPatch.kt) | Stops the Google Play rating prompt from appearing. |
| <a id="vpnify-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/vpnify/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium, removes ads and the free session time limit. |

---

## Wallmaa (com.wallmaa.minimal.wallpapers)

<img src="https://publicmetadata.co.uk/com.wallmaa.minimal.wallpapers" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.0.4 |
| :---: |

| Patch | Details |
|---|---|
| <a id="wallmaa-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/wallmaa/misc/tracking/DisableTrackingPatch.kt) | Stops the LevelPlay ad mediation SDK and its ad networks from starting. Ads no longer load. |
| <a id="wallmaa-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/wallmaa/misc/premium/UnlockPremiumPatch.kt) | Unlocks every collection and removes ads and the download and apply limits. |

---

## WalP (com.stresscodes.wallp)

<img src="https://publicmetadata.co.uk/com.stresscodes.wallp" width="48" height="48" align="top" alt="">

**Supported versions:**

| 10.2.6 |
| :---: |

| Patch | Details |
|---|---|
| <a id="walp-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/walp/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro filters and wallpaper rotation from categories, and removes ads. |

---

## WATCH IT (com.watchit.vod)

<img src="https://publicmetadata.co.uk/com.watchit.vod" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.30.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="watch-it-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/watchit/misc/tracking/DisableTrackingPatch.kt) | Stops Firebase Analytics, Crashlytics, Facebook app events and Brightcove video analytics from collecting usage data. |
| <a id="watch-it-remove-ads"></a>[**Remove ads**](patches/src/main/kotlin/app/morphe/patches/watchit/ads/RemoveAdsPatch.kt) | Removes video ads, pause ads, sponsor overlays, interactive ads and advertiser logos. |

---

## Wattpad (wp.wattpad)

<img src="https://publicmetadata.co.uk/wp.wattpad" width="48" height="48" align="top" alt="">

**Supported versions:**

| 11.43.4 |
| :---: |

| Patch | Details |
|---|---|
| <a id="wattpad-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="wattpad-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/wattpad/ads/HideAdsPatch.kt) | Hides ads in the library, story pages, reader and comments. Rewarded videos for coins still play. |
| <a id="wattpad-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/wattpad/misc/premium/UnlockPremiumPatch.kt) | Unlocks Premium+ with unlimited offline stories, reading themes and no interstitials in the reader. Paid stories still cost coins. |

---

## weather24 (com.wetter.androidclient)

<img src="https://publicmetadata.co.uk/com.wetter.androidclient" width="48" height="48" align="top" alt="">

**Supported versions:**

| 2.78.2 |
| :---: |

| Patch | Details |
|---|---|
| <a id="weather24-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/weather24/misc/premium/UnlockPremiumPatch.kt) | Removes banner, interstitial and video ads and unlocks the premium weather icons. |

---

## X-plore File Manager (com.lonelycatgames.Xplore)

<img src="https://publicmetadata.co.uk/com.lonelycatgames.Xplore" width="48" height="48" align="top" alt="">

**Supported versions:**

| 4.53.05 |
| :---: |

| Patch | Details |
|---|---|
| <a id="x-plore-file-manager-gmscore-support"></a>**GmsCore support** | Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed. |
| <a id="x-plore-file-manager-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/xplore/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium features and removes ads. |

---

## Xianyu (com.taobao.idlefish)

<img src="https://publicmetadata.co.uk/com.taobao.idlefish" width="48" height="48" align="top" alt="">

**Supported versions:**

| 7.28.86.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="xianyu-allow-sharing-without-login"></a>[**Allow sharing without login**](patches/src/main/kotlin/app/morphe/patches/idlefish/misc/share/ShareWithoutLoginPatch.kt) | Shares a public listing link through the Android share sheet when signed out. |
| <a id="xianyu-skip-login-prompts"></a>[**Skip login prompts**](patches/src/main/kotlin/app/morphe/patches/idlefish/misc/login/SkipLoginPromptsPatch.kt) | Skips login screens that open at launch, on search and listing taps and during background requests. |

---

## Xtream Player (me.aap.fermatamod.dear.google.why.bingo2.rec)

<img src="https://publicmetadata.co.uk/me.aap.fermatamod.dear.google.why.bingo2.rec" width="48" height="48" align="top" alt="">

**Supported versions:**

| 1.8.12 | 2.0.10 |
| :---: | :---: |

| Patch | Details |
|---|---|
| <a id="xtream-player-disable-tracking"></a>[**Disable tracking**](patches/src/main/kotlin/app/morphe/patches/xtreamplayer/misc/tracking/DisableTrackingPatch.kt) | Disables ML Kit usage reporting. |
| <a id="xtream-player-full-android-auto-screen"></a>[**Full Android Auto screen**](patches/src/main/kotlin/app/morphe/patches/xtreamplayer/misc/androidauto/AndroidAutoSupportPatch.kt) | Shows full app in Android Auto while parked. Requires Android 15 or newer and Unknown sources in Android Auto developer settings. |
| <a id="xtream-player-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/xtreamplayer/misc/premium/UnlockPremiumPatch.kt) | Unlocks premium media features and visualizers. |

---

## Yanosik (pl.neptis.yanosik.mobi.android)

<img src="https://publicmetadata.co.uk/pl.neptis.yanosik.mobi.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 26.10.0 |
| :---: |

| Patch | Details |
|---|---|
| <a id="yanosik-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/yanosik/misc/premium/UnlockPremiumPatch.kt) | Removes ads and enables the floating widget, dynamic island, route editing and tab customization. Points, quests and rankings are not included. |

---

## Yi iot (com.yunyi.smartcamera)

<img src="https://publicmetadata.co.uk/com.yunyi.smartcamera" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.2.1_20261110 |
| :---: |

| Patch | Details |
|---|---|
| <a id="yi-iot-hide-ads"></a>[**Hide ads**](patches/src/main/kotlin/app/morphe/patches/yiiot/ads/HideAdsPatch.kt) | Removes splash, interstitial, banner and native ads. Hides promotional cards and popups on Home and Highlights. Keeps the optional ad that unlocks an alarm video. |

---

## Yuka (io.yuka.android)

<img src="https://publicmetadata.co.uk/io.yuka.android" width="48" height="48" align="top" alt="">

**Supported versions:**

| 5.9.1 |
| :---: |

| Patch | Details |
|---|---|
| <a id="yuka-sign-in-token"></a>[**Sign-in token**](patches/src/main/kotlin/app/morphe/patches/yuka/misc/signin/SignInTokenPatch.kt) | Signs in with an App Check token. Requires a token copied from Yuka installed from Google Play on a rooted phone. Steps are in the Yuka section of PATCHES.md.<br><sub>Options: App Check token</sub> |
| <a id="yuka-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/yuka/misc/premium/UnlockPremiumPatch.kt) | Unlocks food preferences, diet alerts and the full history. Search and offline downloads don't work. Signing in needs Sign-in token, except on a root mount install. |

**Sign-in token** needs an App Check token from Yuka installed from Google Play on a rooted phone. Yuka uses
the token only to sign in. You need a new one only if you sign out or clear Yuka's data. A root mount install
signs in without a token.

1. On the rooted phone, open Yuka, go to the sign-in screen, enter any email address and tap **Continue**. You
   don't need to enter a password.
2. In [Termux](https://f-droid.org/packages/com.termux), run this command and allow root. It prints one long
   line starting with `eyJ`.

   ```sh
   su --mount-master -c "grep -o 'eyJ[A-Za-z0-9_.-]*' /data/data/io.yuka.android/shared_prefs/com.google.firebase.appcheck.store.*.xml"
   ```

   `No such file or directory` means Yuka hasn't fetched a token yet. Repeat step 1, then run the command again.

   With [Termux:API](https://f-droid.org/packages/com.termux.api) and `pkg install termux-api`, add
   `| termux-clipboard-set` to copy the token straight to the clipboard.

3. Paste the line into the **App Check token** option in Morphe Manager, then patch, install and sign in on
   the same day.

---

## µTorrent (com.utorrent.client)

<img src="https://publicmetadata.co.uk/com.utorrent.client" width="48" height="48" align="top" alt="">

**Supported versions:**

| 8.3.20 |
| :---: |

| Patch | Details |
|---|---|
| <a id="torrent-amoled-dark-theme"></a>[**AMOLED dark theme**](patches/src/main/kotlin/app/morphe/patches/utorrent/misc/theme/AmoledThemePatch.kt) | Adds an option to make dark surfaces pure black. |
| <a id="torrent-fix-screen-insets"></a>[**Fix screen insets**](patches/src/main/kotlin/app/morphe/patches/utorrent/layout/insets/FixScreenInsetsPatch.kt) | Keeps main screens and settings clear of system bars and display cutouts on Android 17. |
| <a id="torrent-unlock-premium"></a>[**Unlock premium**](patches/src/main/kotlin/app/morphe/patches/utorrent/misc/premium/UnlockPremiumPatch.kt) | Unlocks Pro features and removes ads. |

---

## Universal

| Patch | Details |
|---|---|
| <a id="universal-override-certificate-pinning"></a>[**Override certificate pinning**](patches/src/main/kotlin/app/morphe/patches/all/misc/network/OverrideCertificatePinningPatch.kt) | Lets a proxy inspect the app's network traffic. Requires the proxy's certificate installed on the device. |
