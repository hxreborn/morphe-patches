/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hourlychime.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val CONTEXT_TYPE = "Landroid/content/Context;"
private const val BOXED_BOOLEAN_TYPE = "Ljava/lang/Boolean;"
private const val HOME_AD_URL = "https://ads.caynax.com/v1/HomeAds"
private const val AD_LABEL_ID_NAME = "jacw_mxeSmbvnrbSbpuauulx"
private const val AD_CONTAINER_ID_NAME = "jacw_mxeSmbvnrbMbno"

internal object LicenseStateFingerprint : Fingerprint(
    isStatic = true,
    returnType = BOXED_BOOLEAN_TYPE,
    parameters = listOf(CONTEXT_TYPE),
    filters = listOf(
        methodCall(parameters = listOf(CONTEXT_TYPE), returnType = "Z", opcode = Opcode.INVOKE_STATIC),
        fieldAccess(definingClass = BOXED_BOOLEAN_TYPE, name = "TRUE"),
        fieldAccess(definingClass = BOXED_BOOLEAN_TYPE, name = "FALSE"),
    ),
)

internal object PremiumDecisionFingerprint : Fingerprint(
    classFingerprint = LicenseStateFingerprint,
    isStatic = true,
    returnType = "Z",
    parameters = listOf(CONTEXT_TYPE),
    filters = listOf(
        methodCall(
            parameters = listOf(CONTEXT_TYPE),
            returnType = BOXED_BOOLEAN_TYPE,
            opcode = Opcode.INVOKE_STATIC,
        ),
    ),
)

internal object AdEligibilityFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(parameters = listOf(CONTEXT_TYPE), returnType = "I", opcode = Opcode.INVOKE_STATIC),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        methodCall(
            parameters = listOf("I"),
            returnType = "Z",
            opcode = Opcode.INVOKE_STATIC,
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object HomeAdUrlFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    filters = listOf(string(HOME_AD_URL)),
)

internal object HomeAdRequestFingerprint : Fingerprint(
    classFingerprint = HomeAdUrlFingerprint,
    filters = listOf(newInstance("Ljava/net/URL;")),
)

internal object HomeAdRenderFingerprint : Fingerprint(
    name = "setAdUnit",
    returnType = "V",
    filters = listOf(string(HOME_AD_URL)),
)

internal object DelayedHomeAdFingerprint : Fingerprint(
    name = "run",
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.ID, AD_CONTAINER_ID_NAME),
        resourceLiteral(ResourceType.ID, AD_LABEL_ID_NAME),
    ),
)
