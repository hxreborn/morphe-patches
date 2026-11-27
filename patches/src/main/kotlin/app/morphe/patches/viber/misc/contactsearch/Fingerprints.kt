/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.contactsearch

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal const val SQLITE_DATABASE_CLASS = "Lorg/sqlite/database/sqlite/SQLiteDatabase;"

internal const val SQLITE_CONFIGURATION_CLASS = "Lorg/sqlite/database/sqlite/SQLiteDatabaseConfiguration;"

internal object SQLiteDatabaseOpenInnerFingerprint : Fingerprint(
    definingClass = SQLITE_DATABASE_CLASS,
    name = "openInner",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = "Lorg/sqlite/database/sqlite/SQLiteConnectionPool;", name = "open"),
    ),
)

internal object ContactNameLikeClauseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Ljava/util/HashMap;", "Ljava/lang/String;", "Ljava/lang/String;"),
    filters = listOf(
        string(" LIKE \"%"),
        string("%\""),
    ),
)
