/*
 * Copyright (C) 2026 kiraio-moe
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from kiraio-moe/Lain-Patches:
 * https://github.com/kiraio-moe/Lain-Patches/commit/6e972994f9631e46869029eb43e96f2cc206dfac
 * Commit 6e972994f9631e46869029eb43e96f2cc206dfac (2026-07-09),
 * patches/src/main/kotlin/kiraio/lain/ilovepdf/premium/Fingerprints.kt
 */
package app.morphe.patches.ilovepdf.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

internal object PremiumBooleanFingerprint : Fingerprint(
    filters = listOf(fieldAccess(name = "premium", type = "Z", opcode = Opcode.IPUT_BOOLEAN)),
    custom = { _, classDef ->
        classDef.type in setOf(
            "Lcom/android/data/api/models/UserApiModel;",
            "Lcom/android/ilovepdf/database/models/UserDBModel;",
        )
    },
)

internal object PremiumBoxedBooleanFingerprint : Fingerprint(
    filters = listOf(fieldAccess(name = "premium", type = "Ljava/lang/Boolean;", opcode = Opcode.IPUT_OBJECT)),
    custom = { _, classDef ->
        classDef.type in setOf(
            "Lcom/android/data/models/UserDataModel;",
            "Lcom/android/domain/models/UserDomainModel;",
            "Lcom/android/ilovepdf/presentation/models/UserModel;",
        )
    },
)
