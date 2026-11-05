/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.habitify.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/habitify/Premium;"
private const val FIREBASE_AUTH_CLASS = "Lcom/google/firebase/auth/FirebaseAuth;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks lifetime Pro features and unlimited habits. Requires a signed-in account.",
) {
    compatibleWith(AppCompatibilities.HABITIFY)
    extendWith("extensions/extension.mpe")
    dependsOn(removePairipProtectionPatch)

    execute {
        val snapshot = SnapshotToStringFingerprint.matchSingle()
        val snapshotConstructor = snapshot.classDef.methods.single { it.name == "<init>" }
        val indexedType = snapshotConstructor.parameterTypes.last().toString()
        val indexedClass = classDefBy(indexedType)
        val parseNode = ParseNodeFingerprint.matchSingle().method
        val nodeType = parseNode.returnType
        val nodeValueGetter = classDefBy(nodeType).methods.single {
            it.name == "getValue" && it.parameterTypes.isEmpty()
        }
        val nodePriorityGetter = classDefBy(nodeType).methods.single {
            it.returnType == nodeType && it.parameterTypes.isEmpty()
        }
        val indexedNodeField = indexedClass.fields.single { it.type == nodeType }
        val indexedNodeFactory = indexedClass.methods.single {
            it.returnType == indexedType && it.parameterTypes == listOf(nodeType)
        }
        val userType = FirebaseUserMapFingerprint.matchSingle().method.parameterTypes.single().toString()
        val currentUserField = classDefBy(FIREBASE_AUTH_CLASS).fields.single { it.type == userType }
        val userIdMatch = FirebaseUserIdFingerprint.matchSingle()
        val (_, userIdCall) = userIdMatch.instructionMatches
        val userIdGetter = userIdMatch.method.getInstruction(userIdCall.index).getReference<MethodReference>()!!

        mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).apply {
            replaceStub(
                "currentUserId",
                1,
                """
                    invoke-static {}, $FIREBASE_AUTH_CLASS->getInstance()$FIREBASE_AUTH_CLASS
                    move-result-object v0
                    iget-object v0, v0, $currentUserField
                    if-eqz v0, :no_user
                    invoke-interface { v0 }, $userIdGetter
                    move-result-object v0
                    return-object v0
                    :no_user
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            replaceStub(
                "rewriteNode",
                7,
                """
                    invoke-virtual { p0 }, Ljava/lang/Object;->toString()Ljava/lang/String;
                    move-result-object v0
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->currentUserId()Ljava/lang/String;
                    move-result-object v1
                    invoke-static { v0, v1 }, $EXTENSION_CLASS_DESCRIPTOR->userRelativePath(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :original
                    check-cast p1, $indexedType
                    iget-object v2, p1, $indexedNodeField
                    invoke-interface { v2 }, $nodeValueGetter
                    move-result-object v3
                    invoke-static { v0, v3 }, $EXTENSION_CLASS_DESCRIPTOR->unlockValue(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v4
                    if-eq v3, v4, :original
                    invoke-interface { v2 }, $nodePriorityGetter
                    move-result-object v2
                    invoke-static { v4, v2 }, $parseNode
                    move-result-object v2
                    invoke-static { v2 }, $indexedNodeFactory
                    move-result-object p1
                    :original
                    return-object p1
                """,
            )
        }
        snapshotConstructor.addInstructions(
            1,
            """
                invoke-static { p1, p2 }, $EXTENSION_CLASS_DESCRIPTOR->rewriteNode(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object p2
                check-cast p2, $indexedType
            """,
        )
    }
}
