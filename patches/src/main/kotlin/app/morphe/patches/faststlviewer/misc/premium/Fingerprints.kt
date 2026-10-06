/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.faststlviewer.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object ProductButtonTextFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf(" (Pro)"),
    filters = listOf(fieldAccess(type = "Z", opcode = Opcode.IGET_BOOLEAN)),
)

internal object PurchasesUpdatedFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("pro_purchase_completed"),
    filters = listOf(fieldAccess(type = "Z", opcode = Opcode.IPUT_BOOLEAN)),
)

internal object RequestAdConsentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = emptyList(),
    strings = listOf(" Instrumentation test detected, skipping consent."),
)

internal fun booleanFieldReadFingerprint(field: FieldReference) = Fingerprint(
    filters = listOf(fieldAccess(field, Opcode.IGET_BOOLEAN)),
)
