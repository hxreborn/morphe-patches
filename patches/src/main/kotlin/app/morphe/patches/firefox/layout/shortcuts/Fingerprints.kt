/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * automation/iceraven/patches/top_sites_no_most_visted_sites.patch
 */
package app.morphe.patches.firefox.layout.shortcuts

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object TopSitesFingerprint : Fingerprint(
    parameters = listOf(
        "I",
        "Lmozilla/components/feature/top/sites/TopSitesFrecencyConfig;",
        "Lmozilla/components/feature/top/sites/TopSitesProviderConfig;",
        "Lkotlin/coroutines/jvm/internal/ContinuationImpl;",
    ),
    filters = listOf(
        methodCall(
            definingClass = "Lmozilla/components/browser/storage/sync/PlacesHistoryStorage\$getTopFrecentSites\$2;",
            name = "<init>",
        ),
    ),
)
