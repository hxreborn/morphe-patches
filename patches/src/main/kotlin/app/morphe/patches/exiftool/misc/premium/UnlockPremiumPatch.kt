/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.exiftool.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.exiftoolMapExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import app.morphe.patches.shared.misc.smali.replaceStub

private const val MAP_PICKER_CLASS = "Lapp/hxreborn/extension/exiftool/MapPickerScreen;"
private const val LAT_LNG_ITEM_CLASS = "Lcom/exiftool/free/model/LatLngItem;"
private const val PHOTO_MAP_CLASS = "Lapp/hxreborn/extension/exiftool/PhotoMapScreen;"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium editing and watermark features.",
) {
    compatibleWith(AppCompatibilities.EXIFTOOL)
    dependsOn(sharedRevenuecatExtensionPatch, exiftoolMapExtensionPatch, removePairipProtectionPatch)

    execute {
        grantRevenueCatEntitlements("premium" to "premium")

        val mapActivityCreate = MapActivityCreateFingerprint.match()
        mapActivityCreate.method.addInstructions(
            mapActivityCreate.instructionMatches.last().index,
            "invoke-static/range {p0 .. p0}, $MAP_PICKER_CLASS->show(Landroid/app/Activity;)V",
        )

        mutableClassDefBy(MAP_PICKER_CLASS).replaceStub("save", 9, """
            new-instance v0, $LAT_LNG_ITEM_CLASS
            invoke-direct {v0, p1, p2, p3, p4}, $LAT_LNG_ITEM_CLASS-><init>(DD)V
            new-instance v1, Landroid/os/Bundle;
            invoke-direct {v1}, Landroid/os/Bundle;-><init>()V
            const-string v2, "BUNDLE_LATLNG"
            invoke-virtual {v1, v2, v0}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V
            const-string v2, "BUNDLE_UPDATE_OTHERS"
            const/4 v0, 0x1
            invoke-virtual {v1, v2, v0}, Landroid/os/Bundle;->putBoolean(Ljava/lang/String;Z)V
            new-instance v0, Landroid/content/Intent;
            invoke-direct {v0}, Landroid/content/Intent;-><init>()V
            const-string v2, "BUNDLE_RESULT"
            invoke-virtual {v0, v2, v1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Landroid/os/Bundle;)Landroid/content/Intent;
            const/4 v1, -0x1
            invoke-virtual {p0, v1, v0}, Landroid/app/Activity;->setResult(ILandroid/content/Intent;)V
            invoke-virtual {p0}, Landroid/app/Activity;->finish()V
            return-void
        """)

        val photoMapFragment = PhotoMapFragmentFingerprint.match().classDef
        val photoMapList = PhotoMapListFingerprint.match(photoMapFragment).method
        val photoMapOpen = PhotoMapOpenFingerprint.match(photoMapFragment).method
        val photoType = photoMapOpen.parameterTypes.single().toString()
        val photoLocationField = photoLocationFingerprint(photoType)
            .match(photoMapOpen, photoMapFragment).instructionMatches.first().fieldReference.name
        val (latitudeField, longitudeField) = LatLngCoordinatesFingerprint.instructionMatches.map { it.fieldReference.name }
        val photoListField = photoListFingerprint(photoMapFragment.type)
            .match(photoMapList, photoMapFragment).instructionMatches.first().fieldReference.name

        val photoMapResume = PhotoMapResumeFingerprint.match(photoMapFragment)
        photoMapResume.method.addInstructions(
            photoMapResume.instructionMatches.first().index,
            """
                invoke-virtual {p0}, Landroidx/fragment/app/m;->getView()Landroid/view/View;
                move-result-object v0
                const v1, ${resourceId(ResourceType.ID, "frMapContainer")}
                invoke-static {p0, v0, v1}, $PHOTO_MAP_CLASS->attach(Ljava/lang/Object;Landroid/view/View;I)V
                invoke-static {p0}, $PHOTO_MAP_CLASS->update(Ljava/lang/Object;)V
            """,
        )

        PhotoMapRefreshFingerprint.match(photoMapFragment).method
            .addInstructions(0, "invoke-static {p0}, $PHOTO_MAP_CLASS->update(Ljava/lang/Object;)V")

        mutableClassDefBy(PHOTO_MAP_CLASS).apply {
            replaceStub("update", 9, """
                invoke-static {p0}, $PHOTO_MAP_CLASS->clear(Ljava/lang/Object;)V
                move-object v0, p0
                check-cast v0, ${photoMapFragment.type}
                iget-object v0, v0, ${photoMapFragment.type}->$photoListField:Ljava/util/List;
                if-eqz v0, :photo_done
                invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                move-result-object v1
                :photo_loop
                invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z
                move-result v2
                if-eqz v2, :photo_done
                invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v3
                check-cast v3, $photoType
                iget-object v2, v3, $photoType->$photoLocationField:$GOOGLE_LAT_LNG_CLASS
                if-eqz v2, :photo_loop
                iget-wide v4, v2, $GOOGLE_LAT_LNG_CLASS->$latitudeField:D
                iget-wide v6, v2, $GOOGLE_LAT_LNG_CLASS->$longitudeField:D
                move-object v2, p0
                invoke-static/range {v2 .. v7}, $PHOTO_MAP_CLASS->addPhoto(Ljava/lang/Object;Ljava/lang/Object;DD)V
                goto :photo_loop
                :photo_done
                invoke-static {p0}, $PHOTO_MAP_CLASS->finish(Ljava/lang/Object;)V
                return-void
            """)
            replaceStub("open", 2, """
                check-cast p0, ${photoMapFragment.type}
                check-cast p1, $photoType
                invoke-virtual {p0, p1}, ${photoMapFragment.type}->${photoMapOpen.name}($photoType)V
                return-void
            """)
        }
    }
}
