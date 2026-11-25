package app.morphe.patches.zarchiver.passwords

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral

private const val BUILDER = "Landroid/app/AlertDialog\$Builder;"
private const val VIEW = "Landroid/view/View;"

private val setDialogView = methodCall(definingClass = BUILDER, name = "setView", parameters = listOf(VIEW))

internal object PasswordPromptFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(resourceLiteral(ResourceType.LAYOUT, "dlg_enter_pwd"), setDialogView),
)

internal object CompressionOptionsFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "dlg_compress"),
        setDialogView,
        resourceLiteral(ResourceType.ID, "edt_password"),
    ),
)

internal object ArchiveFileOptionsFingerprint : Fingerprint(
    isStatic = false,
    parameters = listOf("Landroid/content/Context;", "I", "Z", "Z"),
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "dlg_select_file"),
        setDialogView,
        resourceLiteral(ResourceType.ID, "cb_encrypt"),
    ),
)
