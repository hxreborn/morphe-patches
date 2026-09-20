/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atvtools.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object BillingHelperConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf("Ldev/vodik7/atvtools/App;", "Ljava/util/List;"),
    strings = listOf(
        "Please provide a valid listener for purchases updates.",
        "Pending purchases for one-time products must be supported.",
    ),
    filters = listOf(
        fieldAccess(type = "Ljava/util/List;", opcode = Opcode.IPUT_OBJECT),
        fieldAccess(type = "Lkotlinx/coroutines/flow/MutableStateFlow;", opcode = Opcode.IPUT_OBJECT),
        fieldAccess(type = "Lkotlinx/coroutines/flow/StateFlow;", opcode = Opcode.IPUT_OBJECT),
    ),
)

internal object PurchaseGetProductsFingerprint : Fingerprint(
    returnType = "Ljava/util/ArrayList;",
    strings = listOf("productIds", "productId"),
)

internal object PremiumActivityOnCreateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    custom = { _, classDef -> classDef.type == "Ldev/vodik7/atvtools/PremiumActivity;" },
)
