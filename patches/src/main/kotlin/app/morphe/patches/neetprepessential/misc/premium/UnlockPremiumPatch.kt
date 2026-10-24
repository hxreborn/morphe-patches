/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.neetprepessential.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

private const val USER_COURSES_PATH = "$.data.me.userCourses.edges"
private const val PHONE_AND_EMAIL_PATH = "$.data.me['phone', 'email']"
private const val SNAPSHOT_STRING_TERMINATOR_BIT = 0x80

private val USER_COURSES_PATH_LENGTH_BYTE = ((USER_COURSES_PATH.length shl 1) or SNAPSHOT_STRING_TERMINATOR_BIT).toByte()

internal val USER_COURSES_PATH_ENTRY =
    byteArrayOf(USER_COURSES_PATH_LENGTH_BYTE) + USER_COURSES_PATH.toByteArray(Charsets.US_ASCII)

internal val USER_COURSES_PATH_ENTRY_MASK = ByteArray(USER_COURSES_PATH_ENTRY.size) { 0xff.toByte() }

internal val COURSE_STATUS_STORE =
    ("22f05ff8427c4cd3f00300aae00302aae20310aa1e881291be7a7ef8c0033fd61f00166b01000054e00316aa00000014" +
        "810780d20000003601f05ff8217c4cd3e00100f9e00301aa640340f9712b85d2").hexToByteArray()

internal val COURSE_STATUS_STORE_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff1f0000ffffffffff000000fc" +
        "ffffffff1f00f8ffffffffffffffffffffffffffffffffffff03c0ffffffffff").hexToByteArray()

internal val LOAD_PAID_STATUS_AND_JUMP_TO_STORE = "60e7409100c841f9140000141f2003d5".hexToByteArray()

private const val COURSE_STATUS_STORE_OFFSET = 32

@Suppress("unused")
val unlockPremiumPatch = resourcePatch(
    name = "Unlock premium",
    description = "Unlocks all chapters, practice sets, flashcards, hardest questions and mock tests.",
) {
    compatibleWith(AppCompatibilities.NEETPREP_ESSENTIAL)
    availability(requireArm64)

    execute {
        require(PHONE_AND_EMAIL_PATH.length == USER_COURSES_PATH.length)

        val library = dartAotLibrary()

        val coursePathReplaced = library.replaceMasked(
            USER_COURSES_PATH_ENTRY,
            USER_COURSES_PATH_ENTRY_MASK,
            mapOf(1 to PHONE_AND_EMAIL_PATH.toByteArray(Charsets.US_ASCII)),
        )
        if (!coursePathReplaced) throw PatchException("Could not find the course list path in $DART_AOT_LIBRARY")

        val courseStatusReplaced = library.replaceMasked(
            COURSE_STATUS_STORE,
            COURSE_STATUS_STORE_MASK,
            mapOf(COURSE_STATUS_STORE_OFFSET to LOAD_PAID_STATUS_AND_JUMP_TO_STORE),
        )
        if (!courseStatusReplaced) throw PatchException("Could not find the course status assignment in $DART_AOT_LIBRARY")
    }
}
