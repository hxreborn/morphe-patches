/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.addAppliedPatchMetadata
import app.morphe.patches.shared.misc.extension.outlookSettingsExtensionPatch
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.parameterRegisterCount
import app.morphe.patches.shared.misc.smali.registerWidth
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

internal const val OUTLOOK_PATCH_SETTINGS_CLASS = "Lapp/hxreborn/extension/outlook/settings/PatchSettings;"
private const val OUTLOOK_APPLIED_PATCHES_CLASS = "Lapp/hxreborn/extension/outlook/AppliedPatches;"
private const val PATCH_CONTEXT_CLASS = "Lapp/hxreborn/extension/outlook/settings/PatchContext;"
private const val PATCHES_MENU_CLASS = "Lapp/hxreborn/extension/outlook/settings/PatchesMenu;"
private const val SETTINGS_STYLE_CLASS = "Lapp/hxreborn/extension/outlook/settings/SettingsStyle;"
private const val SETTINGS_ROW_TITLE_FACTORY = "$PATCHES_MENU_CLASS->settingsRowTitle(Ljava/lang/Class;)Ljava/lang/Object;"
private const val SETTINGS_ROW_ICON_FACTORY = "$PATCHES_MENU_CLASS->settingsRowIcon(Ljava/lang/Class;)Ljava/lang/Object;"
private const val SETTINGS_ROW_PATH = "settings/hxreborn_patches"
private const val ENTRY_ICON = "ic_fluent_puzzle_piece_24_regular"

private const val CATEGORY_FIELD = "category"
private const val TITLE_FIELD = "titleString"
private const val ICON_FIELD = "icon"
private const val DEEP_LINK_FIELD = "deepLinkUri"
private const val SEARCH_TERMS_FIELD = "searchTerms"
private const val DEFAULT_CONSTRUCTOR_TRAILING_PARAMETERS = 2
private const val COMPOSABLE_TRAILING_PARAMETERS = 3

private val logger = Logger.getLogger("app.morphe.patches.outlook.misc.settings")

internal fun markPatchApplied(key: String, patch: Patch) =
    addAppliedPatchMetadata(OUTLOOK_APPLIED_PATCHES_CLASS, key, patch, withDescription = true)

private fun MethodReference.parameter(index: Int) = parameterTypes[index].toString()

private fun defaultMask(parameterCount: Int, provided: Collection<Int>) =
    (0 until parameterCount).filter { it !in provided }.fold(0) { mask, index -> mask or (1 shl index) }

private val INSTANCE_FIELD_WRITES = setOf(
    Opcode.IPUT,
    Opcode.IPUT_WIDE,
    Opcode.IPUT_OBJECT,
    Opcode.IPUT_BOOLEAN,
    Opcode.IPUT_BYTE,
    Opcode.IPUT_CHAR,
    Opcode.IPUT_SHORT,
)

private val REGISTER_MOVES = setOf(
    Opcode.MOVE,
    Opcode.MOVE_FROM16,
    Opcode.MOVE_16,
    Opcode.MOVE_WIDE,
    Opcode.MOVE_WIDE_FROM16,
    Opcode.MOVE_WIDE_16,
    Opcode.MOVE_OBJECT,
    Opcode.MOVE_OBJECT_FROM16,
    Opcode.MOVE_OBJECT_16,
)

private fun constructorParametersByField(constructor: Method): Map<String, Int> {
    val implementation = checkNotNull(constructor.implementation) { "${descriptor(constructor)} has no body" }
    var register = implementation.registerCount - constructor.parameterRegisterCount()
    val parameterOf = mutableMapOf<Int, Int>()
    constructor.parameterTypes.forEachIndexed { index, type ->
        parameterOf[++register] = index
        register += type.registerWidth() - 1
    }
    val parameterByField = mutableMapOf<String, Int>()
    implementation.instructions.forEach { instruction ->
        val reference = instruction.getReference<FieldReference>()
        if (reference != null && reference.definingClass == constructor.definingClass &&
            instruction.opcode in INSTANCE_FIELD_WRITES
        ) {
            parameterOf[(instruction as TwoRegisterInstruction).registerA]?.let { parameterByField[reference.name] = it }
            return@forEach
        }
        if (!instruction.opcode.setsRegister()) return@forEach
        val target = (instruction as OneRegisterInstruction).registerA
        val source = (instruction as? TwoRegisterInstruction)?.registerB?.takeIf { instruction.opcode in REGISTER_MOVES }
        val parameter = source?.let(parameterOf::get)
        if (parameter == null) parameterOf.remove(target) else parameterOf[target] = parameter
    }
    return parameterByField
}

private fun settingsRowSmali(
    rowConstructor: MethodReference,
    category: FieldReference,
    searchExclusion: String,
): Pair<Int, String> {
    val parameterCount = rowConstructor.parameterTypes.size - DEFAULT_CONSTRUCTOR_TRAILING_PARAMETERS
    val primaryConstructor = classDefBy(rowConstructor.definingClass).methods.singleOrNull {
        it.name == "<init>" && it.parameterTypes.map(CharSequence::toString) ==
            rowConstructor.parameterTypes.take(parameterCount).map(CharSequence::toString)
    } ?: throw PatchException("${rowConstructor.definingClass} has no constructor matching ${descriptor(rowConstructor)}")
    val parameterByField = constructorParametersByField(primaryConstructor)
    fun parameter(field: String) = parameterByField[field]
        ?: throw PatchException("${rowConstructor.definingClass} constructor stores no $field field")
    val categoryParameter = parameter(CATEGORY_FIELD)
    val titleParameter = parameter(TITLE_FIELD)
    val iconParameter = parameter(ICON_FIELD)
    val deepLinkParameter = parameter(DEEP_LINK_FIELD)
    val searchTermsParameter = parameter(SEARCH_TERMS_FIELD)
    if (rowConstructor.parameter(categoryParameter) != category.type) {
        throw PatchException("${rowConstructor.definingClass}.$CATEGORY_FIELD is not ${category.type}")
    }
    fun proxy(parameter: Int, factory: String): String {
        val type = rowConstructor.parameter(parameter)
        return """
            const-class v0, $type
            invoke-static {v0}, $factory
            move-result-object v0
            check-cast v0, $type
            move-object/from16 v${parameter + 1}, v0
        """
    }
    val arguments = (0 until parameterCount).joinToString("\n") { parameter ->
        val register = "v${parameter + 1}"
        when (parameter) {
            categoryParameter -> "sget-object $register, ${descriptor(category)}"
            titleParameter -> proxy(parameter, SETTINGS_ROW_TITLE_FACTORY)
            iconParameter -> proxy(parameter, SETTINGS_ROW_ICON_FACTORY)
            deepLinkParameter -> "const-string $register, \"$SETTINGS_ROW_PATH\""
            searchTermsParameter -> "invoke-static {}, $searchExclusion\nmove-result-object v0\nmove-object/from16 $register, v0"
            else -> "const/16 $register, 0x0"
        }
    }
    val mask = defaultMask(
        parameterCount,
        listOf(categoryParameter, titleParameter, iconParameter, deepLinkParameter, searchTermsParameter),
    )
    val lastRegister = parameterCount + DEFAULT_CONSTRUCTOR_TRAILING_PARAMETERS
    val smali = """
        $arguments
        const v${parameterCount + 1}, $mask
        const/16 v$lastRegister, 0x0
        new-instance v0, ${rowConstructor.definingClass}
        invoke-direct/range {v0 .. v$lastRegister}, ${descriptor(rowConstructor)}
        return-object v0
    """
    return (lastRegister + 1) to smali
}

private fun settingsRowIconSmali(iconCall: MethodReference): Pair<Int, String> {
    val composerParameter = iconCall.parameterTypes.size - COMPOSABLE_TRAILING_PARAMETERS
    var register = 0
    val arguments = iconCall.parameterTypes.mapIndexed { parameter, type ->
        val target = "v$register"
        register += type.registerWidth()
        when {
            parameter == 0 -> "const $target, ${resourceId(ResourceType.DRAWABLE, ENTRY_ICON)}"
            parameter == composerParameter -> "move-object $target, p0"
            parameter == composerParameter + 2 -> "const $target, ${defaultMask(composerParameter, setOf(0))}"
            type.registerWidth() == 2 -> "const-wide/16 $target, 0x0"
            else -> "const/4 $target, 0x0"
        }
    }.joinToString("\n")
    val smali = """
        check-cast p0, ${iconCall.parameter(composerParameter)}
        $arguments
        invoke-static/range {v0 .. v${register - 1}}, ${descriptor(iconCall)}
        return-void
    """
    return (register + 1) to smali
}

internal val patchesSettingsPatch = patch {
    dependsOn(outlookSettingsExtensionPatch)

    execute {
        OutlookApplicationCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $PATCH_CONTEXT_CLASS->attach(Landroid/app/Application;)V",
        )
        mutableClassDefBy(SETTINGS_STYLE_CLASS).replaceResourceIdStubs(
            ResourceType.DRAWABLE,
            mapOf(
                "backIconId" to "ic_fluent_arrow_left_24_regular",
                "unreadIconId" to "ic_fluent_mail_unread_24_regular",
                "adsIconId" to "ic_fluent_megaphone_24_regular",
                "templatesIconId" to "ic_fluent_document_24_regular",
                "searchIconId" to "ic_fluent_checkbox_checked_24_regular",
                "copilotIconId" to "ic_fluent_copilot_24_regular",
                "shortcutIconId" to "ic_fluent_apps_24_regular",
                "calendarTabIconId" to "ic_fluent_calendar_24_regular",
                "appsTabIconId" to "ic_fluent_grid_24_regular",
                "navigationBarIconId" to "ic_fluent_navigation_24_regular",
                "resetIconId" to "ic_fluent_arrow_reset_24_regular",
                "infoIconId" to "ic_fluent_info_24_regular",
                "patchesIconId" to ENTRY_ICON,
            ),
        )

        try {
            addSettingsRow()
        } catch (ex: PatchException) {
            logger.warning("Skipped the patches settings row: ${ex.message}")
        }
    }
}

private fun addSettingsRow() {
    val components = SettingsComponentsFingerprint.match()
    val (generalCategoryRead, _, rowConstructorCall, componentsReturn) = components.instructionMatches
    val generalCategory = generalCategoryRead.instruction.getReference<FieldReference>()!!
    val rowConstructor = rowConstructorCall.instruction.getReference<MethodReference>()!!
    val componentsRegister = componentsReturn.getInstruction<OneRegisterInstruction>().registerA

    val mailIcon = MailSettingsIconFingerprint.match()
    val (_, iconCall, unitRead) = mailIcon.instructionMatches
    val iconReference = iconCall.instruction.getReference<MethodReference>()!!
    val unit = unitRead.instruction.getReference<FieldReference>()!!

    val searchExclusion = descriptor(SettingsSearchExclusionFingerprint.originalMethod)

    val rowClick = SettingsRowClickFingerprint.match()
    val viewModelType = rowClick.originalMethod.parameterTypes[0].toString()
    val componentType = rowClick.originalMethod.parameterTypes[1].toString()
    if (rowClick.originalMethod.returnType != unit.type) {
        throw PatchException("Settings row click returns ${rowClick.originalMethod.returnType}, expected ${unit.type}")
    }
    val componentMethods = classDefBy(componentType).methods
    fun componentGetter(name: String) = componentMethods.singleOrNull { it.name == name }
        ?.let(::descriptor) ?: throw PatchException("$componentType has no single $name method")
    val categoryGetter = componentGetter("getCategory")
    val deepLinkGetter = componentGetter("getDeepLinkUri")

    val (rowRegisters, rowSmali) = settingsRowSmali(rowConstructor, generalCategory, searchExclusion)
    val (iconRegisters, iconSmali) = settingsRowIconSmali(iconReference)

    mutableClassDefBy(PATCHES_MENU_CLASS).apply {
        replaceStub("settingsRow", rowRegisters, rowSmali)
        replaceStub("drawSettingsRowIcon", iconRegisters, iconSmali)
        replaceStub(
            "unit",
            1,
            """
                sget-object v0, ${descriptor(unit)}
                return-object v0
            """,
        )
        replaceStub(
            "category",
            2,
            """
                check-cast p0, $componentType
                invoke-interface {p0}, $categoryGetter
                move-result-object v0
                return-object v0
            """,
        )
        replaceStub(
            "deepLinkUri",
            2,
            """
                check-cast p0, $componentType
                invoke-interface {p0}, $deepLinkGetter
                move-result-object v0
                return-object v0
            """,
        )
    }

    components.method.addInstructions(
        componentsReturn.index,
        """
            invoke-static/range { v$componentsRegister .. v$componentsRegister }, $PATCHES_MENU_CLASS->addSettingsRow(Ljava/util/List;)Ljava/util/List;
            move-result-object v$componentsRegister
        """,
    )

    rowClick.method.addInstructionsWithLabels(
        0,
        """
            invoke-static {p0, p1}, $PATCHES_MENU_CLASS->clickedViewModel(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object p0
            if-nez p0, :stock
            sget-object p0, ${descriptor(unit)}
            return-object p0
            :stock
            check-cast p0, $viewModelType
        """,
    )
}
