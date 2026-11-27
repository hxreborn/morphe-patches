package app.morphe.patches.zarchiver.theme

import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceMethod
import app.morphe.util.findElementByAttributeValueOrThrow
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal const val THEME_PREFERENCE_KEY = "iTheme"
internal const val DEFAULT_THEME_INDEX = 1L
internal const val ACCENT_INDEX_MASK = 0xff
private const val THEME_INDEX_MASK = 0xf00
private const val THEME_INDEX_SHIFT = 8
private const val THEME_STYLE_REGISTER_COUNT = 3

private val accentNames = listOf(
    "Pink", "Purple", "DeepPurple", "Indigo", "Blue", "LightBlue", "Cyan", "Teal", "Green", "LightGreen",
    "Lime", "Yellow", "Amber", "Orange", "DeepOrange", "Brown", "Grey", "BlueGrey", "DarkGrey",
)

private val themeNames = listOf("Dark", "Light", "Light.DarkActionBar", "Dark.Amoled", "DayNight", "Light.EInk")

private fun themeStyles(theme: String): List<Long> {
    val baseStyle = "AppTheme.$theme"
    val accentStyle = if (theme == "DayNight") baseStyle else "$baseStyle.Material"
    val styles = listOf(baseStyle, "AppTheme.Dynamic.$theme") + accentNames.map { "$accentStyle.$it" }
    return styles.map { resourceId(ResourceType.STYLE, it) }
}

@Suppress("unused")
val unlockThemesPatch = patch(
    name = "Unlock themes",
    description = "Enables six color themes in settings.",
) {
    compatibleWith(AppCompatibilities.ZARCHIVER)

    execute {
        val themePreference = ThemePreferenceFingerprint.match()
        val (defaultTheme, _, eInkTheme) = themePreference.instructionMatches.asReversed()
        themePreference.method.removeInstructions(eInkTheme.index, defaultTheme.index - eInkTheme.index + 1)

        val themeStyle = ThemeStyleFingerprint.match(themePreference.classDef)
        val (themeSettingsFieldRead) = themeStyle.instructionMatches
        val themeSettingsField = themeSettingsFieldRead.instruction.getReference<FieldReference>()!!
        val stylesByTheme = themeNames.map(::themeStyles)
        val accentCount = stylesByTheme.first().size
        val styleIds = stylesByTheme.flatten()
        mutableClassDefBy(themeStyle.classDef).replaceMethod(
            name = themeStyle.method.name,
            registers = THEME_STYLE_REGISTER_COUNT,
            smali = """
                sget v0, $themeSettingsField
                and-int/lit16 v1, v0, $ACCENT_INDEX_MASK
                and-int/lit16 v0, v0, $THEME_INDEX_MASK
                ushr-int/lit8 v0, v0, $THEME_INDEX_SHIFT
                if-lez v0, :default_theme
                const/4 v2, ${themeNames.size}
                if-le v0, v2, :theme_selected
                :default_theme
                const/4 v0, $DEFAULT_THEME_INDEX
                :theme_selected
                add-int/lit8 v0, v0, -1
                const/16 v2, $accentCount
                if-lt v1, v2, :accent_selected
                const/4 v1, 0x0
                :accent_selected
                mul-int/2addr v0, v2
                add-int/2addr v0, v1
                const/16 v1, ${styleIds.size}
                new-array v1, v1, [I
                fill-array-data v1, :theme_styles
                aget v0, v1, v0
                return v0
                :theme_styles
                .array-data 4
                    ${styleIds.joinToString("\n    ")}
                .end array-data
            """,
        )

        for (directory in listOf("xml", "xml-v21", "xml-v33")) {
            document("res/$directory/pref_gui.xml").use { document ->
                val preference = document.getElementsByTagName("ListPreference")
                    .findElementByAttributeValueOrThrow("android:key", THEME_PREFERENCE_KEY)
                preference.removeAttribute("android:enabled")
                preference.setAttribute("android:entryValues", "@array/OPT_LIST_THEME_VALUE")
                preference.setAttribute("android:summary", "%s")
                preference.setAttribute("android:dialogTitle", "@string/OPT_THEME")
            }
        }
        fixFabIconContrast()
    }
}
