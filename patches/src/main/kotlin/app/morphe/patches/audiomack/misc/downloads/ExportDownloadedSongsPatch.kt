/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audiomack.misc.downloads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.audiomackExportDownloadsExtensionPatch
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/audiomack/ExportActions;"
private const val ROW_CLASS = "Lapp/hxreborn/extension/audiomack/ExportRow;"
private const val MAX_INVOKE_REGISTER = 15

private val exportRowLabelsPatch = patch {
    execute {
        document("res/values/strings.xml").use { document ->
            mapOf(
                "hx_audiomack_save_to_device" to "Save to device",
                "hx_audiomack_share_file" to "Share file",
            ).forEach { (name, value) ->
                val text = document.createElement("string")
                text.setAttribute("name", name)
                text.setAttribute("translatable", "false")
                text.textContent = value
                document.documentElement.appendChild(text)
            }
        }
    }
}

@Suppress("unused")
val exportDownloadedSongsPatch: Patch = patch(
    name = "Export downloaded songs",
    description = "Adds Save to device and Share file to the menu of downloaded songs. " +
        "Saves songs to Music/Audiomack as Artist - Title, " +
        "with title, artist, album and cover art tags. " +
        "Requires Android 10 or later.",
) {
    compatibleWith(AppCompatibilities.AUDIOMACK)
    dependsOn(audiomackExportDownloadsExtensionPatch, exportRowLabelsPatch)

    execute {
        ApplicationCreateFingerprint.method.addInstruction(
            0,
            "invoke-static {p0}, $EXTENSION_CLASS->init(Landroid/content/Context;)V",
        )

        val artistMatch = MediaMetadataArtistFingerprint.match()
        val (idRead, _, artistRead) = artistMatch.instructionMatches
        val (albumRead, _, albumFallbackRead) =
            mediaMetadataAlbumFingerprint(artistMatch.method).match().instructionMatches
        val (titleRead) = mediaMetadataTitleFingerprint(artistMatch.method).match().instructionMatches
        val (coverUrlRead) =
            originalImageCopyFingerprint(OriginalImageSetterFingerprint.method).match().instructionMatches
        val coverFileMethod = CoverFileFingerprint.method
        val downloadFolder = DownloadFolderFingerprint.matchAll(DOWNLOAD_FOLDER_SITES).map { match ->
            val (_, _, folderNameRead) = match.instructionMatches
            folderNameRead.getInstruction<ReferenceInstruction>().getReference<StringReference>()!!.string
        }.distinct().singleOrNull() ?: throw PatchException("The download folder sites disagree on the folder name")

        mutableClassDefBy(EXTENSION_CLASS).apply {
            mapOf(
                "musicId" to idRead.fieldReference,
                "musicTitle" to titleRead.fieldReference,
                "musicArtist" to artistRead.fieldReference,
                "musicCoverUrl" to coverUrlRead.fieldReference,
            ).forEach { (stub, field) ->
                replaceStub(
                    stub,
                    2,
                    """
                        check-cast p0, $MUSIC_CLASS
                        iget-object v0, p0, ${descriptor(field)}
                        return-object v0
                    """,
                )
            }
            replaceStub(
                "musicAlbum",
                2,
                """
                    check-cast p0, $MUSIC_CLASS
                    iget-object v0, p0, ${descriptor(albumRead.fieldReference)}
                    if-nez v0, :album
                    iget-object v0, p0, ${descriptor(albumFallbackRead.fieldReference)}
                    :album
                    return-object v0
                """,
            )
            replaceStub(
                "coverFile",
                3,
                """
                    if-eqz p1, :no_cover_url
                    const/4 v0, 0x0
                    invoke-static {p0, p1, v0}, ${descriptor(coverFileMethod)}
                    move-result-object v0
                    return-object v0
                    :no_cover_url
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            replaceStub(
                "downloadFolderName",
                1,
                """
                    const-string v0, "$downloadFolder"
                    return-object v0
                """,
            )
            replaceResourceIdStubs(
                ResourceType.DRAWABLE,
                mapOf("saveIcon" to "ic_music_menu_download", "shareIcon" to "ic_share"),
            )
        }

        val downloadRowType = DownloadRowLabelFingerprint.classDef.type
        val rowBaseType = DownloadRowLabelFingerprint.classDef.superclass
            ?: throw PatchException("The download row has no superclass")
        val rowBase = classDefBy(rowBaseType)
        fun abstractMethodName(returnType: String) = rowBase.methods.single {
            AccessFlags.ABSTRACT.isSet(it.accessFlags) && it.returnType == returnType && it.parameterTypes.isEmpty()
        }.name

        mutableClassDefBy(ROW_CLASS).apply {
            setSuperClass(rowBaseType)
            methods.single { it.name == "label" }.setName(abstractMethodName("I"))
            methods.single { it.name == "disabled" }.setName(abstractMethodName("Z"))
            val constructor = methods.single { it.name == "<init>" }
            val objectConstructorCall = constructor.indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.let {
                    it.definingClass == "Ljava/lang/Object;" && it.name == "<init>"
                } == true
            }
            constructor.replaceInstruction(objectConstructorCall, "invoke-direct {p0, p1}, ${rowBaseType}-><init>(I)V")
        }

        actionRowsFingerprint(downloadRowType).match().let { match ->
            val (musicRead, rowInit, rowAdd) = match.instructionMatches
            val music = musicRead.getInstruction<TwoRegisterInstruction>().registerB
            val downloaded = rowInit.getInstruction<FiveRegisterInstruction>().registerD
            val rows = rowAdd.getInstruction<FiveRegisterInstruction>().registerC
            if (maxOf(music, downloaded, rows) > MAX_INVOKE_REGISTER) {
                throw PatchException("Download row registers exceed the invoke-static encoding limit")
            }
            match.method.addInstruction(
                rowAdd.index + 1,
                "invoke-static {v${rows}, v${music}, v${downloaded}}, " +
                    "$EXTENSION_CLASS->addRows(Ljava/util/List;Ljava/lang/Object;Z)V",
            )
        }

        actionClickFingerprint(rowBaseType).match().let { match ->
            val (rowRead, _, unitRead) = match.instructionMatches
            val row = rowRead.getInstruction<TwoRegisterInstruction>().registerA
            val handled = match.method.getFreeRegisterProvider(rowRead.index + 1, 1, row).getFreeRegister()
            if (maxOf(row, handled) > MAX_INVOKE_REGISTER) {
                throw PatchException("Row click registers exceed the invoke-static encoding limit")
            }
            match.method.addInstructionsWithLabels(
                rowRead.index + 1,
                """
                    invoke-static {v${row}}, $EXTENSION_CLASS->onRowClick(Ljava/lang/Object;)Z
                    move-result v${handled}
                    if-eqz v${handled}, :stock_row
                    sget-object v${handled}, ${descriptor(unitRead.fieldReference)}
                    return-object v${handled}
                    :stock_row
                    nop
                """,
            )
        }
    }
}
