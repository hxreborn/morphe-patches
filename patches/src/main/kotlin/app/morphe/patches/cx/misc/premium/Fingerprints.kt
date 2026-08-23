/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cx.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

internal object LicenseHolderFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        fieldAccess(
            "Lcom/alphainventor/filemanager/license/datatypes/ProductCatalogImpl;" +
                "->CATEGORY_PREMIUM_BASIC:Ljava/lang/String;",
            Opcode.SGET_OBJECT,
        ),
    ),
)
