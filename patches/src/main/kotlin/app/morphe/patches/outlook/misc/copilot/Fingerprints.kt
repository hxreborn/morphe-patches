/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.copilot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val GEN_AI_UI_PACKAGE = "Lcom/microsoft/office/outlook/genai/ui"
private const val PLATFORM_SDK_PACKAGE = "Lcom/microsoft/office/outlook/platform/sdk"
internal const val FEATURE_REQUIREMENT_FACTORY_CLASS = "$PLATFORM_SDK_PACKAGE/FeatureRequirementFactory;"
private const val FEATURE_REQUIREMENT_CLASS = "$PLATFORM_SDK_PACKAGE/FeatureRequirement;"
private const val GEN_AI_MANAGER_CLASS = "Lcom/microsoft/office/outlook/genai/manager/OlmGenAIManager;"
private const val INTERFACES_PACKAGE = "Lcom/microsoft/office/outlook/olmcore/managers/interfaces"
private const val ACCOUNT_ID_CLASS = "Lcom/microsoft/office/outlook/olmcore/model/interfaces/AccountId;"
private const val COPILOT_TYPE_CLASS = "$INTERFACES_PACKAGE/CopilotType;"
private const val COPILOT_CAPABILITY_CLASS = "$INTERFACES_PACKAGE/CopilotCapability;"
private const val COPILOT_SUPPORTED_STATE_CLASS = "$INTERFACES_PACKAGE/CopilotSupportedState;"

internal object CopilotTypeSupportedFingerprint : Fingerprint(
    definingClass = GEN_AI_MANAGER_CLASS,
    name = "copilotTypeSupported",
    returnType = COPILOT_SUPPORTED_STATE_CLASS,
    parameters = listOf(ACCOUNT_ID_CLASS, COPILOT_TYPE_CLASS, "Z"),
)

internal object CopilotTypeEnabledFingerprint : Fingerprint(
    definingClass = GEN_AI_MANAGER_CLASS,
    name = "copilotTypeEnabled",
    returnType = "Z",
    parameters = listOf(ACCOUNT_ID_CLASS, COPILOT_TYPE_CLASS, "Z"),
)

internal object CopilotCapabilitySupportedFingerprint : Fingerprint(
    definingClass = GEN_AI_MANAGER_CLASS,
    name = "copilotCapabilitySupported",
    returnType = COPILOT_SUPPORTED_STATE_CLASS,
    parameters = listOf(ACCOUNT_ID_CLASS, COPILOT_CAPABILITY_CLASS),
)

internal object CopilotCapabilityEnabledFingerprint : Fingerprint(
    definingClass = GEN_AI_MANAGER_CLASS,
    name = "copilotCapabilityEnabled",
    returnType = "Z",
    parameters = listOf(ACCOUNT_ID_CLASS, COPILOT_CAPABILITY_CLASS),
)

internal object FullCopilotAccountAvailableFingerprint : Fingerprint(
    definingClass = GEN_AI_MANAGER_CLASS,
    name = "isFullCopilotAccountAvailable",
    returnType = "Z",
    parameters = emptyList(),
)

private fun copilotPartnerRequirementFingerprint(partnerConfigClass: String) = Fingerprint(
    definingClass = partnerConfigClass,
    name = "getFeatureRequirements",
    returnType = FEATURE_REQUIREMENT_CLASS,
    filters = listOf(
        methodCall(definingClass = FEATURE_REQUIREMENT_FACTORY_CLASS, name = "alwaysOn"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal val scheduleWithCopilotPartnerFingerprint =
    copilotPartnerRequirementFingerprint("$GEN_AI_UI_PACKAGE/schedulefromemail/ScheduleFromEmailPartnerConfig;")

internal val forwardToCopilotPartnerFingerprint =
    copilotPartnerRequirementFingerprint("$GEN_AI_UI_PACKAGE/delegateViaEmail/DelegateViaEmailPartnerConfig;")
