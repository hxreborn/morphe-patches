/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.skipheaders

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.readera.shared.TTS_SKIP_HEADERS_ROW
import app.morphe.patches.readera.shared.bindTtsPreference
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.showTtsPreferenceRow
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val SPEECH_CROP_NATIVE = "pageSpeechCropJni"
private const val SPEECH_CROP_SIGNATURE = "(IIFFFFFF[F)I"
private const val SPEECH_CROP_RECT_METHOD = "speechCropRect"
private const val SPEECH_CROP_PAGE_METHOD = "ingestCroppedPage"
private const val SPEECH_CROPS_FIELD = "speechCrops"
private const val RECT_TYPE = "Landroid/graphics/RectF;"
private const val SPARSE_ARRAY_TYPE = "Landroid/util/SparseArray;"

@Suppress("unused")
val skipHeadersPatch = bytecodePatch(
    name = "Skip headers and footers",
    description = "Adds an option to skip headers, footers and page numbers during text-to-speech. " +
        "Applies to PDF and DjVu documents.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    execute {
        val skipHeaders = bindTtsPreference(TTS_SKIP_HEADERS_ROW)
        showTtsPreferenceRow(TTS_SKIP_HEADERS_ROW)

        mutableClassDefBy(JNI_DOC_CLASS).methods.add(
            ImmutableMethod(
                JNI_DOC_CLASS,
                SPEECH_CROP_NATIVE,
                listOf("I", "I", "F", "F", "F", "F", "F", "F", "[F").map { ImmutableMethodParameter(it, null, null) },
                "I",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value or AccessFlags.NATIVE.value,
                null,
                null,
                null,
            ).toMutable(),
        )

        val controller = SpeechTextIngestFingerprint.classDef
        val ingest = SpeechTextIngestFingerprint.method

        val smartCrop = SmartCropFingerprint.method
        val pageType = smartCrop.parameterTypes[0].toString()
        val sizeType = smartCrop.parameterTypes[2].toString()
        val speechCropRect = ImmutableMethod(
            controller.type,
            SPEECH_CROP_RECT_METHOD,
            smartCrop.parameters,
            RECT_TYPE,
            AccessFlags.PRIVATE.value,
            null,
            null,
            MutableMethodImplementation(smartCrop.implementation!!),
        ).toMutable().apply {
            SmartCropFingerprint.instructionMatches.let { matches ->
                val cropIndex = matches[2].index
                val crop = getInstruction<RegisterRangeInstruction>(cropIndex)
                replaceInstruction(
                    cropIndex,
                    "invoke-static/range { v${crop.startRegister} .. v${crop.startRegister + crop.registerCount - 1} }, " +
                        "$JNI_DOC_CLASS->$SPEECH_CROP_NATIVE$SPEECH_CROP_SIGNATURE",
                )
                val boundsIndex = matches[0].index
                val bounds = getInstruction<FiveRegisterInstruction>(boundsIndex)
                val rectRegister = getInstruction<OneRegisterInstruction>(matches[1].index).registerA
                removeInstruction(matches[1].index)
                replaceInstruction(boundsIndex, "new-instance v$rectRegister, $RECT_TYPE")
                addInstruction(
                    boundsIndex + 1,
                    "invoke-direct { v$rectRegister, v${bounds.registerE}, v${bounds.registerE}, " +
                        "v${bounds.registerD}, v${bounds.registerD} }, $RECT_TYPE-><init>(FFFF)V",
                )
            }
        }
        controller.methods.add(speechCropRect)

        val pageIndexes = SpeechPagesFingerprint.instructionMatches[5].instruction.getReference<FieldReference>()!!
        val pagesHolder = classDefBy(controller.superclass!!).fields.single { field ->
            classDefByOrNull(field.type)?.fields?.any { it.type == "[$pageType" } == true
        }
        val pages = classDefBy(pagesHolder.type).fields.single { it.type == "[$pageType" }
        val sizes: FieldReference
        val sizeOf: MethodReference
        pageSizeLookupFingerprint(controller.type, sizeType).instructionMatches.let {
            sizes = it[0].instruction.getReference<FieldReference>()!!
            sizeOf = it[1].instruction.getReference<MethodReference>()!!
        }

        controller.fields.add(
            ImmutableField(controller.type, SPEECH_CROPS_FIELD, SPARSE_ARRAY_TYPE, AccessFlags.PRIVATE.value, null, null, null)
                .toMutable(),
        )
        val speechCrops = "${controller.type}->$SPEECH_CROPS_FIELD:$SPARSE_ARRAY_TYPE"
        val ingestCroppedPage = ImmutableMethod(
            controller.type,
            SPEECH_CROP_PAGE_METHOD,
            listOf(ImmutableMethodParameter("I", null, null)),
            "V",
            AccessFlags.PRIVATE.value,
            null,
            null,
            MutableMethodImplementation(8),
        ).toMutable().apply {
            addInstructionsWithLabels(
                0,
                """
                    iget-object v0, p0, $pageIndexes
                    aget v0, v0, p1
                    iget-object v1, p0, $sizes
                    invoke-virtual { v1, v0 }, $sizeOf
                    move-result-object v1
                    iget-object v2, p0, $speechCrops
                    if-nez v2, :lookup
                    new-instance v2, $SPARSE_ARRAY_TYPE
                    invoke-direct { v2 }, $SPARSE_ARRAY_TYPE-><init>()V
                    iput-object v2, p0, $speechCrops
                    :lookup
                    invoke-virtual { v2, v0 }, $SPARSE_ARRAY_TYPE->get(I)Ljava/lang/Object;
                    move-result-object v3
                    check-cast v3, $RECT_TYPE
                    if-nez v3, :apply
                    iget-object v3, p0, ${pagesHolder.definingClass}->${pagesHolder.name}:${pagesHolder.type}
                    iget-object v3, v3, ${pages.definingClass}->${pages.name}:${pages.type}
                    aget-object v3, v3, p1
                    invoke-direct { p0, v3, v0, v1 }, $speechCropRect
                    move-result-object v3
                    if-eqz v3, :whole
                    invoke-virtual { v2, v0, v3 }, $SPARSE_ARRAY_TYPE->put(ILjava/lang/Object;)V
                    goto :apply
                    :whole
                    new-instance v3, $RECT_TYPE
                    const/4 v4, 0x0
                    const/high16 v5, 0x3f800000
                    invoke-direct { v3, v4, v4, v5, v5 }, $RECT_TYPE-><init>(FFFF)V
                    :apply
                    invoke-direct { p0, v0, v3 }, $ingest
                    return-void
                """,
            )
        }
        controller.methods.add(ingestCroppedPage)

        SpeechPagesFingerprint.let { match ->
            val firstPage = match.method.getInstruction<OneRegisterInstruction>(match.instructionMatches[1].index).registerA
            val lastPage = match.method.getInstruction<OneRegisterInstruction>(match.instructionMatches[3].index).registerA
            val rectIndex = match.instructionMatches[4].index
            val flagRegister = match.method.getInstruction<OneRegisterInstruction>(rectIndex).registerA
            match.method.addInstructionsWithLabels(
                rectIndex,
                """
                    invoke-static { }, ${skipHeaders.settingsGetter}
                    move-result-object v$flagRegister
                    iget-boolean v$flagRegister, v$flagRegister, ${skipHeaders.flag}
                    if-eqz v$flagRegister, :whole_pages
                    :next_page
                    if-gt v$firstPage, v$lastPage, :done
                    invoke-direct { p0, v$firstPage }, $ingestCroppedPage
                    add-int/lit8 v$firstPage, v$firstPage, 0x1
                    goto :next_page
                    :done
                    return-void
                """,
                ExternalLabel("whole_pages", match.method.getInstruction(rectIndex)),
            )
        }
    }
}
