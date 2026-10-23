/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.pairip

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchPayload
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SPLASH_CALLBACKS = "Lcom/vpn/lib/feature/splash/d;"
private const val SPLASH_FUNCTIONS = "Lcom/vpn/lib/feature/splash/b;"
private const val NAVIGATION_CLICKS = "Lcom/vpn/lib/feature/naviagation/e;"
private const val NAVIGATION_CALLBACKS = "Lcom/vpn/lib/feature/naviagation/c;"
private const val NAVIGATION_FOCUS = "Lcom/vpn/lib/feature/naviagation/d;"
private const val NAVIGATION_RUNNABLES = "Lcom/vpn/lib/feature/naviagation/a;"
private const val RX_ERROR_HANDLERS = "Lcom/google/gson/internal/c;"
private const val NAVIGATION_PRESENTER = "Lcom/vpn/lib/feature/naviagation/NavigationPresenter;"
private const val EMERGENCY_MODE = "Lcom/vpn/lib/troubleshooting/EmergencyMode;"
private const val DRAWER_LAYOUT = "Landroidx/drawerlayout/widget/DrawerLayout;"

private const val ON_CLICK = "onClick(Landroid/view/View;)V"
private const val ON_FOCUS_CHANGE = "onFocusChange(Landroid/view/View;Z)V"
private const val ON_ACTIVITY_RESULT = "onActivityResult(Ljava/lang/Object;)V"
private const val RUN = "run()V"

private val GOTO_OPCODES = setOf(Opcode.GOTO, Opcode.GOTO_16, Opcode.GOTO_32)

private class MergedLambdaBinding(val type: String, val method: String, val selector: Int, val expectedReference: String)

private val MERGED_LAMBDA_BINDINGS = listOf(
    MergedLambdaBinding(RX_ERROR_HANDLERS, "accept(Ljava/lang/Object;)V", 4, "$APP->l(Ljava/lang/Throwable;)V"),
    MergedLambdaBinding(RX_ERROR_HANDLERS, "accept(Ljava/lang/Object;)V", 5, "$SPLASH->J:I"),
    MergedLambdaBinding(
        "Lcom/google/firebase/firestore/pipeline/evaluation/d;",
        "invoke(Ljava/lang/Object;)Ljava/lang/Object;",
        4,
        "Landroid/app/PendingIntent;->getActivity(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;",
    ),
    MergedLambdaBinding(
        SPLASH_CALLBACKS,
        "a(Ljava/lang/String;Ljava/lang/Throwable;)V",
        1,
        "$SPLASH->j1(${PREPARING_STEPS}Ljava/lang/Throwable;)V",
    ),
    MergedLambdaBinding(
        SPLASH_CALLBACKS,
        "onFailure(Ljava/lang/Throwable;)V",
        2,
        "$SPLASH->j1(${PREPARING_STEPS}Ljava/lang/Throwable;)V",
    ),
    MergedLambdaBinding(
        SPLASH_CALLBACKS,
        "f(Landroid/view/View;Landroidx/core/view/WindowInsetsCompat;)Landroidx/core/view/WindowInsetsCompat;",
        0xd,
        "Landroid/view/View;->setPadding(IIII)V",
    ),
    MergedLambdaBinding(SPLASH_CALLBACKS, "c(Lcom/vpn/lib/troubleshooting/EmergencyMode\$State;)V", 4, "$SPLASH->d1()V"),
    MergedLambdaBinding(SPLASH_CALLBACKS, ON_ACTIVITY_RESULT, 5, "$EMERGENCY_MODE->d(Z)V"),
    MergedLambdaBinding(SPLASH_FUNCTIONS, "invoke()Ljava/lang/Object;", 0, "$SPLASH->p1(I)V"),
    MergedLambdaBinding(
        SPLASH_FUNCTIONS,
        "invoke()Ljava/lang/Object;",
        1,
        "Lcom/services/RemoteConfigHelper;->a(Landroid/app/Activity;)V",
    ),
    MergedLambdaBinding("Lcom/vpn/lib/feature/splash/k;", RUN, 2, "$SPLASH->k1()V"),
    MergedLambdaBinding(NAVIGATION_CLICKS, ON_CLICK, 0, "$DRAWER_LAYOUT->q()V"),
    MergedLambdaBinding(NAVIGATION_CLICKS, ON_CLICK, 2, "$NAVIGATION_PRESENTER->e()V"),
    MergedLambdaBinding(NAVIGATION_CLICKS, ON_CLICK, 3, "$NAVIGATION_PRESENTER->j()V"),
    MergedLambdaBinding(NAVIGATION_CLICKS, ON_CLICK, 5, "$NAVIGATION->f()V"),
    MergedLambdaBinding(NAVIGATION_CLICKS, ON_CLICK, 7, "$NAVIGATION_PRESENTER->e()V"),
    MergedLambdaBinding(
        NAVIGATION_CALLBACKS,
        "onMenuItemClick(Landroid/view/MenuItem;)Z",
        4,
        "$NAVIGATION_PRESENTER->e()V",
    ),
    MergedLambdaBinding(NAVIGATION_CALLBACKS, ON_ACTIVITY_RESULT, 5, "$EMERGENCY_MODE->d(Z)V"),
    MergedLambdaBinding(NAVIGATION_FOCUS, ON_FOCUS_CHANGE, 0, "Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z"),
    MergedLambdaBinding(NAVIGATION_FOCUS, ON_FOCUS_CHANGE, 1, "Landroid/view/View;->callOnClick()Z"),
    MergedLambdaBinding(NAVIGATION_FOCUS, ON_FOCUS_CHANGE, 2, "$DRAWER_LAYOUT->c()V"),
    MergedLambdaBinding(NAVIGATION_RUNNABLES, RUN, 4, "Lcom/vpn/lib/feature/dashboard/DashboardFragment;->f1()V"),
    MergedLambdaBinding(NAVIGATION_RUNNABLES, RUN, 8, "$SETTINGS->x(Z)V"),
)

private fun Method.referencesForSelector(selector: Int): List<String> {
    val instructions = implementation!!.instructions.toList()
    val addresses = instructions.runningFold(0) { address, instruction -> address + instruction.codeUnits }
    fun indexAt(address: Int) = addresses.indexOf(address)

    val switchIndex = instructions.indexOfFirst {
        it.opcode == Opcode.PACKED_SWITCH || it.opcode == Opcode.SPARSE_SWITCH
    }
    var index = if (switchIndex < 0) {
        0
    } else {
        val switchAddress = addresses[switchIndex]
        val payloadOffset = (instructions[switchIndex] as OffsetInstruction).codeOffset
        val payload = instructions[indexAt(switchAddress + payloadOffset)] as SwitchPayload
        payload.switchElements.firstOrNull { it.key == selector }
            ?.let { indexAt(switchAddress + it.offset) }
            ?: (switchIndex + 1)
    }

    val references = mutableListOf<String>()
    val visited = mutableSetOf<Int>()
    while (visited.add(index)) {
        val instruction = instructions[index]
        when (val reference = (instruction as? ReferenceInstruction)?.reference) {
            is MethodReference -> references += descriptor(reference)
            is FieldReference -> references += descriptor(reference)
        }
        index = when {
            instruction.opcode in GOTO_OPCODES ->
                indexAt(addresses[index] + (instruction as OffsetInstruction).codeOffset)
            instruction.opcode.canContinue() -> index + 1
            else -> break
        }
    }
    return references
}

internal fun BytecodePatchContext.checkMergedLambdaBindings() = MERGED_LAMBDA_BINDINGS.forEach { binding ->
    val signature = "${binding.type}->${binding.method}"
    val method = classDefBy(binding.type).methods.singleOrNull { descriptor(it) == signature }
        ?: throw PatchException("$signature not found")
    val references = method.referencesForSelector(binding.selector)
    if (binding.expectedReference !in references) {
        throw PatchException(
            "$signature selector ${binding.selector} reaches ${references.joinToString()}, expected ${binding.expectedReference}",
        )
    }
}
