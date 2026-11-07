/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.mediaprivacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ScreenshotTakenFingerprint : Fingerprint(
    name = "onConversationScreenShotTaken",
    returnType = "V",
    filters = listOf(methodCall(definingClass = "Lcom/viber/jni/controller/PhoneController;", name = "generateSequence")),
)

internal object SecretEventSendFingerprint : Fingerprint(
    parameters = listOf("I", "J", "I", "I", "Ljava/lang/String;"),
    returnType = "V",
    filters = listOf(newInstance("Lcom/viber/jni/im2/CSecretChatSendEventMsg;")),
)

internal object ReadReceiptEnqueueFingerprint : Fingerprint(
    filters = listOf(
        string("conversation_id=? AND unread>0 AND (token<=? OR (CAST(? AS INTEGER)>0 AND message_global_id<=?)) AND [messages].[comment_thread_id] = 0"),
        methodCall(
            parameters = listOf("Ljava/lang/String;", "Landroid/content/ContentValues;", "Ljava/lang/String;", "[Ljava/lang/String;"),
            returnType = "I",
        ),
    ),
)

internal object ReadTimestampWriteFingerprint : Fingerprint(
    parameters = listOf("J", "J", "J", "Z"),
    returnType = "V",
    filters = listOf(
        string("conversation_id=? AND send_type=? AND read_message_time=0 AND token<>0 AND token<=? AND messages.extra_flags & (1 << 58) = 0 AND [messages].[comment_thread_id] = 0"),
        methodCall(parameters = listOf("Ljava/lang/String;", "Landroid/content/ContentValues;", "Ljava/lang/String;", "[Ljava/lang/String;"), returnType = "I"),
    ),
)

internal fun inferredReadReceiptFingerprint(writer: MethodReference) = Fingerprint(
    filters = listOf(
        methodCall(definingClass = "Lcom/viber/voip/feature/model/main/message/MessageEntity;", name = "getTimebombInSec"),
        methodCall(definingClass = writer.definingClass, name = writer.name, parameters = writer.parameterTypes.map(CharSequence::toString), returnType = "V"),
    ),
)
