/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.exiftool.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.Opcode

internal const val GOOGLE_LAT_LNG_CLASS = "Lcom/google/android/gms/maps/model/LatLng;"

internal object MapActivityCreateFingerprint : Fingerprint(
    definingClass = "Lcom/exiftool/free/ui/map/MapActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        methodCall(name = "setOnClickListener"),
        opcode(Opcode.RETURN_VOID, InstructionLocation.MatchAfterImmediately()),
    ),
)

internal object LatLngCoordinatesFingerprint : Fingerprint(
    definingClass = GOOGLE_LAT_LNG_CLASS,
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("lat/lng: ("),
    filters = listOf(
        fieldAccess(definingClass = GOOGLE_LAT_LNG_CLASS, type = "D", opcode = Opcode.IGET_WIDE),
        fieldAccess(definingClass = GOOGLE_LAT_LNG_CLASS, type = "D", opcode = Opcode.IGET_WIDE),
    ),
)

internal fun photoLocationFingerprint(photoType: String) = Fingerprint(
    filters = listOf(fieldAccess(definingClass = photoType, type = GOOGLE_LAT_LNG_CLASS, opcode = Opcode.IGET_OBJECT)),
)

internal fun photoListFingerprint(fragmentType: String) = Fingerprint(
    filters = listOf(fieldAccess(definingClass = fragmentType, type = "Ljava/util/List;", opcode = Opcode.IPUT_OBJECT)),
)

internal object PhotoMapFragmentFingerprint : Fingerprint(
    returnType = "Landroid/view/View;",
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;"),
    filters = listOf(resourceLiteral(ResourceType.LAYOUT, "fragment_photo_map")),
)

internal object PhotoMapListFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I", "Ljava/util/ArrayList;"),
    filters = listOf(resourceLiteral(ResourceType.STRING, "photo_map_loading_progress")),
)

internal object PhotoMapOpenFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("photo_map_item_open"),
    custom = { method, _ -> method.parameterTypes.size == 1 },
)

internal object PhotoMapRefreshFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/List;"),
)

internal object PhotoMapResumeFingerprint : Fingerprint(
    name = "onResume",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(opcode(Opcode.RETURN_VOID, InstructionLocation.MatchAfterLast())),
)
