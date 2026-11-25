package app.morphe.patches.zarchiver.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val DARK_STYLE = "AppTheme.Dark"
internal const val PRIMARY_COLOR_ATTR = "colorPrimary"
private const val APPLY_FAB_STYLE_WINDOW = 2
private const val E_INK_THEME_INDEX = 6L

internal object ThemePreferenceFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    filters = listOf(
        string(THEME_PREFERENCE_KEY),
        methodCall(
            definingClass = "Landroid/content/SharedPreferences;",
            name = "getString",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("I", "Ljava/lang/String;"),
            returnType = "I",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.ADD_INT_2ADDR, location = MatchAfterImmediately()),
        literal(E_INK_THEME_INDEX, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQ, location = MatchAfterImmediately()),
        literal(DEFAULT_THEME_INDEX, location = MatchAfterImmediately()),
    ),
)

internal object ThemeStyleFingerprint : Fingerprint(
    isStatic = true,
    returnType = "I",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET, definingClass = "this", type = "I"),
        literal(
            ACCENT_INDEX_MASK.toLong(),
            opcodes = listOf(Opcode.AND_INT_LIT16),
            location = MatchAfterImmediately(),
        ),
        fieldAccess(opcode = Opcode.SGET_OBJECT, definingClass = "this", type = "[I"),
        opcode(Opcode.AGET, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
        resourceLiteral(ResourceType.STYLE, DARK_STYLE),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
    ),
)

internal object FabBackgroundColorFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Landroid/content/Context;", "Z"),
    filters = listOf(
        resourceLiteral(ResourceType.ATTR, PRIMARY_COLOR_ATTR),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Landroid/content/Context;", "I"),
            returnType = "I",
            location = MatchAfterImmediately(),
        ),
        resourceLiteral(ResourceType.ATTR, "colorPrimaryLight"),
    ),
)

private val fabIconStyleFilters = listOf(
    methodCall(definingClass = "Landroid/view/View;", name = "getResources", opcode = Opcode.INVOKE_VIRTUAL),
    opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    methodCall(definingClass = "Landroid/content/res/Resources;", name = "newTheme", location = MatchAfterImmediately()),
    opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    resourceLiteral(ResourceType.STYLE, DARK_STYLE, location = MatchAfterImmediately()),
    methodCall(
        definingClass = "Landroid/content/res/Resources\$Theme;",
        name = "applyStyle",
        location = MatchAfterWithin(APPLY_FAB_STYLE_WINDOW),
    ),
)

internal object FabMenuConstructorFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;"),
    filters = fabIconStyleFilters + resourceLiteral(ResourceType.DRAWABLE, "l_add"),
)

internal fun fabIconSetterFingerprint() = Fingerprint(
    isStatic = false,
    parameters = listOf("I"),
    returnType = "V",
    filters = fabIconStyleFilters,
)

internal object FabSubmenuIconFingerprint : Fingerprint(
    isStatic = false,
    parameters = listOf("I", "I"),
    returnType = "V",
    filters = fabIconStyleFilters,
)
