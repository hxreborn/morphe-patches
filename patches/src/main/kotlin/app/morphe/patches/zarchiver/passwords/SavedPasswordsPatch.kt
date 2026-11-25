package app.morphe.patches.zarchiver.passwords

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.zarchiverSavedPasswordsExtensionPatch
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val BUILDER = "Landroid/app/AlertDialog\$Builder;"
private const val VIEW = "Landroid/view/View;"
private const val SAVED_PASSWORDS = "Lapp/hxreborn/extension/zarchiver/passwords/SavedPasswords;"

@Suppress("unused")
val savedPasswordsPatch = patch(
    name = "Saved passwords",
    description = "Saves and reuses archive passwords. Requires Android 6.0 or later.",
) {
    compatibleWith(AppCompatibilities.ZARCHIVER)
    dependsOn(zarchiverSavedPasswordsExtensionPatch)

    execute {
        mutableClassDefBy(SAVED_PASSWORDS).replaceResourceIdStubs(
            ResourceType.ID,
            mapOf("passwordPromptInputId" to "edt_text", "archiveOptionsInputId" to "edt_password"),
        )
        listOf(
            PasswordPromptFingerprint.match() to "setPasswordPrompt",
            CompressionOptionsFingerprint.match() to "setArchiveOptions",
        ).forEach { (match, entryPoint) ->
            val (_, setView) = match.instructionMatches
            val call = setView.getInstruction<FiveRegisterInstruction>()
            match.method.replaceInstruction(
                setView.index,
                "invoke-static {v${call.registerC}, v${call.registerD}}, $SAVED_PASSWORDS->$entryPoint($BUILDER$VIEW)$BUILDER",
            )
        }
        val archiveFileOptions = ArchiveFileOptionsFingerprint.match()
        val (_, setView) = archiveFileOptions.instructionMatches
        val call = setView.getInstruction<FiveRegisterInstruction>()
        archiveFileOptions.method.replaceInstruction(
            setView.index,
            "invoke-static {v${call.registerC}, v${call.registerD}, p4}, " +
                "$SAVED_PASSWORDS->setArchiveFileOptions($BUILDER${VIEW}Z)$BUILDER",
        )
    }
}
