/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.theme

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.outlook.misc.fix.signin.fixSignInPatch
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import app.morphe.patches.shared.misc.extension.outlookCustomThemeExtensionPatch
import app.morphe.patches.shared.misc.hostedcomponents.declarePublicResources
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.asSequence
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import org.w3c.dom.Element

private const val CUSTOM_THEME_CLASS = "Lapp/hxreborn/extension/outlook/theme/CustomTheme;"
private const val TOKENS_CLASS = "Lcom/microsoft/office/outlook/uicomposekit/theme/Fluent2ThemeColorTokens;"
private const val PALETTE_CLASS = "Lcom/microsoft/office/outlook/uicomposekit/theme/Fluent2Palette;"
private const val COMPOSE_THEME_CLASS = "Lcom/microsoft/office/outlook/uicomposekit/theme/outlookthemes/OutlookComposeThemeV2;"
private const val THEME_ICON_SUPPLIER = "Ljava/util/function/IntSupplier;"
private const val THEME_ASSET_DATA = "Lcom/microsoft/office/outlook/uikit/accessibility/ThemeAssetData;"
private const val THEME_ASSET_FILE_NAMES = "Lcom/microsoft/office/outlook/uikit/accessibility/ThemeAssetFileNames;"
private const val URI = "Landroid/net/Uri;"
private const val OBJECT = "Ljava/lang/Object;"

private const val STOCK_STYLE_SUFFIX = ".BlueV2"
private const val CUSTOM_STYLE_SUFFIX = ".HxCustom"
private val STOCK_STYLE_NAME = Regex("""\.BlueV2(\.HighContrastColors)?$""")
private const val STOCK_COLOR_PREFIX = "color_comm"
private const val CUSTOM_COLOR_PREFIX = "hx_custom_comm"
private val STOCK_COLOR_REFERENCE = Regex("""@color/color_comm(\w+)""")
private const val REFERENCE_COLOR = "color_comm80"
private const val CUSTOMIZE_OVERLAY = "ThemeOverlay.Outlook.Customize.HxCustom"
private const val CUSTOMIZE_OVERLAY_HIGH_CONTRAST = "ThemeOverlay.Outlook.Customize.HxCustom.HighContrastColors"
private const val PROMINENT_DIALOG_OVERLAY = "ThemeOverlay.Outlook.Dialog.Alert.Prominent.HxCustom"
private const val WHITE_TOOLBAR_OVERLAY = "PhotoToolbarBackgroundTheme.Default"
private const val DARK_TOOLBAR_OVERLAY = "PhotoToolbarBackgroundTheme.Dark"
private const val STATUS_BAR_TEXT_COLOR = "Lcom/microsoft/office/outlook/uikit/accessibility/ThemeColorOption\$ThemeStatusBarTextColor;"

private const val CUSTOM_THEME_STYLE = "HxCustom"
private const val CUSTOM_COLOR_OPTION = "HxCustomColor"
private const val CUSTOM_PICTURE_OPTION = "HxCustomPicture"
private const val COLOR_NAME = "hx_custom_theme_color"
private const val PICTURE_NAME = "hx_custom_theme_picture"
private val STRINGS = mapOf(COLOR_NAME to "Custom color", PICTURE_NAME to "Custom picture")

private const val TOKEN_GETTER_PREFIX = "getColor"
private const val TOKEN_COUNT = 16
private const val FIRST_TOKEN_REGISTER = 2
private const val TOKEN_MARKER_REGISTER = FIRST_TOKEN_REGISTER + 2 * TOKEN_COUNT
private const val CHROME_URIS = 11
private const val BACKGROUND_URIS = 16
private const val PICTURE_URIS = CHROME_URIS + BACKGROUND_URIS
private const val SET_PICTURE_THEME_PARAMETERS = 5

private class CustomThemeResources(
    val styleIds: Map<String, Long>,
    val colorPairs: List<Pair<Long, Long>>,
    val stringIds: Map<String, Long>,
)

private lateinit var customThemeResources: CustomThemeResources

private val customThemeResourcesPatch = patch {
    execute {
        val stockColors = sortedSetOf<String>()
        val styleNames = linkedSetOf<String>()
        resourceFile("res").listFiles { file -> file.name.startsWith("values") }.orEmpty()
            .filter { resourceFile("res/${it.name}/styles.xml").isFile }
            .forEach { directory ->
                document("res/${directory.name}/styles.xml").use { styles ->
                    styles.getElementsByTagName("style").asSequence().filterIsInstance<Element>()
                        .filter { STOCK_STYLE_NAME.containsMatchIn(it.getAttribute("name")) }.toList()
                        .forEach { stock ->
                            val custom = stock.cloneNode(true) as Element
                            val name = stock.getAttribute("name").replace(STOCK_STYLE_SUFFIX, CUSTOM_STYLE_SUFFIX)
                            custom.setAttribute("name", name)
                            custom.getElementsByTagName("item").asSequence().filterIsInstance<Element>().forEach { item ->
                                stockColors += STOCK_COLOR_REFERENCE.findAll(item.textContent).map { it.groupValues[1] }
                                item.textContent = item.textContent
                                    .replace("@color/$STOCK_COLOR_PREFIX", "@color/$CUSTOM_COLOR_PREFIX")
                                    .replace(STOCK_STYLE_SUFFIX, CUSTOM_STYLE_SUFFIX)
                            }
                            styles.documentElement.appendChild(custom)
                            styleNames += name
                        }
                }
            }
        if (CUSTOMIZE_OVERLAY !in styleNames || PROMINENT_DIALOG_OVERLAY !in styleNames) {
            throw PatchException("Missing the stock Blue theme styles")
        }
        document("res/values/colors.xml").use { colors ->
            val stockValues = colors.getElementsByTagName("color").asSequence().filterIsInstance<Element>()
                .associate { it.getAttribute("name") to it.textContent }
            stockColors.forEach { step ->
                colors.documentElement.appendChild(
                    colors.createElement("color").apply {
                        setAttribute("name", CUSTOM_COLOR_PREFIX + step)
                        textContent = stockValues[STOCK_COLOR_PREFIX + step]
                            ?: throw PatchException("Missing color $STOCK_COLOR_PREFIX$step")
                    },
                )
            }
        }
        document("res/values/strings.xml").use { strings ->
            STRINGS.forEach { (name, text) ->
                strings.documentElement.appendChild(
                    strings.createElement("string").apply {
                        setAttribute("name", name)
                        textContent = text
                    },
                )
            }
        }
        val customColorIds = declarePublicResources("color", stockColors.map { CUSTOM_COLOR_PREFIX + it })
        customThemeResources = CustomThemeResources(
            declarePublicResources("style", styleNames.toList()),
            stockColors.map { step ->
                resourceId(ResourceType.COLOR, STOCK_COLOR_PREFIX + step) to
                    customColorIds.getValue(CUSTOM_COLOR_PREFIX + step)
            },
            declarePublicResources("string", STRINGS.keys.toList()),
        )
    }
}

@Suppress("unused")
val customThemesPatch: Patch = patch(
    name = "Add custom themes",
    description = "Adds Custom color and Custom picture to Outlook's theme picker. " +
        "Pick any color on a color wheel or any photo from the gallery.",
) {
    compatibleWith(AppCompatibilities.OUTLOOK)

    dependsOn(
        outlookAppliedPatchesExtensionPatch,
        outlookCustomThemeExtensionPatch,
        fixSignInPatch,
        customThemeResourcesPatch,
    )

    execute {
        markPatchApplied("addCustomThemes", customThemesPatch)
        mutableClassDefBy(THEME_STYLE).addCustomThemeStyle()
        mutableClassDefBy(THEME_COLOR_OPTION).addCustomThemeOptions()

        ApplyThemeFingerprint.method.addInstructions(0, "invoke-static { p0 }, $CUSTOM_THEME_CLASS->applySavedTheme(Landroid/content/Context;)V")

        ThemePaletteFingerprint.method.apply {
            val palette = getFreeRegisterProvider(0, 1).getFreeRegister()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p0 }, $CUSTOM_THEME_CLASS->palette($OBJECT)$OBJECT
                    move-result-object v$palette
                    if-eqz v$palette, :stock
                    check-cast v$palette, $THEME_PALETTE
                    return-object v$palette
                """,
                ExternalLabel("stock", getInstruction(0)),
            )
        }

        ProminentDialogContextFingerprint.method.apply {
            val context = getFreeRegisterProvider(0, 1).getFreeRegister()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p0 }, $CUSTOM_THEME_CLASS->prominentDialogContext(Landroid/content/Context;)Landroid/content/Context;
                    move-result-object v$context
                    if-eqz v$context, :stock
                    return-object v$context
                """,
                ExternalLabel("stock", getInstruction(0)),
            )
        }

        val selection = ThemeSelectionFingerprint.match()
        val (_, _, _, _, unitRead) = selection.instructionMatches
        val selectTheme = selection.method.apply {
            val unitType = (getInstruction<ReferenceInstruction>(unitRead.index).reference as? FieldReference)?.type
            if (unitType != returnType) throw PatchException("Theme selection returns $returnType, not $unitType")
            accessFlags = accessFlags and AccessFlags.PRIVATE.value.inv() or AccessFlags.PUBLIC.value
            val opened = getFreeRegisterProvider(0, 1).getFreeRegister()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p0, p1, p2, p3 }, $CUSTOM_THEME_CLASS->openEditor($OBJECT$OBJECT${OBJECT}Landroid/content/Context;)Z
                    move-result v$opened
                    if-nez v$opened, :done
                """,
                ExternalLabel("done", getInstruction(unitRead.index)),
            )
        }

        val themeCheck = ActivityThemeCheckFingerprint.match()
        val (_, _, optionCompare, recreateWindow) = themeCheck.instructionMatches
        themeCheck.method.apply {
            val outdated = getFreeRegisterProvider(optionCompare.index, 1).getFreeRegister()
            addInstructionsWithLabels(
                optionCompare.index,
                """
                    invoke-static { p0 }, $CUSTOM_THEME_CLASS->isOutdated(Landroid/app/Activity;)Z
                    move-result v$outdated
                    if-nez v$outdated, :recreate
                """,
                ExternalLabel("recreate", getInstruction(recreateWindow.index)),
            )
        }

        val overlay = ImmersiveThemeOverlayFingerprint.match()
        val (_, _, overlayResult) = overlay.instructionMatches
        overlay.method.apply {
            val overlayId = getInstruction<OneRegisterInstruction>(overlayResult.index).registerA
            val context = getFreeRegisterProvider(overlayResult.index + 1, 1).getFreeRegister()
            addInstructions(
                overlayResult.index + 1,
                """
                    invoke-virtual { p0 }, Landroidx/fragment/app/Fragment;->requireContext()Landroid/content/Context;
                    move-result-object v$context
                    invoke-static { v$context, v$overlayId }, $CUSTOM_THEME_CLASS->immersiveThemeOverlay(Landroid/content/Context;I)I
                    move-result v$overlayId
                """,
            )
        }

        val click = ThemeItemClickFingerprint.match()
        val (_, _, invokeCallback) = click.instructionMatches
        click.method.apply {
            val custom = getFreeRegisterProvider(0, 1).getFreeRegister()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p1 }, $CUSTOM_THEME_CLASS->isCustomOption($OBJECT)Z
                    move-result v$custom
                    if-nez v$custom, :open
                """,
                ExternalLabel("open", getInstruction(invokeCallback.index)),
            )
        }

        val setStatusBarTextColor = mutableClassDefBy(THEME_COLOR_OPTION).addLightThemeStatusBarTextColorSetter()

        mutableClassDefBy(CUSTOM_THEME_CLASS).apply {
            replaceStaticField("colorOption", THEME_COLOR_OPTION, CUSTOM_COLOR_OPTION)
            replaceStaticField("pictureOption", THEME_COLOR_OPTION, CUSTOM_PICTURE_OPTION)
            replaceStaticField("customThemeStyle", THEME_STYLE, CUSTOM_THEME_STYLE)
            replaceStub(
                "photoThemeOptions",
                1,
                """
                    sget-object v0, $THEME_CATEGORY->PHOTOS:$THEME_CATEGORY
                    invoke-static { v0 }, $THEME_COLOR_OPTION->getThemeOptionsByCategory($THEME_CATEGORY)Ljava/util/List;
                    move-result-object v0
                    return-object v0
                """,
            )
            replaceStub(
                "currentOption",
                1,
                """
                    invoke-static { p0 }, $COLOR_PALETTE_MANAGER->getThemeColorOption(Landroid/content/Context;)$THEME_COLOR_OPTION
                    move-result-object p0
                    return-object p0
                """,
            )
            replaceStub(
                "themeStyle",
                1,
                """
                    check-cast p0, $THEME_COLOR_OPTION
                    invoke-virtual { p0 }, $THEME_COLOR_OPTION->getThemeStyle()$THEME_STYLE
                    move-result-object p0
                    return-object p0
                """,
            )
            replaceStub(
                "accentColor",
                2,
                """
                    check-cast p0, $THEME_COLOR_OPTION
                    invoke-virtual { p0, p1 }, $THEME_COLOR_OPTION->getThemeAccentColor(Landroid/content/Context;)I
                    move-result p0
                    return p0
                """,
            )
            replaceStub("setPictureTheme", PICTURE_URIS + 1 + SET_PICTURE_THEME_PARAMETERS, setPictureThemeSmali())
            replaceStub(
                "setLightThemeStatusBarTextDark",
                2,
                """
                    check-cast p0, $THEME_COLOR_OPTION
                    invoke-static { p0, p1 }, $setStatusBarTextColor
                    return-void
                """,
            )
            replaceStub(
                "saveThemeOption",
                2,
                """
                    check-cast p1, $THEME_COLOR_OPTION
                    invoke-static { p0, p1 }, $COLOR_PALETTE_MANAGER->setThemeColorOption(Landroid/content/Context;$THEME_COLOR_OPTION)V
                    return-void
                """,
            )
            replaceStub(
                "selectTheme",
                4,
                """
                    check-cast p0, ${selectTheme.parameterTypes[0]}
                    check-cast p1, ${selectTheme.parameterTypes[1]}
                    check-cast p2, $THEME_COLOR_OPTION
                    invoke-static { p0, p1, p2, p3 }, ${descriptor(selectTheme)}
                    return-void
                """,
            )
            replaceStub("customPalette", TOKEN_MARKER_REGISTER + 1, customPaletteSmali())
            replaceStub("colorPairs", 3, colorPairsSmali())
            replaceIntStub("referenceColorId", resourceId(ResourceType.COLOR, REFERENCE_COLOR))
            replaceIntStub("prominentDialogStyleId", customThemeResources.styleIds.getValue(PROMINENT_DIALOG_OVERLAY))
            replaceResourceIdStubs(
                ResourceType.DRAWABLE,
                mapOf("colorIconId" to "ic_fluent_color_24_regular", "pictureIconId" to "ic_fluent_image_add_24_regular"),
            )
            replaceResourceIdStubs(
                ResourceType.STYLE,
                mapOf("whiteToolbarOverlayId" to WHITE_TOOLBAR_OVERLAY, "darkToolbarOverlayId" to DARK_TOOLBAR_OVERLAY),
            )
        }
    }
}

private fun MutableClass.addLightThemeStatusBarTextColorSetter(): String {
    val field = fields.single { it.type == STATUS_BAR_TEXT_COLOR }.name
    return addStaticMethod(
        "hxSetLightThemeStatusBarTextColor",
        listOf(type, "Z"),
        "V",
        3,
        """
            sget-object v0, $STATUS_BAR_TEXT_COLOR->LIGHT:$STATUS_BAR_TEXT_COLOR
            if-eqz p1, :write
            sget-object v0, $STATUS_BAR_TEXT_COLOR->DARK:$STATUS_BAR_TEXT_COLOR
            :write
            iput-object v0, p0, $type->$field:$STATUS_BAR_TEXT_COLOR
            return-void
        """,
    )
}

private fun MutableClass.addEnumField(name: String) = fields.add(
    ImmutableField(
        type,
        name,
        type,
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.FINAL.value or AccessFlags.ENUM.value,
        null,
        null,
        null,
    ).toMutable(),
)

private fun MutableClass.addCustomThemeStyle() {
    addEnumField(CUSTOM_THEME_STYLE)
    val overlay = customThemeResources.styleIds.getValue(CUSTOMIZE_OVERLAY)
    val highContrastOverlay = customThemeResources.styleIds.getValue(CUSTOMIZE_OVERLAY_HIGH_CONTRAST)
    val addValues = addStaticMethod(
        "hxAddCustomThemeStyle",
        listOf("[$type"),
        "[$type",
        10,
        """
            array-length v0, p0
            add-int/lit8 v1, v0, 0x1
            new-array v1, v1, [$type
            const/4 v2, 0x0
            invoke-static { p0, v2, v1, v2, v0 }, Ljava/lang/System;->arraycopy(${OBJECT}I${OBJECT}II)V
            new-instance v2, $type
            const-string v3, "$CUSTOM_THEME_STYLE"
            move v4, v0
            const v5, $overlay
            const v6, $highContrastOverlay
            move v7, v5
            move v8, v6
            invoke-direct/range { v2 .. v8 }, $type-><init>(Ljava/lang/String;IIIII)V
            sput-object v2, $type->$CUSTOM_THEME_STYLE:$type
            aput-object v2, v1, v0
            return-object v1
        """,
    )
    appendValues(ThemeStyleInitializerFingerprint.match(), addValues)
}

private fun MutableClass.addCustomThemeOptions() {
    addEnumField(CUSTOM_COLOR_OPTION)
    addEnumField(CUSTOM_PICTURE_OPTION)
    val constructor = "$type-><init>(Ljava/lang/String;II$THEME_CATEGORY$THEME_ICON_SUPPLIER$THEME_ASSET_FILE_NAMES)V"
    val addValues = addStaticMethod(
        "hxAddCustomThemeOptions",
        listOf("[$type"),
        "[$type",
        10,
        """
            array-length v0, p0
            add-int/lit8 v1, v0, 0x2
            new-array v1, v1, [$type
            const/4 v2, 0x0
            invoke-static { p0, v2, v1, v2, v0 }, Ljava/lang/System;->arraycopy(${OBJECT}I${OBJECT}II)V
            new-instance v2, $type
            const-string v3, "$CUSTOM_COLOR_OPTION"
            move v4, v0
            const v5, ${customThemeResources.stringIds.getValue(COLOR_NAME)}
            sget-object v6, $THEME_CATEGORY->STANDARD:$THEME_CATEGORY
            invoke-static { }, $CUSTOM_THEME_CLASS->colorIcon()$OBJECT
            move-result-object v7
            check-cast v7, $THEME_ICON_SUPPLIER
            const/4 v8, 0x0
            invoke-direct/range { v2 .. v8 }, $constructor
            sget-object v3, $THEME_STYLE->$CUSTOM_THEME_STYLE:$THEME_STYLE
            iput-object v3, v2, $type->themeStyle:$THEME_STYLE
            sput-object v2, $type->$CUSTOM_COLOR_OPTION:$type
            aput-object v2, v1, v0
            new-instance v2, $type
            const-string v3, "$CUSTOM_PICTURE_OPTION"
            add-int/lit8 v4, v0, 0x1
            const v5, ${customThemeResources.stringIds.getValue(PICTURE_NAME)}
            sget-object v6, $THEME_CATEGORY->PHOTOS:$THEME_CATEGORY
            invoke-static { }, $CUSTOM_THEME_CLASS->pictureIcon()$OBJECT
            move-result-object v7
            check-cast v7, $THEME_ICON_SUPPLIER
            const/4 v8, 0x0
            invoke-direct/range { v2 .. v8 }, $constructor
            sput-object v2, $type->$CUSTOM_PICTURE_OPTION:$type
            aput-object v2, v1, v4
            return-object v1
        """,
    )
    appendValues(ThemeColorOptionInitializerFingerprint.match(), addValues)
}

private fun appendValues(initializer: Match, addValues: String) {
    val (_, valuesWrite) = initializer.instructionMatches
    initializer.method.apply {
        val values = getInstruction<OneRegisterInstruction>(valuesWrite.index).registerA
        addInstructions(
            valuesWrite.index,
            """
                invoke-static { v$values }, $addValues
                move-result-object v$values
            """,
        )
    }
}

private fun MutableClass.replaceStaticField(stub: String, owner: String, field: String) = replaceStub(
    stub,
    1,
    """
        sget-object v0, $owner->$field:$owner
        return-object v0
    """,
)

private fun MutableClass.replaceIntStub(stub: String, value: Long) = replaceStub(
    stub,
    1,
    """
        const v0, $value
        return v0
    """,
)

private fun setPictureThemeSmali() = buildString {
    appendLine("new-instance v0, $THEME_ASSET_DATA")
    appendLine("invoke-direct { v0 }, $THEME_ASSET_DATA-><init>()V")
    appendLine("const/4 v1, 0x0")
    (2..CHROME_URIS).forEach { appendLine("move-object v$it, v1") }
    (CHROME_URIS + 1..PICTURE_URIS).forEach {
        val isLightModeBackground = (it - CHROME_URIS) % 2 == 1
        val pictureRegister = if (isLightModeBackground) "p2" else "p3"
        appendLine("move-object/from16 v$it, $pictureRegister")
    }
    appendLine("invoke-virtual/range { v0 .. v$PICTURE_URIS }, $THEME_ASSET_DATA->mergeAssets(${URI.repeat(PICTURE_URIS)})V")
    appendLine("move-object/from16 v1, p0")
    appendLine("check-cast v1, $THEME_COLOR_OPTION")
    appendLine("move-object/from16 v2, p1")
    appendLine("invoke-virtual { v1, v2, v0 }, $THEME_COLOR_OPTION->setThemeAssets(Landroid/content/Context;$THEME_ASSET_DATA)V")
    appendLine("move-object/from16 v3, p4")
    appendLine("check-cast v3, $THEME_STYLE")
    appendLine("const/4 v4, 0x0")
    appendLine("invoke-virtual { v1, v2, v3, v4 }, $THEME_COLOR_OPTION->setThemeProperties(Landroid/content/Context;${THEME_STYLE}I)V")
    appendLine("return-void")
}

private fun tokenGettersInConstructorOrder(): List<Method> {
    val tokens = classDefBy(TOKENS_CLASS)
    val constructor = tokens.methods.single { method ->
        method.name == "<init>" && method.parameterTypes.size == TOKEN_COUNT && method.parameterTypes.all { it == "J" }
    }
    val implementation = constructor.implementation!!
    val firstParameter = implementation.registerCount - 2 * TOKEN_COUNT
    val parameterByRegister = (0 until TOKEN_COUNT).associateBy { firstParameter + 2 * it }.toMutableMap()
    val parameterByField = mutableMapOf<String, Int>()
    implementation.instructions.forEach { instruction ->
        when (instruction.opcode) {
            Opcode.MOVE_WIDE, Opcode.MOVE_WIDE_FROM16, Opcode.MOVE_WIDE_16 -> {
                val move = instruction as TwoRegisterInstruction
                parameterByRegister[move.registerA] = parameterByRegister.getValue(move.registerB)
            }
            Opcode.IPUT_WIDE -> {
                val field = ((instruction as ReferenceInstruction).reference as FieldReference).name
                parameterByField[field] = parameterByRegister.getValue((instruction as TwoRegisterInstruction).registerA)
            }
            else -> Unit
        }
    }
    val getters = tokens.methods
        .filter { it.name.startsWith(TOKEN_GETTER_PREFIX) && it.returnType == "J" && it.parameterTypes.isEmpty() }
        .associateBy { getter ->
            val field = getter.implementation!!.instructions
                .singleOrNull { it.opcode == Opcode.IGET_WIDE }
                ?.let { ((it as ReferenceInstruction).reference as FieldReference).name }
            parameterByField[field] ?: throw PatchException("${getter.name} reads no constructor field")
        }
    if (getters.keys != (0 until TOKEN_COUNT).toSet()) {
        throw PatchException("Expected $TOKEN_COUNT color tokens, found ${getters.keys.sorted()}")
    }
    return getters.toSortedMap().values.toList()
}

private fun customPaletteSmali(): String {
    val getters = tokenGettersInConstructorOrder()
    return buildString {
        appendLine("sget-object v0, $PALETTE_CLASS->INSTANCE:$PALETTE_CLASS")
        appendLine("invoke-virtual { v0 }, $PALETTE_CLASS->getBlueColorTokens()$TOKENS_CLASS")
        appendLine("move-result-object v0")
        getters.forEachIndexed { index, getter ->
            val register = FIRST_TOKEN_REGISTER + 2 * index
            appendLine("invoke-virtual { v0 }, ${descriptor(getter)}")
            appendLine("move-result-wide v$register")
            appendLine("invoke-static/range { v$register .. v${register + 1} }, $CUSTOM_THEME_CLASS->transformPackedColor(J)J")
            appendLine("move-result-wide v$register")
        }
        appendLine("new-instance v1, $TOKENS_CLASS")
        appendLine("const/16 v$TOKEN_MARKER_REGISTER, 0x0")
        appendLine("invoke-direct/range { v1 .. v$TOKEN_MARKER_REGISTER }, $TOKENS_CLASS-><init>(${"J".repeat(TOKEN_COUNT)}Lkotlin/jvm/internal/DefaultConstructorMarker;)V")
        appendLine("new-instance v0, $COMPOSE_THEME_CLASS")
        appendLine("invoke-direct { v0, v1 }, $COMPOSE_THEME_CLASS-><init>($TOKENS_CLASS)V")
        appendLine("return-object v0")
    }
}

private fun colorPairsSmali() = buildString {
    val pairs = customThemeResources.colorPairs
    appendLine("const/16 v0, ${pairs.size * 2}")
    appendLine("new-array v0, v0, [I")
    pairs.forEachIndexed { index, (stockId, customId) ->
        appendLine("const v1, $stockId")
        appendLine("const/16 v2, ${index * 2}")
        appendLine("aput v1, v0, v2")
        appendLine("const v1, $customId")
        appendLine("const/16 v2, ${index * 2 + 1}")
        appendLine("aput v1, v0, v2")
    }
    appendLine("return-object v0")
}
