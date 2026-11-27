package app.morphe.patches.videomp3converter.ads

import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.document
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes ads.",
) {
    compatibleWith(AppCompatibilities.VIDEO_MP3_CONVERTER)

    execute {
        AdInitializationFingerprint.method.returnEarly()

        listOf("res/layout/activity_main.xml", "res/layout-land/activity_main.xml").forEach { path ->
            document(path).use { layout ->
                layout.getElementsByTagName("*")
                    .findElementByAttributeValueOrThrow("android:id", "@id/main_ad_container")
                    .setAttribute("android:visibility", "gone")
            }
        }

        document("res/layout/ui_edit_media.xml").use { layout ->
            layout.getElementsByTagName("*")
                .findElementByAttributeValueOrThrow("android:id", "@id/edit_ad_layout")
                .setAttribute("android:visibility", "gone")
        }
    }
}
