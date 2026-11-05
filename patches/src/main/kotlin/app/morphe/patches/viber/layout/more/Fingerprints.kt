/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.more

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object BuildMoreItemsFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf(),
            returnType = "L",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/util/List;", name = "add"),
        opcode(Opcode.ADD_INT_LIT8, location = MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/util/Collections;", name = "sort"),
    ),
)

internal fun itemConstructorFingerprint(itemType: String) = Fingerprint(
    definingClass = itemType,
    name = "<init>",
    parameters = listOf("L"),
)

internal fun itemBuilderConstructorFingerprint(builderType: String) = Fingerprint(
    definingClass = builderType,
    name = "<init>",
    parameters = listOf("Landroid/content/Context;", "I", "I"),
    filters = listOf(
        fieldAccess(definingClass = "this", type = "I", opcode = Opcode.IPUT),
        fieldAccess(definingClass = "this", type = "I", opcode = Opcode.IPUT, location = MatchAfterImmediately()),
    ),
)

internal fun itemIdCopyFingerprint(itemType: String, builderIdField: FieldReference) = Fingerprint(
    definingClass = itemType,
    name = "<init>",
    filters = listOf(
        fieldAccess(reference = builderIdField, opcode = Opcode.IGET),
        fieldAccess(definingClass = "this", type = "I", opcode = Opcode.IPUT, location = MatchAfterImmediately()),
    ),
)

internal fun moreViewAttachedFingerprint(itemsMethod: MethodReference) = Fingerprint(
    name = "onViewAttached",
    returnType = "V",
    parameters = listOf("Lcom/viber/voip/user/more/MoreState;"),
    filters = listOf(
        fieldAccess(name = "mView", opcode = Opcode.IGET_OBJECT),
        fieldAccess(type = itemsMethod.definingClass, opcode = Opcode.IGET_OBJECT),
        methodCall(reference = itemsMethod),
        methodCall(
            definingClass = "Lcom/viber/voip/user/more/MoreView;",
            name = "setItems",
            parameters = listOf("Ljava/util/List;"),
            returnType = "V",
        ),
    ),
)

internal fun moreResumeFingerprint(presenterType: String) = Fingerprint(
    definingClass = presenterType,
    name = "onResume",
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(methodCall(opcode = Opcode.INVOKE_SUPER, name = "onResume")),
)

internal object MainMenuItemVisibilityFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("Landroid/view/MenuItem;", "Z"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/view/MenuItem;",
            name = "setVisible",
            parameters = listOf("Z"),
            returnType = "Landroid/view/MenuItem;",
        ),
    ),
)
