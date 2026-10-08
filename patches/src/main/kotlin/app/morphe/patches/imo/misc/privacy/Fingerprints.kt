/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

private const val FULL_AVATAR_KEY = "allow_show_full_icon_in_profile"

internal val avatarPrivacyKeys = listOf(FULL_AVATAR_KEY, "icon_visible_allowed")

internal val jsonFlagRead = methodCall(
    returnType = "Z",
    parameters = listOf("Lorg/json/JSONObject;", "Ljava/lang/String;", "Ljava/lang/Boolean;"),
)

private fun jsonFlagReadFilters(key: String) = listOf(
    string(key),
    jsonFlagRead,
    opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
)

internal object ProfileAvatarFlagsFingerprint : Fingerprint(
    filters = avatarPrivacyKeys.flatMap(::jsonFlagReadFilters),
    strings = listOf("privacy_profile"),
)

internal object VoiceRoomFullAvatarFingerprint : Fingerprint(
    definingClass = "Lcom/imo/android/imoim/clubhouse/profile/data/RevenueUserProfile;",
    returnType = "Ljava/lang/Boolean;",
    parameters = emptyList(),
    custom = { method, classDef ->
        val flagField = classDef.fields.firstOrNull { field ->
            field.annotations.any { annotation ->
                annotation.elements.any { (it.value as? StringEncodedValue)?.value == FULL_AVATAR_KEY }
            }
        }
        flagField != null && method.implementation?.instructions?.any { instruction ->
            ((instruction as? ReferenceInstruction)?.reference as? FieldReference)?.name == flagField.name
        } == true
    },
)

private const val CHAT_PROTECTION_PACKAGE = "Lcom/imo/android/imoim/im/business/protection/"

internal val protectionFlagRead = fieldAccess(type = "Z", opcode = Opcode.SGET_BOOLEAN)

internal object ProtectedMessageFingerprint : Fingerprint(
    definingClass = CHAT_PROTECTION_PACKAGE,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L"),
    filters = listOf(
        protectionFlagRead,
        fieldAccess(name = "RECEIVED", opcode = Opcode.SGET_OBJECT),
    ),
)

internal val copyProtectionCheck = methodCall(returnType = "Z", parameters = listOf("L"))

internal object CopyAvailabilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/LinkedHashSet;"),
    filters = listOf(
        copyProtectionCheck,
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        fieldAccess(name = "COPY", opcode = Opcode.SGET_OBJECT),
        fieldAccess(name = "COPY_DISABLE_BY_PRIVACY", opcode = Opcode.SGET_OBJECT),
    ),
)
