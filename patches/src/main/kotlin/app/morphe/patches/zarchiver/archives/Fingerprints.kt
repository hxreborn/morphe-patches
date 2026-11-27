package app.morphe.patches.zarchiver.archives

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val STRING = "Ljava/lang/String;"
internal const val URI = "Landroid/net/Uri;"
internal const val DESCRIPTOR = "Landroid/os/ParcelFileDescriptor;"
internal const val INTENT = "Landroid/content/Intent;"
private const val MISSING_ENTRY_INDEX = -1L
private const val READ_MODE_CHARACTER = 'r'.code.toLong()

internal object ArchiveEntryProviderFingerprint : Fingerprint(
    name = "openFile",
    parameters = listOf(URI, STRING),
    returnType = DESCRIPTOR,
    filters = listOf(
        methodCall(definingClass = "this", parameters = listOf(URI), returnType = "L"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        string("ZArchiver: Edit file is supported only in Donation/Pro version"),
    ),
)

internal object ArchiveEntryModeFingerprint : Fingerprint(
    filters = listOf(
        literal(MISSING_ENTRY_INDEX),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("I", STRING), returnType = "I", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        literal(READ_MODE_CHARACTER),
        methodCall(definingClass = STRING, name = "indexOf", parameters = listOf("I"), location = MatchAfterImmediately()),
    ),
)

private fun stringFieldRead() = fieldAccess(
    opcode = Opcode.IGET_OBJECT,
    type = STRING,
    location = MatchAfterImmediately(),
)

internal object ArchiveReadArgumentsFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = STRING),
        stringFieldRead(),
        stringFieldRead(),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(STRING, STRING, STRING),
            returnType = "L",
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object ArchiveEntryStreamFingerprint : Fingerprint(
    isStatic = false,
    parameters = listOf("I", STRING),
    returnType = "L",
    filters = listOf(methodCall(name = "cGetStream")),
)

internal object ArchiveNativeHandleFingerprint : Fingerprint(
    isStatic = false,
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_WIDE, definingClass = "this", type = "J"),
        methodCall(name = "cSetComment", location = MatchAfterImmediately()),
    ),
)

internal fun archiveUriReadGrantFingerprint(grant: String, parameters: List<String>) = Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = parameters,
    filters = listOf(
        string("AbsolutePath"),
        string("file"),
        methodCall(definingClass = URI, name = "getScheme", location = MatchAfterImmediately()),
        methodCall(definingClass = INTENT, name = grant, parameters = listOf("I")),
    ),
)
