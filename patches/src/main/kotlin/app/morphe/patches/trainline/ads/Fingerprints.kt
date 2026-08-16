/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.trainline.ads

import app.morphe.patcher.Fingerprint

internal object AdvertContentFingerprint : Fingerprint(
    definingClass = "Lcom/thetrainline/ads/google_ad/advert_component/AdvertContentKt;",
    returnType = "V",
    parameters = listOf(
        "Landroidx/compose/ui/Modifier;",
        "Landroidx/compose/ui/Modifier;",
        "Lcom/thetrainline/ads/google_ad/load/IAdvertViewProvider;",
        "Lcom/thetrainline/ads/google_ad/advert_component/AdvertState;",
        "Lkotlin/jvm/functions/Function3;",
        "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;",
        "Landroidx/compose/runtime/Composer;",
        "I",
        "I",
    ),
)
