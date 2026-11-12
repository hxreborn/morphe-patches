/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.speechfilter

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.smali.assembleDex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SPEECH_CLASS = "Lfixture/Speech;"

private val SPEECH_FIXTURE = """
    .class public Lfixture/Speech;
    .super Ljava/lang/Object;
    .field private h:Landroid/content/Context;
    .field private j:Landroid/speech/tts/TextToSpeech;
    .method public B(Ljava/lang/String;Ljava/lang/String;ILjava/util/HashMap;)V
        .registers 6
        const-string v0, "SpeechHelper speek [%s]"
        iget-object p2, p0, Lfixture/Speech;->j:Landroid/speech/tts/TextToSpeech;
        invoke-virtual {p2, p1, p3, p4}, Landroid/speech/tts/TextToSpeech;->speak(Ljava/lang/String;ILjava/util/HashMap;)I
        return-void
    .end method
    .method public F(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)V
        .registers 9
        const/4 v1, 0x2
        const/4 v2, 0x0
        const/4 v3, 0x1
        new-instance p2, Landroid/os/Bundle;
        invoke-direct {p2}, Landroid/os/Bundle;-><init>()V
        iget-object v0, p0, Lfixture/Speech;->j:Landroid/speech/tts/TextToSpeech;
        invoke-virtual {v0, p1, p2, p4, p3}, Landroid/speech/tts/TextToSpeech;->synthesizeToFile(Ljava/lang/CharSequence;Landroid/os/Bundle;Ljava/io/File;Ljava/lang/String;)I
        move-result p2
        invoke-static {p2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
        move-result-object p2
        new-array p3, v1, [Ljava/lang/Object;
        aput-object p1, p3, v2
        aput-object p2, p3, v3
        return-void
    .end method
""".trimIndent()

class SpeechTextFilterCallsTest {
    @Test
    fun filtersSpokenTextWithoutWritingReceiverRegister() {
        val directory = Files.createTempDirectory("readera-speech-filter").toFile()
        try {
            val apk = directory.resolve("fixture.apk")
            ZipOutputStream(apk.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                checkNotNull(javaClass.getResourceAsStream("/metadata/AndroidManifest.axml")).use { it.copyTo(zip) }
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("classes.dex"))
                zip.write(assembleDex(listOf(SPEECH_FIXTURE)))
                zip.closeEntry()
            }
            Patcher(PatcherConfig(apk, directory.resolve("work"))).use { patcher ->
                patcher += setOf(bytecodePatch { execute { addSpeechTextFilter() } })
                runBlocking { patcher().collect { assertNull(it.exception) } }
                val speech = patcher.get().dexFiles.flatMap { output ->
                    output.stream.use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it.buffered()).classes }
                }.single { it.type == SPEECH_CLASS }
                val speechCalls = listOf("B" to "speak", "F" to "synthesizeToFile")
                    .map { (name, speechCall) -> speech.methods.single { it.name == name } to speechCall }
                speechCalls.forEach { (method, _) -> assertReceiverRegisterUnwritten(method) }
                speechCalls.forEach { (method, speechCall) -> assertFilterCallBefore(method, speechCall) }
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun receiverRegister(method: Method) =
        method.implementation!!.registerCount - MethodUtil.getParameterRegisterCount(method)

    private fun assertReceiverRegisterUnwritten(method: Method) {
        val receiver = receiverRegister(method)
        val receiverWrites = method.implementation!!.instructions.filter {
            it.opcode.setsRegister() && it is OneRegisterInstruction && it.registerA == receiver
        }
        assertTrue(receiverWrites.isEmpty(), "${method.name} writes its receiver register: $receiverWrites")
    }

    private fun assertFilterCallBefore(method: Method, speechCall: String) {
        val instructions = method.implementation!!.instructions.toList()
        val receiver = receiverRegister(method)
        val callIndex = instructions.indexOfFirst {
            ((it as? ReferenceInstruction)?.reference as? MethodReference)?.name == speechCall
        }
        val textRegister = (instructions[callIndex] as FiveRegisterInstruction).registerD
        val (filterCall, filterResult) = instructions.subList(callIndex - 2, callIndex)
        assertEquals(Opcode.INVOKE_DIRECT, filterCall.opcode)
        assertEquals("filterSpeechText", ((filterCall as ReferenceInstruction).reference as MethodReference).name)
        assertEquals(receiver, (filterCall as FiveRegisterInstruction).registerC)
        assertEquals(textRegister, filterCall.registerD)
        assertEquals(Opcode.MOVE_RESULT_OBJECT, filterResult.opcode)
        assertEquals(textRegister, (filterResult as OneRegisterInstruction).registerA)
    }
}
