/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.opal.misc.signin

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/opal/LocalAccount;"
private const val LOCAL_USER_ID = "local-user"

private const val INSERT_LOCAL_USER = "INSERT OR IGNORE INTO `users` (`id`, `creationDateEpochMillis`) VALUES " +
    "('$LOCAL_USER_ID', CAST(strftime('%s', 'now') AS INTEGER) * 1000)"

private const val RESTORE_LOCAL_USER_TRIGGER = "CREATE TRIGGER IF NOT EXISTS `keep_local_user` AFTER DELETE ON `users` " +
    "WHEN old.`id` = '$LOCAL_USER_ID' BEGIN $INSERT_LOCAL_USER; END"

private const val MARK_ONBOARDING_COMPLETED =
    "INSERT OR REPLACE INTO `onboarding` (`id`, `step`) VALUES (0, 'Completed')"

@Suppress("unused")
val skipSignInPatch = bytecodePatch(
    name = "Skip sign-in",
    description = "Skips sign-in and runs Opal without an account. Features that need the Opal server are unavailable.",
) {
    compatibleWith(AppCompatibilities.OPAL)
    dependsOn(removePairipProtectionPatch)
    extendWith("extensions/extension.mpe")

    execute {
        FirebaseAuthStoreFingerprint.matchSingle().apply {
            val storeIndex = instructionMatches.last().index
            val storeRegister = method.getInstruction<OneRegisterInstruction>(storeIndex).registerA
            val idRegister = method.getFreeRegisterProvider(storeIndex + 1, 1, storeRegister).getFreeRegister4Bit()

            method.addInstructions(
                storeIndex + 1,
                """
                    const-string v$idRegister, "$LOCAL_USER_ID"
                    invoke-static { v$storeRegister, v$idRegister }, $EXTENSION_CLASS->seedFirebaseUser(Landroid/content/SharedPreferences;Ljava/lang/String;)V
                """,
            )
        }

        CreateAllTablesFingerprint.matchSingle().method.apply {
            listOf(
                ONBOARDING_TABLE_STATEMENT to listOf(MARK_ONBOARDING_COMPLETED),
                USERS_TABLE_STATEMENT to listOf(INSERT_LOCAL_USER, RESTORE_LOCAL_USER_TRIGGER),
            ).forEach { (createStatement, statements) ->
                val createIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.CONST_STRING &&
                        getReference<StringReference>()!!.string.startsWith(createStatement)
                }
                val statementRegister = getInstruction<OneRegisterInstruction>(createIndex).registerA
                val executeIndex = createIndex + 1
                val executeCall = getInstruction<ReferenceInstruction>(executeIndex)
                check(executeCall.opcode == Opcode.INVOKE_STATIC) { "$createStatement is not followed by a static call" }
                val connectionRegister = (executeCall as FiveRegisterInstruction).registerC

                val executeCallDescriptor = descriptor(executeCall.getReference<MethodReference>()!!)
                addInstructions(
                    executeIndex + 1,
                    statements.joinToString("\n") { statement ->
                        """
                            const-string v$statementRegister, "$statement"
                            invoke-static { v$connectionRegister, v$statementRegister }, $executeCallDescriptor
                        """
                    },
                )
            }
        }

        IsProfileIncompleteFingerprint.matchSingle().method.returnEarly(false)
    }
}
