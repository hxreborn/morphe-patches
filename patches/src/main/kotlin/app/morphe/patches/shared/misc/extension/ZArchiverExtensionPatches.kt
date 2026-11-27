package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch

internal val zarchiverSavedPasswordsExtensionPatch = patch {
    extendWith("extensions/hxreborn/zarchiver-saved-passwords.mpe")
}

internal val zarchiverEditArchivesExtensionPatch = patch {
    extendWith("extensions/hxreborn/zarchiver-edit-archives.mpe")
}
