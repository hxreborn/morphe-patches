package app.morphe.patches.zarchiver.archives

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.zarchiverEditArchivesExtensionPatch
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EDITOR = "Lapp/hxreborn/extension/zarchiver/archives/ArchiveEntryEditing;"
private const val PROVIDER = "Landroid/content/ContentProvider;"
private const val UPDATE_ENTRY = "cUpdateFile"
private const val MIN_EDITOR_SDK = 23
private const val WRITE_MODE_CHARACTER = 'w'.code
private const val NATIVE_WRITE_REGISTERS = 5
private const val NATIVE_OPEN_REGISTERS = 4
private const val EDITOR_MODE_REGISTERS = 3
private const val OPEN_WRITER_REGISTERS = 11
private const val OPEN_ENTRY_STUB_REGISTERS = 3
private const val AUTHORITY_STUB_REGISTERS = 1
private const val BUNDLE = "Landroid/os/Bundle;"

@Suppress("unused")
val editArchiveFilesPatch = patch(
    name = "Edit files in archives",
    description = "Saves changes to files in local ZIP, 7z, and TAR archives. Requires Android 6.0 or later.",
) {
    compatibleWith(AppCompatibilities.ZARCHIVER)
    dependsOn(zarchiverEditArchivesExtensionPatch)

    execute {
        val provider = ArchiveEntryProviderFingerprint.match()
        val (_, metadata, pathInfo) = provider.instructionMatches
        val (pathRead, typeRead, passwordRead, archiveOpenCall) =
            ArchiveReadArgumentsFingerprint.match(provider.method).instructionMatches
        val path = pathRead.instruction.getReference<FieldReference>()!!
        val archiveType = typeRead.instruction.getReference<FieldReference>()!!
        val archivePassword = passwordRead.instruction.getReference<FieldReference>()!!
        val archiveOpen = archiveOpenCall.instruction.getReference<MethodReference>()!!

        val archiveClass = mutableClassDefBy(archiveOpen.returnType)
        val reads = ArchiveEntryStreamFingerprint.match(archiveClass)
        val (handleRead) = ArchiveNativeHandleFingerprint.match(archiveClass).instructionMatches
        val handle = handleRead.instruction.getReference<FieldReference>()!!
        archiveClass.methods.add(
            ImmutableMethod(
                archiveClass.type,
                UPDATE_ENTRY,
                listOf("J", "I", STRING).map { ImmutableMethodParameter(it, null, null) },
                "I",
                AccessFlags.PRIVATE.value or AccessFlags.STATIC.value or AccessFlags.NATIVE.value,
                null,
                null,
                null,
            ).toMutable(),
        )
        val replaceEntry = archiveClass.addStaticMethod(
            name = "hxReplaceArchiveEntry",
            parameters = listOf(archiveClass.type, "I", STRING),
            returnType = "I",
            registers = NATIVE_WRITE_REGISTERS,
            smali = """
                iget-wide v0, p0, $handle
                invoke-static {v0, v1, p1, p2}, ${archiveClass.type}->$UPDATE_ENTRY(JI$STRING)I
                move-result v0
                return v0
            """,
        )
        val openChecked = archiveClass.addStaticMethod(
            name = "hxOpenArchiveForEditing",
            parameters = listOf(STRING, STRING, STRING),
            returnType = archiveClass.type,
            registers = NATIVE_OPEN_REGISTERS,
            smali = """
                :try_start
                invoke-static {p0, p1, p2}, $archiveOpen
                move-result-object v0
                :try_end
                return-object v0
                .catch Ljava/lang/Exception; {:try_start .. :try_end} :failure
                :failure
                move-exception v0
                new-instance v0, Ljava/io/IOException;
                invoke-direct {v0}, Ljava/io/IOException;-><init>()V
                throw v0
            """,
        )
        mutableClassDefBy(EDITOR).apply {
            replaceStub("openArchive", NATIVE_OPEN_REGISTERS, """
                invoke-static {p0, p1, p2}, $openChecked
                move-result-object v0
                return-object v0
            """)
            replaceStub("openEntry", NATIVE_OPEN_REGISTERS, """
                :try_start
                check-cast p0, ${archiveClass.type}
                invoke-virtual {p0, p1, p2}, ${descriptor(reads.method)}
                move-result-object v0
                :try_end
                return-object v0
                .catch Ljava/lang/Exception; {:try_start .. :try_end} :failure
                :failure
                move-exception v0
                new-instance v0, Ljava/io/IOException;
                invoke-direct {v0}, Ljava/io/IOException;-><init>()V
                throw v0
            """)
            replaceStub("replaceEntry", OPEN_ENTRY_STUB_REGISTERS, """
                check-cast p0, ${archiveClass.type}
                invoke-static {p0, p1, p2}, $replaceEntry
                move-result p0
                return p0
            """)
            replaceStub("archiveAuthority", AUTHORITY_STUB_REGISTERS, """
                const-string v0, "${AppCompatibilities.ZARCHIVER.packageName}.archive"
                return-object v0
            """)
        }
        val providerClass = mutableClassDefBy(provider.classDef)
        val isWriteMode = providerClass.addStaticMethod(
            name = "hxIsArchiveWriteMode",
            parameters = listOf(STRING),
            returnType = "Z",
            registers = EDITOR_MODE_REGISTERS,
            smali = """
                sget v0, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
                const/16 v1, $MIN_EDITOR_SDK
                if-lt v0, v1, :stock_mode
                const/16 v0, $WRITE_MODE_CHARACTER
                invoke-virtual {p0, v0}, $STRING->indexOf(I)I
                move-result v0
                if-ltz v0, :stock_mode
                const/4 v0, 0x1
                return v0
                :stock_mode
                const/4 v0, 0x0
                return v0
            """,
        )
        val writer = providerClass.addStaticMethod(
            name = "hxOpenArchiveEntryForWrite",
            parameters = listOf(PROVIDER, STRING, path.definingClass, archivePassword.definingClass, "I"),
            returnType = DESCRIPTOR,
            registers = OPEN_WRITER_REGISTERS,
            smali = """
                move-object v0, p0
                move-object v1, p1
                iget-object v2, p2, $path
                iget-object v3, p2, $archiveType
                iget-object v4, p3, $archivePassword
                move v5, p4
                invoke-static/range {v0 .. v5}, $EDITOR->openForWrite($PROVIDER$STRING$STRING$STRING${STRING}I)$DESCRIPTOR
                move-result-object v0
                return-object v0
            """,
        )
        val (_, _, entryIndex, stockReadMode, modeIndexOf) =
            ArchiveEntryModeFingerprint.match(provider.method).instructionMatches
        val mode = modeIndexOf.getInstruction<FiveRegisterInstruction>().registerC
        val scratch = stockReadMode.getInstruction<OneRegisterInstruction>().registerA
        val pathRegister = pathInfo.getInstruction<TwoRegisterInstruction>().registerA
        val metadataRegister = metadata.getInstruction<OneRegisterInstruction>().registerA
        val entry = entryIndex.getInstruction<OneRegisterInstruction>().registerA
        provider.method.addInstructionsWithLabels(
            stockReadMode.index,
            """
                invoke-static {v$mode}, $isWriteMode
                move-result v$scratch
                if-eqz v$scratch, :stock_open
                invoke-static {p0, v$mode, v$pathRegister, v$metadataRegister, v$entry}, $writer
                move-result-object v$scratch
                return-object v$scratch
            """,
            ExternalLabel("stock_open", stockReadMode),
        )

        val view = archiveUriReadGrantFingerprint("addFlags", listOf("L", "L", "J", STRING, "L", BUNDLE)).match()
        val openAs = archiveUriReadGrantFingerprint("setFlags", listOf("L", "L", URI, "J", STRING, BUNDLE))
            .match(view.classDef)
        listOf(view, openAs).forEach { launch ->
            val (_, _, _, readGrant) = launch.instructionMatches
            val intent = readGrant.getInstruction<FiveRegisterInstruction>().registerC
            launch.method.addInstruction(readGrant.index + 1, "invoke-static {v$intent}, $EDITOR->grantWriteAccess($INTENT)V")
        }
    }
}
