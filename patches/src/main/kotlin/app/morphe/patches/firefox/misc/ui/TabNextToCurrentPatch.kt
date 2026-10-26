/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/TabNextToCurrentPatch.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.childElementsSequence
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val BUTTON_REGISTERS = 16
private const val PILL_REGISTERS = 22
private const val PILL_CONTENT_REGISTERS = 20
private const val TRAY_PILL_REGISTERS = 5
private const val SIZE_KT = "Landroidx/compose/foundation/layout/SizeKt;"
private const val SPACER_KT = "Landroidx/compose/foundation/layout/SpacerKt;"
private const val LAYOUT_WEIGHT_ELEMENT = "Landroidx/compose/foundation/layout/LayoutWeightElement;"
private const val ARRANGEMENT = "Landroidx/compose/foundation/layout/Arrangement;"
private const val MODIFIER_COMPANION =
    "Landroidx/compose/ui/Modifier\$Companion;->\$\$INSTANCE:Landroidx/compose/ui/Modifier\$Companion;"
private const val BUTTON_CALL = "$TAB_TOOLBAR->button($FUNCTION0${COMPOSER}IILjava/lang/String;)V"
private const val TAB_PILL_CONTENT = "Lapp/hxreborn/extension/firefox/TabPillContent;"
private const val TAB_ORDER = "Lapp/hxreborn/extension/firefox/TabOrder;"
private const val TAB_END_CLICK = "Lapp/hxreborn/extension/firefox/TabEndClick;"


private const val TAB_LIST_REDUCER = "Lmozilla/components/browser/state/reducer/TabListReducer;"
private const val TAB_BROWSER_STATE = "Lmozilla/components/browser/state/state/BrowserState;"
private const val TAB_SESSION_STATE = "Lmozilla/components/browser/state/state/TabSessionState;"
private const val TAB_LIST_ACTION = "Lmozilla/components/browser/state/action/TabListAction;"


private object TabReduceFingerprint : Fingerprint(
    definingClass = TAB_LIST_REDUCER,
    name = "reduce",
    returnType = TAB_BROWSER_STATE,
    parameters = listOf(TAB_BROWSER_STATE, TAB_LIST_ACTION),
    filters = listOf(
        fieldAccess(definingClass = TAB_BROWSER_STATE, name = "selectedTabId", opcode = Opcode.IGET_OBJECT),
        fieldAccess(definingClass = TAB_SESSION_STATE, name = "parentId", opcode = Opcode.IGET_OBJECT),
        opcode(Opcode.IF_EQZ, MatchAfterImmediately()),
    ),
)

private object FabCallFingerprint : Fingerprint(
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT, OBJECT, OBJECT),
    filters = listOf(
        methodCall(name = "padding-VpY3zN4\$default"),
        fieldAccess(definingClass = ARRANGEMENT, name = "Start", opcode = Opcode.SGET_OBJECT),
        methodCall(definingClass = LAYOUT_WEIGHT_ELEMENT, name = "<init>"),
        methodCall(definingClass = SPACER_KT, name = "Spacer"),
        methodCall(definingClass = LAYOUT_WEIGHT_ELEMENT, name = "<init>"),
        methodCall(
            definingClass = TOOLBAR_KT,
            name = "FloatingToolbarFAB",
            parameters = listOf(TABS_TRAY_STATE, FUNCTION0, FUNCTION0, FUNCTION0, FUNCTION0, COMPOSER, "I"),
            returnType = "V",
            opcode = Opcode.INVOKE_STATIC_RANGE,
        ),
    ),
)

private fun BytecodePatchContext.hookTabInsert() {
    val fingerprint = TabReduceFingerprint
    val method = fingerprint.method
    val (selectedGet, parentGet, parentCheck) = fingerprint.instructionMatches
    val tabRegister = (parentGet.instruction as TwoRegisterInstruction).registerB
    val selectedRegister = (selectedGet.instruction as OneRegisterInstruction).registerA

    val appendIndex = method.jumpTarget(parentCheck.index)
    val append = method.getInstruction<ReferenceInstruction>(appendIndex)
    val appendReference = append.reference as? MethodReference
    if (append.opcode != Opcode.INVOKE_STATIC || appendReference == null ||
        appendReference.name != "plus" || appendReference.parameterTypes.size != 2
    ) {
        throw PatchException("Unexpected add-tab append instruction: ${append.opcode} ${append.reference}")
    }
    val appendArguments = append as FiveRegisterInstruction
    if (appendArguments.registerC != tabRegister) {
        throw PatchException("Append call does not take the new tab register v$tabRegister")
    }
    val listRegister = appendArguments.registerD

    if (maxOf(tabRegister, listRegister, selectedRegister) > 15) {
        throw PatchException("Register out of range for non-range invoke")
    }
    if (!Liveness(method).isLive(appendIndex, selectedRegister)) {
        throw PatchException("selectedTabId register v$selectedRegister is not live at the append point")
    }

    method.replaceInstruction(
        appendIndex,
        "invoke-static {v$tabRegister, v$listRegister, v$selectedRegister}, " +
            "$TAB_ORDER->add(Ljava/lang/Object;Ljava/util/Collection;Ljava/lang/String;)Ljava/util/ArrayList;",
    )
}

private fun BytecodePatchContext.installTabToolbar() {
    val call = FabCallFingerprint.matchSingle()
    val (padding, arrangement, startWeight, spacerCall, endWeight) = call.instructionMatches
    val callIndex = call.instructionMatches.last().index
    val callMethod = call.method
    val range = callMethod.getInstruction<RegisterRangeInstruction>(callIndex)

    val contentInit = FabContentLambdaFingerprint.instructionMatches.single()
        .getInstruction<ReferenceInstruction>().reference as MethodReference
    val contentLambda = contentInit.definingClass

    val layoutNodeConstructor = fieldRef("Landroidx/compose/ui/node/LayoutNode;", "Constructor")
    val nodeCompanion = "Landroidx/compose/ui/node/ComposeUiNode\$Companion;"
    val setMeasurePolicy = fieldRef(nodeCompanion, "SetMeasurePolicy")
    val setLocals = fieldRef(nodeCompanion, "SetResolvedCompositionLocals")
    val setKeyHash = fieldRef(nodeCompanion, "SetCompositeKeyHash")
    val setModifier = fieldRef(nodeCompanion, "SetModifier")
    val centerVertically = fieldRef("Landroidx/compose/ui/Alignment\$Companion;", "CenterVertically")

    val addTabRegister = range.startRegister + 1
    val stateField = callMethod.capturedField(range.startRegister, callIndex)
    val addTabField = callMethod.capturedField(addTabRegister, callIndex)

    callMethod.addInstructions(
        callIndex,
        """
            invoke-static/range {v$addTabRegister .. v$addTabRegister}, $TAB_END_CLICK->regular(Ljava/lang/Object;)$TAB_END_CLICK
            move-result-object v$addTabRegister
        """,
    )

    callMethod.removeWeight(endWeight.index)

    val spacerIndex = spacerCall.index
    val spacer = callMethod.getInstruction<FiveRegisterInstruction>(spacerIndex)
    val scratch = spacer.registerD
    if (Liveness(callMethod).isLive(spacerIndex + 1, scratch)) {
        throw PatchException("Spacer modifier register v$scratch is still live after the call")
    }
    callMethod.replaceInstruction(spacerIndex, "move-object/from16 v$scratch, p0")
    callMethod.addInstruction(
        spacerIndex + 1,
        "invoke-static {v$scratch, v${spacer.registerC}}, $TAB_TOOLBAR->trayPill(Ljava/lang/Object;$COMPOSER)V",
    )

    callMethod.removeWeight(startWeight.index)
    callMethod.spaceBetween(arrangement.index, padding.index, fieldRef(ARRANGEMENT, "SpaceBetween"))

    val trayPillSmali = $$$"""
        check-cast p0, $$${callMethod.definingClass}
        iget-object v0, p0, $$${stateField}
        iget-object v1, p0, $$${addTabField}
        invoke-static {v0}, $$${TAB_TOOLBAR}->pillVisible(Ljava/lang/Object;)Z
        move-result v2
        if-eqz v2, :hx_skip
        const v2, 0x5a11b0c1
        invoke-interface {p1, v2}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V
        invoke-static {v0, v1, p1}, $$${TAB_TOOLBAR}->pill(Ljava/lang/Object;Ljava/lang/Object;Landroidx/compose/runtime/Composer;)V
        invoke-interface {p1}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V
        return-void
        :hx_skip
        const v2, 0x5a11b0c2
        invoke-interface {p1, v2}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V
        invoke-interface {p1}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V
        return-void
    """

    val buttonSmali = $$$"""
        new-instance v6, Lkotlin/jvm/internal/Ref$ObjectRef;
        invoke-direct {v6}, Lkotlin/jvm/internal/Ref$ObjectRef;-><init>()V
        sget-object v0, Landroidx/compose/ui/Modifier$Companion;->$$INSTANCE:Landroidx/compose/ui/Modifier$Companion;
        iput-object v0, v6, Lkotlin/jvm/internal/Ref$ObjectRef;->element:Ljava/lang/Object;
        new-instance v1, $$${contentLambda}
        const/4 v2, 0x0
        invoke-direct {v1, p2, v2, v6}, $$${contentLambda}-><init>(ILjava/lang/String;Lkotlin/jvm/internal/Ref$ObjectRef;)V
        invoke-static {p3, v1, p1}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(ILkotlin/Function;Landroidx/compose/runtime/Composer;)Landroidx/compose/runtime/internal/ComposableLambdaImpl;
        move-result-object v7
        move-object v0, p0
        move-object v1, p4
        sget-object v2, Landroidx/compose/ui/Modifier$Companion;->$$INSTANCE:Landroidx/compose/ui/Modifier$Companion;
        const/4 v3, 0x0
        const/4 v4, 0x0
        const/4 v5, 0x0
        const/4 v6, 0x0
        move-object v8, p1
        const/4 v9, 0x0
        const/16 v10, 0x78
        invoke-static/range {v0 .. v10}, Lmozilla/components/compose/base/button/IconButtonKt;->IconButton(Lkotlin/jvm/functions/Function0;Ljava/lang/String;Landroidx/compose/ui/Modifier;Landroidx/compose/material3/IconButtonColors;Ljava/lang/String;ZLandroidx/compose/foundation/interaction/MutableInteractionSource;Lkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;II)V
        return-void
    """

    val pillSmali = $$$"""
        move-object/from16 v0, p0
        move-object/from16 v9, p1
        move-object/from16 v6, p2
        sget-object v1, Landroidx/compose/ui/Modifier$Companion;->$$INSTANCE:Landroidx/compose/ui/Modifier$Companion;
        const/high16 v2, 0x42600000
        invoke-static {v1, v2}, Landroidx/compose/foundation/layout/SizeKt;->height-3ABfNKs(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;
        move-result-object v10
        sget-object v11, Landroidx/compose/foundation/shape/RoundedCornerShapeKt;->CircleShape:Landroidx/compose/foundation/shape/RoundedCornerShape;
        sget-object v1, Landroidx/compose/material3/ColorSchemeKt;->LocalColorScheme:Landroidx/compose/runtime/StaticProvidableCompositionLocal;
        invoke-interface {v6, v1}, Landroidx/compose/runtime/Composer;->consume(Landroidx/compose/runtime/ProvidableCompositionLocal;)Ljava/lang/Object;
        move-result-object v1
        check-cast v1, Landroidx/compose/material3/ColorScheme;
        iget-wide v2, v1, Landroidx/compose/material3/ColorScheme;->surfaceContainerHighest:J
        iget-wide v4, v1, Landroidx/compose/material3/ColorScheme;->onSurface:J
        const/16 v7, 0x6000
        const/16 v8, 0xc
        invoke-static/range {v2 .. v8}, Landroidx/compose/material3/CardDefaults;->cardColors-ro_MJ88(JJLandroidx/compose/runtime/Composer;II)Landroidx/compose/material3/CardColors;
        move-result-object v12
        const/high16 v1, 0x40c00000
        invoke-static {v1}, Landroidx/compose/material3/CardDefaults;->elevatedCardElevation-aqJV_2Y(F)Landroidx/compose/material3/CardElevation;
        move-result-object v13
        const/4 v14, 0x0
        new-instance v1, $$${TAB_PILL_CONTENT}
        invoke-direct {v1, v0, v9}, $$${TAB_PILL_CONTENT}-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
        const v2, 0x5a11b0c8
        invoke-static {v2, v1, v6}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(ILkotlin/Function;Landroidx/compose/runtime/Composer;)Landroidx/compose/runtime/internal/ComposableLambdaImpl;
        move-result-object v15
        move-object/from16 v16, v6
        const v17, 0x30006
        const/16 v18, 0x10
        invoke-static/range {v10 .. v18}, Landroidx/compose/material3/CardKt;->Card(Landroidx/compose/ui/Modifier;Landroidx/compose/ui/graphics/Shape;Landroidx/compose/material3/CardColors;Landroidx/compose/material3/CardElevation;Landroidx/compose/foundation/BorderStroke;Landroidx/compose/runtime/internal/ComposableLambdaImpl;Landroidx/compose/runtime/Composer;II)V
        return-void
    """

    val pillContentSmali = $$$"""
        move-object/from16 v12, p2
        move-object/from16 v13, p0
        move-object/from16 v14, p1
        sget-object v0, Landroidx/compose/ui/Modifier$Companion;->$$INSTANCE:Landroidx/compose/ui/Modifier$Companion;
        const/high16 v1, 0x40800000
        const/high16 v2, 0x41000000
        invoke-static {v0, v1, v2}, Landroidx/compose/foundation/layout/PaddingKt;->padding-VpY3zN4(Landroidx/compose/ui/Modifier;FF)Landroidx/compose/ui/Modifier;
        move-result-object v0
        new-instance v1, Landroidx/compose/foundation/layout/Arrangement$SpacedAligned;
        const/4 v2, 0x0
        const/4 v3, 0x1
        const/4 v4, 0x0
        invoke-direct {v1, v2, v3, v4}, Landroidx/compose/foundation/layout/Arrangement$SpacedAligned;-><init>(FZLandroidx/compose/foundation/layout/Arrangement$SpacingAlignmentCalculator;)V
        sget-object v2, $$${centerVertically}
        const/4 v3, 0x0
        invoke-static {v1, v2, v12, v3}, Landroidx/compose/foundation/layout/RowKt;->rowMeasurePolicy(Landroidx/compose/foundation/layout/Arrangement$Horizontal;Landroidx/compose/ui/Alignment$Vertical;Landroidx/compose/runtime/Composer;I)Landroidx/compose/foundation/layout/RowMeasurePolicy;
        move-result-object v1
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->getCompositeKeyHashCode()J
        move-result-wide v2
        invoke-static {v2, v3}, Ljava/lang/Long;->hashCode(J)I
        move-result v2
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->getCurrentCompositionLocalMap()Landroidx/compose/runtime/PersistentCompositionLocalMap;
        move-result-object v3
        invoke-static {v12, v0}, Landroidx/compose/ui/ComposedModifierKt;->materializeModifier(Landroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;
        move-result-object v4
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->getApplier()Landroidx/compose/runtime/Applier;
        move-result-object v5
        if-eqz v5, :hx_invalid
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->startReusableNode()V
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->getInserting()Z
        move-result v5
        sget-object v6, $$${layoutNodeConstructor}
        if-eqz v5, :hx_use
        invoke-interface {v12, v6}, Landroidx/compose/runtime/Composer;->createNode(Lkotlin/jvm/functions/Function0;)V
        goto :hx_created
        :hx_use
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->useNode()V
        :hx_created
        sget-object v5, $$${setMeasurePolicy}
        invoke-static {v12, v1, v5}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
        sget-object v1, $$${setLocals}
        invoke-static {v12, v3, v1}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
        invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
        move-result-object v2
        sget-object v3, $$${setKeyHash}
        invoke-static {v12, v2, v3}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
        invoke-static {v12}, Landroidx/compose/runtime/Updater;->reconcile-impl(Landroidx/compose/runtime/Composer;)V
        sget-object v2, $$${setModifier}
        invoke-static {v12, v4, v2}, Landroidx/compose/runtime/Updater;->set-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
        invoke-static {v13}, $$${TAB_TOOLBAR}->endVisible(Ljava/lang/Object;)Z
        move-result v0
        if-eqz v0, :hx_end_skip
        const v2, 0x5a11b0c9
        invoke-interface {v12, v2}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V
        $$${iconButton(
            click = "invoke-static {v14}, $TAB_END_CLICK->of(Ljava/lang/Object;)$TAB_END_CLICK",
            icon = "invoke-static {}, $TAB_TOOLBAR->endIcon()I",
            key = 0x5a11b0c3,
            label = "Add tab at end",
        )}
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V
        goto :hx_end_done
        :hx_end_skip
        const v2, 0x5a11b0ca
        invoke-interface {v12, v2}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V
        :hx_end_done
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->endNode()V
        return-void
        :hx_invalid
        invoke-static {}, Landroidx/compose/runtime/ComposablesKt;->invalidApplier()V
        const/4 v0, 0x0
        throw v0
    """

    mutableClassDefBy(TAB_TOOLBAR).apply {
        replaceStub("button", BUTTON_REGISTERS, buttonSmali)
        replaceStub("pill", PILL_REGISTERS, pillSmali)
        replaceStub("pillContent", PILL_CONTENT_REGISTERS, pillContentSmali)
        replaceStub("trayPill", TRAY_PILL_REGISTERS, trayPillSmali)
    }
}

private fun MutableMethod.removeWeight(index: Int) {
    val weight = getInstruction<FiveRegisterInstruction>(index).registerC
    addInstruction(index + 1, "sget-object v$weight, $MODIFIER_COMPANION")
}

private fun MutableMethod.spaceBetween(arrangementIndex: Int, paddingIndex: Int, arrangementField: String) {
    val arrangementRegister = getInstruction<OneRegisterInstruction>(arrangementIndex).registerA
    replaceInstruction(arrangementIndex, "sget-object v$arrangementRegister, $arrangementField")

    val rowModifier = getInstruction<OneRegisterInstruction>(paddingIndex + 1).registerA
    addInstructions(
        paddingIndex + 2,
        """
            const/high16 v$arrangementRegister, 0x3f800000
            invoke-static {v$rowModifier, v$arrangementRegister}, $SIZE_KT->fillMaxWidth(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;
            move-result-object v$rowModifier
        """,
    )
}

private fun MutableMethod.capturedField(register: Int, before: Int): String {
    val index = indexOfFirstInstructionReversedOrThrow(before) {
        opcode == Opcode.IGET_OBJECT && (this as TwoRegisterInstruction).registerA == register
    }
    val field = getInstruction<ReferenceInstruction>(index).reference as FieldReference
    if (field.definingClass != definingClass) {
        throw PatchException("v$register comes from ${field.definingClass}, not a $definingClass capture")
    }
    return descriptor(field)
}

internal fun iconButton(click: String, icon: String, key: Int, label: String) = $$$"""
        $$$click
        move-result-object v2
        $$$icon
        move-result v3
        const v4, $$${"0x%x".format(key)}
        const-string v5, "$$$label"
        invoke-static {v2, v12, v3, v4, v5}, $$$BUTTON_CALL
"""

private val tabGridBottomPaddingPatch = resourcePatch {
    execute {
        document("res/values/dimens.xml").use { document ->
            val padding = document.documentElement.childElementsSequence()
                .firstOrNull { it.tagName == "dimen" && it.getAttribute("name") == "tab_tray_grid_bottom_padding" }
                ?: throw PatchException("Dimen tab_tray_grid_bottom_padding not found")
            padding.textContent = "@dimen/tab_tray_list_bottom_padding"
        }
    }
}

@Suppress("unused")
val tabNextToCurrentPatch = bytecodePatch(
    name = "New tab next to current tab",
    description = "Opens new tabs next to the current tab and adds a button beside + in the tab tray " +
        "that opens a tab at the end.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    dependsOn(tabIconsPatch, tabGridBottomPaddingPatch)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("tabNext")

        hookTabInsert()
        installTabToolbar()
    }
}
