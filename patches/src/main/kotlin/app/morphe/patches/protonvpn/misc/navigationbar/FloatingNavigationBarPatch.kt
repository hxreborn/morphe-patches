/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.FLOATING_NAVIGATION_BAR_CLASS
import app.morphe.patches.shared.misc.proton.NAVIGATION_BAR_BACKDROP_CLASS
import app.morphe.patches.shared.misc.proton.GraphicsLayerElementFingerprint
import app.morphe.patches.shared.misc.proton.bindNavigationBarPreview
import app.morphe.patches.shared.misc.proton.injectFloatingNavigationBar
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.markPatchMetadata
import app.morphe.patches.shared.misc.proton.captureScaffoldPageColor
import app.morphe.patches.shared.misc.proton.paddingElementFingerprint
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField

private const val NAVIGATION_INSETS_PARAMETER = 4
private const val SCAFFOLD_BACKGROUND_PARAMETER = 6
private const val SCAFFOLD_BACKGROUND_DEFAULT = 0x40
private const val BOTTOM_INSET_SIDE = 0x20
private const val SETTINGS_CONTENT_ARGUMENT = 6
private const val PADDING_VALUES_ARGUMENT = 1
private const val CALLBACK_ARGUMENT = 1
private const val LIST_CONTENT_PADDING_ARGUMENT = 2
private const val PROFILE_BUTTON_COUNT = 2
private const val CLEAR_DEFAULT_CONTENT_PADDING = -5

@Suppress("unused")
val floatingNavigationBarPatch: BytecodePatch = bytecodePatch(
    name = "Floating navigation bar",
    description = "Adds optional floating navigation with adjustable blur and glass.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        bindNavigationBarPreview(
            icons = listOf(
                "ic_proton_house",
                "ic_proton_earth",
                "ic_proton_window_terminal",
                "ic_proton_cog_wheel_filled",
            ),
            labels = listOf(
                "bottom_nav_home",
                "bottom_nav_countries",
                "bottom_nav_profiles",
                "botton_nav_settings",
            ),
            showLabels = true,
        )
        markPatchMetadata("floatingNavigationBar", floatingNavigationBarPatch)
        markFeaturePatched(FLOATING_NAVIGATION_BAR_CLASS)
        val mainBar = MainBottomBarFingerprint.matchSingle()
        captureScaffoldPageColor(
            mainScaffoldFingerprint(mainBar.originalClassDef.type).matchSingle(),
            SCAFFOLD_BACKGROUND_PARAMETER, SCAFFOLD_BACKGROUND_DEFAULT,
        )
        val (barCall) = mainBar.instructionMatches
        val bar = barCall.instruction.getReference<MethodReference>()!!
        val navigation = navigationBarFingerprint(bar).matchSingle()
        val (navigationCall) = navigation.instructionMatches
        val nativeBar = navigationCall.instruction.getReference<MethodReference>()!!
        val nativeMethod = classDefBy(nativeBar.definingClass).methods.single {
            it.name == nativeBar.name && it.parameterTypes == nativeBar.parameterTypes
        }
        val insetType = nativeBar.parameterTypes[NAVIGATION_INSETS_PARAMETER].toString()
        val composerType = nativeBar.parameterTypes.dropLastWhile { it == "I" }.last().toString()
        val calls = nativeMethod.implementation!!.instructions.mapNotNull { it.getReference<MethodReference>() }.distinctBy(::descriptor)
        val insetConstructor = calls.single {
            it.name == "<init>" && it.parameterTypes == listOf(insetType, "I")
        }
        val insetFactory = calls.single {
            it.parameterTypes == listOf(composerType) &&
                classDefByOrNull(it.returnType)?.interfaces?.contains(insetType) == true
        }
        val holder = calls.single {
            it.name == "<init>" &&
                it.parameterTypes == nativeBar.parameterTypes.dropLastWhile { type -> type == "I" }.dropLast(1)
        }.definingClass
        val content = navigationInsetsContentFingerprint(
            holder, insetType, nativeBar.parameterTypes.first().toString(),
        ).matchSingle()
        val contentInstructions = content.method.instructions.toList()
        val padding = contentInstructions.mapNotNull { it.getReference<MethodReference>() }.single {
            it.parameterTypes == listOf(nativeBar.parameterTypes.first().toString(), insetType) &&
                it.returnType == nativeBar.parameterTypes.first().toString()
        }
        val sideMatch = navigationInsetSidesFingerprint(nativeBar, insetConstructor).matchSingle()
        val (sidesRead) = sideMatch.instructionMatches
        val sides = sidesRead.instruction.getReference<FieldReference>()!!
        mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS).replaceStub(
            "outerInsets",
            5,
            """
                check-cast p0, ${nativeBar.parameterTypes.first()}
                check-cast p1, $composerType
                invoke-static { p1 }, ${descriptor(insetFactory)}
                move-result-object v1
                new-instance v0, ${insetConstructor.definingClass}
                sget v2, ${descriptor(sides)}
                or-int/lit8 v2, v2, $BOTTOM_INSET_SIDE
                invoke-direct { v0, v1, v2 }, ${descriptor(insetConstructor)}
                invoke-static { p0, v0 }, ${descriptor(padding)}
                move-result-object p0
                return-object p0
            """,
        )
        val heightIndex = contentInstructions.indexOfFirst {
            it.opcode == Opcode.SGET && it.getReference<FieldReference>()?.let { field ->
                field.definingClass == nativeBar.definingClass && field.type == "F"
            } == true
        }
        check(heightIndex >= 0) { "Missing navigation bar height" }
        val heightRegister = (contentInstructions[heightIndex] as OneRegisterInstruction).registerA
        content.method.addInstructions(
            heightIndex + 1,
            """
                invoke-static { v$heightRegister }, $FLOATING_NAVIGATION_BAR_CLASS->height(F)F
                move-result v$heightRegister
            """,
        )
        injectFloatingNavigationBar(navigation.method, navigationCall.index, fixedHeight = true)
        val modifierType = nativeBar.parameterTypes.first().toString()
        val navType = mainBar.originalClassDef.methods.filter { it.name == "<init>" }.flatMap { it.parameterTypes }.map(CharSequence::toString).distinct().single { type ->
            classDefByOrNull(type)?.methods?.any {
                it.parameterTypes == listOf(modifierType, composerType, "I") && it.returnType == "V"
            } == true
        }
        installFloatingContentPadding(navType, modifierType)
        val indicatorMatch = navigationIndicatorFingerprint(bar).matchSingle()
        val (_, _, _, indicatorResult) = indicatorMatch.instructionMatches
        val indicatorRegister = indicatorResult.getInstruction<OneRegisterInstruction>().registerA
        val indicator = mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS).addStaticMethod(
            "indicator",
            listOf("J"),
            "J",
            3,
            """
                invoke-static {}, $FLOATING_NAVIGATION_BAR_CLASS->isEnabled()Z
                move-result v0
                if-eqz v0, :original
                const-wide/16 p0, 0x0
                :original
                return-wide p0
            """,
        )
        indicatorMatch.method.addInstructions(
            indicatorResult.index + 1,
            """
                invoke-static/range { v$indicatorRegister .. v${indicatorRegister + 1} }, $indicator
                move-result-wide v$indicatorRegister
            """,
        )
    }
}

private fun BytecodePatchContext.installFloatingContentPadding(navType: String, modifierType: String) {
    val values = PaddingValuesFingerprint.matchSingle().originalClassDef
    val paddingType = values.interfaces.single()
    val constructor = values.methods.single { it.name == "<init>" && it.parameterTypes == List(4) { "F" } }
    val bottomParameter = constructor.implementation!!.registerCount - 1
    val bottomField = constructor.implementation!!.instructions.single {
        it.opcode == Opcode.IPUT && (it as TwoRegisterInstruction).registerA == bottomParameter
    }.getReference<FieldReference>()!!
    val bottom = values.methods.single { method ->
        method.parameterTypes.isEmpty() && method.returnType == "F" &&
            method.implementation?.instructions?.any { it.getReference<FieldReference>() == bottomField } == true
    }
    val extension = mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS)
    extension.interfaces.add(paddingType)
    val original = ImmutableField(extension.type, "originalPadding", paddingType, AccessFlags.PRIVATE.value or AccessFlags.FINAL.value, null, null, null)
    extension.fields.add(original.toMutable())
    val reserve = ImmutableField(extension.type, "reservePadding", "Z", AccessFlags.PRIVATE.value or AccessFlags.FINAL.value, null, null, null)
    val footprint = ImmutableField(extension.type, "scaffoldPadding", paddingType, AccessFlags.PRIVATE.value or AccessFlags.STATIC.value, null, null, null)
    extension.fields.add(reserve.toMutable())
    extension.fields.add(footprint.toMutable())
    val footprintHeight = extension.addStaticMethod("bottomFootprint", emptyList(), "F", 1, """
        sget-object v0, ${descriptor(footprint)}
        if-eqz v0, :empty
        invoke-interface { v0 }, $paddingType->${bottom.name}()F
        move-result v0
        return v0
        :empty
        const/4 v0, 0x0
        return v0
    """)
    extension.addMethod(
        "<init>", listOf(paddingType, "Z"), "V", AccessFlags.PUBLIC.value or AccessFlags.CONSTRUCTOR.value, 3,
        """
            invoke-direct { p0 }, Ljava/lang/Object;-><init>()V
            iput-object p1, p0, ${descriptor(original)}
            iput-boolean p2, p0, ${descriptor(reserve)}
            return-void
        """,
    )
    classDefBy(paddingType).methods.forEach { method ->
        val parameters = method.parameterTypes.map(CharSequence::toString)
        check(method.returnType == "F" && parameters.size <= 1) { "Unexpected padding interface method ${descriptor(method)}" }
        val arguments = if (parameters.isEmpty()) "v0" else "v0, p1"
        val body = if (method.name == bottom.name && parameters == bottom.parameterTypes) {
            """
                iget-boolean v0, p0, ${descriptor(reserve)}
                if-eqz v0, :overlay
                iget-object v0, p0, ${descriptor(original)}
                invoke-interface { v0 }, ${descriptor(method)}
                move-result v0
                invoke-static {}, $footprintHeight
                move-result v1
                add-float/2addr v0, v1
                return v0
                :overlay
                const/4 v0, 0x0
            """
        } else {
            """
                iget-object v0, p0, ${descriptor(original)}
                invoke-interface { $arguments }, ${descriptor(method)}
                move-result v0
            """
        }
        extension.addMethod(method.name, parameters, "F", AccessFlags.PUBLIC.value, parameters.size + 3, "$body\nreturn v0")
    }
    val wrapper = extension.addStaticMethod(
        "floatingContentPadding", listOf(paddingType), paddingType, 3,
        """
            invoke-static {}, $FLOATING_NAVIGATION_BAR_CLASS->isEnabled()Z
            move-result v0
            if-eqz v0, :original
            sput-object p0, ${descriptor(footprint)}
            new-instance v0, $FLOATING_NAVIGATION_BAR_CLASS
            const/4 v1, 0x0
            invoke-direct { v0, p0, v1 }, $FLOATING_NAVIGATION_BAR_CLASS-><init>(${paddingType}Z)V
            return-object v0
            :original
            return-object p0
        """,
    )
    installContentClearance(modifierType, paddingType, footprintHeight)
    val content = mainContentPaddingFingerprint(navType, modifierType, paddingType).matchSingle()
    val (paddingCall, paddingResult) = content.instructionMatches
    val modifierRegister = paddingResult.getInstruction<OneRegisterInstruction>().registerA
    content.method.addInstructions(
        paddingResult.index + 1,
        """
            invoke-static/range { v$modifierRegister .. v$modifierRegister }, $NAVIGATION_BAR_BACKDROP_CLASS->source(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object v$modifierRegister
            check-cast v$modifierRegister, $modifierType
        """,
    )
    val register = paddingCall.instruction.argumentRegister(PADDING_VALUES_ARGUMENT)
    content.method.addInstructions(
        paddingCall.index,
        """
            invoke-static/range { v$register .. v$register }, $wrapper
            move-result-object v$register
        """,
    )
}

private fun BytecodePatchContext.installContentClearance(modifier: String, padding: String, footprintHeight: String) {
    val extension = mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS)
    val values = PaddingValuesFingerprint.matchSingle().originalClassDef
    val valuesConstructor = values.methods.single { it.name == "<init>" && it.parameterTypes == List(4) { "F" } }
    val reserve = extension.addStaticMethod("reserveContentPadding", listOf(padding), padding, 3, """
        invoke-static {}, $FLOATING_NAVIGATION_BAR_CLASS->isEnabled()Z
        move-result v0
        if-eqz v0, :original
        if-nez p0, :wrap
        new-instance p0, ${values.type}
        const/4 v0, 0x0
        invoke-direct { p0, v0, v0, v0, v0 }, ${descriptor(valuesConstructor)}
        :wrap
        new-instance v0, $FLOATING_NAVIGATION_BAR_CLASS
        const/4 v1, 0x1
        invoke-direct { v0, p0, v1 }, $FLOATING_NAVIGATION_BAR_CLASS-><init>(${padding}Z)V
        return-object v0
        :original
        return-object p0
    """)
    val layer = GraphicsLayerElementFingerprint.matchSingle().originalClassDef
    val element = paddingElementFingerprint(layer.superclass!!).matchSingle().originalClassDef
    val constructor = element.methods.single { it.name == "<init>" }
    val then = classDefBy(modifier).methods.single { it.parameterTypes == listOf(modifier) && it.returnType == modifier }
    val append = extension.addStaticMethod("appendContentClearance", listOf(modifier), modifier, 6, """
        invoke-static {}, $FLOATING_NAVIGATION_BAR_CLASS->isEnabled()Z
        move-result v0
        if-eqz v0, :original
        new-instance v0, ${element.type}
        const/4 v1, 0x0
        const/4 v2, 0x0
        const/4 v3, 0x0
        invoke-static {}, $footprintHeight
        move-result v4
        invoke-direct { v0, v1, v2, v3, v4 }, ${descriptor(constructor)}
        invoke-interface { p0, v0 }, ${descriptor(then)}
        move-result-object p0
        :original
        return-object p0
    """)
    fun transformArgument(method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod, index: Int, register: Int, bridge: String) {
        method.addInstructions(index, """
            invoke-static/range { v$register .. v$register }, $bridge
            move-result-object v$register
        """)
    }
    val settings = settingsScreenFingerprint(modifier).matchSingle()
    val (_, scaffoldCall) = settings.instructionMatches
    val callbackRegister = (scaffoldCall.instruction as RegisterRangeInstruction).startRegister + SETTINGS_CONTENT_ARGUMENT
    val instructions = settings.method.instructions.toList()
    val callbackResult = instructions.take(scaffoldCall.index).indexOfLast {
        it.opcode == Opcode.MOVE_RESULT_OBJECT && (it as OneRegisterInstruction).registerA == callbackRegister
    }
    check(callbackResult > 0) { "Missing settings content callback" }
    val callbackFactory = instructions[callbackResult - 1]
    val callbackReference = callbackFactory.getReference<MethodReference>()!!
    check(callbackReference.parameterTypes.first() == "I" && callbackReference.parameterTypes.size == 3)
    val lambdaRegister = callbackFactory.argumentRegister(CALLBACK_ARGUMENT)
    val allocation = instructions.take(callbackResult - 1).last {
        it.opcode == Opcode.NEW_INSTANCE && (it as OneRegisterInstruction).registerA == lambdaRegister
    }
    val lambdaType = allocation.getReference<TypeReference>()!!.type
    val scrolling = settingsScrollFingerprint(lambdaType, modifier, padding).matchSingle()
    val (_, _, stateCall, _, scrollCall) = scrolling.instructionMatches
    val scrollResult = scrolling.instructionMatches.last()
    check(stateCall.instruction.getReference<MethodReference>()!!.returnType == scrollCall.instruction.getReference<MethodReference>()!!.parameterTypes[1])
    transformArgument(scrolling.method, scrollResult.index + 1, scrollResult.getInstruction<OneRegisterInstruction>().registerA, append)
    val profileButtons = profileButtonPaddingFingerprint(modifier).matchAll().toList()
    check(profileButtons.size == PROFILE_BUTTON_COUNT) { "Expected populated and empty profile buttons" }
    profileButtons.forEach { match ->
        val (_, _, result) = match.instructionMatches
        transformArgument(match.method, result.index + 1, result.getInstruction<OneRegisterInstruction>().registerA, append)
    }
    val countries = countriesListFingerprint(modifier, padding).matchSingle()
    val (listCall) = countries.instructionMatches
    val list = listCall.instruction.getReference<MethodReference>()!!
    val listRegister = (listCall.instruction as RegisterRangeInstruction).startRegister
    transformArgument(countries.method, listCall.index, listRegister + LIST_CONTENT_PADDING_ARGUMENT, reserve)
    val recents = recentsListFingerprint(list).matchSingle()
    val (recentsCall) = recents.instructionMatches
    val recentsRange = recentsCall.instruction as RegisterRangeInstruction
    check(recentsCall.instruction.opcode == Opcode.INVOKE_STATIC_RANGE) { "Expected static recents list call" }
    val listParameters = list.parameterTypes.map(CharSequence::toString)
    val recentsBridge = extension.addStaticMethod(
        "recentsList",
        listParameters,
        "V",
        listParameters.size,
        """
            invoke-static/range { p$LIST_CONTENT_PADDING_ARGUMENT .. p$LIST_CONTENT_PADDING_ARGUMENT }, $reserve
            move-result-object p$LIST_CONTENT_PADDING_ARGUMENT
            and-int/lit8 p${listParameters.lastIndex}, p${listParameters.lastIndex}, $CLEAR_DEFAULT_CONTENT_PADDING
            invoke-static/range { p0 .. p${listParameters.lastIndex} }, ${descriptor(list)}
            return-void
        """,
    )
    recents.method.replaceInstruction(
        recentsCall.index,
        "invoke-static/range { v${recentsRange.startRegister} .. v${recentsRange.startRegister + recentsRange.registerCount - 1} }, $recentsBridge",
    )
}

private fun Instruction.argumentRegister(index: Int): Int = when (this) {
    is RegisterRangeInstruction -> startRegister + index
    is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG)[index]
    else -> error("Expected method invocation")
}
