package app.morphe.patches.flipboard.home

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.immutable.ImmutableField

private const val STRING = "Ljava/lang/String;"
private const val PREFS = "Landroid/content/SharedPreferences;"
private const val EDITOR = "Landroid/content/SharedPreferences\$Editor;"
private const val LIST = "Ljava/util/List;"
private const val OPTED_OUT = "Lflipboard/model/flapresponse/OptOutsResults;"

internal fun persistExplicitHomeRestores(): String {
    val restore = RestoreDynamicHomeFeedFingerprint.match()
    val api = restore.instructionMatches.single { it.filter === restoreHomeRequest }
        .instruction.getReference<MethodReference>()!!
    val endpoint = "v1/userstate/optoutCarouselFavorite/{uid}"
    val apiMethods = classDefBy(api.definingClass).methods
    check(apiMethods.single { it == api }.let {
        it.annotations.hasValue(endpoint) && it.parameters.single().annotations.hasValue("removeSections")
    })
    val hideApi = apiMethods.single {
        it.parameterTypes == api.parameterTypes && it.returnType == api.returnType &&
            it.annotations.hasValue(endpoint) && it.parameters.single().annotations.hasValue("addSections")
    }
    val hide = hideDynamicHomeFeedFingerprint(hideApi).match()

    val account = HomeAccountPreferencesFingerprint.match()
    val (contextCall, _, uidPrefsRead, resolvePrefs) = account.instructionMatches
    val singleton = account.originalClassDef.staticFields.single { it.type == account.originalClassDef.type }
    val getContext = contextCall.instruction.getReference<MethodReference>()!!
    val uidPrefs = uidPrefsRead.instruction.getReference<FieldReference>()!!
    val getPrefs = resolvePrefs.instruction.getReference<MethodReference>()!!
    val remoteId = classDefBy(FEED_SECTION).instanceFields.single { it.name == "remoteid" && it.type == STRING }

    val callbacks = mutableClassDefBy(HOME_CALLBACK)
    val originalCallback = restore.instructionMatches.single { it.filter === homeFavoritesCallback }
        .instruction.getReference<FieldReference>()!!
    check(hide.instructionMatches.single { it.filter === homeFavoritesCallback }
        .instruction.getReference<FieldReference>() == originalCallback)
    val mode = callbacks.instanceFields.single { it.type == "B" }
    val constructor = callbacks.methods.single { it.name == "<init>" && it.parameterTypes == listOf("B") }
    val success = callbacks.methods.single { it.name == "accept" && it.parameterTypes == listOf("Ljava/lang/Object;") }

    val preferences = callbacks.addStaticMethod(
        name = "morpheHomeRestorePreferences",
        parameters = emptyList(),
        returnType = PREFS,
        registers = 5,
        smali = """
            sget-object v0, $singleton
            invoke-virtual {v0}, $getContext
            move-result-object v0
            sget-object v1, $uidPrefs
            invoke-static {v0, v1}, $getPrefs
            move-result-object v1
            const-string v2, "uid"
            const/4 v3, 0x0
            invoke-interface {v1, v2, v3}, $PREFS->getString($STRING$STRING)$STRING
            move-result-object v1
            if-eqz v1, :no_account
            const-string v2, "0"
            invoke-virtual {v2, v1}, $STRING->equals(Ljava/lang/Object;)Z
            move-result v2
            if-nez v2, :no_account
            invoke-virtual {v1}, $STRING->isEmpty()Z
            move-result v2
            if-nez v2, :no_account
            const-string v2, "morphe_restored_home_"
            invoke-virtual {v2, v1}, $STRING->concat($STRING)$STRING
            move-result-object v2
            const/4 v1, 0x0
            invoke-virtual {v0, v2, v1}, Landroid/content/Context;->getSharedPreferences(${STRING}I)$PREFS
            move-result-object v0
            return-object v0
            :no_account
            return-object v3
        """,
    )

    val prefsField = localField("morpheHomeRestorePreferences", PREFS)
    val keyField = localField("morpheHomeRestoreKey", STRING)
    val hideField = localField("morpheHomeRestoreOptOut", "Z")
    callbacks.instanceFields.addAll(listOf(prefsField, keyField, hideField).map { it.toMutable() })

    val wrapCallback = callbacks.addStaticMethod(
        name = "morpheHomeOptOutCallback",
        parameters = listOf(STRING, "Z"),
        returnType = HOME_CALLBACK,
        registers = 5,
        smali = """
            sget-object v0, $originalCallback
            if-eqz p0, :done
            invoke-static {}, $preferences
            move-result-object v1
            if-eqz v1, :done
            iget-byte v2, v0, $mode
            new-instance v0, $HOME_CALLBACK
            invoke-direct {v0, v2}, $constructor
            iput-object v1, v0, $prefsField
            iput-object p0, v0, $keyField
            iput-boolean p1, v0, $hideField
            :done
            return-object v0
        """,
    )

    val saveChoice = callbacks.addStaticMethod(
        name = "morpheSaveHomeRestore",
        parameters = listOf(HOME_CALLBACK, "Ljava/lang/Object;"),
        returnType = "V",
        registers = 7,
        smali = """
            iget-object v0, p0, $prefsField
            if-eqz v0, :done
            instance-of v1, p1, $FAVORITES_RESPONSE
            if-eqz v1, :done
            check-cast p1, $FAVORITES_RESPONSE
            invoke-virtual {p1}, $FAVORITES_RESPONSE->getResults()$LIST
            move-result-object v1
            if-eqz v1, :done
            iget-boolean v2, p0, $hideField
            if-eqz v2, :check_result
            invoke-virtual {p1}, $FAVORITES_RESPONSE->getOptOut()$OPTED_OUT
            move-result-object v1
            if-eqz v1, :done
            invoke-virtual {v1}, $OPTED_OUT->getFeedSections()$LIST
            move-result-object v1
            if-eqz v1, :done
            :check_result
            iget-object v3, p0, $keyField
            invoke-interface {v1}, $LIST->iterator()Ljava/util/Iterator;
            move-result-object v1
            :next
            invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z
            move-result v4
            if-eqz v4, :done
            invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v4
            check-cast v4, $FEED_SECTION
            iget-object v4, v4, $remoteId
            invoke-virtual {v3, v4}, $STRING->equals(Ljava/lang/Object;)Z
            move-result v4
            if-eqz v4, :next
            invoke-interface {v0}, $PREFS->edit()$EDITOR
            move-result-object v0
            if-eqz v2, :record_restore
            invoke-interface {v0, v3}, $EDITOR->remove($STRING)$EDITOR
            goto :save
            :record_restore
            const/4 v1, 0x1
            invoke-interface {v0, v3, v1}, $EDITOR->putBoolean(${STRING}Z)$EDITOR
            :save
            invoke-interface {v0}, $EDITOR->apply()V
            :done
            return-void
        """,
    )
    success.addInstructions(0, "invoke-static/range {p0 .. p1}, $saveChoice")

    for ((action, optOut) in listOf(restore to false, hide to true)) {
        val key = action.instructionMatches.single { it.filter === homeFeedKey }
        val callback = action.instructionMatches.single { it.filter === homeFavoritesCallback }
        val keyRegister = (key.instruction as OneRegisterInstruction).registerA
        val callbackRegister = (callback.instruction as OneRegisterInstruction).registerA
        val savedKey = action.method.implementation!!.registerCount
        val flag = savedKey + 1
        val (method, prefix) = action.method.addFreeRegisters(2)
        val callbackIndex = callback.index + prefix
        method.replaceInstruction(callbackIndex, "const/16 v$flag, ${if (optOut) 1 else 0}")
        method.addInstructions(
            callbackIndex + 1,
            """
                invoke-static/range {v$savedKey .. v$flag}, $wrapCallback
                move-result-object v$callbackRegister
            """,
        )
        method.addInstructions(key.index + prefix + 1, "move-object/16 v$savedKey, v$keyRegister")
    }

    return callbacks.addStaticMethod(
        name = "morpheWasHomeTabRestored",
        parameters = listOf(FEED_SECTION),
        returnType = "Z",
        registers = 4,
        smali = """
            iget-object v0, p0, $remoteId
            const/4 v2, 0x0
            if-eqz v0, :done
            invoke-static {}, $preferences
            move-result-object v1
            if-eqz v1, :done
            invoke-interface {v1, v0, v2}, $PREFS->getBoolean(${STRING}Z)Z
            move-result v2
            :done
            return v2
        """,
    )
}

private fun Collection<Annotation>.hasValue(value: String) =
    any { annotation -> annotation.elements.any { (it.value as? StringEncodedValue)?.value == value } }

private fun localField(name: String, type: String) =
    ImmutableField(HOME_CALLBACK, name, type, AccessFlags.PRIVATE.value, null, null, null)
