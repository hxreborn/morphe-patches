/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.loadingindicator

import app.morphe.patches.shared.misc.proton.pullRefreshContentFingerprint

internal val PullToRefreshContentFingerprint = pullRefreshContentFingerprint(
    "Landroidx/compose/material3/ProgressIndicatorKt;",
    "Landroidx/compose/material3/pulltorefresh/PullToRefreshKt;",
)
