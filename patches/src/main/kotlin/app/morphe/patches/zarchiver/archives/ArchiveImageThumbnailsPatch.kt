package app.morphe.patches.zarchiver.archives

import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow

private const val ARCHIVE_THUMBNAILS_KEY = "iFMShowArchiveThumb"

@Suppress("unused")
val archiveImageThumbnailsPatch = patch(
    name = "Archive image thumbnails",
    description = "Enables the option to show image thumbnails inside archives.",
) {
    compatibleWith(AppCompatibilities.ZARCHIVER)

    execute {
        for ((directory, preferenceTag) in listOf("xml" to "CheckBoxPreference", "xml-v21" to "SwitchPreference")) {
            document("res/$directory/pref_fm.xml").use { document ->
                val preference = document.getElementsByTagName(preferenceTag)
                    .findElementByAttributeValueOrThrow("android:key", ARCHIVE_THUMBNAILS_KEY)
                preference.removeAttribute("android:enabled")
                preference.setAttribute("android:summary", "@string/OPT_ARCHIVE_IMG_THUMB_SETUP")
            }
        }
    }
}
