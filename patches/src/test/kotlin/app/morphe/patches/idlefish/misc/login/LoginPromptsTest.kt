/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.idlefish.misc.login

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.idlefish.assembledClasses
import app.morphe.patches.idlefish.body
import app.morphe.patches.idlefish.describe
import app.morphe.patches.idlefish.patchedClasses
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import kotlin.test.Test
import kotlin.test.assertEquals

private const val LOGIN_MODULE = "Lcom/taobao/idlefish/login/LoginModule;"

private const val REMOTE_LOGIN = "Lcom/taobao/tao/remotebusiness/login/RemoteLogin;"

private const val REMOTE_LOGIN_REGISTERS = 8

private const val REMOTE_LOGIN_PARAMETERS = 4

private const val FIRST_PARAMETER_REGISTER = REMOTE_LOGIN_REGISTERS - REMOTE_LOGIN_PARAMETERS

private const val SECOND_PARAMETER_REGISTER = FIRST_PARAMETER_REGISTER + 1

private val loginModuleSmali = """
    .class public final $LOGIN_MODULE
    .super Ljava/lang/Object;

    .method public final needShowFirstLaunchLoginGuide(Landroid/content/Context;)Z
        .registers 4
        const-string v0, "first_launch"
        invoke-static {v0}, Lfixture/Preferences;->read(Ljava/lang/String;)Z
        move-result v0
        return v0
    .end method

    .method public final extraOpenLogin(Ljava/lang/String;Ljava/lang/String;)Z
        .registers 5
        invoke-static {}, Lfixture/Preferences;->extra()Z
        move-result v0
        return v0
    .end method

    .method public final isLoginPageOpened()Z
        .registers 2
        const/4 v0, 0x1
        return v0
    .end method
""".trimIndent()

private val remoteLoginSmali = """
    .class public $REMOTE_LOGIN
    .super Ljava/lang/Object;

    .method public static login(Lmtopsdk/mtop/intf/Mtop;Ljava/lang/String;ZLjava/lang/Object;)V
        .registers $REMOTE_LOGIN_REGISTERS
        invoke-static {p0, p1}, $LOGIN_HANDLER->instance(Lmtopsdk/mtop/intf/Mtop;Ljava/lang/String;)$LOGIN_HANDLER
        move-result-object v0
        const-string v1, "login"
        invoke-static {v1}, Lfixture/Log;->d(Ljava/lang/String;)V
        return-void
    .end method
""".trimIndent()

private val fixtureSmali = listOf(loginModuleSmali, remoteLoginSmali)

private val promptsOnly = bytecodePatch { execute { skipLoginPrompts() } }

private val original by lazy { assembledClasses(fixtureSmali) }

private val patched by lazy { patchedClasses(promptsOnly, fixtureSmali) }

internal class LoginPromptsTest {
    private fun List<ClassDef>.methodBody(type: String, name: String) =
        single { it.type == type }.methods.single { it.name == name }.body()

    private fun assertReturnsFalseFirst(name: String) {
        val body = patched.methodBody(LOGIN_MODULE, name)

        val value = body[0]
        val result = body[1]
        assertEquals(Opcode.CONST_4, value.opcode)
        assertEquals(0, (value as NarrowLiteralInstruction).narrowLiteral)
        assertEquals(Opcode.RETURN, result.opcode)
        assertEquals((value as OneRegisterInstruction).registerA, (result as OneRegisterInstruction).registerA)
    }

    @Test
    fun `answers no to the first launch login guide`() {
        assertReturnsFalseFirst("needShowFirstLaunchLoginGuide")
    }

    @Test
    fun `answers no to the extra login prompt`() {
        assertReturnsFalseFirst("extraOpenLogin")
    }

    @Test
    fun `leaves the other login module methods unchanged`() {
        val unchanged = { classes: List<ClassDef> ->
            classes.methodBody(LOGIN_MODULE, "isLoginPageOpened").map { it.describe() }
        }

        assertEquals(unchanged(original), unchanged(patched))
    }

    @Test
    fun `cancels the login a request waits for and returns`() {
        val body = patched.methodBody(REMOTE_LOGIN, "login")

        val (instance, instanceResult, cancel, finish) = body
        val handler = ((instance as ReferenceInstruction).reference as MethodReference)
        assertEquals(Opcode.INVOKE_STATIC, instance.opcode)
        assertEquals(LOGIN_HANDLER, handler.definingClass)
        assertEquals("instance", handler.name)
        assertEquals(FIRST_PARAMETER_REGISTER, (instance as FiveRegisterInstruction).registerC)
        assertEquals(SECOND_PARAMETER_REGISTER, instance.registerD)
        assertEquals(Opcode.MOVE_RESULT_OBJECT, instanceResult.opcode)
        val cancelCall = ((cancel as ReferenceInstruction).reference as MethodReference)
        assertEquals(LOGIN_HANDLER, cancelCall.definingClass)
        assertEquals("onLoginCancel", cancelCall.name)
        assertEquals((instanceResult as OneRegisterInstruction).registerA, (cancel as FiveRegisterInstruction).registerC)
        assertEquals(Opcode.RETURN_VOID, finish.opcode)
    }

    @Test
    fun `keeps the original login body after the cancel`() {
        val originalBody = original.methodBody(REMOTE_LOGIN, "login").map { it.describe() }
        val patchedBody = patched.methodBody(REMOTE_LOGIN, "login").map { it.describe() }

        assertEquals(originalBody, patchedBody.drop(patchedBody.size - originalBody.size))
    }
}
