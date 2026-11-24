/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal object AdsEligibilityCheckFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf("Z"),
    filters = listOf(
        methodCall(name = "<init>", parameters = emptyList(), returnType = "V", opcode = Opcode.INVOKE_DIRECT),
        fieldAccess(
            definingClass = "Lcom/microsoft/office/outlook/feature/FeatureManager\$Feature;",
            name = "NATIVE_ADS_MAIN",
            opcode = Opcode.SGET_OBJECT,
        ),
        methodCall(
            definingClass = "Lcom/microsoft/office/outlook/feature/FeatureSnapshot;",
            name = "isFeatureOn",
            location = MatchAfterImmediately(),
        ),
    ),
)
