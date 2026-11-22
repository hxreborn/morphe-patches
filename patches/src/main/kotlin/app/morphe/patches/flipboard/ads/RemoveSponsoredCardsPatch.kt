package app.morphe.patches.flipboard.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.p0Register
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.AccessFlags.PUBLIC
import com.android.tools.smali.dexlib2.AccessFlags.STATIC
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val removeSponsoredCardsPatch = patch(
    name = "Remove sponsored cards",
    description = "Removes sponsored cards from feeds and banners from the article reader.",
) {
    compatibleWith(AppCompatibilities.FLIPBOARD)

    execute {
        val preference = FeedAdPreferenceFingerprint.match()
        val (_, _, preferenceResult, storeEnabled, readEnabled) = preference.instructionMatches
        preference.method.apply {
            val enabled = preferenceResult.getInstruction<OneRegisterInstruction>().registerA
            val store = storeEnabled.getInstruction<TwoRegisterInstruction>()
            val read = readEnabled.getInstruction<TwoRegisterInstruction>()
            val field = storeEnabled.fieldReference
            check(store.registerA == enabled && store.registerB == read.registerB &&
                field == readEnabled.instruction.getReference<FieldReference>()) {
                "The feed ad preference no longer flows into the ad controller flag"
            }
            check(classDefBy(field.definingClass).instanceFields.any { it == field }) {
                "Missing feed ad controller flag $field"
            }
            replaceInstruction(preferenceResult.index, "const/16 v$enabled, 0x0")
        }
        LoadSectionAdsFingerprint.method.returnEarly()

        val detailBanner = CheckDetailBannerPacingFingerprint.match()
        val (_, pacingResult, pacingBranch) = detailBanner.instructionMatches
        val pacing = (pacingResult.instruction as OneRegisterInstruction).registerA
        check((pacingBranch.instruction as OneRegisterInstruction).registerA == pacing) {
            "Article banner pacing no longer controls the disabled branch"
        }
        detailBanner.method.replaceInstruction(pacingResult.index, "const/16 v$pacing, 0x0")

        val registration = RegisterAmazonAdsFingerprint.match()
        listOf(registerAmazonAds, setAmazonMraidVersions, setAmazonMraidPolicy).forEach { call ->
            val match = registration.instructionMatches.single { it.filter === call }
            registration.method.replaceInstruction(match.index, "nop")
        }
        val googleAds = InitializeGoogleAdsFingerprint.match()
        val (_, initializeAds) = googleAds.instructionMatches
        googleAds.method.replaceInstruction(initializeAds.index, "nop")
        skipAmazonLayoutBid()

        val serverModel = classDefBy(SERVER_ITEM)
        val feedModel = classDefBy(FEED_ITEM)
        check(feedModel.superclass == serverModel.type)
        val sponsored = serverModel.methods.single { it.isPublicGetter("getSponsored", "Z") }
        val flintAd = feedModel.methods.single { it.isPublicGetter("getFlintAd", "Lflipboard/model/Ad;") }
        val adFlags = listOf(
            "isNativeAd",
            "isNativeAdx",
            "isMraidAd",
            "isMraidAdx",
            "isVideoAd",
            "isPromoted",
            "isPromotedAction",
            "isSponsoredByBrand",
        ).map { name -> feedModel.methods.single { it.isPublicGetter(name, "Z") } }
        val checkAdFlags = adFlags.joinToString("\n") { flag ->
            """
                invoke-virtual {p0}, $flag
                move-result v0
                if-nez v0, :sponsored
            """.trimIndent()
        }

        val serverItem = mutableClassDefBy(SERVER_ITEM)
        val isSponsoredItem = serverItem.addStaticMethod(
            name = "morpheIsSponsoredItem",
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "Z",
            registers = 2,
            smali = """
                instance-of v0, p0, $SERVER_ITEM
                if-eqz v0, :organic
                check-cast p0, $SERVER_ITEM
                invoke-virtual {p0}, $sponsored
                move-result v0
                if-nez v0, :sponsored
                instance-of v0, p0, $FEED_ITEM
                if-eqz v0, :organic
                check-cast p0, $FEED_ITEM
                invoke-virtual {p0}, $flintAd
                move-result-object v0
                if-nez v0, :sponsored
                $checkAdFlags
                goto :organic
                :sponsored
                const/4 v0, 0x1
                return v0
                :organic
                const/4 v0, 0x0
                return v0
            """,
        )
        val filterSponsoredItems = serverItem.addStaticMethod(
            name = "morpheFilterSponsoredItems",
            parameters = listOf(ITEM_LIST),
            returnType = ITEM_LIST,
            registers = 5,
            smali = """
                if-eqz p0, :unchanged
                invoke-interface {p0}, $ITEM_LIST->listIterator()Ljava/util/ListIterator;
                move-result-object v0
                const/4 v2, 0x0
                :next_item
                invoke-interface {v0}, Ljava/util/ListIterator;->hasNext()Z
                move-result v3
                if-eqz v3, :done
                invoke-interface {v0}, Ljava/util/ListIterator;->next()Ljava/lang/Object;
                move-result-object v1
                invoke-static {v1}, $isSponsoredItem
                move-result v3
                if-eqz v3, :keep_item
                if-nez v2, :next_item
                invoke-interface {v0}, Ljava/util/ListIterator;->previousIndex()I
                move-result v3
                const/4 v1, 0x0
                invoke-interface {p0, v1, v3}, $ITEM_LIST->subList(II)$ITEM_LIST
                move-result-object v1
                new-instance v2, Ljava/util/ArrayList;
                invoke-direct {v2, v1}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
                goto :next_item
                :keep_item
                if-eqz v2, :next_item
                invoke-virtual {v2, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                goto :next_item
                :done
                if-eqz v2, :unchanged
                return-object v2
                :unchanged
                return-object p0
            """,
        )

        ReplaceSectionItemsFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range {p1 .. p1}, $filterSponsoredItems
                move-result-object p1
            """,
        )
        nestedFeedItemFingerprints.forEach { fingerprint ->
            val match = fingerprint.match()
            val (readItems, returnItems) = match.instructionMatches
            match.method.apply {
                val read = readItems.instruction as TwoRegisterInstruction
                val items = (returnItems.instruction as OneRegisterInstruction).registerA
                check(implementation!!.instructions.count() == 2 && read.registerA == items &&
                    read.registerB == p0Register) { "Unexpected item getter body: $this" }
                addInstructions(
                    returnItems.index,
                    """
                        invoke-static/range {v$items .. v$items}, $filterSponsoredItems
                        move-result-object v$items
                    """,
                )
            }
        }
        skipEmptyRoundupHeader()
    }
}

private fun skipEmptyRoundupHeader() {
    val binding = BindRoundupAdapterFingerprint.match()
    val (_, createAdapter) = binding.instructionMatches
    val adapter = createAdapter.methodReference.definingClass
    val count = roundupAdapterCountFingerprint(adapter).match()
    val (readItems, countItems, itemCount, countHeader, returnCount) = count.instructionMatches
    val itemsRegister = (readItems.instruction as TwoRegisterInstruction).registerA
    val countRegister = (itemCount.instruction as OneRegisterInstruction).registerA
    val addHeader = countHeader.instruction as TwoRegisterInstruction
    check((countItems.instruction as FiveRegisterInstruction).registerC == itemsRegister &&
        (countHeader.instruction as WideLiteralInstruction).wideLiteral == 1L &&
        addHeader.registerA == countRegister && addHeader.registerB == countRegister &&
        (returnCount.instruction as OneRegisterInstruction).registerA == countRegister) {
        "Unexpected header count in the roundup adapter"
    }
    count.method.addInstructionsWithLabels(
        countHeader.index,
        "if-eqz v$countRegister, :return_count",
        ExternalLabel("return_count", count.method.getInstruction(returnCount.index)),
    )
}

private fun skipAmazonLayoutBid() {
    val failure = CompleteAmazonLayoutBidFailureFingerprint.match()
    val (_, emptyBid, emitBid, completeBid) = failure.instructionMatches
    val emptyBidField = emptyBid.fieldReference
    val emitBidMethod = emitBid.methodReference
    val completeBidMethod = completeBid.methodReference
    val layout = LoadAmazonLayoutBidFingerprint.match()
    val (_, createCallback, loadAd) = layout.instructionMatches
    val callbackInit = createCallback.methodReference
    val callbackCall = createCallback.instruction as FiveRegisterInstruction
    val request = loadAd.instruction as FiveRegisterInstruction
    val emitterType = layout.method.parameterTypes.single()
    val (readEmitter, castEmitter) = failure.originalMethod.implementation!!.instructions.toList()
    val capturedEmitter = readEmitter.getReference<FieldReference>()!!
    val capture = readEmitter as TwoRegisterInstruction
    val emitter = (castEmitter as OneRegisterInstruction).registerA
    val mapRegister = (emptyBid.instruction as OneRegisterInstruction).registerA
    val emit = emitBid.instruction as FiveRegisterInstruction
    val complete = completeBid.instruction as FiveRegisterInstruction
    check(callbackInit.definingClass == failure.classDef.type &&
        callbackCall.registerC == request.registerD &&
        callbackCall.registerD == layout.method.p0Register + 1) {
        "Amazon layout requests no longer pass the emitter to the failure callback"
    }
    check(readEmitter.opcode == Opcode.IGET_OBJECT && capturedEmitter.type == "Ljava/lang/Object;" &&
        failure.classDef.instanceFields.any { it == capturedEmitter } &&
        capture.registerB == failure.originalMethod.p0Register && capture.registerA == emitter &&
        castEmitter.opcode == Opcode.CHECK_CAST && castEmitter.getReference<TypeReference>()?.type == emitterType) {
        "The Amazon layout failure callback no longer reads its captured emitter"
    }
    check(emit.registerC == emitter && emit.registerD == mapRegister && complete.registerC == emitter &&
        emitBidMethod.definingClass == emitterType && completeBidMethod.definingClass == emitterType) {
        "The Amazon layout callback no longer emits its empty map on the request emitter"
    }
    val emptyMapClass = classDefBy(emptyBidField.type)
    check("Ljava/util/Map;" in emptyMapClass.interfaces &&
        classDefBy(emptyBidField.definingClass).staticFields.any { it == emptyBidField && PUBLIC.isSet(it.accessFlags) }) {
        "Missing accessible empty map field $emptyBidField"
    }
    val emitterClass = classDefBy(emitBidMethod.definingClass)
    listOf(emitBidMethod, completeBidMethod).forEach { reference ->
        check(emitterClass.methods.any { it == reference && PUBLIC.isSet(it.accessFlags) && !STATIC.isSet(it.accessFlags) }) {
            "Missing public emitter method $reference"
        }
    }
    layout.method.addInstructions(
        0,
        """
            move-object/from16 v0, p1
            sget-object v1, $emptyBidField
            invoke-virtual {v0, v1}, $emitBidMethod
            invoke-virtual {v0}, $completeBidMethod
            return-void
        """,
    )
}

private fun Method.isPublicGetter(name: String, type: String) =
    this.name == name && parameterTypes.isEmpty() && returnType == type &&
        PUBLIC.isSet(accessFlags) && !STATIC.isSet(accessFlags)
