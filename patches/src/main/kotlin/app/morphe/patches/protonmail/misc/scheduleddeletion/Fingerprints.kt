/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.scheduleddeletion

import app.morphe.patcher.Fingerprint
import app.morphe.patches.all.misc.resources.resourceLiteral
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val MAIL_UNIFFI_CLASS = "Luniffi/mail_uniffi/Mail_uniffiKt;"
private const val MAILBOX = "Luniffi/mail_uniffi/Mailbox;"
internal const val ID = "Luniffi/mail_uniffi/Id;"
internal object NewMailboxFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            "$MAIL_UNIFFI_CLASS->newMailbox(Luniffi/mail_uniffi/MailUserSession;$ID)" +
                "Luniffi/mail_uniffi/NewMailboxResult;",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object ScrollMessagesForLabelFingerprint : Fingerprint(
    definingClass = MAIL_UNIFFI_CLASS,
    name = "scrollMessagesForLabel",
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        MAILBOX, ID, "Luniffi/mail_uniffi/MessageScrollerLiveQueryCallback;", "L",
    ),
)

internal object ScrollConversationsForLabelFingerprint : Fingerprint(
    definingClass = MAIL_UNIFFI_CLASS,
    name = "scrollConversationsForLabel",
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        MAILBOX, ID, "Luniffi/mail_uniffi/ConversationScrollerLiveQueryCallback;", "L",
    ),
)

internal object AutoDeleteBannerTextFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(
            ResourceType.STRING,
            "mailbox_action_auto_delete_on_clear_trash_spam_banner_text",
        ),
    ),
)

internal object IoDispatcherFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Dispatchers.IO"),
)
