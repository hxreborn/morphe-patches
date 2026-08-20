<div align="center">

# 🧩 hxreborn Patches

**Patches for the Android apps I use, built for [Morphe](https://morphe.software).**

[![Release badge](https://img.shields.io/github/v/release/hxreborn/morphe-patches?style=for-the-badge&label=Release&color=D29922)](https://github.com/hxreborn/morphe-patches/releases/latest)
[![Downloads badge](https://img.shields.io/github/downloads/hxreborn/morphe-patches/total?style=for-the-badge&label=Downloads&color=2F81F7)](https://github.com/hxreborn/morphe-patches/releases/latest)
[![Documentation badge](https://img.shields.io/badge/Documentation-8957E5?style=for-the-badge&logo=github)](https://github.com/MorpheApp/morphe-documentation#readme)
[![License badge](https://img.shields.io/badge/License-GPLv3-3FB950?style=for-the-badge)](LICENSE)

<a href="https://morphe.software/add-source?github=hxreborn/morphe-patches" title="Add this source to Morphe">
  <img alt="Add to Morphe" src="https://img.shields.io/badge/Morphe-Add%20this%20source-00A8FF?style=for-the-badge" height="38"/>
</a>

</div>

&nbsp;
## ❓ About

I maintain these because I use them, but [requests](https://github.com/hxreborn/morphe-patches/issues/new?template=feature_request.yml) for other apps are welcome.

Based off the prior work of [ReVanced](https://github.com/ReVanced). All modifications made
here, along with their dates, can be found in the Git history.

App icons in the patches list belong to their respective developers and are used only to
identify each app. They are not covered by this repository's licence. See
[the icon notice](.github/assets/icons/README.md).

&nbsp;
## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.9.2](https://github.com/hxreborn/morphe-patches/releases/tag/v1.9.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;20 patches total
<details open>
<summary><img src=".github/assets/icons/kick.png" width="18" align="top">&nbsp;&nbsp;Kick&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="kick-amoled-dark-theme"></a>[AMOLED dark theme](patches/src/main/kotlin/app/morphe/patches/kick/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black. Disables over-the-air updates that would restore the original background. |

</details>

<details open>
<summary><img src=".github/assets/icons/perplexity.png" width="18" align="top">&nbsp;&nbsp;Perplexity&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="perplexity-amoled-dark-theme"></a>[AMOLED dark theme](patches/src/main/kotlin/app/morphe/patches/perplexity/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black. |

</details>

<details open>
<summary><img src=".github/assets/icons/protonmail.png" width="18" align="top">&nbsp;&nbsp;Proton Mail&nbsp;&nbsp;•&nbsp;&nbsp;6 patches</summary>
<br>

**🎯 Supported versions:**

| 7.10.4 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="proton-mail-amoled-dark-theme"></a>[AMOLED dark theme](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black. |
| <a id="proton-mail-hide-upgrade-upselling"></a>[Hide upgrade upselling](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/upselling/HideUpgradeUpsellingPatch.kt) | Hides the top-bar upgrade button and promotional sidebar rows. |
| <a id="proton-mail-remove-sent-from-signature"></a>[Remove 'Sent from' signature](patches/src/main/kotlin/app/morphe/patches/protonmail/signature/RemoveSentFromSignaturePatch.kt) | Removes the 'Sent from Proton Mail' signature from emails. |
| <a id="proton-mail-remove-free-accounts-limit"></a>[Remove free accounts limit](patches/src/main/kotlin/app/morphe/patches/protonmail/account/RemoveFreeAccountsLimitPatch.kt) | Removes the limit for maximum free accounts logged in. |
| <a id="proton-mail-spoof-signature"></a>[Spoof signature](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/fix/signature/SpoofSignaturePatch.kt) | Restores push notifications by spoofing the original app signature. |
| <a id="proton-mail-unlock-custom-time-picker"></a>[Unlock custom time picker](patches/src/main/kotlin/app/morphe/patches/protonmail/misc/scheduling/UnlockCustomTimePickerPatch.kt) | Enables picking a custom date and time when snoozing conversations and scheduling messages. |

</details>

<details open>
<summary><img src=".github/assets/icons/showly.png" width="18" align="top">&nbsp;&nbsp;Showly&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="showly-amoled-dark-theme"></a>[AMOLED dark theme](patches/src/main/kotlin/app/morphe/patches/showly/misc/theme/AmoledThemePatch.kt) | Replaces the dark theme background with pure black. |
| <a id="showly-unlock-premium"></a>[Unlock premium](patches/src/main/kotlin/app/morphe/patches/showly/misc/premium/UnlockPremiumPatch.kt) | Unlocks ad removal, light theme, custom images, list view types, quick ratings, and transparent widgets. The News feed is not included. |

</details>

<details open>
<summary><img src=".github/assets/icons/projectivy.png" width="18" align="top">&nbsp;&nbsp;Projectivy Launcher&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="projectivy-launcher-disable-tracking"></a>[Disable tracking](patches/src/main/kotlin/app/morphe/patches/projectivy/misc/tracking/DisableTrackingPatch.kt) | Disables analytics and crash reporting. |
| <a id="projectivy-launcher-unlock-premium"></a>[Unlock premium](patches/src/main/kotlin/app/morphe/patches/projectivy/misc/premium/UnlockPremiumPatch.kt) | Unlocks all premium features. |

</details>

<details open>
<summary><img src=".github/assets/icons/etsy.png" width="18" align="top">&nbsp;&nbsp;Etsy&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 7.90.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="etsy-hide-ads"></a>[Hide ads](patches/src/main/kotlin/app/morphe/patches/etsy/ads/HideAdsPatch.kt) | Removes promoted listings and the "with Ads" label from search results. |

</details>

<details open>
<summary><img src=".github/assets/icons/qrscanner.png" width="18" align="top">&nbsp;&nbsp;QR & Barcode Scanner&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 2.2.221 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="qr-barcode-scanner-hide-ads"></a>[Hide ads](patches/src/main/kotlin/app/morphe/patches/gammascan/ads/HideAdsPatch.kt) | Disables banner, interstitial, and native ads. |

</details>

<details open>
<summary><img src=".github/assets/icons/trainline.png" width="18" align="top">&nbsp;&nbsp;Trainline&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 407.0.0.178994 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="trainline-hide-ads"></a>[Hide ads](patches/src/main/kotlin/app/morphe/patches/trainline/ads/HideAdsPatch.kt) | Removes the adverts shown between search results. |

</details>

<details open>
<summary><img src=".github/assets/icons/audible.png" width="18" align="top">&nbsp;&nbsp;Audible&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="audible-hide-membership-upselling"></a>[Hide membership upselling](patches/src/main/kotlin/app/morphe/patches/audible/misc/upselling/HideMembershipUpsellingPatch.kt) | Hides the membership promotion on the Home screen and the free trial bottom sheet. |
| <a id="audible-open-library-on-launch"></a>[Open Library on launch](patches/src/main/kotlin/app/morphe/patches/audible/startup/OpenLibraryOnLaunchPatch.kt) | Opens the Library tab instead of Home on launch. Applies only while signed in. |

</details>

<details open>
<summary><img src=".github/assets/icons/readera.png" width="18" align="top">&nbsp;&nbsp;ReadEra&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 26.05.20+2300 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="readera-remove-nags"></a>[Remove nags](patches/src/main/kotlin/app/morphe/patches/readera/misc/nags/RemoveNagsPatch.kt) | Removes the rate this app dialog and the promotional dialogs shown on startup. |

</details>

<details open>
<summary><img src=".github/assets/icons/forus.png" width="18" align="top">&nbsp;&nbsp;ForusApp&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="forusapp-unlock-premium"></a>[Unlock premium](patches/src/main/kotlin/app/morphe/patches/forus/misc/premium/UnlockPremiumPatch.kt) | Unlocks all premium features. |

</details>

<details open>
<summary><img src=".github/assets/icons/symfonium.png" width="18" align="top">&nbsp;&nbsp;Symfonium&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 14.0.0 | 14.1.0 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| <a id="symfonium-unlock-premium"></a>[Unlock premium](patches/src/main/kotlin/app/morphe/patches/symfonium/misc/premium/UnlockPremiumPatch.kt) | Unlocks all premium features. |

</details>

<details open>
<summary><img src=".github/assets/icons/tiktok.png" width="18" align="top">&nbsp;&nbsp;TikTok&nbsp;&nbsp;•&nbsp;&nbsp;separate bundle</summary>
<br>

**🎯 Supported versions:**

| 46.2.3 |
| :---: |

| 📦&nbsp;Bundle | 📜&nbsp;Description |
|----------|----------------|
| [hxreborn-tiktok-patches](https://github.com/hxreborn/hxreborn-tiktok-patches) | Not part of this bundle, so it has to be added to Morphe as its own patch source. Forked from [icysymmetra/tiktok-patches-for-morphe](https://github.com/icysymmetra/tiktok-patches-for-morphe). [Add to Morphe](https://morphe.software/add-source?github=hxreborn/hxreborn-tiktok-patches) |

</details>

<!-- PATCHES_END -->
&nbsp;
## 📲 Installing

[Add this source](https://morphe.software/add-source?github=hxreborn/morphe-patches) to Morphe
Manager, then patch any app listed above.

&nbsp;
## 🛠️ Building

You need Java 21 and a GitHub token with `read:packages`:

```bash
./gradlew buildAndroid
```

The bundle lands in `patches/build/libs/`. See the
[Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for the full setup.

&nbsp;
## 📜 License

hxreborn Patches are licensed under the [GNU General Public License v3.0](LICENSE), with
additional conditions under GPLv3 Section 7 inherited from Morphe:

- **Attribution (7b):** all original notices and disclaimers are preserved.
- **Name & branding (7c & 7e):** the **"Morphe"** name, logos, and trademarks are not used to
  brand this project, which is a third-party bundle *for use with* Morphe.

See [NOTICE](NOTICE) for the full conditions.
