/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castle.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.secshell.editSecShellPayload
import app.morphe.patches.shared.misc.secshell.secShellPayloadPatch
import app.morphe.util.findInstructionIndicesReversed
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val USER_CLASS = "Lcom/gxgx/base/bean/User;"
private const val VIDEO_CLASS = "Lcom/gxgx/daqiandy/bean/VideoBean;"
private const val RECHARGE_COUNTDOWN_COLOR = "#F0D953"
private const val DOWNLOAD_PLAYLIST_TASK_CLASS = "Lcom/arialyy/aria/m3u8/M3U8InfoTask;"
private const val PLAYLIST_HEADER_ERROR = "Input does not start with the #EXTM3U header."
private const val EXTENSION_PACKAGE = "Lapp/hxreborn/extension/castle/"
private const val PREVIEW_PLAYLIST_CLASS = "${EXTENSION_PACKAGE}PreviewPlaylist;"
private const val FULL_HD_PLAYLIST_CLASS = "${EXTENSION_PACKAGE}FullHdPlaylist;"
private const val VIDEO_BODY_CLASS = "Lcom/gxgx/daqiandy/requestBody/VideoBody;"
private const val DEFAULT_CONSTRUCTOR_MARKER = "Lkotlin/jvm/internal/DefaultConstructorMarker;"
private const val DEVICE_GUID_CLASS = "${EXTENSION_PACKAGE}DeviceGuid;"
private const val GUID_GETTER_LOG = "getServiceOauthDeviceId Guid="
private const val REQUEST_SIGNING_LOG = "differenceTime==="
private const val RESPONSE_DECRYPT_LOG = "responseData==="
private const val STRING_CLASS = "Ljava/lang/String;"
private const val MEDIA_TYPE_CLASS = "Lokhttp3/MediaType;"
private const val RESPONSE_BODY_COMPANION_CLASS = "Lokhttp3/ResponseBody${'$'}Companion;"
private const val REQUEST_CLASS = "Lokhttp3/Request;"
private const val RESPONSE_CLASS = "Lokhttp3/Response;"
private const val REAL_CHAIN_CLASS = "Lokhttp3/internal/http/RealInterceptorChain;"
private const val CHAIN_CLASS = "Lokhttp3/Interceptor${'$'}Chain;"

private const val PREMIUM_VIP_INFO = """
    new-instance v0, Lcom/gxgx/base/bean/VipInfo;
    const-string v1, "4102358400000"
    const-string v2, "Premium"
    const/4 v3, 0x2
    invoke-static { v3 }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v3
    const/4 v4, 0x0
    invoke-static { v4 }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v4
    invoke-direct { v0, v1, v2, v3, v4 }, Lcom/gxgx/base/bean/VipInfo;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;)V
    return-object v0
"""

private const val NOT_PREVIEW = """
    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
    return-object v0
"""

private const val USE_SWITCHED_GUID = """
    invoke-static { }, $DEVICE_GUID_CLASS->switchedGuid()Ljava/lang/String;
    move-result-object v0
    if-eqz v0, :guid_not_switched
    return-object v0
    :guid_not_switched
    nop
"""

private const val REQUEST_HD_RESOLUTION = """
    invoke-static/range { p4 .. p4 }, $FULL_HD_PLAYLIST_CLASS->resolution(Ljava/lang/Integer;)Ljava/lang/Integer;
    move-result-object p4
"""

private const val EXPAND_PREVIEW_PLAYLIST = """
    invoke-static { p1, p2 }, $PREVIEW_PLAYLIST_CLASS->expand(Landroid/net/Uri;Ljava/io/InputStream;)Ljava/io/InputStream;
    move-result-object p2
"""

@Suppress("unused")
val unlockPremiumPatch = rawResourcePatch(
    name = "Unlock premium",
    description = "Unlocks member-only titles, shows the account as Premium and removes the premium bundle offer. " +
        "Requires a signed-in account.",
) {
    compatibleWith(AppCompatibilities.CASTLE)

    dependsOn(secShellPayloadPatch)

    execute {
        editSecShellPayload {
            replaceBody(USER_CLASS, "getVipInfos", registers = 6, smali = PREMIUM_VIP_INFO)
            replaceBody(VIDEO_CLASS, "isPreview", registers = 2, smali = NOT_PREVIEW)
            methodsUsingString(GUID_GETTER_LOG)
                .single { it.parameterTypes.isEmpty() && it.returnType == "Ljava/lang/String;" }
                .addInstructionsWithLabels(0, USE_SWITCHED_GUID)
            methodUsingString(RESPONSE_DECRYPT_LOG, firstParameter = CHAIN_CLASS).apply {
                val bodyCreation = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.let {
                        it.definingClass == RESPONSE_BODY_COMPANION_CLASS &&
                            it.parameterTypes.map { type -> type.toString() }.toSet() == setOf(MEDIA_TYPE_CLASS, STRING_CLASS)
                    } == true
                }
                val creation = getInstruction<FiveRegisterInstruction>(bodyCreation)
                val argumentRegisters = listOf(creation.registerC, creation.registerD, creation.registerE, creation.registerF)
                val receivers = if (creation.opcode == Opcode.INVOKE_STATIC) 0 else 1
                val parameters = (creation as ReferenceInstruction).reference.let { (it as MethodReference).parameterTypes }
                val json = argumentRegisters[receivers + parameters.indexOfFirst { it == STRING_CLASS }]
                addInstruction(bodyCreation, "invoke-static { v$json }, $DEVICE_GUID_CLASS->observeResponse($STRING_CLASS)V")
            }
            methodUsingString(REQUEST_SIGNING_LOG, firstParameter = CHAIN_CLASS).apply {
                val proceedCalls = findInstructionIndicesReversed {
                    getReference<MethodReference>()?.let {
                        (it.definingClass == REAL_CHAIN_CLASS || it.definingClass == CHAIN_CLASS) &&
                            it.parameterTypes == listOf(REQUEST_CLASS) && it.returnType == RESPONSE_CLASS
                    } == true
                }
                check(proceedCalls.isNotEmpty()) { "No proceed call in the request signer" }
                proceedCalls.forEach { index ->
                    val call = getInstruction<FiveRegisterInstruction>(index)
                    val response = getInstruction<OneRegisterInstruction>(index + 1).registerA
                    check(getInstruction(index + 1).opcode == Opcode.MOVE_RESULT_OBJECT)
                    val registers = getFreeRegisterProvider(index, 2, response, call.registerC, call.registerD)
                    val chain = registers.getFreeRegister4Bit()
                    val signer = registers.getFreeRegister4Bit()
                    addInstructionsWithLabels(
                        index + 2,
                        """
                            invoke-static { }, $DEVICE_GUID_CLASS->shouldRetry()Z
                            move-result v$signer
                            if-eqz v$signer, :signed_once_$index
                            new-instance v$signer, $definingClass
                            invoke-direct { v$signer }, $definingClass-><init>()V
                            invoke-virtual { v$signer, v$chain }, $definingClass->$name($CHAIN_CLASS)$RESPONSE_CLASS
                            move-result-object v$response
                            :signed_once_$index
                            nop
                        """,
                    )
                    addInstruction(index, "move-object v$chain, v${call.registerC}")
                }
            }
            val rechargeSheetClass = methodUsingString(RECHARGE_COUNTDOWN_COLOR).definingClass
            methods(rechargeSheetClass).single {
                it.returnType == "V" && it.parameterTypes.size == 2 && it.parameterTypes[1] == "Ljava/lang/String;"
            }.returnEarly()
            methods(VIDEO_BODY_CLASS, "<init>")
                .filter { DEFAULT_CONSTRUCTOR_MARKER !in it.parameterTypes && it.parameterTypes.getOrNull(3) == "Ljava/lang/Integer;" }
                .maxBy { it.parameterTypes.size }
                .addInstructions(0, REQUEST_HD_RESOLUTION)
            val playlistParser = prependToMethodUsingString(
                PLAYLIST_HEADER_ERROR,
                firstParameter = "Landroid/net/Uri;",
                smali = EXPAND_PREVIEW_PLAYLIST,
            )
            addExtensionPackage(EXTENSION_PACKAGE, besideClass = playlistParser)

            method(DOWNLOAD_PLAYLIST_TASK_CLASS, "handleConnect").apply {
                val streamIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC && getReference<MethodReference>()?.name == "convertInputStream"
                } + 1
                val stream = getInstruction<OneRegisterInstruction>(streamIndex).registerA
                addInstructions(
                    streamIndex + 1,
                    """
                        invoke-static { p1, v$stream }, $PREVIEW_PLAYLIST_CLASS->expand(Ljava/lang/String;Ljava/io/InputStream;)Ljava/io/InputStream;
                        move-result-object v$stream
                    """,
                )
            }
        }
    }
}
