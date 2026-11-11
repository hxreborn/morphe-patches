/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.idlefish.misc.share

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.idlefish.assembledClasses
import app.morphe.patches.idlefish.body
import app.morphe.patches.idlefish.describe
import app.morphe.patches.idlefish.patchedClasses
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROUTE_CLASS = "Lcom/taobao/idlefish/router/interrupter/pre/NavInterrupterFunShare;"

private const val PARAMETER_COUNT_WITH_RECEIVER = 4

private const val CONTEXT_PARAMETER = 1

private const val URL_PARAMETER = 2

private const val REQUEST_PARAMETER = 3

private const val SHARE_URL_REGISTER = 0

private const val SHARE_INFO_KEY_REGISTER = 2

private val routeSmali = """
    .class public final $ROUTE_CLASS
    .super Ljava/lang/Object;

    .method public final checkInterrupt(Landroid/content/Context;Landroid/content/Intent;)Z
        .registers 3
        const/4 p1, 0x0
        return p1
    .end method

    .method public final checkInterrupt(Landroid/content/Context;Ljava/lang/String;Lcom/taobao/idlefish/protocol/nav/IRouteRequest;)Z
        .registers 9
        if-eqz p2, :not_a_share_route
        const-string v0, "fleamarket://FunShare?"
        invoke-virtual {p2, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
        move-result v0
        if-eqz v0, :not_a_share_route
        if-eqz p3, :not_a_share_route
        invoke-static {p2}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
        move-result-object p2
        const-string v0, "https://share.invalid/page"
        invoke-interface {p3}, Lcom/taobao/idlefish/protocol/nav/IRouteRequest;->getExtras()Landroid/os/Bundle;
        move-result-object v1
        const-string/jumbo v2, "shareInfo"
        if-eqz v1, :no_extras
        invoke-virtual {v1, v2}, Landroid/os/BaseBundle;->getString(Ljava/lang/String;)Ljava/lang/String;
        move-result-object v3
        invoke-static {v3}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
        move-result v4
        if-eqz v4, :share_info_read
        const-string v4, "__cstm_url_params__"
        invoke-virtual {v1, v4}, Landroid/os/BaseBundle;->get(Ljava/lang/String;)Ljava/lang/Object;
        move-result-object v1
        instance-of v4, v1, Ljava/util/HashMap;
        if-eqz v4, :new_map
        check-cast v1, Ljava/util/HashMap;
        goto :map_ready
        :new_map
        new-instance v1, Ljava/util/HashMap;
        invoke-direct {v1}, Ljava/util/HashMap;-><init>()V
        :map_ready
        invoke-virtual {v1, v2}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z
        move-result v4
        if-eqz v4, :share_info_read
        invoke-virtual {v1, v2}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;
        move-result-object v1
        instance-of v4, v1, Ljava/lang/String;
        if-eqz v4, :share_info_read
        move-object v3, v1
        check-cast v3, Ljava/lang/String;
        invoke-interface {p3, v2, v3}, Lcom/taobao/idlefish/protocol/nav/IRouteRequest;->putExtra(Ljava/lang/String;Ljava/lang/String;)Lcom/taobao/idlefish/protocol/nav/IRouteRequest;
        goto :share_info_checked
        :no_extras
        const/4 v3, 0x0
        :share_info_read
        :share_info_checked
        invoke-static {v3}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
        move-result v1
        if-eqz v1, :share_page_open
        invoke-virtual {p2, v2}, Landroid/net/Uri;->getQueryParameter(Ljava/lang/String;)Ljava/lang/String;
        move-result-object v1
        invoke-static {v1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
        move-result v3
        if-nez v3, :share_page_open
        invoke-interface {p3, v2, v1}, Lcom/taobao/idlefish/protocol/nav/IRouteRequest;->putExtra(Ljava/lang/String;Ljava/lang/String;)Lcom/taobao/idlefish/protocol/nav/IRouteRequest;
        :share_page_open
        invoke-static {v0}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
        move-result-object v0
        invoke-static {p2}, Lcom/taobao/android/remoteobject/util/UriUtil;->getQueryMap(Landroid/net/Uri;)Ljava/util/Map;
        move-result-object p2
        invoke-interface {p2, v2}, Ljava/util/Map;->remove(Ljava/lang/Object;)Ljava/lang/Object;
        invoke-static {v0, p2}, Lcom/taobao/android/remoteobject/util/UriUtil;->addQueryMap(Landroid/net/Uri;Ljava/util/Map;)Landroid/net/Uri;
        move-result-object p2
        invoke-virtual {p2}, Landroid/net/Uri;->toString()Ljava/lang/String;
        move-result-object p2
        invoke-interface {p3, p2}, Lcom/taobao/idlefish/protocol/nav/IRouteRequest;->setUrl(Ljava/lang/String;)Lcom/taobao/idlefish/protocol/nav/IRouteRequest;
        move-result-object p2
        invoke-interface {p2, p1}, Lcom/taobao/idlefish/protocol/nav/IRouteRequest;->open(Landroid/content/Context;)V
        const/4 p1, 0x1
        return p1
        :not_a_share_route
        const/4 p1, 0x0
        return p1
    .end method
""".trimIndent()

private val rewriteOnly = bytecodePatch { execute { rewriteShareRoute() } }

private val original by lazy { assembledClasses(listOf(routeSmali)) }

private val patched by lazy { patchedClasses(rewriteOnly, listOf(routeSmali)) }

internal class ShareRouteRewriteTest {
    private fun List<ClassDef>.routeMethod(parameterCount: Int): Method =
        single { it.type == ROUTE_CLASS }.methods.single { it.name == "checkInterrupt" && it.parameterTypes.size == parameterCount }

    private fun Method.parameterRegister(parameter: Int) =
        implementation!!.registerCount - PARAMETER_COUNT_WITH_RECEIVER + parameter

    private fun List<Instruction>.callsTo(name: String) = indices.filter { index ->
        val reference = (this[index] as? ReferenceInstruction)?.reference
        reference is MethodReference && reference.name == name
    }

    private fun List<Instruction>.rewrittenParse() = callsTo("parse").last()

    private fun List<Instruction>.injectedStart() = callsTo("putExtra").last { it < rewrittenParse() } + 1

    private fun List<Instruction>.branchTarget(index: Int): Int {
        val addresses = runningFold(0) { position, instruction -> position + instruction.codeUnits }
        return addresses.indexOf(addresses[index] + (this[index] as OffsetInstruction).codeOffset)
    }

    private fun List<Instruction>.conditionalBranches(range: IntRange) = range.filter {
        this[it] is OffsetInstruction && !this[it].opcode.name.startsWith("goto")
    }

    @Test
    fun `leaves the route check for intents unchanged`() {
        val intentCheck = { classes: List<ClassDef> -> classes.routeMethod(2).body().map { it.describe() } }

        assertEquals(intentCheck(original), intentCheck(patched))
    }

    @Test
    fun `sends both branches that skip the query read through the injected code`() {
        val instructions = patched.routeMethod(3).body()
        val rewrittenParse = instructions.rewrittenParse()
        val injectedStart = instructions.injectedStart()

        val branchesIntoInjectedCode = instructions.conditionalBranches(instructions.indices)
            .filter { instructions.branchTarget(it) == injectedStart }

        assertTrue(injectedStart < rewrittenParse)
        assertEquals(2, branchesIntoInjectedCode.size)
    }

    @Test
    fun `sends every case it does not handle to the original route rewrite`() {
        val instructions = patched.routeMethod(3).body()
        val rewrittenParse = instructions.rewrittenParse()
        val injectedStart = instructions.injectedStart()

        val fallbackLanding = rewrittenParse - 1

        val fallbacks = instructions.conditionalBranches(injectedStart until fallbackLanding)

        assertEquals(Opcode.NOP, instructions[fallbackLanding].opcode)
        assertTrue(fallbacks.isNotEmpty())
        assertTrue(fallbacks.all { instructions.branchTarget(it) == fallbackLanding })
    }

    @Test
    fun `keeps the original route rewrite after the injected code`() {
        val originalBody = original.routeMethod(3).body()
        val patchedBody = patched.routeMethod(3).body()

        val originalTail = originalBody.drop(originalBody.callsTo("parse").last()).map { it.describe() }
        val patchedTail = patchedBody.drop(patchedBody.callsTo("parse").last()).map { it.describe() }

        assertEquals(originalTail, patchedTail)
    }

    @Test
    fun `leaves the registers the original route rewrite reads untouched`() {
        val method = patched.routeMethod(3)
        val instructions = method.body()
        val rewrittenParse = instructions.rewrittenParse()
        val injectedStart = instructions.injectedStart()
        val readAfterwards = setOf(
            SHARE_URL_REGISTER,
            SHARE_INFO_KEY_REGISTER,
            method.parameterRegister(URL_PARAMETER),
            method.parameterRegister(REQUEST_PARAMETER),
        )

        val written = (injectedStart until rewrittenParse)
            .filter { instructions[it].opcode.setsRegister() }
            .map { (instructions[it] as OneRegisterInstruction).registerA }
            .toSet()

        assertTrue(written.none { it in readAfterwards }, "written $written, read afterwards $readAfterwards")
    }

    @Test
    fun `overwrites the context register only to return true`() {
        val method = patched.routeMethod(3)
        val instructions = method.body()
        val rewrittenParse = instructions.rewrittenParse()
        val injectedStart = instructions.injectedStart()
        val context = method.parameterRegister(CONTEXT_PARAMETER)

        val contextWrites = (injectedStart until rewrittenParse).filter {
            instructions[it].opcode.setsRegister() && (instructions[it] as OneRegisterInstruction).registerA == context
        }

        val write = contextWrites.single()
        assertEquals(Opcode.CONST_4, instructions[write].opcode)
        assertEquals(1, (instructions[write] as NarrowLiteralInstruction).narrowLiteral)
        assertEquals(Opcode.RETURN, instructions[write + 1].opcode)
        assertEquals(context, (instructions[write + 1] as OneRegisterInstruction).registerA)
    }

    @Test
    fun `builds the link in the extension before opening the share sheet`() {
        val instructions = patched.routeMethod(3).body()

        val linkBuilds = instructions.callsTo("publicItemUrl")
        val shareSheetOpens = instructions.callsTo("share")

        assertEquals(1, linkBuilds.size)
        assertEquals(1, shareSheetOpens.size)
        assertTrue(linkBuilds.single() < shareSheetOpens.single())
    }
}
