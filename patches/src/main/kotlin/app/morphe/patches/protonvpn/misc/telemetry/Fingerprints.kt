/*
 * Copyright (C) 2026 Paresh Maheshwari
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from Paresh-Maheshwari/paresh-patches:
 * https://gitlab.com/Paresh-Maheshwari/paresh-patches/-/commit/7c732a6b7791eade5d22fdd64f0d9addac57f56b
 * Commit 7c732a6b7791eade5d22fdd64f0d9addac57f56b (2026-04-29),
 * patches/src/main/kotlin/app/paresh/patches/protonvpn/misc/DisableTelemetryPatch.kt
 */
package app.morphe.patches.protonvpn.misc.telemetry

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val PAYMENT_FFI = "Lme/proton/android/payment/core/PaymentFfi;"

internal object VpnTelemetryEventFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lcom/protonvpn/android/telemetry/TelemetryEvent;", "Z", "L"),
)

internal object CoreTelemetryUploadSchedulerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lme/proton/core/domain/entity/UserId;", "J"),
    filters = listOf(
        fieldAccess(definingClass = "Lme/proton/core/telemetry/data/worker/TelemetryWorker;", opcode = Opcode.SGET_OBJECT),
    ),
)

internal object ObservabilityUploadFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/util/List;", "L"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lme/proton/core/observability/data/api/request/MetricEvent;",
            opcode = Opcode.SGET_OBJECT,
        ),
    ),
)

internal object ObservabilityEnabledFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L"),
    filters = listOf(
        resourceField(ResourceType.BOOL, "core_feature_observability_enabled"),
        methodCall(definingClass = "Landroid/content/res/Resources;", name = "getBoolean"),
    ),
)

internal object PaymentsObservabilityWorkerFingerprint : Fingerprint(
    name = "start",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef -> classDef.fields.any { it.type == PAYMENT_FFI } },
)
