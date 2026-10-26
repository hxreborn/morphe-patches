/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/OldMenuPatch.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.util.logging.Logger

private const val ACCESS_POINT = "Lorg/mozilla/fenix/components/menu/MenuAccessPoint;"
private const val REMEMBER_LAMBDA =
    "Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda" +
        "(ILkotlin/Function;$COMPOSER)Landroidx/compose/runtime/internal/ComposableLambdaImpl;"
private const val MENU_GROUP =
    "$MENU_GROUP_KT->MenuGroup(Landroidx/compose/runtime/internal/ComposableLambdaImpl;${COMPOSER}I)V"
private const val DIVIDER = "$MENU_COMPOSE->divider(Ljava/lang/Object;)V"
private const val OBJ5 = "Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;"

private const val EXTENSION_PIN = "Lapp/hxreborn/extension/firefox/ExtensionPin;"
private const val PIN_PREFIX = "Lapp/hxreborn/extension/firefox/"
private const val TOOLBAR_EVENT =
    "Lmozilla/components/compose/browser/toolbar/store/BrowserToolbarInteraction\$BrowserToolbarEvent;"

private fun BytecodePatchContext.installPinEventInterface() {
    val interfaces = mutableClassDefBy("${PIN_PREFIX}PinEvent;").interfaces
    if (TOOLBAR_EVENT !in interfaces) interfaces.add(TOOLBAR_EVENT)
}

private fun BytecodePatchContext.installComposeMethods() {
    mutableClassDefBy(MENU_COMPOSE).apply {
        COMPOSE_METHODS.forEach { replaceStub(it.name, it.registers, it.smali) }
    }
}

private fun MutableMethod.lastReturnObject() =
    implementation!!.instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }

private fun MutableMethod.lastReturnVoid() =
    implementation!!.instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }

private fun BytecodePatchContext.installWindowHooks() {
    listOf(DialogFragmentCreateDialogFingerprint, MenuFragmentCreateDialogFingerprint).forEach {
        it.method.apply {
            val at = lastReturnObject()
            val register = getInstruction<OneRegisterInstruction>(at).registerA
            addInstructions(
                at,
                "invoke-static/range {v$register .. v$register}, $OLD_MENU->onCreateDialog(Landroid/app/Dialog;)V",
            )
        }
    }

    DialogFragmentViewCreatedFingerprint.let {
        it.method.addInstructions(
            it.instructionMatches.single().index + 1,
            """
                iget-object v0, p0, Landroidx/fragment/app/DialogFragment;->mDialog:Landroid/app/Dialog;
                invoke-static {v0}, $OLD_MENU->onViewCreated(Landroid/app/Dialog;)V
            """,
        )
    }

    listOf(
        DialogFragmentWidthFingerprint to 240,
        MenuFragmentWidthFingerprint to 300,
    ).forEach { (fingerprint, dp) ->
        fingerprint.method.applyEdits(
            returnWhenOldMenu(
                0,
                0,
                """
                    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->getResources()Landroid/content/res/Resources;
                    move-result-object v0
                    const/16 v1, $dp
                    invoke-static {v0, v1}, $OLD_MENU->menuWidth(Landroid/content/res/Resources;I)I
                    move-result v0
                    return v0
                """,
            ),
        )
    }

    listOf(DialogFragmentShowFingerprint, MenuFragmentShowFingerprint).forEach {
        it.method.apply {
            addInstructions(
                lastReturnVoid(),
                "invoke-static {}, $OLD_MENU->afterShow()V",
            )
            addInstructions(
                0,
                "invoke-static {p1}, $OLD_MENU->onShowStart(Landroid/content/DialogInterface;)V",
            )
        }
    }

    listOf(DialogFragmentShowFingerprint, MenuFragmentShowFingerprint)
        .map { insetsListenerFingerprint(it.method.insetsListenerType()) }
        .forEach {
            it.method.applyEdits(
                returnWhenOldMenu(
                    0,
                    0,
                    """
                        const/4 v0, 0x0
                        invoke-virtual {p1, v0, v0, v0, v0}, Landroid/view/View;->setPadding(IIII)V
                        return-object p2
                    """,
                ),
            )
        }
}

private fun MutableMethod.insetsListenerType(): String {
    val registration = indexOfFirstInstruction { calls("setOnApplyWindowInsetsListener") }
    if (registration < 0) unexpectedShape("Menu insets listener registration")
    val construction = lastIndexBefore(registration, registration) { it.opcode == Opcode.NEW_INSTANCE }
    if (construction < 0) unexpectedShape("Menu insets listener construction")
    return (getInstruction<ReferenceInstruction>(construction).reference as TypeReference).type
}

private val logger = Logger.getLogger("OldMenuPatch")

private const val LAMBDA_IMPL = "Landroidx/compose/runtime/internal/ComposableLambdaImpl;"

private fun unexpectedShape(what: String): Nothing =
    throw PatchException("Unexpected shape: $what")

private fun BytecodePatchContext.requireFields(type: String, expected: Map<String, String>) {
    val fields = mutableClassDefBy(type).fields.associate { it.name to it.type }
    expected.forEach { (name, fieldType) -> if (fields[name] != fieldType) unexpectedShape("$type->$name") }
}

private fun Exception.summary() = "${this::class.simpleName}: ${message?.lineSequence()?.firstOrNull()}"

private fun optional(name: String, block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        logger.info("Skipped $name: ${e.summary()}")
    }
}

private class ClonedCall(val start: Int, val scratch: Int, val smali: String)

private fun cloneLibraryCall(
    method: MutableMethod,
    callIndex: Int,
    composer: Int,
    self: Int,
    live: Liveness,
    insertAt: Int,
): ClonedCall {
    val call = method.getInstruction<RegisterRangeInstruction>(callIndex)
    val reference = (call as ReferenceInstruction).reference as MethodReference
    val first = call.startRegister
    val count = call.registerCount

    val loads = HashMap<Int, Instruction>()
    var start = callIndex
    while (start > 0) {
        val previous = method.getInstruction<Instruction>(start - 1)
        val dest = (previous as? OneRegisterInstruction)?.registerA ?: break
        val loadOpcode = previous.opcode.name.let { n -> n.startsWith("const") || n.startsWith("iget") }
        if (!loadOpcode || dest !in first until first + count || dest in loads) break
        loads[dest] = previous
        start--
    }

    val scratch = live.takeLow(1, insertAt, exclude = setOf(self, composer))[0]
    val base = live.takeRun(count, insertAt, exclude = setOf(self, composer, scratch))
    val lines = ArrayList<String>()
    for (register in first until first + count) {
        val target = base + (register - first)
        val load = loads[register]
        when {
            load == null && register == composer -> lines += "move-object/from16 v$target, v$composer"
            load == null -> unexpectedShape("LibraryMenuGroup call")
            load.opcode.name.startsWith("const") ->
                lines += "const v$target, 0x${Integer.toHexString((load as NarrowLiteralInstruction).narrowLiteral)}"
            load.opcode.name.startsWith("iget") -> {
                val field = (load as ReferenceInstruction).reference as FieldReference
                val owner = (load as TwoRegisterInstruction).registerB
                if (owner != self || self !in live.copiesOf(method.parameterBase(), insertAt)) {
                    unexpectedShape("LibraryMenuGroup capture owner")
                }
                val ref = "${field.definingClass}->${field.name}:${field.type}"
                val name = load.opcode.name
                lines += "$name v$scratch, v$self, $ref"
                lines += if (name == "iget-object") "move-object/from16 v$target, v$scratch"
                else "move/from16 v$target, v$scratch"
            }
            else -> unexpectedShape("LibraryMenuGroup call")
        }
    }
    val params = reference.parameterTypes.joinToString("")
    lines += "invoke-static/range {v$base .. v${base + count - 1}}, " +
        "${reference.definingClass}->${reference.name}($params)${reference.returnType}"
    return ClonedCall(start, scratch, lines.joinToString("\n"))
}

private const val TEXT_KT = "Landroidx/compose/material3/TextKt;"
private const val ICON_LIST_ITEM_OWNER = "Lorg/mozilla/fenix/compose/list/ListItemKt;"
private const val WEB_EXTENSION_ITEM = "Lorg/mozilla/fenix/components/menu/store/WebExtensionMenuItem;"
private const val PIN_STORE_CALL = "$EXTENSION_PIN->remember(Ljava/lang/Object;Ljava/lang/Object;)V"
private const val PIN_LIST_CALL = "$EXTENSION_PIN->addPinned(Ljava/util/ArrayList;Ljava/lang/Object;)V"
private const val PIN_WRAP_CALL = "$EXTENSION_PIN->wrap(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
private const val PIN_LONG_CALL = "$EXTENSION_PIN->longOf(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
private const val PIN_EVENT_CALL = "$EXTENSION_PIN->onPinEvent(Ljava/lang/Object;Ljava/lang/Object;)V"

private fun BytecodePatchContext.prepareEndActions(): () -> Unit {
    val fingerprint = ToolbarEndActionsFingerprint
    val method = fingerprint.method
    val start = fingerprint.instructionMatches.first().index
    val created = method.indexOfFirstInstruction(start) {
        opcode == Opcode.NEW_INSTANCE && refersToType("Ljava/util/ArrayList;")
    }
    if (created < 0) unexpectedShape("Toolbar end actions list")
    val list = method.getInstruction<OneRegisterInstruction>(created).registerA
    val constructed = method.indexOfFirstInstruction(created + 1) {
        opcode == Opcode.INVOKE_DIRECT && calls("<init>") &&
            callRegisters().firstOrNull() == list
    }
    if (constructed < 0) unexpectedShape("Toolbar end actions list constructor")
    val at = constructed + 1
    val owner = Liveness(method).holder(method.parameterBase(), at, "Toolbar middleware")
    val call = method.staticCall(at, PIN_LIST_CALL, listOf(list, owner))
    return { method.applyEdits(insert(at, call)) }
}

private fun BytecodePatchContext.prepareToolbarMiddleware(): () -> Unit {
    val fingerprint = ToolbarMiddlewareFingerprint
    val method = fingerprint.method
    val (_, _, actionClassRead, _, menuClickedCheck) = fingerprint.instructionMatches
    val live = Liveness(method)
    val owner = method.parameterBase()
    val menuClicked = menuClickedCheck.index
    val rememberAt = actionClassRead.index + 1

    val rememberCall = method.staticCall(
        rememberAt,
        PIN_STORE_CALL,
        listOf(live.holder(parameterRegister(method, 0), rememberAt, "Toolbar store"), live.holder(owner, rememberAt, "Toolbar middleware")),
    )

    val action = method.lowHolder(live, parameterRegister(method, 2), menuClicked, "Toolbar action")
    val next = method.lowHolder(live, parameterRegister(method, 1), menuClicked, "Toolbar next")
    val middleware = method.lowHolder(live, owner, menuClicked, "Toolbar middleware")
    val skip = method.lastIndexBefore(menuClicked, 8) { it.opcode.name.startsWith("goto") }
    if (skip < 0) unexpectedShape("Toolbar middleware exit")
    val end = method.jumpTarget(skip)
    val scratch = live.takeLow(1, menuClicked, joins = listOf(end), exclude = setOf(action, next, middleware))[0]

    return {
        method.applyEdits(
            insert(rememberAt, rememberCall),
            insert(
                menuClicked,
                """
                    instance-of v$scratch, v$action, ${PIN_PREFIX}PinEvent;
                    if-eqz v$scratch, :hx_next
                    invoke-static {v$action, v$middleware}, $PIN_EVENT_CALL
                    invoke-interface {v$next, v$action}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
                    goto/16 :hx_end
                """,
                mapOf(
                    "hx_next" to { menuClicked },
                    "hx_end" to { end },
                ),
            ),
        )
    }
}

private fun BytecodePatchContext.prepareExtensionItems(): () -> Unit {
    val fingerprint = WebExtensionMenuItemsPinFingerprint
    val method = fingerprint.method
    val at = fingerprint.instructionMatches.first().index

    val castAt = method.lastIndexBefore(at, 12) {
        it.opcode == Opcode.CHECK_CAST && it.refersToType(FUNCTION0)
    }
    if (castAt < 0) unexpectedShape("Extension menu click handler")
    val click = method.getInstruction<OneRegisterInstruction>(castAt).registerA

    val itemAt = method.lastIndexBefore(at, 80) {
        it.opcode == Opcode.IGET_OBJECT &&
            ((it as ReferenceInstruction).reference as FieldReference).definingClass == WEB_EXTENSION_ITEM
    }
    if (itemAt < 0) unexpectedShape("Extension menu item")
    val item = method.getInstruction<TwoRegisterInstruction>(itemAt).registerB
    if (Liveness(method).redefinedBetween(item, itemAt + 1, at)) unexpectedShape("Extension menu item register")

    val call = method.staticCall(at, PIN_WRAP_CALL, listOf(click, item))
    return {
        method.applyEdits(
            insert(
                at,
                """
                    $call
                    move-result-object v$click
                    check-cast v$click, $FUNCTION0
                """,
            ),
        )
    }
}

private fun BytecodePatchContext.prepareIconListItem(): () -> Unit {
    val method = IconListItemFingerprint.method
    val callAt = method.indexOfFirstInstruction {
        val reference = (this as? ReferenceInstruction)?.reference as? MethodReference
        reference != null && reference.definingClass == ICON_LIST_ITEM_OWNER &&
            (reference.name == "ListItem" || reference.name.startsWith("ListItem-"))
    }
    if (callAt < 0) unexpectedShape("Icon list item call")
    val functions = method.callParameters(callAt).filter { it.first == FUNCTION0 }
    if (functions.size < 2) unexpectedShape("Icon list item click arguments")
    val source = functions[0].second
    val target = functions[1].second
    if (source == target) unexpectedShape("Icon list item click registers")
    if (Liveness(method).isLive(callAt + 1, target)) unexpectedShape("Icon list item long click register")
    val call = method.staticCall(callAt, PIN_LONG_CALL, listOf(source, target))
    return {
        method.applyEdits(
            insert(
                callAt,
                """
                    $call
                    move-result-object v$target
                    check-cast v$target, $FUNCTION0
                """,
            ),
        )
    }
}

private fun BytecodePatchContext.installPinHooks() {
    val edits = try {
        listOf(
            prepareEndActions(),
            prepareToolbarMiddleware(),
            prepareExtensionItems(),
        )
    } catch (e: Exception) {
        logger.warning("Extension pinning skipped: ${e.summary()}")
        return
    }
    val longPress = try {
        prepareIconListItem()
    } catch (e: Exception) {
        logger.warning("Extension pin long press skipped: ${e.summary()}")
        null
    }
    edits.forEach { it() }
    longPress?.invoke()
}

private fun BytecodePatchContext.installMenuTweaks() {
    optional("BadgeHide") {
        BadgeHideFingerprint.method.applyEdits(
            insert(
                0,
                """
                    invoke-static/range {p0 .. p0}, $OLD_MENU->hideBadge(Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :hx_stock
                    return-void
                """,
                mapOf("hx_stock" to { 0 }),
            ),
        )
    }

    optional("MenuNavigation") {
        MenuNavigationFingerprint.let {
            val at = it.instructionMatches.first().index
            val register = it.method.getInstruction<OneRegisterInstruction>(at).registerA
            it.method.applyEdits(
                swap(
                    at,
                    register,
                    """
                        invoke-static {}, $OLD_MENU->navPadding()F
                        move-result v$register
                    """,
                ),
            )
        }
    }

    optional("BottomSheetHandle") {
        BottomSheetHandleFingerprint.method.applyEdits(returnWhenOldMenu(0, 0, "return-void"))
    }

    optional("ExtensionsMenuItem") {
        ExtensionsMenuItemFingerprint.let {
            val menuItem = it.instructionMatches.single().index
            val description = it.method.callParameters(menuItem)[5].second
            it.method.applyEdits(
                zeroTo(it.method.lastWriteBefore(menuItem, description)),
            )
        }
    }
    optional("WebExtensionMenuItems") {
        WebExtensionMenuItemsFingerprint.let {
            it.method.applyEdits(zeroTo(it.instructionMatches.single().index))
        }
    }

    optional("IPProtectionMenuItem") {
        IPProtectionMenuItemFingerprint.let {
            val (surfaceRead, height) = it.instructionMatches
            it.method.applyEdits(
                surface(surfaceRead.index),
                floatTo(height.index, 0x42400000),
            )
        }
    }
    optional("IPProtectionBadge") {
        IPProtectionBadgeFingerprint.let {
            val (badgeCall, padding) = it.instructionMatches
            val badge = badgeCall.index
            val text = it.method.getInstruction<FiveRegisterInstruction>(badge).registerC
            val scratch = Liveness(it.method).takeLow(1, badge)[0]
            it.method.applyEdits(
                swap(
                    badge,
                    scratch,
                    "invoke-static {v$text}, $OLD_MENU->hideBadge(Ljava/lang/String;)Z\nmove-result v$scratch\nif-eqz v$scratch, :hx_stock",
                    count = 1,
                ),
                floatTo(padding.index, 0x40000000),
            )
        }
    }

    MainMenuAddonsFingerprint.let {
        val method = it.method
        val (spacing, home, manageLabel, manageItem) = it.instructionMatches
        val scratch = home.getInstruction<OneRegisterInstruction>().registerA
        val manageArguments = method.callParameters(manageItem.index)
        val composer = manageArguments.single { (type, _) -> type == COMPOSER }.second
        val manageClick = manageArguments.single { (type, _) -> type == FUNCTION0 }.second
        val live = Liveness(method)
        if (live.redefinedBetween(composer, home.index, manageItem.index) ||
            live.redefinedBetween(manageClick, home.index, manageItem.index)
        ) {
            unexpectedShape("Main menu add-ons composer or manage click")
        }
        val manageCheck = method.lastIndexBefore(manageLabel.index, manageLabel.index) { instruction ->
            instruction.calls("isEmpty")
        }
        if (manageCheck < 0) unexpectedShape("Main menu manage extensions check")
        val isEmpty = method.getInstruction<OneRegisterInstruction>(manageCheck + 1).registerA
        val manageGroupEnd = method.indexOfFirstInstruction(manageItem.index + 1) {
            calls("end")
        }
        if (manageGroupEnd < 0) unexpectedShape("Main menu manage extensions group end")
        method.applyEdits(
            zeroTo(spacing.index),
            swap(
                home.index,
                scratch,
                """
                    const v$scratch, 0x7a11b001
                    invoke-virtual {v$composer, v$scratch}, Landroidx/compose/runtime/GapComposer;->startReplaceGroup(I)V
                    sget-boolean v$scratch, $OLD_MENU->extensionsActive:Z
                    if-eqz v$scratch, :hx_row
                    invoke-static {v$composer, v$manageClick}, $MENU_COMPOSE->managerRow(Ljava/lang/Object;Ljava/lang/Object;)V
                    :hx_row
                    const/4 v$scratch, 0x0
                    invoke-virtual {v$composer, v$scratch}, Landroidx/compose/runtime/GapComposer;->end(Z)V
                """,
                count = 0,
            ),
            insert(
                manageCheck,
                """
                    sget-boolean v$isEmpty, $OLD_MENU->extensionsActive:Z
                    if-nez v$isEmpty, :hx_skip
                """,
                mapOf("hx_skip" to { manageGroupEnd + 1 }),
            ),
        )
    }

    MainMenuLibraryGroupFingerprint.method.let { method ->
        val types = method.parameterTypes.map { type -> type.toString() }
        val composerAt = types.indexOf(COMPOSER)
        val functionsAt = composerAt - 4
        if (functionsAt !in 1..2 ||
            types.subList(functionsAt, composerAt).any { type -> type != FUNCTION0 } ||
            types.subList(0, functionsAt).any { type -> type != "Z" }
        ) {
            unexpectedShape("LibraryMenuGroup")
        }
        val flagged = functionsAt == 2
        val first = if (flagged) 0 else functionsAt
        val target = if (flagged) {
            "libraryGroupFlagged(ZZ$OBJ5)V"
        } else {
            "libraryGroup($OBJ5)V"
        }
        method.applyEdits(
            returnWhenOldMenu(
                0,
                0,
                """
                    invoke-static/range {p$first .. p$composerAt}, $MENU_COMPOSE->$target
                    return-void
                """,
            ),
        )
    }

    MainMenuFingerprint.let {
        val params = it.method.parameterTypes.map { type -> type.toString() }
        if (params.getOrNull(0) != ACCESS_POINT || params.getOrNull(7) != "Z") unexpectedShape("MainMenu")
        val method = it.method
        val (horizontalPadding, modifierRead, paddingCall, menuFrame) = it.instructionMatches
        val scratch = modifierRead.getInstruction<OneRegisterInstruction>().registerA
        val bottom = paddingCall.getInstruction<FiveRegisterInstruction>().registerG
        val content = method.lastIndexBefore(menuFrame.index, menuFrame.index) { instruction ->
            instruction.opcode == Opcode.NEW_INSTANCE
        }
        if (content < 0) unexpectedShape("MainMenu content lambda")
        val (owner, flag) = Liveness(method).takeLow(2, content)
        method.applyEdits(
            zeroTo(horizontalPadding.index),
            swap(
                modifierRead.index,
                scratch,
                """
                    move/from16 v$scratch, p7
                    invoke-static {v$bottom, v$scratch}, $OLD_MENU->bottomPadding(FZ)F
                    move-result v$bottom
                """,
                count = 0,
            ),
            swap(
                content,
                owner,
                """
                    move-object/from16 v$owner, p0
                    move/from16 v$flag, p7
                    invoke-static {v$owner, v$flag}, $OLD_MENU->setExtensionsActive(Ljava/lang/Object;Z)V
                """,
                count = 0,
            ),
        )
    }

    optional("MoreExtensionsMenuItem") {
        MoreExtensionsMenuItemFingerprint.let {
            it.method.applyEdits(surface(it.instructionMatches.single().index))
        }
    }

    listOf(MainMenuNavigationLambdaFingerprint, MainMenuDividerLambdaFingerprint).forEach {
        val (_, ifEqMatch) = it.instructionMatches
        val ifEq = ifEqMatch.index
        val scratch = Liveness(it.method).takeLow(1, ifEq + 1)[0]
        it.method.applyEdits(
            insert(
                ifEq + 1,
                """
                    sget-boolean v$scratch, $OLD_MENU->extensionsActive:Z
                    if-nez v$scratch, :hx_skip
                """,
                mapOf("hx_skip" to { jumpTarget(ifEq) }),
            ),
        )
    }

    MainMenuColumnLambdaFingerprint.let {
        val matches = it.instructionMatches
        val method = it.method
        val lambda = method.definingClass
        requireFields(
            lambda,
            mapOf(
                "f${'$'}1" to ACCESS_POINT,
                "f${'$'}10" to "Z",
                "f${'$'}15" to FUNCTION0,
                "f${'$'}23" to LAMBDA_IMPL,
            ),
        )
        val (shouldExecute, _, contentGate, _, secondGroup) = matches
        val (_, browserAccess, libraryGroup, accountGroup, quitGroup) = matches.drop(5)
        val composer = shouldExecute.getInstruction<FiveRegisterInstruction>().registerC
        val live = Liveness(method)
        val extensionsAt = contentGate.index + 1
        val self = method.getInstruction<TwoRegisterInstruction>(extensionsAt).registerB
        val secondGroupJoin = method.indexOfFirstInstruction(secondGroup.index + 1) {
            opcode == Opcode.GOTO
        }
        if (secondGroupJoin < 0) unexpectedShape("MainMenu second group join")
        val libraryAt = method.jumpTarget(secondGroupJoin)
        val browserReplaceGroup = method.indexOfFirstInstruction(browserAccess.index + 1) { calls("startReplaceGroup") }
        if (browserReplaceGroup < 0) unexpectedShape("MainMenu browser group start")
        val browserGroupStart = browserReplaceGroup + 1
        val quitReplaceGroup = method.lastIndexBefore(quitGroup.index, quitGroup.index) { instruction ->
            instruction.calls("startReplaceGroup")
        }
        if (quitReplaceGroup < 0) unexpectedShape("MainMenu quit group start")
        val quitGroupStart = quitReplaceGroup + 1
        val libraryCall = libraryGroup.index
        val (r1, r2, r3) = live.takeLow(3, extensionsAt, exclude = setOf(composer, self))
        val library = cloneLibraryCall(method, libraryCall, composer, self, live, libraryAt)
        val divider = "invoke-static {v$composer}, $DIVIDER"
        method.applyEdits(
            swap(
                extensionsAt,
                r1,
                """
                    iget-boolean v$r1, v$self, $lambda->f${'$'}10:Z
                    if-eqz v$r1, :hx_stock
                    iget-object v$r2, v$self, $lambda->f${'$'}1:$ACCESS_POINT
                    sget-object v$r1, $ACCESS_POINT->Browser:$ACCESS_POINT
                    if-ne v$r2, v$r1, :hx_stock
                    const v$r1, 0x6e7a3b10
                    invoke-interface {v$composer, v$r1}, $COMPOSER->startReplaceGroup(I)V
                    iget-object v$r2, v$self, $lambda->f${'$'}23:$LAMBDA_IMPL
                    iget-object v$r3, v$self, $lambda->f${'$'}15:$FUNCTION0
                    invoke-static {v$r2, v$r3}, $MENU_LAMBDA->extensionsPage(Ljava/lang/Object;Ljava/lang/Object;)$MENU_LAMBDA
                    move-result-object v$r1
                    const v$r2, 0x6e7a3b11
                    invoke-static {v$r2, v$r1, v$composer}, $REMEMBER_LAMBDA
                    move-result-object v$r1
                    const/4 v$r2, 0x6
                    invoke-static {v$r1, v$composer, v$r2}, $MENU_GROUP
                    invoke-interface {v$composer}, $COMPOSER->endReplaceGroup()V
                    goto/16 :hx_end
                """,
                count = 0,
                fallThrough = false,
                labels = mapOf("hx_end" to { implementation!!.instructions.size - 2 }),
            ),
            swap(libraryAt, library.scratch, "$divider\n${library.smali}", count = 0),
            swap(browserGroupStart, live.takeLow(1, browserGroupStart)[0], divider, count = 0),
            swap(
                library.start,
                live.takeLow(1, library.start)[0],
                divider,
                count = libraryCall - library.start + 1,
            ),
            insert(
                accountGroup.index + 1,
                "invoke-static {v$composer}, $MENU_COMPOSE->patchesRow(Ljava/lang/Object;)V",
            ),
            swap(quitGroupStart, live.takeLow(1, quitGroupStart)[0], divider, count = 0),
        )
    }

    optional("MenuGroup") {
        MenuGroupFingerprint.let { it.method.applyEdits(zeroTo(it.instructionMatches.single().index)) }
    }

    optional("Badge") {
        BadgeFingerprint.let {
            val (size, padding) = it.instructionMatches
            it.method.applyEdits(
                floatTo(size.index, 0x41400000),
                floatTo(padding.index, 0x40000000),
            )
        }
    }

    optional("MenuBadgeItem") {
        MenuBadgeItemFingerprint.let {
            val (surfaceRead, padding, size) = it.instructionMatches
            it.method.applyEdits(
                surface(surfaceRead.index),
                floatTo(padding.index, 0x40000000),
                floatTo(size.index, 0x41400000),
            )
        }
    }

    optional("MenuItem") {
        MenuItemFingerprint.let {
            val (surfaceRead, tallHeight, shortHeight, listItem) = it.instructionMatches
            val description = it.method.callParameters(listItem.index)[5].second
            it.method.applyEdits(
                zeroTo(it.method.lastWriteBefore(listItem.index, description)),
                surface(surfaceRead.index),
                floatTo(tallHeight.index, 0x42200000),
                floatTo(shortHeight.index, 0x42200000),
            )
        }
    }

    optional("MenuTextItem") {
        MenuTextItemFingerprint.let {
            val (tallHeight, shortHeight, surfaceRead) = it.instructionMatches
            it.method.applyEdits(
                floatTo(tallHeight.index, 0x42200000),
                floatTo(shortHeight.index, 0x42200000),
                surface(surfaceRead.index),
            )
        }
    }

    optional("WebExtensionMenuItem") {
        WebExtensionMenuItemFingerprint.let {
            val method = it.method
            val (surfaceRead, trailingInit, _, trailingResult, flags) = it.instructionMatches
            val listItem = it.instructionMatches.last().index
            val trailingInstance = trailingInit.getInstruction<FiveRegisterInstruction>()
            val text = trailingInstance.registerD
            val scratch = trailingInstance.registerC
            val trailing = trailingResult.getInstruction<OneRegisterInstruction>().registerA
            val flagsRegister = flags.getInstruction<OneRegisterInstruction>().registerA
            val height = method.callParameters(listItem).single { (type, _) -> type == "F" }.second
            val live = Liveness(method)
            if (live.isLive(flags.index, scratch) || live.redefinedBetween(text, trailingInit.index, flags.index)) {
                unexpectedShape("WebExtensionMenuItem badge text")
            }
            method.applyEdits(
                surface(surfaceRead.index),
                swap(
                    flags.index,
                    flagsRegister,
                    """
                        const v$flagsRegister, 0x1fe47c
                        if-eqz v$text, :hx_empty
                        invoke-virtual {v$text}, Ljava/lang/String;->length()I
                        move-result v$scratch
                        if-nez v$scratch, :hx_filled
                        :hx_empty
                        const/16 v$trailing, 0x0
                        :hx_filled
                    """,
                ),
                floatTo(method.lastWriteBefore(listItem, height), 0x42400000),
            )
        }
    }

    optional("MenuItemIconLambda") {
        MenuItemIconLambdaFingerprint.let {
            val iget = it.instructionMatches.first().index
            val register = it.method.getInstruction<OneRegisterInstruction>(iget).registerA
            it.method.applyEdits(
                insert(
                    iget + 1,
                    """
                        invoke-static {v$register}, $OLD_MENU->trailingIcon(Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$register
                        check-cast v$register, Lkotlin/jvm/functions/Function0;
                    """,
                ),
            )
        }
    }

    optional("MenuNavItem") {
        MenuNavItemFingerprint.let {
            val (iconCall, typography, textCall) = it.instructionMatches
            val icon = iconCall.index
            val spacer = typography.index - 1
            val caption = textCall.index
            val live = Liveness(it.method)
            val description = it.method.callParameters(icon).singleOrNull { (type, _) -> type == "Ljava/lang/String;" }
                ?.second ?: unexpectedShape("Nav item icon description")
            if (live.isLive(icon + 1, description)) unexpectedShape("Nav item icon description register")
            it.method.applyEdits(
                swap(icon, live.takeLow(1, icon)[0], "move-object/from16 v$description, p3", count = 0),
                swap(spacer, live.takeLow(1, spacer)[0], ""),
                swap(caption, live.takeLow(1, caption)[0], ""),
            )
        }
    }

    optional("MenuFrame") {
        MenuFrameFingerprint.let { it.method.applyEdits(zeroTo(it.instructionMatches.single().index)) }
    }

    optional("AccountMenuItem") {
        AccountMenuItemFingerprint.let {
            val (surfaceRead, height) = it.instructionMatches
            it.method.applyEdits(surface(surfaceRead.index), floatTo(height.index, 0x42400000))
        }
    }

    optional("AccountSubtitleLambda") {
        AccountSubtitleLambdaFingerprint.let {
            val method = it.method
            val (_, subtitleRead) = it.instructionMatches
            val iget = subtitleRead.index
            val callAt = method.indexOfFirstInstruction(iget + 1) {
                val reference = (this as? ReferenceInstruction)?.reference as? MethodReference
                reference != null && reference.definingClass == TEXT_KT && reference.name.startsWith("Text-")
            }
            if (callAt < 0) unexpectedShape("Account subtitle text")
            val text = method.callParameters(callAt).firstOrNull()
            if (text == null || text.first != "Ljava/lang/String;") unexpectedShape("Account subtitle text argument")
            val register = text.second
            val call = method.staticCall(
                callAt,
                "$OLD_MENU->accountSubtitle(Ljava/lang/String;)Ljava/lang/String;",
                listOf(register),
            )
            method.applyEdits(
                insert(
                    callAt,
                    """
                        $call
                        move-result-object v$register
                        if-eqz v$register, :hx_skip
                    """,
                    mapOf("hx_skip" to { callAt + 1 }),
                ),
            )
        }
    }
}

@Suppress("unused")
val oldMenuPatch = bytecodePatch(
    name = "Old-style menu and extension pinning",
    description = "Restores the popup 3-dot menu and pins extensions to the toolbar on a long press. " +
        "Adds an hxreborn patches menu entry with a switch for each patch.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        installPinEventInterface()
        installComposeMethods()
        installWindowHooks()
        installMenuTweaks()
        installPinHooks()
    }
}
