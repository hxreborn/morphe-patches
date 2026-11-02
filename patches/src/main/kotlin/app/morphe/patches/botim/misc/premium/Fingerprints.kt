/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object SelfProfileMapperFingerprint : Fingerprint(
    parameters = listOf("Lim/thebot/bind/proto/profile/Profile\$UserProfileInfoPb;"),
    filters = listOf(
        methodCall(
            definingClass = "Lim/thebot/bind/proto/profile/Profile\$UserProfileInfoPb;",
            name = "getIsVip",
            returnType = "Z",
        ),
    ),
)

internal object VipUpsellCardFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Lim/thebot/main/main_page/home/bean/Section;",
        "Ljava/util/List;",
        "Lkotlin/jvm/functions/Function3;",
        "Landroidx/compose/runtime/Composer;",
        "I",
    ),
    filters = listOf(
        string(
            "im.thebot.main.main_page.home.cards.FillLineSection (CardFormat.kt:",
            StringComparisonType.STARTS_WITH,
        ),
    ),
)

internal object SduiCollectionItemsFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    parameters = listOf("Lim/thebot/cep/sdui/model/SduiCollectionData;", "L"),
    strings = listOf("dataContext"),
)
