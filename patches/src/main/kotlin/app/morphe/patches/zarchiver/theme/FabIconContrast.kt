package app.morphe.patches.zarchiver.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val ICON_STYLE_REGISTER_COUNT = 3
private const val OPAQUE_WHITE = -1

internal fun fixFabIconContrast() {
    val fab = FabBackgroundColorFingerprint.match()
    val (_, colorRead) = fab.instructionMatches
    val readColor = colorRead.instruction.getReference<MethodReference>()!!
    val fabMenu = FabMenuConstructorFingerprint.match()
    val primaryColor = resourceId(ResourceType.ATTR, PRIMARY_COLOR_ATTR)
    val darkStyle = resourceId(ResourceType.STYLE, DARK_STYLE)
    val lightStyle = resourceId(ResourceType.STYLE, "AppTheme.Light")
    val iconStyle = mutableClassDefBy(fabMenu.classDef).addStaticMethod(
        name = "hxFabIconStyle",
        parameters = listOf("Landroid/view/View;"),
        returnType = "I",
        registers = ICON_STYLE_REGISTER_COUNT,
        smali = """
            invoke-virtual {p0}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v0
            const v1, $primaryColor
            invoke-static {v0, v1}, $readColor
            move-result v0
            const/4 v1, $OPAQUE_WHITE
            if-ne v0, v1, :use_dark_style
            const v0, $lightStyle
            return v0
            :use_dark_style
            const v0, $darkStyle
            return v0
        """,
    )

    listOf(
        fabMenu,
        fabIconSetterFingerprint().match(fabMenu.classDef),
        fabIconSetterFingerprint().match(fab.classDef),
        FabSubmenuIconFingerprint.match(),
    ).forEach { match ->
        val (resourcesRead, _, _, _, styleLoad) = match.instructionMatches
        val viewRegister = resourcesRead.getInstruction<FiveRegisterInstruction>().registerC
        val styleRegister = styleLoad.getInstruction<OneRegisterInstruction>().registerA
        match.method.replaceInstruction(styleLoad.index, "invoke-static/range {v$viewRegister .. v$viewRegister}, $iconStyle")
        match.method.addInstruction(styleLoad.index + 1, "move-result v$styleRegister")
    }
}
