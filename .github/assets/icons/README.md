# Third-party app icons

**These icons are not mine and are not covered by this repository's GPLv3 licence.**

Each file is the property of the developer of the app it depicts, and may be a
registered or unregistered trademark. They appear in this repository's README for one
purpose: to identify which app a given set of patches applies to. That is nominative
use: naming a product to refer to that product. No affiliation, sponsorship, or
endorsement by any of these developers is claimed or implied, and none of them have
reviewed or approved this patch bundle.

Nothing here is a modification of any app's branding. The files are unaltered copies of
each developer's own published icon, downscaled to 96x96 for display.

## Sources

Every icon was taken from the developer's own published listing or source repository. None were extracted from an APK.

| File | App | Developer | Source |
|---|---|---|---|
| `projectivy.png` | Projectivy Launcher (`com.spocky.projengmenu`) | Spocky | [Google Play listing](https://play.google.com/store/apps/details?id=com.spocky.projengmenu) (`og:image`) |
| `forus.png` | ForusApp (`com.myvitale.forus`) | MyVitale | [Google Play listing](https://play.google.com/store/apps/details?id=com.myvitale.forus) (`og:image`) |
| `showly.png` | Showly (`com.michaldrabik.showly2`) | Michał Drabik / Trakt | [`trakt/showly`](https://github.com/trakt/showly), `fastlane/metadata/android/en-US/images/icon.png` |

`showly.png` comes from a repository published under GPLv3, so its copyright terms are
compatible with this one. The other two are used under nominative fair use only, with no
licence claimed.

## Removal

If you develop one of these apps and would prefer your icon not appear here, open an
issue or contact the repository owner and it will be removed promptly, with no argument and no counter-notice. The patches list reads fine without icons; the generator renders apps
with no mapped icon as plain text.

## Adding an icon for a new app

Icons are wired in `.github/scripts/generate_patches_readme.py` via the `ICONS` map,
keyed by package name. Use the developer's published icon, add a row to the table above,
and keep it at 96x96.
