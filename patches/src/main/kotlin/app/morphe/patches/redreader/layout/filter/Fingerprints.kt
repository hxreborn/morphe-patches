/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.redreader.layout.filter

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object PostListingFingerprint : Fingerprint(
    strings = listOf("Inline previews: ", "Precaching images: "),
    returnType = "V",
    filters = listOf(
        fieldAccess(name = "mPostIds", type = "Ljava/util/HashSet;", opcode = Opcode.IGET_OBJECT),
        fieldAccess(name = "id", type = "Ljava/lang/String;", opcode = Opcode.IGET_OBJECT,
            location = MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/util/HashSet;", name = "add", returnType = "Z",
            location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)

internal object ApplicationCreateFingerprint : Fingerprint(
    strings = listOf("Application created."),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(methodCall(definingClass = "Landroid/app/Application;", name = "onCreate",
        opcode = Opcode.INVOKE_SUPER)),
)

internal object PostToStringFingerprint : Fingerprint(
    strings = listOf("RedditPost(id="),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)

internal object EncodedStringToStringFingerprint : Fingerprint(
    strings = listOf("UrlEncodedString(decoded="),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)
