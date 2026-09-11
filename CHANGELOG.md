## [1.18.0](https://github.com/hxreborn/morphe-patches/compare/v1.17.0...v1.18.0) (2026-09-11)

### Bug Fixes

* **MovieBox - All-In-One:** load content on devices with a region-restricted SIM ([666fa11](https://github.com/hxreborn/morphe-patches/commit/666fa11d1fddccbacfda8f3d82b1d003e59b3812))

### New Features

* **AlpineQuest:** add support for 2.4.0e ([6133b04](https://github.com/hxreborn/morphe-patches/commit/6133b042a15a1ba919b58b608db9ab763375459b))
* **Audible:** add support for 26.30.05 ([1ad5ed1](https://github.com/hxreborn/morphe-patches/commit/1ad5ed1e426bce42c236383c3bef4d3d62e1185c))
* **ForusApp:** add support for 3.0.15 ([b9e91b1](https://github.com/hxreborn/morphe-patches/commit/b9e91b194600949ab999418c75f551fb3521f422))
* **MovieBox - All-In-One:** open titles in streaming mode regardless of region ([3ffd22b](https://github.com/hxreborn/morphe-patches/commit/3ffd22b9fd28ae686696ad789e57acfc0325a442))
* **Notesnook:** add support for 3.4.12 ([496b6d0](https://github.com/hxreborn/morphe-patches/commit/496b6d08a133ee43c7924fa7f7de8f83be9c0b08))
* **Perplexity:** add support for 2.95.0 ([53d7a3b](https://github.com/hxreborn/morphe-patches/commit/53d7a3bcb80738b69618e03a2ce4faf07e2e1e48))
* **Projectivy:** add support for 4.70 and 4.71 ([a73a0c1](https://github.com/hxreborn/morphe-patches/commit/a73a0c1776582fea8b32cc0a7924f1eadcad0eb1))
* **RateGlance:** add support for 1.14.8 ([b459e41](https://github.com/hxreborn/morphe-patches/commit/b459e41a4e60f8e8720723199ea6099182bd0dc7))
* **RISE - Disable telemetry:** stop crash and error reports going to Sentry ([8c407fc](https://github.com/hxreborn/morphe-patches/commit/8c407fcd4a77668a3794ecba46f66f5f4d072158))
* **RISE - Disable usage tracking:** stop app usage events being uploaded ([c6012b8](https://github.com/hxreborn/morphe-patches/commit/c6012b8e1e8a2dd0588ab8aafd330ef466648a8e))
* **Rubber Bands:** add support for 3.9 ([b752035](https://github.com/hxreborn/morphe-patches/commit/b75203524b307be9e950bbdf8f21a5150f7838ac))
* **Showly:** add support for 3.70.0 ([290b1ff](https://github.com/hxreborn/morphe-patches/commit/290b1ff1defe694bd810de0d12b25d2f5622da94))
* **Symfonium:** support 15.0.1 ([b67007c](https://github.com/hxreborn/morphe-patches/commit/b67007c16997d9fccccdaaeb763bc28c89293595))

## [1.17.0](https://github.com/hxreborn/morphe-patches/compare/v1.16.0...v1.17.0) (2026-09-11)

### Bug Fixes

* **iJiami:** run app hook on loaded app class loader ([84bcb4c](https://github.com/hxreborn/morphe-patches/commit/84bcb4ceaad7e3d100d956f832de50e1b6b21653))
* **RevenueCat unlock:** keep premium active on accounts with lapsed subscriptions ([b03aa4c](https://github.com/hxreborn/morphe-patches/commit/b03aa4c17615100583a32c240430a88a688b3a84))

### New Features

* **360 Jiagu packer:** add initial support for Jiagu-packed apps with patch-time payload rewriting ([c147073](https://github.com/hxreborn/morphe-patches/commit/c14707335218be96a8c9915e81b88b9dae181fa7))
* **DWG FastView - Block telemetry:** block analytics and ad network endpoints ([3df50f9](https://github.com/hxreborn/morphe-patches/commit/3df50f97f47784c9c82a57e23e71d57f4638253a))
* **DWG FastView - Hide rating dialog:** remove prompt asking for store review ([1a4530d](https://github.com/hxreborn/morphe-patches/commit/1a4530ded4024b9636965307526356e60b7e20d3))
* **DWG FastView - Unlock premium:** unlock paid drawing and measurement tools and remove ads ([135eb68](https://github.com/hxreborn/morphe-patches/commit/135eb68034a3a2b8b0b0d5482f0562b38caedd23))
* **DWG FastView:** add support for 5.19.4, 5.20.0 and 5.21.0 ([d1c5802](https://github.com/hxreborn/morphe-patches/commit/d1c5802fe84e45253bc7ee1746f103c491fbf5da))
* **iJiami packer:** add initial profile-based support for apps protected by iJiami ([b38b485](https://github.com/hxreborn/morphe-patches/commit/b38b4859fe4c249b48c570d69d61cada64cd7399))
* **iJiami packer:** add support for per-app runtime hook dexes in apps protected by iJiami ([508b956](https://github.com/hxreborn/morphe-patches/commit/508b9562a7567729210dd10f67ae67b7ae193f56))
* **MovieBox - All-In-One:** enable video playback and downloads ([33868b2](https://github.com/hxreborn/morphe-patches/commit/33868b2eef8102eded2526844f78dd176e0664c1))
* **MovieBox - All-In-One:** mark unhosted titles unavailable and show member days ([3267403](https://github.com/hxreborn/morphe-patches/commit/32674031ad979e269086a3de0d743ec7035d68e1))
* **MovieBox - All-In-One:** move the port from 3.0.14 to the 4.0.02 builds protected by iJiami ([02dc6f9](https://github.com/hxreborn/morphe-patches/commit/02dc6f9c2f61406c15009ffb6b0a9c911f1c3700))
* **MovieBox - All-In-One:** port rushiranpise's 3.0.14 unlock, ad removal and region bypass ([0d0c3fa](https://github.com/hxreborn/morphe-patches/commit/0d0c3fa8ca7373260e25a66e2a5e9d5b2c94127d))

### Improvements

* **DWG FastView - Unlock premium:** drop 4 MB of pre-patched ciphertext from bundle ([c8a8b53](https://github.com/hxreborn/morphe-patches/commit/c8a8b537d340c66b354921d5715d3df8a26cd45b))

## [1.16.0](https://github.com/hxreborn/morphe-patches/compare/v1.15.0...v1.16.0) (2026-09-08)

### Bug Fixes

* **All-In-One Calculator:** stop listing the pairip patch that only serves the unlocks ([2f93695](https://github.com/hxreborn/morphe-patches/commit/2f93695904bad66bd4ed1b02f64a92f6e8c7a4e1))
* **AlpineQuest - Bypass signature check:** stop listing a patch that only serves the unlock ([4959f5e](https://github.com/hxreborn/morphe-patches/commit/4959f5e070d79130977df9b8f73b198945985095))
* **Photo Editor Pro - Spoof signature:** stop listing a patch the app cannot run without ([0bdb3db](https://github.com/hxreborn/morphe-patches/commit/0bdb3dba66a2e669c6610d02452c65f722621460))
* **Photo Editor Pro:** stop listing the iOS spoof the AI tools cannot work without ([a44cffd](https://github.com/hxreborn/morphe-patches/commit/a44cffd9a8ded18908277244eabc2054af5bf00f))
* **Proton Mail:** keep push notifications working whichever patches are selected ([e17ead1](https://github.com/hxreborn/morphe-patches/commit/e17ead1c4fa5d8adfbe7cae759f1d24451fb8969))
* **Quranify - Unlock premium:** pull in the integrity and pairip patches as dependencies ([06db4dd](https://github.com/hxreborn/morphe-patches/commit/06db4dd2dbebd6ba9cab2b7bf579ceb5ac912353))
* **RateGlance - Unlock premium:** pull in the pairip patch as a dependency ([5359f91](https://github.com/hxreborn/morphe-patches/commit/5359f913df2c84be9c5bb2c66f001578454fb6b2))
* **Remove pairip protection:** fail at patch time when the hoisted-field table does not match ([5972cc3](https://github.com/hxreborn/morphe-patches/commit/5972cc3fdd23c81866b42215d558058fb30202ef))
* **Remove pairip protection:** stop listing a patch that only serves the unlocks ([8d8acf6](https://github.com/hxreborn/morphe-patches/commit/8d8acf6e35833bec3af6875feba4b45ab28a510a))

### New Features

* **BetterSleep - Spoof signature:** keep account sign-up and Firebase working on a re-signed build ([eac36d1](https://github.com/hxreborn/morphe-patches/commit/eac36d1efbee1b5ddec40732cc490121a5ef7267))
* **BetterSleep - Unlock premium:** unlock all content and skip the free trial screen ([8cac3d5](https://github.com/hxreborn/morphe-patches/commit/8cac3d59efcb71922a8884f00f17962b5f2201ec))
* **RISE - Unlock premium:** unlock the energy schedule and habit tools without a subscription ([d9df4c1](https://github.com/hxreborn/morphe-patches/commit/d9df4c1ff09a965c5ef365fbc04e648d21249881))

## [1.15.0](https://github.com/hxreborn/morphe-patches/compare/v1.14.1...v1.15.0) (2026-09-07)

### Bug Fixes

* **Proton Mail - Hide upgrade upselling:** hide special offer sidebar row ([3642fbd](https://github.com/hxreborn/morphe-patches/commit/3642fbd241656adbf04c916a0b042815b258eea4))
* **Remove pairip protection:** return early from initializeLicenseCheck ([4677d7f](https://github.com/hxreborn/morphe-patches/commit/4677d7f778f4a419e456b1c99058c73ede820f24))

### New Features

* **Photo Editor Pro:** warn on an APK that is not the stock build ([e3f3a18](https://github.com/hxreborn/morphe-patches/commit/e3f3a18e2505fa1f6cc91a8891e2f78c0870ea6b))
* **Proton Mail - Remove free accounts limit:** apply on 7.11.5 ([5e39b74](https://github.com/hxreborn/morphe-patches/commit/5e39b74a08693a928b278cbac9e0ea5008299797))
* **Proton Mail:** support 7.11.5 ([528d9a7](https://github.com/hxreborn/morphe-patches/commit/528d9a7b751cb3ff14992256d7d00cf91638c466))
* **Proton Mail:** warn on an APK that is not the stock build ([1d030a6](https://github.com/hxreborn/morphe-patches/commit/1d030a61439ca37eb91b33b0925b89410cd2473b))
* **Quranify - Bypass integrity check:** load reciters without a Google Play check ([8a89e66](https://github.com/hxreborn/morphe-patches/commit/8a89e66e26ce3135b56b69cac053902cb786f980))
* **Quranify - Unlock premium:** unlock the premium surah downloads and insights ([2c0aa72](https://github.com/hxreborn/morphe-patches/commit/2c0aa72c01ddc02cf3e40dd4a4408f0cee4d70fe))
* **Spoof signature:** read the certificate from the APK being patched ([b20cf21](https://github.com/hxreborn/morphe-patches/commit/b20cf21025568d67efb51d2e92c8260abacc117d))

## [1.14.1](https://github.com/hxreborn/morphe-patches/compare/v1.14.0...v1.14.1) (2026-09-02)

### Bug Fixes

* **Proton Mail:** apply the patches on any app version ([a30d68d](https://github.com/hxreborn/morphe-patches/commit/a30d68d47da5356b4c0d4f76dcf6137aa60dbe98))

## [1.14.0](https://github.com/hxreborn/morphe-patches/compare/v1.13.0...v1.14.0) (2026-08-30)

### Bug Fixes

* **Cx File Explorer - Dark theme:** darken the file operation progress dialog ([f072429](https://github.com/hxreborn/morphe-patches/commit/f072429435c5d77774e4d5a0cd6eaaecf2104bff))

### New Features

* **All-In-One Calculator - GmsCore support:** sign in through GmsCore ([29267f9](https://github.com/hxreborn/morphe-patches/commit/29267f9bd56162f920bd858a15da0bf296a7f11b))
* **RateGlance - Remove pairip protection:** drop the Play Integrity license check ([401f975](https://github.com/hxreborn/morphe-patches/commit/401f975e428c56a1ebe6b8cfa61a9a91cecc9339))
* **RateGlance - Unlock premium:** unlock alerts widgets and full rate history ([366a1f3](https://github.com/hxreborn/morphe-patches/commit/366a1f3d92560f23b5692c6bc7ec29271030007f))

## [1.13.0](https://github.com/hxreborn/morphe-patches/compare/v1.12.0...v1.13.0) (2026-08-27)

### Bug Fixes

* **Photo Editor Pro - Inspect AI requests:** apply by default and let the runtime toggle decide ([c961e75](https://github.com/hxreborn/morphe-patches/commit/c961e75f18f5ed6ecce77d0b811ddaf3dd508d15))
* **Photo Editor Pro:** align the panel summaries with the patch descriptions ([b489e8b](https://github.com/hxreborn/morphe-patches/commit/b489e8b52fda416856a93314d2776a117df40ca6))

### New Features

* **All-In-One Calculator:** add `Unlock premium` and `Remove pairip protection` ([6511bcc](https://github.com/hxreborn/morphe-patches/commit/6511bccd3693f23ad4fb617c6f377a9eef3f30e5))
* **Kick - AMOLED dark theme:** support the Android TV app ([5e7b9bc](https://github.com/hxreborn/morphe-patches/commit/5e7b9bc08d0db3d3120ee3f44a9ab5163c5e4dea))
* **Photo Editor Pro - Inspect AI requests:** log the response body of each AI request ([5fea9a7](https://github.com/hxreborn/morphe-patches/commit/5fea9a7d9d5d96b6c0f0dc0ec409570295ad9a69))
* **Photo Editor Pro - Spoof iOS platform:** restore the AI tools on certified devices ([f973d5f](https://github.com/hxreborn/morphe-patches/commit/f973d5fec5c5ebabe5c1612a2793059d263d4f9b))
* **Photo Editor Pro:** list the always-on patches in the settings panel ([92f1b0b](https://github.com/hxreborn/morphe-patches/commit/92f1b0bc138ec0aa5f49d6f8a2fa9f3deda96ae0))

## [1.12.0](https://github.com/hxreborn/morphe-patches/compare/v1.11.1...v1.12.0) (2026-08-26)

### Bug Fixes

* **Kick - AMOLED dark theme:** support older builds that have no React Native bundle ([8f50e51](https://github.com/hxreborn/morphe-patches/commit/8f50e51a0486cf3548cd81a0bf478f216adfb62e))

### New Features

* **Photo Editor Pro:** add `Hide ads` patch ([e49c9bd](https://github.com/hxreborn/morphe-patches/commit/e49c9bd1d33c79462761a9f2a64aa544000a03ad))
* **Photo Editor Pro:** add `Inspect AI requests` patch ([886bca1](https://github.com/hxreborn/morphe-patches/commit/886bca1e042e8b8aaf779323bbf0cb512b05cd0f))
* **Photo Editor Pro:** add `Show AI progress` patch ([1e51b7a](https://github.com/hxreborn/morphe-patches/commit/1e51b7adaf2fdd1d5107d27b45bcb3a10bcab685))
* **Photo Editor Pro:** add `Speed up AI tools` patch ([73c868b](https://github.com/hxreborn/morphe-patches/commit/73c868b2762e4767c233216716161a111cebb5b3))
* **Photo Editor Pro:** add `Spoof signature` patch ([8dea264](https://github.com/hxreborn/morphe-patches/commit/8dea2642e5ff2ff5a4ec3167144c4bae21ec89d2))
* **Photo Editor Pro:** add `Unlock premium` patch ([bbd30c9](https://github.com/hxreborn/morphe-patches/commit/bbd30c918833ffd11ee89137d48631e710f1c243))

## [1.11.1](https://github.com/hxreborn/morphe-patches/compare/v1.11.0...v1.11.1) (2026-08-24)

### Bug Fixes

* **Cx File Explorer - Dark theme:** darken the selection mode toolbar and bottom bar ([2575fbf](https://github.com/hxreborn/morphe-patches/commit/2575fbf27ba9d19b7948c0912f8e6bae2eda2ae8))
* **Cx File Explorer - Dark theme:** render the bottom sheet dialogs dark ([5fefefc](https://github.com/hxreborn/morphe-patches/commit/5fefefce1e9a7cce18d73d1c0b444633e699cb32))

## [1.11.0](https://github.com/hxreborn/morphe-patches/compare/v1.10.0...v1.11.0) (2026-08-23)

### New Features

* **Cx File Explorer - AMOLED dark theme:** add a pure black option to the dark theme ([c90c1d3](https://github.com/hxreborn/morphe-patches/commit/c90c1d34cfd143692852848832a11449755acd89))
* **Cx File Explorer - Dark theme:** render the dark theme and add it to the settings ([933bc15](https://github.com/hxreborn/morphe-patches/commit/933bc15841100f51c5a5434f80cf403d77814d02))
* **Cx File Explorer - Unlock premium:** unlock premium and remove ads ([f373beb](https://github.com/hxreborn/morphe-patches/commit/f373beb9ded78b84bec8a1ecfe3a2a0352de63af))
* **Notesnook - Unlock pro:** unlock the on-device pro features ([5cc8777](https://github.com/hxreborn/morphe-patches/commit/5cc8777b6b9c14931819539f9937280270640952))
* **Proton Mail:** extend patch support to 7.11.4 ([230b7e7](https://github.com/hxreborn/morphe-patches/commit/230b7e72925882c4a5e7e731ec5b4926e8268192))
* **Rubber Bands - Unlock premium:** unlock the paywalled Pro features ([0058964](https://github.com/hxreborn/morphe-patches/commit/0058964775f1f7e3ecdabb740d354fe1f4e5498f))
* **Symfonium - Unlock premium:** support the 14.0.0 TV build ([a8ee80d](https://github.com/hxreborn/morphe-patches/commit/a8ee80d0b4b6976b7d032c9de45c0b0b51497533))

## [1.10.0](https://github.com/hxreborn/morphe-patches/compare/v1.9.2...v1.10.0) (2026-08-21)

### New Features

* **AlpineQuest - Bypass signature check:** restore search and auto-routing on re-signed builds ([db4dcc9](https://github.com/hxreborn/morphe-patches/commit/db4dcc94e128372c1f0a422db62b043187cc4651))
* **AlpineQuest - Unlock full version:** unlock the Off-Road Explorer features ([03ab456](https://github.com/hxreborn/morphe-patches/commit/03ab456fd6791c19055d6df132b17d32b4a169f9))

## [1.9.2](https://github.com/hxreborn/morphe-patches/compare/v1.9.1...v1.9.2) (2026-08-20)

### Bug Fixes

* **Perplexity - AMOLED dark theme:** keep the tinted surfaces at stock tone ([00d4af1](https://github.com/hxreborn/morphe-patches/commit/00d4af12851c31edc7e9b97658a7bec6567f319d))
* **Proton Mail - AMOLED dark theme:** keep the sidebar selected row and separators ([98a511f](https://github.com/hxreborn/morphe-patches/commit/98a511ff290d5607c24271d5c35ad4f6b42eaa76))

## [1.9.1](https://github.com/hxreborn/morphe-patches/compare/v1.9.0...v1.9.1) (2026-08-19)

### Bug Fixes

* **Showly - AMOLED dark theme:** keep the search bar and bottom menu tone ([db46e3a](https://github.com/hxreborn/morphe-patches/commit/db46e3a48e7f1ea5ec321a5a2b3c188a05564b20))

## [1.9.0](https://github.com/hxreborn/morphe-patches/compare/v1.8.0...v1.9.0) (2026-08-19)

### Bug Fixes

* **Perplexity - AMOLED dark theme:** stop blackening the raised surface ([288a1ea](https://github.com/hxreborn/morphe-patches/commit/288a1eab9249cc424c9000d9825ea5b72b94df0f))

### New Features

* **Kick - AMOLED dark theme:** add patch ([3f35f90](https://github.com/hxreborn/morphe-patches/commit/3f35f90bc83642f208b2614b70eb84ed0c1d7d11))

## [1.8.0](https://github.com/hxreborn/morphe-patches/compare/v1.7.0...v1.8.0) (2026-08-18)

### New Features

* **Etsy - Hide ads:** drop promoted listings from search results ([856b590](https://github.com/hxreborn/morphe-patches/commit/856b590fe7cbe7165ae4fb2a0563f4f9cd98de5b))
* **QR & Barcode Scanner - Hide ads:** disable all AdMob ad loads ([337350a](https://github.com/hxreborn/morphe-patches/commit/337350a1d3775652bb40f869a07149f5ea59c5be))

## [1.7.0](https://github.com/hxreborn/morphe-patches/compare/v1.6.0...v1.7.0) (2026-08-16)

### New Features

* **ReadEra - Remove nags:** add patch ([dbd52e3](https://github.com/hxreborn/morphe-patches/commit/dbd52e341cdc9b4a79da86624e4994279963bc2b))
* **Trainline - Hide ads:** add patch ([9903161](https://github.com/hxreborn/morphe-patches/commit/9903161c8a416c641ee46d8147c8f8a5a707f54b))

## [1.6.0](https://github.com/hxreborn/morphe-patches/compare/v1.5.1...v1.6.0) (2026-08-16)

### New Features

* **Audible - Hide membership upselling:** add patch ([7898c5f](https://github.com/hxreborn/morphe-patches/commit/7898c5fc603a0455641d3eff17dc107582c45ea5))
* **Perplexity - AMOLED dark theme:** add patch ([279ce08](https://github.com/hxreborn/morphe-patches/commit/279ce0849ddec8ffbab58b3add0a4fdaea787528))

## [1.5.1](https://github.com/hxreborn/morphe-patches/compare/v1.5.0...v1.5.1) (2026-08-15)

### Bug Fixes

* **Symfonium - Unlock premium:** force the native key check that halts playback ([f35b796](https://github.com/hxreborn/morphe-patches/commit/f35b796d57999dc69b3d7cbaf059cebb3aa2f1a1))

## [1.5.0](https://github.com/hxreborn/morphe-patches/compare/v1.4.0...v1.5.0) (2026-08-15)

### New Features

* **Symfonium - Unlock premium:** add patch ([52dcd24](https://github.com/hxreborn/morphe-patches/commit/52dcd24a62042f59f0339d67e3c1d556b499644c))

## [1.4.0](https://github.com/hxreborn/morphe-patches/compare/v1.3.0...v1.4.0) (2026-08-12)

### Bug Fixes

* **Projectivy - Disable tracking:** pin the crashlytics URL builder for the host redirect ([f3e1d90](https://github.com/hxreborn/morphe-patches/commit/f3e1d9006946882818a21900627c426159321994))

### New Features

* **Audible - Open Library on launch:** add patch ([50d9870](https://github.com/hxreborn/morphe-patches/commit/50d9870841542e8c487296eca61a9f0e92522bf8))
* **Showly - Unlock premium:** force Trakt VIP to skip the paywall ([a7ec4fc](https://github.com/hxreborn/morphe-patches/commit/a7ec4fcb4c149a2e2e2f9bedfb7295f6d43ea47a))

## [1.3.0](https://github.com/hxreborn/morphe-patches/compare/v1.2.0...v1.3.0) (2026-08-05)

### New Features

* **Proton Mail - Spoof signature:** restore push notifications on patched installs ([28ab0f9](https://github.com/hxreborn/morphe-patches/commit/28ab0f97eb618e1b18dac7e00b3f1bab3f9c7cac))
* **Proton Mail - Unlock custom time picker:** add patch ([83657d5](https://github.com/hxreborn/morphe-patches/commit/83657d5d67fe7036d39e699690233c33db1ca001))

## [1.2.0](https://github.com/hxreborn/morphe-patches/compare/v1.1.1...v1.2.0) (2026-08-03)

### New Features

* **Showly - AMOLED dark theme:** add patch ([#16](https://github.com/hxreborn/morphe-patches/issues/16)) ([794378d](https://github.com/hxreborn/morphe-patches/commit/794378de8e2d71feb6d02b79af8ca26c2832714c))

## [1.1.1](https://github.com/hxreborn/morphe-patches/compare/v1.1.0...v1.1.1) (2026-08-01)

### Bug Fixes

* **Proton Mail - Remove free accounts limit:** bypass the native post-login check ([e5e91d2](https://github.com/hxreborn/morphe-patches/commit/e5e91d2e84a5e698e8788db3b7198d7ad45caccc))

## [1.1.0](https://github.com/hxreborn/morphe-patches/compare/v1.0.1...v1.1.0) (2026-07-30)

### Bug Fixes

* **Proton Mail - Remove 'Sent from' signature:** support 7.10.4 ([e5249a0](https://github.com/hxreborn/morphe-patches/commit/e5249a097a74b11dc2490dcc4f03956614a011d3))

### New Features

* **Proton Mail - AMOLED dark theme:** add patch ([57c71b8](https://github.com/hxreborn/morphe-patches/commit/57c71b88367df3397899125d937545f035aa6a8b))
* **Proton Mail - Hide sidebar upselling:** add patch ([799b985](https://github.com/hxreborn/morphe-patches/commit/799b985465528fae922be42f8e39dec99f716497))
* **Proton Mail - Hide upgrade upselling:** hide top-bar button and promotional sidebar rows ([4b15bc9](https://github.com/hxreborn/morphe-patches/commit/4b15bc92367f7de7c4082a1558981dd3bb4aefc8))
* **Proton Mail - Remove 'Sent from' signature:** port patch ([56009c8](https://github.com/hxreborn/morphe-patches/commit/56009c8d280333a31bc363b063fc2b74da524495))
* **Proton Mail - Remove free accounts limit:** port patch ([fe78a8f](https://github.com/hxreborn/morphe-patches/commit/fe78a8fd4e9ae59b0a2b05bd9b99b87bcca53da1))
* **Proton Mail:** support 7.10.4 ([bae3087](https://github.com/hxreborn/morphe-patches/commit/bae3087079651d221625823cfe883c08cf08d91a))

## [1.1.0-dev.1](https://github.com/hxreborn/morphe-patches/compare/v1.0.1...v1.1.0-dev.1) (2026-07-19)

### New Features

* **Proton Mail - Hide sidebar upselling:** add patch ([799b985](https://github.com/hxreborn/morphe-patches/commit/799b985465528fae922be42f8e39dec99f716497))
* **Proton Mail - Remove 'Sent from' signature:** port patch ([56009c8](https://github.com/hxreborn/morphe-patches/commit/56009c8d280333a31bc363b063fc2b74da524495))
* **Proton Mail - Remove free accounts limit:** port patch ([fe78a8f](https://github.com/hxreborn/morphe-patches/commit/fe78a8fd4e9ae59b0a2b05bd9b99b87bcca53da1))

## [1.0.1](https://github.com/hxreborn/morphe-patches/compare/v1.0.0...v1.0.1) (2026-07-17)

### Bug Fixes

* **compat:** add app metadata, drop template patches ([faf5a83](https://github.com/hxreborn/morphe-patches/commit/faf5a83a0e7295feaaa5254ded0ff0a9493fcebc))

## [1.0.1-dev.1](https://github.com/hxreborn/morphe-patches/compare/v1.0.0...v1.0.1-dev.1) (2026-07-17)

### Bug Fixes

* **compat:** add app metadata, drop template patches ([faf5a83](https://github.com/hxreborn/morphe-patches/commit/faf5a83a0e7295feaaa5254ded0ff0a9493fcebc))

## 1.0.0 (2026-07-17)

### Bug Fixes

* **Forus - Unlock premium:** target module access getter ([cc1e9e3](https://github.com/hxreborn/morphe-patches/commit/cc1e9e3f9e3e44cd979cd21d40dad4a0b4fab6db))

### New Features

* **Forus - Unlock premium:** add unlock premium patch ([2a80e5a](https://github.com/hxreborn/morphe-patches/commit/2a80e5a8d0f9a40f5d2756e689948b9940120aa2))
* **Projectivy - Disable tracking:** add disable tracking patch ([7a24927](https://github.com/hxreborn/morphe-patches/commit/7a24927e9da4975ba371b450a941048a111d50c9))
* **Projectivy - Unlock premium:** add unlock premium patch ([30a6a78](https://github.com/hxreborn/morphe-patches/commit/30a6a78c929f7eb6ab979848745cf6d2d7c822b6))
* **Showly - Unlock premium:** add unlock premium patch ([d4fe919](https://github.com/hxreborn/morphe-patches/commit/d4fe9196bb7afb6b740c36843d9628e83a5af537))

## [1.0.0-dev.4](https://github.com/hxreborn/morphe-patches/compare/v1.0.0-dev.3...v1.0.0-dev.4) (2026-07-17)

### New Features

* **Projectivy - Disable tracking:** add disable tracking patch ([7a24927](https://github.com/hxreborn/morphe-patches/commit/7a24927e9da4975ba371b450a941048a111d50c9))
* **Projectivy - Unlock premium:** add unlock premium patch ([30a6a78](https://github.com/hxreborn/morphe-patches/commit/30a6a78c929f7eb6ab979848745cf6d2d7c822b6))

## [1.0.0-dev.3](https://github.com/hxreborn/morphe-patches/compare/v1.0.0-dev.2...v1.0.0-dev.3) (2026-07-17)

### Bug Fixes

* **Forus - Unlock premium:** target module access getter ([cc1e9e3](https://github.com/hxreborn/morphe-patches/commit/cc1e9e3f9e3e44cd979cd21d40dad4a0b4fab6db))

## [1.0.0-dev.2](https://github.com/hxreborn/morphe-patches/compare/v1.0.0-dev.1...v1.0.0-dev.2) (2026-07-17)

### New Features

* **Showly - Unlock premium:** add unlock premium patch ([d4fe919](https://github.com/hxreborn/morphe-patches/commit/d4fe9196bb7afb6b740c36843d9628e83a5af537))

## 1.0.0-dev.1 (2026-07-17)

### New Features

* **Forus - Unlock premium:** add unlock premium patch ([2a80e5a](https://github.com/hxreborn/morphe-patches/commit/2a80e5a8d0f9a40f5d2756e689948b9940120aa2))
