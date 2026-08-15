/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audible.misc.upselling

import app.morphe.patcher.Fingerprint

internal object BookwallSectionMapperFingerprint : Fingerprint(
    definingClass = "Lcom/audible/application/orchestration/bookwallSection/BookwallSectionMapper;",
    returnType = "Lcom/audible/business/common/orchestration/OrchestrationWidgetModel;",
    parameters = listOf(
        "Ljava/lang/Object;",
        "Lcom/audible/business/common/orchestration/PageSectionData;",
        "Lcom/audible/common/orchestration/OrchestrationScreenContext;",
    ),
)

internal object AppHomeBottomSheetCoordinatorFingerprint : Fingerprint(
    definingClass = "Lcom/audible/application/apphome/domain/AppHomeBottomSheetCoordinatorUseCase;",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Z", "Lkotlin/coroutines/jvm/internal/ContinuationImpl;"),
)
